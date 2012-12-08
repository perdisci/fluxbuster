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

package edu.uga.cs.fluxbuster.db;

import java.io.Reader;
import java.io.StringReader;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.text.SimpleDateFormat;

import edu.uga.cs.fluxbuster.analytics.ClusterSimilarity;
import edu.uga.cs.fluxbuster.analytics.ClusterSimilarityCalculator;
import edu.uga.cs.fluxbuster.clustering.CandidateFluxDomain;
import edu.uga.cs.fluxbuster.clustering.DomainCluster;
import edu.uga.cs.fluxbuster.utils.DomainNameUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;

import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.ConnectionHandle;

/**
 * The implementation of the DBInterface for PostgresSQL.
 * 
 * @author Chris Neasbitt
 */
public class PostgresDBInterface extends DBInterface {
	
	private static Log log = LogFactory.getLog(PostgresDBInterface.class);

	/**
	 * Instantiates a new postgres db interface.
	 *
	 * @param connectstring the connection string for the database 
	 */
	public PostgresDBInterface(BoneCP connectionPool) {
		super(connectionPool);
	}
	
	/**
	 * @see edu.uga.cs.fluxbuster.db.DBInterface#initSimilarityTables(java.util.Date)
	 */
	@Override
	public void initSimilarityTables(Date logdate){
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		String logDateTable = dateFormat.format(logdate);
		
		dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		String logDateStr = dateFormat.format(logdate);
		
		String clusterIpSimilCreateQuery = "CREATE TABLE cluster_ip_similarity_" + logDateTable
				+ " (CONSTRAINT cluster_ip_similarity_" +logDateTable+"_log_date_check CHECK (log_date = '" + logDateStr + "'::date), "
				+ " CONSTRAINT cluster_ip_similarity_" + logDateTable + "_pkey PRIMARY KEY (cluster_id, candidate_cluster_id, similarity, log_date, candidate_log_date) ) "
				+ " INHERITS (cluster_ip_similarity)";
		
		String clusterDomainnameSimilCreateQuery = "CREATE TABLE cluster_domainname_similarity_" + logDateTable
				+ " (CONSTRAINT cluster_domainname_similarity_" +logDateTable+"_log_date_check CHECK (log_date = '" + logDateStr + "'::date), "
				+ " CONSTRAINT cluster_domainname_similarity_" + logDateTable + "_pkey PRIMARY KEY (cluster_id, candidate_cluster_id, similarity, log_date, candidate_log_date) ) "
				+ " INHERITS (cluster_domainname_similarity)";
		
		String clusterSimilIpCreate = "CREATE INDEX cluster_ip_similarity_" + logDateTable +"_logdate "
				  + " ON cluster_ip_similarity_" + logDateTable +" USING btree (log_date)";
		
		String clusterSimilDomainnameCreate = "CREATE INDEX cluster_domainname_similarity_" + logDateTable +"_logdate "
				  + " ON cluster_domainname_similarity_" + logDateTable +" USING btree (log_date)";
		
		try{
			this.executeQueryNoResult("SELECT * FROM cluster_ip_similarity_" + logDateTable + " limit 1", true);
		} catch(Exception e) {
			this.executeQueryNoResult(clusterIpSimilCreateQuery);
			this.executeQueryNoResult(clusterSimilIpCreate);
		}
		
		try{
			this.executeQueryNoResult("SELECT * FROM cluster_domainname_similarity_" + logDateTable + " limit 1", true);
		} catch(Exception e) {
			this.executeQueryNoResult(clusterDomainnameSimilCreateQuery);
			this.executeQueryNoResult(clusterSimilDomainnameCreate);
		}
		
	}
	
	/**
	 * @see edu.uga.cs.fluxbuster.db.DBInterface#initClassificationTables(java.util.Date)
	 */
	@Override
	public void initClassificationTables(Date logdate){
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		String logDateTable = dateFormat.format(logdate);
		
		dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		String logDateStr = dateFormat.format(logdate);
		
		String clusterClassesCreateQuery = "CREATE TABLE cluster_classes_" + logDateTable
	            + " (CONSTRAINT cluster_classes_"+ logDateTable + "_log_date_check CHECK (log_date = '"+ logDateStr + "'::date), "
	            + " CONSTRAINT cluster_classes_"+ logDateTable + "_pkey PRIMARY KEY(cluster_id, sensor_name, log_date) ) "
	            + " INHERITS (cluster_classes)";
		
		String clusterClassesIndexCreate = "CREATE INDEX cluster_classes_"+ logDateTable + "_logdate "
	            + " ON cluster_classes_"+ logDateTable + " USING btree (log_date) ";
		
		//if the table doesn't exist, this query should throw an exception
		try{
			this.executeQueryNoResult("SELECT * FROM cluster_classes_" + logDateTable + " limit 1", true);
		} catch(Exception e) {
			this.executeQueryNoResult(clusterClassesIndexCreate);
			this.executeQueryNoResult(clusterClassesCreateQuery);
		}
	}
	
	/**
	 * @see edu.uga.cs.fluxbuster.db.DBInterface#initClusterTables(java.util.Date)
	 */
	@Override
	public void initClusterTables(Date logdate){
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		String logDateTable = dateFormat.format(logdate);
		
		dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		String logDateStr = dateFormat.format(logdate);
		
        String domainsCreateQuery = "CREATE TABLE domains_"+logDateTable+" (PRIMARY KEY(domain_id), UNIQUE(domain_name), " +
    			"  CONSTRAINT domains_" + logDateTable + "_log_date_check CHECK (log_date = '" + logDateStr + "'::date)) " +
    			"INHERITS (domains)";
    
	    String clustersCreateQuery = "CREATE TABLE clusters_" + logDateTable +" (PRIMARY KEY(domain_id, sensor_name), " +
	    			"CONSTRAINT clusters_" + logDateTable + "_log_date_check CHECK (log_date = '" + logDateStr + "'::date)) " +
	    			"INHERITS (clusters)";
    
		String resolvedIPSCreateQuery = "CREATE TABLE resolved_ips_"+logDateTable+" (PRIMARY KEY(domain_id, log_date, resolved_ip), " +
					"CONSTRAINT resolved_ips_"+logDateTable+"_log_date_check CHECK (log_date = '" + logDateStr + "'::date) " +
					" ) INHERITS(resolved_ips) ";
	
		String clusterResolvedIPSCreateQuery = "CREATE TABLE cluster_resolved_ips_"+logDateTable+" ( " +
	             "PRIMARY KEY(cluster_id, sensor_name, log_date, resolved_ip), " +
	             "CONSTRAINT cluster_resolved_ips_"+logDateTable+"_log_date_check CHECK (log_date = '" + logDateStr + "'::date) ) " +
	             "INHERITS (cluster_resolved_ips)";
		
		String clusterFeaturesCreateQuery = "CREATE TABLE cluster_feature_vectors_"+logDateTable 
				+ " (CONSTRAINT cluster_feature_vectors_"+logDateTable+"_pkey PRIMARY KEY (cluster_id , sensor_name , log_date ), "
				+ " CONSTRAINT cluster_feature_vectors_"+logDateTable+"_log_date_check CHECK (log_date = '" + logDateStr + "'::date) ) "
				+ " INHERITS (cluster_feature_vectors) ";
		
		String domainsIndexCreate = "CREATE INDEX domains_" + logDateTable +"_logdate "
				  + " ON domains_" + logDateTable +" USING btree (log_date)";
				
		String clustersIndexCreate = "CREATE INDEX clusters_" + logDateTable +"_logdate "
				  + " ON clusters_" + logDateTable +" USING btree (log_date)";
		
		String resolvedIPSIndexCreate = "CREATE INDEX resolved_ips_" + logDateTable +"_logdate "
				  + " ON resolved_ips_" + logDateTable +" USING btree (log_date)";
		
		String clusterResolvedIPSIndexCreate = "CREATE INDEX cluster_resolved_ips_" + logDateTable +"_logdate "
				  + " ON cluster_resolved_ips_" + logDateTable +" USING btree (log_date)";
		
		String clusterFeaturesIndexCreate = "CREATE INDEX cluster_feature_vectors_" + logDateTable +"_logdate "
				  + " ON cluster_feature_vectors_" + logDateTable +" USING btree (log_date)";
	
		//create tables
		try{
			this.executeQueryNoResult("SELECT * FROM domains_" + logDateTable + " limit 1", true);
		} catch(Exception e) {
			this.executeQueryNoResult(domainsCreateQuery);
			this.executeQueryNoResult(domainsIndexCreate);
		}
		
		try{
			this.executeQueryNoResult("SELECT * FROM clusters_" + logDateTable + " limit 1", true);
		} catch(Exception e) {
			this.executeQueryNoResult(clustersCreateQuery);
			this.executeQueryNoResult(clustersIndexCreate);
		}
		
		try{
			this.executeQueryNoResult("SELECT * FROM resolved_ips_" + logDateTable + " limit 1", true);
		} catch(Exception e) {
			this.executeQueryNoResult(resolvedIPSCreateQuery);
			this.executeQueryNoResult(resolvedIPSIndexCreate);
		}
		
		try{
			this.executeQueryNoResult("SELECT * FROM cluster_resolved_ips_" + logDateTable + " limit 1", true);
		} catch(Exception e) {
			this.executeQueryNoResult(clusterResolvedIPSCreateQuery);
			this.executeQueryNoResult(clusterResolvedIPSIndexCreate);
		}
		
		try{
			this.executeQueryNoResult("SELECT * FROM cluster_feature_vectors_" + logDateTable + " limit 1", true);
		} catch(Exception e) {
			this.executeQueryNoResult(clusterFeaturesCreateQuery);
			this.executeQueryNoResult(clusterFeaturesIndexCreate);
		}
	}
	
	/**
	 * @see edu.uga.cs.fluxbuster.db.DBInterface#initAllTables(java.util.Date)
	 */
	@Override
	public void initAllTables(Date logdate){
		this.initClusterTables(logdate);
		this.initSimilarityTables(logdate);
		this.initClassificationTables(logdate);
	}
	

	/**
	 * @see edu.uga.cs.fluxbuster.db.DBInterface#storeBasicDnsFeatures(java.util.List, java.lang.String, java.util.Date)
	 */
	@Override
	public void storeBasicDnsFeatures(List<DomainCluster> clusters,
			String sensorname, Date logdate) {
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		String logDateTable = dateFormat.format(logdate);
				
		Connection con = null;
		PreparedStatement domainsInsertStmt = null;
		PreparedStatement domainsSelectStmt = null;
		PreparedStatement clustersInsertStmt = null;
		PreparedStatement resolvedIPSInsertStmt = null;
		PreparedStatement clusterResolvedIPSInsertStmt = null;
		PreparedStatement clusterFeatureVectorsInsertStmt = null;
		
		
		try {
			con = this.getConnection();
			domainsInsertStmt = con.prepareStatement("INSERT INTO domains_" + logDateTable +
					" VALUES(DEFAULT, ?, ?, ?)");
			domainsSelectStmt = con.prepareStatement("SELECT domain_id FROM domains_"+ logDateTable + 
            		" WHERE domain_name = ?");
			clustersInsertStmt = con.prepareStatement("INSERT INTO clusters_"+ logDateTable +" VALUES " +
            		"(?, ?, ?, ?)");
			resolvedIPSInsertStmt = con.prepareStatement("INSERT INTO resolved_ips_" + logDateTable + " VALUES " +
            		"( ?, ?, inet(?))");
			clusterResolvedIPSInsertStmt = con.prepareStatement("INSERT INTO cluster_resolved_ips_" + logDateTable + " VALUES " +
            		"( ?, ?, ?, inet(?))");
			clusterFeatureVectorsInsertStmt = con.prepareStatement("INSERT INTO cluster_feature_vectors_" + logDateTable + 
            		"(cluster_id, sensor_name, log_date, network_cardinality, ip_diversity, " +
            		"number_of_domains, ttl_per_domain, ip_growth_ratio, queries_per_domain, avg_last_growth_ratio_single_entry, " +
            		"avg_last_growth_ratio_entries, avg_last_growth_prefix_ratio_entries, last_growth_ratio_cluster," +
            		"last_growth_prefix_ratio_cluster) VALUES( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
			
	        int clusterId = 1;
	        for (DomainCluster cluster : clusters)
	        {
	        	for (CandidateFluxDomain candidateDomain : cluster.getCandidateDomains())
	        	{
	        		String domainName = filterChars(candidateDomain.getDomainName());
	        		String domainNameRev = DomainNameUtils.reverseDomainName(domainName);
	       		
	        		String secondLevelDomainName = getDomainBase(domainName);
	        		String secondLevelDomainNameRev = DomainNameUtils.reverseDomainName(secondLevelDomainName);
	        		
	        		domainsInsertStmt.setString(1, domainNameRev);
	        		domainsInsertStmt.setDate(2, new java.sql.Date(logdate.getTime()));
	        		domainsInsertStmt.setString(3, secondLevelDomainNameRev);
	                executePreparedStatementNoResult(con, domainsInsertStmt);		
	
	                domainsSelectStmt.setString(1, domainNameRev);
	                ResultSet rs = this.executePreparedStatementWithResult(con, domainsSelectStmt);
	                
	                try
	                {
		                if(rs.next())
		                {
			                int domainId = rs.getInt(1);
			                
			                clustersInsertStmt.setInt(1, clusterId);
			                clustersInsertStmt.setInt(2, domainId);
			                clustersInsertStmt.setString(3, sensorname);
			                clustersInsertStmt.setDate(4,  new java.sql.Date(logdate.getTime()));
			                
			                this.executePreparedStatementNoResult(con, clustersInsertStmt);

			                for (InetAddress resolvedIP : candidateDomain.getIps())  
			                {
			                	resolvedIPSInsertStmt.setInt(1, domainId);
			                	resolvedIPSInsertStmt.setDate(2, new java.sql.Date(logdate.getTime()));
			                	resolvedIPSInsertStmt.setString(3, resolvedIP.getHostAddress());
			                	
			                	this.executePreparedStatementNoResult(con, resolvedIPSInsertStmt);
			                	
			                }
		                }
	                }
	                catch(SQLException ex) {
	        			if(log.isErrorEnabled()){
	        				log.error("", ex);
	        			}
	                }
	                finally{
	                	rs.close();
	                }
	        	}
	        	
	            /*String nickname = getNicknames((List<String>)cluster.getDomains());
	            insertQuery = "INSERT INTO cluster_nicknames_"+ logDateTable +" VALUES" +
	            		"("+clusterId+", '"+sensorname+"', '"+logDateStr+"', '"+nickname+"')";
	            
	            performInsertQuery(insertQuery, clusterNicknamesCreateQuery);*/
	        	
	            for (InetAddress resolvedIP : cluster.getIps())
	            {
	            	clusterResolvedIPSInsertStmt.setInt(1, clusterId);
	            	clusterResolvedIPSInsertStmt.setString(2, sensorname);
	            	clusterResolvedIPSInsertStmt.setDate(3, new java.sql.Date(logdate.getTime()));
	            	clusterResolvedIPSInsertStmt.setString(4, resolvedIP.getHostAddress());
	            	
	            	this.executePreparedStatementNoResult(con, clusterResolvedIPSInsertStmt);
	            }
	            	            	            
	            clusterFeatureVectorsInsertStmt.setInt(1, clusterId);
	            clusterFeatureVectorsInsertStmt.setString(2, sensorname);
	            clusterFeatureVectorsInsertStmt.setDate(3, new java.sql.Date(logdate.getTime()));
	            clusterFeatureVectorsInsertStmt.setInt(4, cluster.getIps().size());
	            clusterFeatureVectorsInsertStmt.setDouble(5, cluster.getIpDiversity());
	            clusterFeatureVectorsInsertStmt.setInt(6, cluster.getDomains().size());
	            clusterFeatureVectorsInsertStmt.setDouble(7, cluster.getAvgTTLPerDomain());
	            clusterFeatureVectorsInsertStmt.setDouble(8, cluster.getIpGrowthRatio());
	            clusterFeatureVectorsInsertStmt.setDouble(9, cluster.getQueriesPerDomain());
	            
	            Double temp = cluster.getAvgLastGrowthRatioSingleEntry();
	            if(temp == null){
	            	clusterFeatureVectorsInsertStmt.setNull(10, java.sql.Types.REAL);
	            } else {
	            	clusterFeatureVectorsInsertStmt.setDouble(10, temp);
	            }
	            
	            temp = cluster.getAvgLastGrowthRatioEntries();
	            if(temp == null){
	            	clusterFeatureVectorsInsertStmt.setNull(11, java.sql.Types.REAL);
	            } else {
	            	clusterFeatureVectorsInsertStmt.setDouble(11, temp);
	            }
	            
	            temp = cluster.getAvgLastGrowthPrefixRatioEntries();
	            if(temp == null){
	            	clusterFeatureVectorsInsertStmt.setNull(12, java.sql.Types.REAL);
	            } else {
	            	clusterFeatureVectorsInsertStmt.setDouble(12, temp);
	            }
	            
	            temp = cluster.getLastGrowthRatioCluster();
	            if(temp == null){
	            	clusterFeatureVectorsInsertStmt.setNull(13, java.sql.Types.REAL);
	            } else {
	            	clusterFeatureVectorsInsertStmt.setDouble(13, temp);
	            }
	            
	            temp = cluster.getLastGrowthPrefixRatioCluster();
	            if(temp == null){
	            	clusterFeatureVectorsInsertStmt.setNull(14, java.sql.Types.REAL);
	            } else {
	            	clusterFeatureVectorsInsertStmt.setDouble(14, temp);
	            }
	            
	            this.executePreparedStatementNoResult(con, clusterFeatureVectorsInsertStmt);
	            
	        	clusterId++;
	        }
		} catch (SQLException e) {
			if(log.isErrorEnabled()){
				log.error("", e);
			}
		} finally {
			try {
				if(domainsInsertStmt != null && !domainsInsertStmt.isClosed()){
					domainsInsertStmt.close();
				}
				if(domainsSelectStmt != null && !domainsSelectStmt.isClosed()){
					domainsSelectStmt.close();
				}
				if(clustersInsertStmt != null && !clustersInsertStmt.isClosed()){
					clustersInsertStmt.close();
				}
				if(resolvedIPSInsertStmt != null && !resolvedIPSInsertStmt.isClosed()){
					resolvedIPSInsertStmt.close();
				}
				if(clusterResolvedIPSInsertStmt != null && !clusterResolvedIPSInsertStmt.isClosed()){
					clusterResolvedIPSInsertStmt.close();
				}
				if(clusterFeatureVectorsInsertStmt != null && !clusterFeatureVectorsInsertStmt.isClosed()){
					clusterFeatureVectorsInsertStmt.close();
				}
				if(con != null && !con.isClosed()){
					con.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Filter characters out of a domain name.  All characters are
	 * filtered except for [a-zA-Z0-9+-.].  
	 *
	 * @param domainName the domain name to filter
	 * @return the filtered domain name
	 */
	private String filterChars(String domainName)
	{
		String returnValue = "";
		
		for(char val : domainName.toCharArray())
		{
			if(Character.isLetterOrDigit(val) || val == '+' || val == '-' || val == '.')
			{
				returnValue += val;
			}
		}
		
		return returnValue;
	}
	
	/**
	 * Gets the nickname for each of the supplied domain names.  NOTE: this
	 * method has not been implemented.
	 *
	 * @param domainNames the domain names
	 * @return the list of nicknames
	 */
	//TODO implement or remove if not necessary
	public List<String> getNicknames(List<String> domainNames)
	{
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Gets the second level domain of the supplied domain name.
	 *
	 * @param domainNameOriginal the domain name
	 * @return the base domain name
	 */
	public String getDomainBase(String domainNameOriginal)
    {
		String temp = domainNameOriginal;
		if (temp.endsWith(".")) {
			temp = temp.substring(0, temp.length() - 1);
		}
		if(temp.startsWith(".")){
			temp = temp.substring(1, temp.length());
		}
		try{
			String domainName2LD = DomainNameUtils.extractEffective2LD(temp);		
			return domainName2LD;
		} catch (Exception e){
			return temp;
		}
    }
	
	/**
	 * @see edu.uga.cs.fluxbuster.db.DBInterface#executeQueryWithResult(java.lang.String)
	 */
	@Override
	public ResultSet executeQueryWithResult(String query){
		ResultSet retval = null;
		Connection con = null;
		Statement stmt = null;
		try {
			con = this.getConnection();
			con.setAutoCommit(false);
			stmt = con.createStatement();
			retval = stmt.executeQuery(query);
			con.commit();
		} catch (SQLException e) {
			retval = null;
			if(log.isErrorEnabled()){
				log.error(query, e);
			}
			try{
				if(con != null && !con.isClosed()){
					con.rollback();
				} 
			} catch (SQLException e1) {
				if(log.isErrorEnabled()){
					log.error("Error during rollback.", e1);
				}
			}
		} finally {
			try {
				if(con != null && !con.isClosed()){
					con.setAutoCommit(true);
					con.close();
				}
			} catch (SQLException e) {
				if(log.isErrorEnabled()){
					log.error("Error during close.", e);
				}
			}
		}
		return retval;
	}
	
	/**
	 * @see edu.uga.cs.fluxbuster.db.DBInterface#executeQueryNoResult(java.lang.String)
	 */
	@Override
	public void executeQueryNoResult(String query)
	{
		try
		{
			executeQueryNoResult(query, false);
		}
		catch(Exception ex){}
	}
	
	/**
	 * Execute query with no result.
	 *
	 * @param query the query
	 * @param giveException throws any exception generated if true, 
	 * 		if false all exceptions are consumed
	 * @throws Exception if giveException is true and their is an error executing
	 * 		the query
	 */
	public void executeQueryNoResult(String query, boolean giveException) throws Exception{
		Connection con = null;
		Statement stmt = null;
		SQLException exc = null;
		try {
			con = this.getConnection();
			con.setAutoCommit(false);
			stmt = con.createStatement();
			stmt.execute(query);
			con.commit();
		} catch (SQLException e) {
			if(!giveException){
				if(log.isErrorEnabled()){
					log.error(query, e);
				}
			}
			
			if(con != null){
				try {
					con.rollback();
				} catch (SQLException e1) {
					if(log.isErrorEnabled()){
						log.error("Error during rollback.", e1);
					}
				}
			}
			if(giveException){
				exc = e;
			}
		} finally {
			try {
				if(con != null && !con.isClosed()){
					con.setAutoCommit(true);
					con.close();
				}
			} catch (SQLException e) {
				if(log.isErrorEnabled()){
					log.error("Error during close.", e);
				}
			}
			if(exc != null && giveException){
				throw exc;
			}
		}
	}
	
	/**
	 * Executes a PostgresSQL copy command.
	 * 
	 * @param query the copy command to execute
	 * @param reader the containing the data to be copied
	 */
	private void executeCopyIn(String query, Reader reader){
		Connection con = null;
		CopyManager manager = null;
		try {
			con = this.getConnection();
			con.setAutoCommit(false);
			if(con instanceof com.jolbox.bonecp.ConnectionHandle){
				ConnectionHandle handle = (ConnectionHandle)con;
				manager = new CopyManager(
						(BaseConnection) handle.getInternalConnection());
			} else {
				manager = new CopyManager((BaseConnection) con);
			}
			
			manager.copyIn(query, reader);
			con.commit();
		} catch (Exception e) {
			if(log.isErrorEnabled()){
				log.error(query, e);
			}
			if(con != null){
				try {
					con.rollback();
				} catch (SQLException e1) {
					if(log.isErrorEnabled()){
						log.error("Error during rollback.", e1);
					}
				}
			}
		} finally {
			try {
				if(con != null && !con.isClosed()){
					con.setAutoCommit(true);
					con.close();
				}
			} catch (SQLException e) {
				if(log.isErrorEnabled()){
					log.error("Error during close.", e);
				}
			}
		}		
	}
	
	/**
	 * Executes a prepared statement with a result.
	 * 
	 * @param con the connection to the database
	 * @param stmt the prepared statement to execute
	 * @return the result of the query
	 */
	private ResultSet executePreparedStatementWithResult(Connection con, PreparedStatement stmt){
		ResultSet retval = null;
		try {
			con.setAutoCommit(false);
			retval = stmt.executeQuery();
			con.commit();
		} catch (SQLException e) {
			if(log.isErrorEnabled()){
				log.error("", e);
			}
			if(con != null){
				try {
					con.rollback();
				} catch (SQLException e1) {
					if(log.isErrorEnabled()){
						log.error("Error during rollback.", e1);
					}
				}
			}
		} finally {
			try {
				if(con != null && !con.isClosed()){
					con.setAutoCommit(true);
				}
			} catch (SQLException e) {
				if(log.isErrorEnabled()){
					log.error("Error setting auto commit.", e);
				}
			}
		}
		return retval;
	}
	
	/**
	 * Executes a prepared statement with no result.
	 * 
	 * @param con the connection to the database
	 * @param stmt the prepared statement to execute
	 */
	private void executePreparedStatementNoResult(Connection con, PreparedStatement stmt){
		try {
			con.setAutoCommit(false);
			stmt.execute();
			con.commit();
		} catch (SQLException e) {
			if(log.isErrorEnabled()){
				log.error("", e);
			}
			if(con != null){
				try {
					con.rollback();
				} catch (SQLException e1) {
					if(log.isErrorEnabled()){
						log.error("Error during rollback.", e1);
					}
				}
			}
		} finally {
			try {
				if(con != null && !con.isClosed()){
					con.setAutoCommit(true);
				}
			} catch (SQLException e) {
				if(log.isErrorEnabled()){
					log.error("Error setting auto commit.", e);
				}
			}
		}
	}

	/**
	 * @see edu.uga.cs.fluxbuster.db.DBInterface#getClusterIds(java.util.Date)
	 */
	@Override
	public List<Integer> getClusterIds(Date logdate) {
		ArrayList<Integer> retval = new ArrayList<Integer>();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		String logDateTable = dateFormat.format(logdate);
		String query = "select distinct cluster_id from clusters_" + logDateTable;
		ResultSet rs = this.executeQueryWithResult(query);
		try{
			while(rs.next()){
				retval.add(rs.getInt(1));
			}
		} catch (Exception e) {
			if(log.isErrorEnabled()){
				log.error("Error retrieving cluster ids.", e);
			}
		} finally {
			try {
				rs.close();
			} catch (SQLException e) {
				if(log.isErrorEnabled()){
					log.error(e);
				}
			}
		}
		return retval;
	}
	
	/**
	 * @see edu.uga.cs.fluxbuster.db.DBInterface#storeIpClusterSimilarities(java.util.List)
	 */
	@Override
	public void storeIpClusterSimilarities(List<ClusterSimilarity> sims){
		storeClusterSimilarities(sims, ClusterSimilarityCalculator.SIM_TYPE.IP);
	}
	
	/**
	 * @see edu.uga.cs.fluxbuster.db.DBInterface#storeDomainnameClusterSimilarities(java.util.List)
	 */
	@Override
	public void storeDomainnameClusterSimilarities(List<ClusterSimilarity> sims){
		storeClusterSimilarities(sims, ClusterSimilarityCalculator.SIM_TYPE.DOMAINNAME);
	}
	
	/**
	 * Store cluster similarities in the database.
	 *
	 * @param sims the similarities to store
	 * @param type the type of similarity
	 */
	private void storeClusterSimilarities(List<ClusterSimilarity> sims, ClusterSimilarityCalculator.SIM_TYPE type){
		String format = "%d\t%d\t%f\t\'%s\'\t\'%s\'\n";
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat dateStrFormat = new SimpleDateFormat("yyyyMMdd");
		StringBuffer databuf = new StringBuffer();
		Formatter formatter = new Formatter(databuf);
		if(sims.size() > 0){
			String tabletype = "";
			
			switch(type){
			case IP:
				tabletype = "ip";
				break;
			case DOMAINNAME:
				tabletype = "domainname";
				break;
			}
			
			String tabDateStr = dateStrFormat.format(sims.get(0).getADate());
			String copyQuery = "COPY cluster_"+tabletype+"_similarity_" + tabDateStr + " (cluster_id, candidate_cluster_id, " +
					"similarity, log_date, candidate_log_date ) FROM stdin;";
		
			for(ClusterSimilarity s : sims){
				formatter.format(format, s.getAClusterId(), s.getBClusterId(), s.getSim(),
						dateFormat.format(s.getADate()), dateFormat.format(s.getBDate()));
			}
			this.executeCopyIn(copyQuery, new StringReader(databuf.toString()));
		}
		formatter.close();
	}

	/**
	 * @see edu.uga.cs.fluxbuster.db.DBInterface#getDnsFeatures(java.util.Date, int)
	 */
	@Override
	public List<List<String>> getDnsFeatures(Date logdate, int minCardinality) {
		List<List<String>> retval = new ArrayList<List<String>>();
		SimpleDateFormat dateStrFormat = new SimpleDateFormat("yyyyMMdd");
		String tabDateStr = dateStrFormat.format(logdate);
		String query = "SELECT cluster_id, log_date, network_cardinality, ip_diversity, " +
				"domains_per_network, number_of_domains, ttl_per_domain, ip_growth_ratio FROM " +
				"cluster_feature_vectors_" + tabDateStr + " WHERE network_cardinality >= " + minCardinality;
		ResultSet rs = this.executeQueryWithResult(query);
		try {
			while(rs.next()){
				List<String> row = new ArrayList<String>();
				for(int i = 1; i <= 8; i++){
					row.add(rs.getString(i));
				}
				retval.add(row);
			}
		} catch (SQLException e) {
			if(log.isErrorEnabled()){
				log.error("Error retrieving dns features.", e);
			}
		} finally {
			try {
				rs.close();
			} catch (SQLException e) {
				if(log.isErrorEnabled()){
					log.error(e);
				}
			}
		}
		return retval;
	}

	
	/**
	 * @see edu.uga.cs.fluxbuster.db.DBInterface#storeDnsClusterClasses(java.util.Date, java.util.Map, boolean)
	 */
	@Override
	public void storeDnsClusterClasses(Date logdate, Map<String, List<Integer>> clusterClasses,
			boolean validated) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		String logDateTable = dateFormat.format(logdate);
		
		Connection con = null;
		PreparedStatement clusterClassesInsertStmt = null;
		try {
			con = this.getConnection();
			clusterClassesInsertStmt = con.prepareStatement(
					"INSERT INTO cluster_classes_" + logDateTable + " VALUES (?, 'SIE', ?, ?, ?)");
			for(String clusclass : clusterClasses.keySet()){
				for(int clusterID : clusterClasses.get(clusclass)){
					clusterClassesInsertStmt.setInt(1, clusterID);
					clusterClassesInsertStmt.setDate(2, new java.sql.Date(logdate.getTime()));
					clusterClassesInsertStmt.setString(3, clusclass);
					clusterClassesInsertStmt.setBoolean(4, validated);
					this.executePreparedStatementNoResult(con, clusterClassesInsertStmt);
				}
			}
		} catch (SQLException e) {
			if(log.isErrorEnabled()){
				log.error("Error storing cluster classes.", e);
			}
		} finally {
			try{
				if(clusterClassesInsertStmt != null && !clusterClassesInsertStmt.isClosed()){
					clusterClassesInsertStmt.close();
				}
			}catch (SQLException e) {
				if(log.isErrorEnabled()){
					log.error("e");
				}
			}
			try{
				if(con != null && !con.isClosed()){
					con.close();
				}
			}catch (SQLException e) {
				if(log.isErrorEnabled()){
					log.error("e");
				}
			}
		}
	}
}
