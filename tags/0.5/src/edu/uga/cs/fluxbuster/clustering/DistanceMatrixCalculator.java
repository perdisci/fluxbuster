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

package edu.uga.cs.fluxbuster.clustering;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

/**
 * This class calculates the distance matrix of a set of candidate
 * flux domains.  The Jaccard index is used as the distance metric.
 * 
 * @author Chris Neasbitt
 */
public class DistanceMatrixCalculator implements Runnable {

	double gamma = 0.0;
	
	Set<Integer> rows = null;
	
	List<CandidateFluxDomain> cfds = null;
	
	List<Vector<Float>> resultList = null;

	/**
	 * Instantiates a new distance matrix calculator.
	 *
	 * @param gamma the gamma value to use in distance calculation
	 * @param rows the rows of the upper triangular distance matrix to
	 * 		calculate.
	 * @param cfds the list of candidate flux domains
	 * @param resultList the buffer in which to place the results of the
	 * 		calculations
	 */
	public DistanceMatrixCalculator(double gamma, Set<Integer> rows,
			List<CandidateFluxDomain> cfds, List<Vector<Float>> resultList) {
		this.gamma = gamma;
		this.rows = rows;
		this.cfds = cfds;
		this.resultList = resultList;

	}

	/**
	 * Compute distance matrix.
	 */
	private void computeDistanceMatrix() {
		for (int i : rows) {
			Vector<Float> rowvals = new Vector<Float>();
			for (int j = i + 1; j < cfds.size(); j++) {
				float dist = computeCFDDistance(cfds.get(j), cfds.get(i));
				rowvals.add(dist);
			}
			resultList.set(i, rowvals);
		}
	}

	/**
	 * Computes the Jaccard index between two candidate flux domains.
	 *
	 * @param cfd1 the first candidate flux domain
	 * @param cfd2 the second candidate flux domain
	 * @return the distance value
	 */
	private float computeCFDDistance(CandidateFluxDomain cfd1,
			CandidateFluxDomain cfd2) {
		float retval = 1.0f;

		HashSet<InetAddress> intersection = new HashSet<InetAddress>(
				cfd1.getIps());
		intersection.retainAll(cfd2.getIps());

		HashSet<InetAddress> union = new HashSet<InetAddress>(cfd1.getIps());
		union.addAll(cfd2.getIps());

		int ilen = intersection.size();
		int ulen = union.size();

		if (ulen == 0) {
			return retval;
		}

		float G = (float) Math.exp(gamma
				- Math.min(cfd1.getIps().size(), cfd2.getIps().size()));
		retval = 1.0f - ilen / ((float) ulen) * (1 / (1 + G));

		return retval;
	}

	/**
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		computeDistanceMatrix();
	}

}
