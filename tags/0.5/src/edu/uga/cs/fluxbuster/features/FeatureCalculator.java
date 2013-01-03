/*
* Copyright (C) 2012 Chris Neasbitt
* Author: Chris Neasbitt
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 2 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package edu.uga.cs.fluxbuster.features;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.Days;

import edu.uga.cs.fluxbuster.db.DBInterface;
import edu.uga.cs.fluxbuster.db.DBInterfaceFactory;
import edu.uga.cs.fluxbuster.utils.PropertiesUtils;

/**
 * This class calculates longitudinal features of each cluster and
 * stores them in the database.
 * 
 * @author Chris Neasbitt
 */
public class FeatureCalculator {

	private DBInterface dbi = null;
	
	private Properties properties = null;
	
	private SimpleDateFormat df = null;
	
	private ArrayList<Date> prevDateBuf = null;
	private Date prevDateBufDate = null;
	private int prevDateBufWindow = 0;
		
	private static final String TABLES_QUERY1KEY = "TABLES_QUERY1";
	
	private static final String DOMAINSPREFIXKEY = "DOMAINS_TABLE_PREFIX";
	
	private static final String RESIPSPREFIXKEY = "RESIPS_TABLE_PREFIX";
	
	private static final String NOVELTY_QUERY1_1KEY = "NOVELTY_QUERY1_PART1";
	
	private static final String NOVELTY_QUERY1_2KEY = "NOVELTY_QUERY1_PART2";
	
	private static final String NOVELTY_QUERY1_3KEY = "NOVELTY_QUERY1_PART3";
	
	private static final String NOVELTY_QUERY2KEY = "NOVELTY_QUERY2";
	
	private static final String NOVELTY_WINDOWSKEY = "NOVELTY_WINDOWS";
	
	private static final String NOVELTY_WINFIELDSKEY = "NOVELTY_WINDOW_FIELDS";
	
	private static final String NOVELTY_QUERY3KEY = "NOVELTY_QUERY3";
	
	private static final String PREVCLUSTER_QUERY1KEY = "PREVCLUSTER_QUERY1";
	
	private static final String PREVCLUSTER_QUERY2KEY = "PREVCLUSTER_QUERY2";
	
	private static final String PREVCLUSTER_QUERY3KEY = "PREVCLUSTER_QUERY3";
	
	private static final String PREVCLUSTER_QUERY4KEY = "PREVCLUSTER_QUERY4";
	
	private static final String PREVCLUSTER_WINDOWKEY = "PREVCLUSTER_WINDOW";
	
	private static final String DOMAINSPERNETWORK_WINDOWKEY = "DOMAINSPERNETWORK_WINDOW";
	
	private static final String DOMAINSPERNETWORK_QUERY1KEY = "DOMAINSPERNETWORK_QUERY1";
	
	private static final String DOMAINSPERNETWORK_QUERY2KEY = "DOMAINSPERNETWORK_QUERY2";
	
	private static final String DOMAINSPERNETWORK_QUERY3KEY = "DOMAINSPERNETWORK_QUERY3";
	
	private static Log log = LogFactory.getLog(FeatureCalculator.class);

	/**
	 * Instantiates a new feature calculator.
	 *
	 * @throws IOException if the FeatureCalculator.properties file
	 * 		can not be read
	 */
	public FeatureCalculator() throws IOException {
		this(DBInterfaceFactory.loadDBInterface());
	}
	
	/**
	 * Instantiates a new feature calculator with a specific database
	 * interface.
	 *
	 * @param dbi the database interface
	 * @throws IOException if the FeatureCalculator.properties file
	 * 		can not be read
	 */
	public FeatureCalculator(DBInterface dbi) throws IOException {
		this.dbi = dbi;
		properties = PropertiesUtils.loadProperties(this.getClass());
		df = new SimpleDateFormat("yyyyMMdd");
	}
	
	/**
	 * Calculates the domains per network feature for each cluster generated
	 * on a specific run date.
	 * 
	 * @param log_date the run date
	 * @param window the number of days previous to use in feature calculation
	 * @return a table of values where the keys are cluster ids and the values 
	 * 		are the feature values
	 * @throws SQLException if there is an error calculating the feature values
	 */
	
	public Map<Integer, Double> calculateDomainsPerNetwork(Date log_date,
			int window) throws SQLException{
		HashMap<Integer, Double> retval = new HashMap<Integer, Double>();
		ArrayList<Date> prevDates = getPrevDates(log_date, window);
		if (prevDates.size() > 0) {
			String logDateStr = df.format(log_date);
			StringBuffer add_query = new StringBuffer();
			Formatter formatter = new Formatter(add_query);
			
			for(Date prevDate : prevDates){
				String prevDateStr = df.format(prevDate);
				formatter.format(" " + properties.getProperty(DOMAINSPERNETWORK_QUERY1KEY) + " ", 
						logDateStr, prevDateStr, prevDateStr);
			}
			formatter.close();
			
			StringBuffer querybuf = new StringBuffer();
			formatter = new Formatter(querybuf);
			formatter.format(properties.getProperty(DOMAINSPERNETWORK_QUERY2KEY), 
					logDateStr, logDateStr, logDateStr,add_query.toString());
			ResultSet rs = null;
			try{
				rs = dbi.executeQueryWithResult(querybuf.toString());		
				while(rs.next()){
					retval.put(rs.getInt(1), rs.getDouble(2));
				}
			} catch (Exception e) {
				if(log.isErrorEnabled()){
					log.error(e);
				}
			} finally {
				if(rs != null && !rs.isClosed()){
					rs.close();
				}
				formatter.close();
			}
		}
		return retval;
	}
	
	/**
	 * Calculates the cluster novelty feature for each cluster generated
	 * on a specific run date.
	 *
	 * @param log_date the run date
	 * @param window the number of days previous to use in feature calculation
	 * @return a table of values where the keys are cluster ids and the values 
	 * 		are the feature values
	 * @throws SQLException if there is an error calculating the feature values
	 */
	public Map<Integer, Double> calculateNoveltyFeature(Date log_date,
			int window) throws SQLException {
		HashMap<Integer, Double> retval = new HashMap<Integer, Double>();
		ArrayList<Date> prevDates = getPrevDates(log_date, window);

		if (prevDates.size() > 0) {
			StringBuffer querybuf = new StringBuffer();
			Formatter formatter = new Formatter(querybuf);
			String curdatestr = df.format(log_date);
			formatter.format(properties.getProperty(NOVELTY_QUERY1_1KEY),
					curdatestr, curdatestr, curdatestr, curdatestr);
			for (Date prevDate : prevDates) {
				formatter
						.format(" "
								+ properties.getProperty(NOVELTY_QUERY1_2KEY)
								+ " ", df.format(prevDate));
			}
			formatter.format(properties.getProperty(NOVELTY_QUERY1_3KEY),
					curdatestr, curdatestr);

			ResultSet rs2 = null;
			Hashtable<Integer, Hashtable<String, Long>> new_resolved_ips 
				= new Hashtable<Integer, Hashtable<String, Long>>();
			try{
				rs2 = dbi.executeQueryWithResult(querybuf.toString());
				while (rs2.next()) {
					int cluster_id = rs2.getInt(2);
					if (!new_resolved_ips.containsKey(cluster_id)) {
						new_resolved_ips.put(cluster_id,
								new Hashtable<String, Long>());
					}
					String secondLevelDomainName = rs2.getString(1);
					long newips = rs2.getLong(3);
					Hashtable<String, Long> clustertable = new_resolved_ips
							.get(cluster_id);
					clustertable.put(secondLevelDomainName, newips);
				}
			} catch(Exception e) {
				if(log.isErrorEnabled()){
					log.error(e);
				}
			} finally{
				if(rs2 != null && !rs2.isClosed()){
					rs2.close();
				}
				formatter.close();
			}
			

			Hashtable<String, List<Integer>> numDays = new Hashtable<String, List<Integer>>();
			for (Date prevDate : prevDates) {
				String prevDateStr = df.format(prevDate);
				querybuf = new StringBuffer();
				formatter = new Formatter(querybuf);
				formatter.format(properties.getProperty(NOVELTY_QUERY2KEY),
						curdatestr, prevDateStr, curdatestr, prevDateStr);
				ResultSet rs3 = null;
				try{
					rs3 = dbi.executeQueryWithResult(querybuf.toString());
					while (rs3.next()) {
						String sldn = rs3.getString(1);
						if (!numDays.containsKey(sldn)) {
							numDays.put(sldn, new ArrayList<Integer>());
						}
						Date pd = rs3.getDate(2);
						DateTime start = new DateTime(pd.getTime());
						DateTime end = new DateTime(log_date.getTime());
						Days d = Days.daysBetween(start, end);
						int diffDays = d.getDays();
						numDays.get(sldn).add(diffDays);
					}
				} catch (Exception e){
					if(log.isErrorEnabled()){
						log.error(e);
					}
				} finally {
					if(rs3 != null && !rs3.isClosed()){
						rs3.close();
					}
					formatter.close();
				}
			}

			Hashtable<Integer, List<Float>> clusterValues = new Hashtable<Integer, List<Float>>();
			for (int clusterID : new_resolved_ips.keySet()) {
				clusterValues.put(clusterID, new ArrayList<Float>());
				
				Hashtable<String, Long> sldnValues = new_resolved_ips.get(clusterID);
				for(String sldn : sldnValues.keySet()){
					if(numDays.keySet().contains(sldn)){
						long newIPCount = sldnValues.get(sldn);
						float f = ((float)newIPCount)/Collections.max(numDays.get(sldn));
						clusterValues.get(clusterID).add(f);
						
					}
				}
			}
			
			for(int clusterID : clusterValues.keySet()){
				if(clusterValues.get(clusterID) == null){  //I dont think it is possible for this to ever be true
					retval.put(clusterID, null);
				} else {
					double sum = 0;
					for(double d : clusterValues.get(clusterID)){
						sum += d;
					}
					double val = 0;
					if(clusterValues.get(clusterID).size() > 0){
						val = sum/clusterValues.get(clusterID).size();
					}
					retval.put(clusterID, val);
				}
			}
		}
		return retval;
	}
	
	/**
	 * Calculates the previous cluster ratio feature for each cluster generated
	 * on a specific run date and within the a specific window
	 *
	 * @param log_date the run date
	 * @param window the number of days previous to use in feature calculation
	 * @return a table of results, the keys of the table are cluster ids and the
	 * 		values are lists of two elements.  The first element is the 
	 * 		last_growth_ratio_prev_clusters value and the second element is the
	 * 		last_growth_prefix_ratio_prev_clusters value
	 * @throws SQLException if there is and error calculating the feature
	 */
	public Hashtable<Integer, List<Double>> calculatePrevClusterRatios
		(Date log_date, int window) throws SQLException{
		Hashtable<Integer, List<Double>> retval = new Hashtable<Integer, List<Double>>();
		
		ArrayList<Date> prevDates = getPrevDates(log_date, window);
		String query1 = properties.getProperty(PREVCLUSTER_QUERY1KEY);
		String query2 = properties.getProperty(PREVCLUSTER_QUERY2KEY);
		String logDateStr = df.format(log_date);
		String completequery = new String();
		
		StringBuffer addQueryBuff = new StringBuffer();
		for(int i = 0; i < prevDates.size(); i++){
			String prevDateStr = df.format(prevDates.get(i));
			StringBuffer querybuf = new StringBuffer();
			Formatter formatter = new Formatter(querybuf);
			formatter.format(query1, logDateStr, logDateStr, 
					prevDateStr, prevDateStr, prevDateStr);
			addQueryBuff.append(querybuf.toString());
			if(i < prevDates.size() - 1){
				addQueryBuff.append(" UNION ");
			}
			formatter.close();
		}
		
		if(addQueryBuff.length() > 0){
			StringBuffer querybuf = new StringBuffer();
			Formatter formatter = new Formatter(querybuf);
			formatter.format(query2, logDateStr, logDateStr, 
					addQueryBuff.toString());
			completequery = querybuf.toString();
			formatter.close();
		}
		
		if(completequery.length() > 0){
			ResultSet rs = null;
			try{
				rs = dbi.executeQueryWithResult(completequery);
				while(rs.next()){
					ArrayList<Double> temp = new ArrayList<Double>();
					temp.add(rs.getDouble(3));
					temp.add(rs.getDouble(4));
					retval.put(rs.getInt(1), temp);
				}
			} catch (Exception e){
				if(log.isErrorEnabled()){
					log.error(e);
				}
			} finally {
				if(rs != null && !rs.isClosed()){
					rs.close();
				}
			}
			Hashtable<Integer, Double> queryPerDomain = getQueriesPerDomain(log_date);
			for(Integer clusterid : retval.keySet()){
				List<Double> values = retval.get(clusterid);
				values.set(0, values.get(0)/queryPerDomain.get(clusterid));
				values.set(1, values.get(1)/queryPerDomain.get(clusterid));
			}
		}
		
		return retval;
	}
	
	/**
	 * Gets run dates previous to a specific date within a window
	 * of days from that date.
	 *
	 * @param log_date the run date
	 * @param window the number of days previous to the current date
	 * @return the list of previous run dates
	 * @throws SQLException if there is an error retrieving the previous
	 * 		run dates
	 */
	public ArrayList<Date> getPrevDates(Date log_date, int window) throws SQLException{
		ArrayList<Date> prevDates = new ArrayList<Date>();
		if(prevDateBufDate != null && prevDateBuf != null && prevDateBufDate.equals(log_date) 
				&& prevDateBufWindow >= window){
			
			//pull the dates within the day window from the prevDateBuf cache
			Date pd = null;
			int windowcount = 0;
			for(Date d : prevDateBuf){
				if(windowcount >= window){
					break;
				}
				if(pd == null){
					pd = d;
					windowcount++;
				} else {
					DateTime morerecent = new DateTime(d.getTime());
					DateTime lessrecent = new DateTime(pd.getTime());
					Days days = Days.daysBetween(morerecent, lessrecent);
					windowcount += days.getDays();
					pd = d;
				}
				prevDates.add(d);
			}
			
		} else {			
			String domainsprefix = properties.getProperty(DOMAINSPREFIXKEY);
			String resipsprefix = properties.getProperty(RESIPSPREFIXKEY);
			
			ArrayList<String> tablenames = new ArrayList<String>();
			ResultSet rs1 = null;
			try{
				rs1 = dbi.executeQueryWithResult(properties
						.getProperty(TABLES_QUERY1KEY));
				while (rs1.next()) {
					tablenames.add(rs1.getString(1));
				}
			} catch(Exception e){
				if(log.isErrorEnabled()){
					log.error(e);
				}
			} finally {
				if(rs1 != null && !rs1.isClosed()){
					rs1.close();
				}
			}
	
			GregorianCalendar cal = new GregorianCalendar();
			cal.setTime(log_date);
			for (int i = 0; i < window; i++) {
				cal.roll(Calendar.DAY_OF_YEAR, false);
				Date temp = cal.getTime();
				String datestr = df.format(temp);
				if (tablenames.contains(domainsprefix + "_" + datestr)
						&& tablenames.contains(resipsprefix + "_" + datestr)) {
					prevDates.add(temp);
				}
			}
			
			//cache the values for later
			if(prevDateBuf == null){
				prevDateBuf = new ArrayList<Date>();
			} else {
				prevDateBuf.clear();
			}
			prevDateBuf.addAll(prevDates);
			prevDateBufDate = log_date;
			prevDateBufWindow = window;
		}
		return prevDates;
	}
	
	
	/**
	 * Retrieves the number of dns queries per domain for each cluster
	 * generated on a specific run date.
	 *
	 * @param log_date the run date
	 * @return a table of values where the keys are cluster ids and the values 
	 * 		are the queries per domain value
	 * @throws SQLException if there is an error retrieving the queries
	 * 		per domain values
	 */
	private Hashtable<Integer, Double> getQueriesPerDomain(Date log_date) throws SQLException{
		Hashtable<Integer, Double> retval = new Hashtable<Integer, Double>();
		StringBuffer querybuf = new StringBuffer();
		Formatter formatter = new Formatter(querybuf);
		formatter.format(properties.getProperty(PREVCLUSTER_QUERY3KEY), df.format(log_date));
		ResultSet rs = null;
		try{
			rs = dbi.executeQueryWithResult(querybuf.toString());
			while(rs.next()){
				retval.put(rs.getInt(1), rs.getDouble(2));
			}
		} catch(Exception e) {
			if(log.isErrorEnabled()){
				log.error(e);
			}
		} finally {
			if(rs != null && !rs.isClosed()){
				rs.close();
			}
			formatter.close();
		}
		return retval;
	}
	
	/**
	 * Calculates the domains per network feature for each cluster generated
	 * on a specific run date and stores them in the database.
	 * 
	 * @param log_date the run date
	 * @throws Exception if there is an error calculating or storing the 
	 * 		feature values
	 */
	public void updateDomainsPerNetwork(Date log_date) throws Exception{
		Map<Integer, Double> dpn = 
				this.calculateDomainsPerNetwork(log_date, 
						Integer.parseInt(properties.getProperty(DOMAINSPERNETWORK_WINDOWKEY)));
		for(int clusterid : dpn.keySet()){
			StringBuffer querybuf = new StringBuffer();
			Formatter formatter = new Formatter(querybuf);
			formatter.format(properties.getProperty(DOMAINSPERNETWORK_QUERY3KEY), 
					df.format(log_date), dpn.get(clusterid).toString(), String.valueOf(clusterid));
			dbi.executeQueryNoResult(querybuf.toString());
			formatter.close();
		}
	}
	
	/**
	 * Updates each cluster's longitudinal features for all clusters
	 * generated during a specific run date.
	 *
	 * @param log_date the run date
	 * @throws Exception if unable to calculate or store the longitudinal
	 * 		feature values
	 */
	public void updateFeatures(Date log_date) throws Exception{
		String simplename = null;
		if(log.isInfoEnabled()){
			simplename = this.getClass().getSimpleName();
			log.info(simplename + " Started: " 
					+ Calendar.getInstance().getTime());
			log.info("Updating novelty features.");
		}
		dbi.initClusterTables(log_date);
		updateNoveltyFeature(log_date);
		if(log.isInfoEnabled()){
			log.info("Novelty features updated.");
			log.info("Updating previous cluster ratio features.");
		}
		updatePrevClusterRatios(log_date);
		if(log.isInfoEnabled()){
			log.info("Previous cluster ratio features updated.");
			log.info("Updating domains per network feature.");
		}
		updateDomainsPerNetwork(log_date);
		if(log.isInfoEnabled()){
			log.info("Domains per network feature updated.");
			log.info(simplename + " Finished: " 
					+ Calendar.getInstance().getTime());
		}
	}
	
	/**
	 * Calculates the cluster novelty feature for each cluster generated
	 * on a specific run date and stores them in the database.
	 *
	 * @param log_date the run date
	 * @throws Exception if there is an error calculating or storing the feature
	 * 		values
	 */
	public void updateNoveltyFeature(Date log_date) throws Exception{
		Map<Integer, String> windowvals = new TreeMap<Integer, String>();
		String[] windowsstr = properties.getProperty(NOVELTY_WINDOWSKEY).split(",");
		String[] windowfields = properties.getProperty(NOVELTY_WINFIELDSKEY).split(",");
		
		if(windowfields.length != windowsstr.length){
			throw new Exception("Number of novelty window values and fields do not match.");
		}
		
		for(int i = 0; i < windowsstr.length; i++){
			windowvals.put(Integer.parseInt(windowsstr[i]), windowfields[i]);
		}

		//We start from largest window to smallest so we can cache the prevDates results for later use
		List<Integer> windowkeys = new ArrayList<Integer>(windowvals.keySet());
		Collections.reverse(windowkeys);
		
		for(int window : windowkeys){
			Map<Integer, Double> novelty = calculateNoveltyFeature(log_date, window);
			for(int clusterid : novelty.keySet()){
				StringBuffer querybuf = new StringBuffer();
				Formatter formatter = new Formatter(querybuf);
				formatter.format(properties.getProperty(NOVELTY_QUERY3KEY),
						df.format(log_date), windowvals.get(window), 
							String.valueOf(novelty.get(clusterid)), 
							String.valueOf(clusterid), df.format(log_date));
				dbi.executeQueryNoResult(querybuf.toString());
				formatter.close();
			}
		}
		
		
	}
	

	/**
	 * Calculates the previous cluster ratio feature for each cluster generated
	 * on a specific run date and stores them in the database.
	 *
	 * @param log_date the run date
	 * @throws SQLException if the feature values can not be stored in the database
	 */
	public void updatePrevClusterRatios(Date log_date) throws SQLException{
		Hashtable<Integer, List<Double>> ratios = 
				this.calculatePrevClusterRatios(log_date, 
						Integer.parseInt(properties.getProperty(PREVCLUSTER_WINDOWKEY)));
		for(int clusterid : ratios.keySet()){
			List<Double> ratiovals = ratios.get(clusterid);
			StringBuffer querybuf = new StringBuffer();
			Formatter formatter = new Formatter(querybuf);
			formatter.format(properties.getProperty(PREVCLUSTER_QUERY4KEY), 
					df.format(log_date), ratiovals.get(0).toString(),
					ratiovals.get(1).toString(), Integer.toString(clusterid));
			dbi.executeQueryNoResult(querybuf.toString());
			formatter.close();
		}
	}
}
