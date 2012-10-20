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

import java.util.List;
import java.util.concurrent.atomic.AtomicReferenceArray;

import edu.uga.cs.fluxbuster.db.DBInterface;

/**
 * The base class for any similarity calculator thread implementation.
 * 
 * @author Chris Neasbitt
 */
public abstract class SimilarityCalculatorThread extends Thread {

	protected List<Integer> aclusters, bclusters = null;
	
	protected AtomicReferenceArray<AtomicReferenceArray<Double>> results = null;
	
	protected String adatestr, bdatestr = null;
	
	protected DBInterface db = null;
	
	/**
	 * Instantiates a new similarity calculator thread.
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
	 */
	public SimilarityCalculatorThread(DBInterface db, String adatestr, String bdatestr, 
			List<Integer> aclusters, List<Integer> bclusters, 
			AtomicReferenceArray<AtomicReferenceArray<Double>> results) {
		this.adatestr = adatestr;
		this.bdatestr = bdatestr;
		this.aclusters = aclusters;
		this.bclusters = bclusters;
		this.results = results;
		this.db = db;
		
	}
	
	/**
	 * Calculate the similarities.
	 */
	protected abstract void calculateSimilarity();
	
	/**
	 * Insert the result into the results buffer.
	 *
	 * @param acluster the first cluster id
	 * @param bcluster the second cluster id
	 * @param value the calculated similarity value
	 */
	protected void insertResult(int acluster, int bcluster, double value){
		results.get(acluster).getAndSet(bcluster, value);
	}
	
	/**
	 * @see java.lang.Thread#run()
	 */
	public void run(){
		this.calculateSimilarity();
	}
}
