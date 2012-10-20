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

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This class represents a similarity measurement between two clusters.
 * 
 * @author Chris Neasbitt
 */
public class ClusterSimilarity {
	
	private Date adate, bdate = null;
	
	private int acluster_id, bcluster_id = 0;
	
	private double sim = 0;
	
	/**
	 * Instantiates a new cluster similarity.
	 *
	 * @param adate the run date of the first cluster
	 * @param bdate the run date of the second cluster
	 * @param acluster_id the id of the first cluster
	 * @param bcluster_id the id of the second cluster
	 * @param sim the similarity measurement
	 */
	public ClusterSimilarity(Date adate, 
			Date bdate, int acluster_id, int bcluster_id, double sim){
		this.setADate(adate);
		this.setBDate(bdate);
		this.setAClusterId(acluster_id);
		this.setBClusterId(bcluster_id);
		this.setSim(sim);
	}
	
	/**
	 * Gets the run date of the first cluster.
	 *
	 * @return the run date
	 */
	public Date getADate() {
		return adate;
	}
	
	/**
	 * Sets the run date of the first cluster.
	 *
	 * @param adate the run date
	 */
	public void setADate(Date adate) {
		this.adate = adate;
	}
	
	/**
	 * Gets the run date of the second cluster.
	 *
	 * @return the run date
	 */
	public Date getBDate() {
		return bdate;
	}
	
	/**
	 * Sets the run date of the second cluster.
	 *
	 * @param bdate the new run date
	 */
	public void setBDate(Date bdate) {
		this.bdate = bdate;
	}
	
	/**
	 * Gets the id of the first cluster.
	 *
	 * @return the first cluster's id
	 */
	public int getAClusterId() {
		return acluster_id;
	}
	
	/**
	 * Sets the id of the first cluster.
	 *
	 * @param acluster_id the first cluster's id
	 */
	public void setAClusterId(int acluster_id) {
		this.acluster_id = acluster_id;
	}
	
	/**
	 * Gets the id of the second cluster.
	 *
	 * @return the second cluster's id
	 */
	public int getBClusterId() {
		return bcluster_id;
	}
	
	/**
	 * Sets the id of the second cluster.
	 *
	 * @param bcluster_id the second cluster's id
	 */
	public void setBClusterId(int bcluster_id) {
		this.bcluster_id = bcluster_id;
	}
	
	/**
	 * Gets the similarity measurement.
	 *
	 * @return the similarity measurement
	 */
	public double getSim() {
		return sim;
	}
	
	/**
	 * Sets the similarity measurement.
	 *
	 * @param sim the similarity measurement
	 */
	public void setSim(double sim) {
		this.sim = sim;
	}
	
	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString(){
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
		return "("+ acluster_id + ":" + df.format(adate) + "," + bcluster_id + ":"
				+ df.format(bdate) + ") " + sim;
	}
	
	
}
