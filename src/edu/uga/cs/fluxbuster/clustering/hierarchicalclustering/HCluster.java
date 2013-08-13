/*
 * Copyright (C) 2009 Roberto Perdisci
 * Author: Roberto Perdisci
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

package edu.uga.cs.fluxbuster.clustering.hierarchicalclustering;

import java.io.Serializable;
import java.util.Vector;

/**
 * This class represents a cluster during hierarchical clustering.
 * 
 * @author Roberto Perdisci
 */
public class HCluster implements Serializable {

	private static final long serialVersionUID = -1152344050995894679L;
	
	private Vector<Integer> indexes = new Vector<Integer>();

	/**
	 * Instantiates a new cluster.
	 */
	public HCluster() {}

	/**
	 * Instantiates a new cluster.
	 *
	 * @param index the index of the first element to add
	 * 		to the cluster
	 */
	public HCluster(int index) {
		indexes.add(index);
	}

	/**
	 * Gets the indexes of the elements in the cluster.
	 *
	 * @return the indexes in the cluster
	 */
	public Vector<Integer> getIndexes() {
		return this.indexes;
	}

	/**
	 * Create a new cluster by merging two clusters.
	 *
	 * @param c1 the first cluster
	 * @param c2 the second cluster
	 * @return the cluster that results from merging two clusters
	 */
	public static HCluster merge(HCluster c1, HCluster c2) {
		HCluster c = new HCluster();
		c.indexes.addAll(c1.indexes);
		c.indexes.addAll(c2.indexes);

		return c;
	}

	/**
	 * Determines if the cluster contains the supplied element.
	 *
	 * @param k the element index
	 * @return true, if the cluster contains the element
	 */
	public boolean contains(int k) {
		return indexes.contains(k);
	}

	/**
	 * Returns a string representing this HCluster.
	 * 
	 * @return a string representing this HCluster.
	 */
	@Override
	public String toString() {
		StringBuffer b = new StringBuffer();
		b.append("[ ");
		for (Integer i : indexes)
			b.append(i + " ");
		b.append("]");

		return b.toString();
	}
}
