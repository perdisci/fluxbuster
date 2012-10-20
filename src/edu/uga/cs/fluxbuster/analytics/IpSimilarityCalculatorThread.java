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
package edu.uga.cs.fluxbuster.analytics;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Formatter;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.uga.cs.fluxbuster.db.DBInterface;
import edu.uga.cs.fluxbuster.utils.PropertiesUtils;

/**
 * The implementation of the SimilarityCalculatorThread for calculating 
 * ip based similarity.
 * 
 * @author Chris Neasbitt
 */
public class IpSimilarityCalculatorThread extends SimilarityCalculatorThread {

	private Properties properties = null;
	
	private String query = null;
	
	private static String INTERSECTION_QUERYKEY = "INTERSECTION_QUERY";
	
	private static Log log = LogFactory.getLog(IpSimilarityCalculatorThread.class);
	
	/**
	 * Instantiates a new ip similarity calculator thread.
	 *
	 * @param db the database interface
	 * @param adatestr the date of the first clustering run
	 * @param bdatestr the date of the second clustering run
	 * @param aclusters the list of cluster id's from the first run
	 * 		for which to calculate similarities
	 * @param bclusters the list of cluster id's from the second run to
	 * 		compare with the clusters in the first list
	 * @param results the result buffer in which to place the calculated 
	 * 		similarity
	 * @throws IOException if the IpSimilarityCalculatorThread.property 
	 * 		file can not be loaded
	 */
	public IpSimilarityCalculatorThread(DBInterface db, String adatestr,  String bdatestr,
			List<Integer> aclusters, List<Integer> bclusters,
			AtomicReferenceArray<AtomicReferenceArray<Double>> results) throws IOException {
		super(db, adatestr, bdatestr, aclusters, bclusters, results);
		properties = PropertiesUtils.loadProperties(this.getClass());
		query = properties.getProperty(INTERSECTION_QUERYKEY);
	}

	/**
	 * @see edu.uga.cs.fluxbuster.analytics.SimilarityCalculatorThread#calculateSimilarity()
	 */
	@Override
	protected void calculateSimilarity() {
		for (int aclus : aclusters){
			for(int bclus : bclusters){
				Double result = null;
				try{
					result = this.executeQuery(aclus, bclus);
					if(result != null && Math.abs(result - 0.0) > 0.00001) {  //remember doubles are approx.
						this.insertResult(aclus, bclus, result);
					}
				} catch (Exception e){
					if(log.isErrorEnabled()){
						log.error("Error: (" + aclus + "," + bclus + ") " + result, e);
					}
				}
			}
		}
	}
	
	/**
	 * Execute the ip similarity query.
	 *
	 * @param aclus the first cluster id
	 * @param bclus the second cluster id
	 * @return the similarity value
	 * @throws SQLException if there is a error executing the query
	 */
	private Double executeQuery(int aclus, int bclus) throws SQLException{
		Double retval = null;
		StringBuffer querybuf = new StringBuffer();
		Formatter formatter = new Formatter(querybuf);
		formatter.format(query, adatestr, aclus, adatestr, aclus, bdatestr, bclus);
		String query = querybuf.toString();
		ResultSet rs = db.executeQueryWithResult(query);
		if(rs.next()){
			retval = rs.getDouble(1);
		}
		rs.getStatement().close();
		formatter.close();
		return retval;
		
	}

}
