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

/**
 * This class represents an entry in a distance matrix
 * 
 * @author Roberto Perdisci
 */
public class ClusterIndexPair implements Serializable {
	
	static final long serialVersionUID = -1235809600124455376L;

	private int i;
	
	private int j;
	
	private float dist;

	/**
	 * Instantiates a new cluster index pair.
	 *
	 * @param i the row coordinate
	 * @param j the column coordinate
	 * @param dist the distance value
	 */
	public ClusterIndexPair(int i, int j, float dist) {
		this.setI(i);
		this.setJ(j);
		this.setDist(dist);
	}

	/**
	 * Sets the distance value.
	 *
	 * @param dist the distance value
	 */
	public void setDist(float dist) {
		this.dist = dist;
	}

	/**
	 * Gets the distance value.
	 *
	 * @return the distance value
	 */
	public float getDist() {
		return dist;
	}

	/**
	 * Sets the row coordinate.
	 *
	 * @param i the row coordinate.
	 */
	public void setI(int i) {
		this.i = i;
	}

	/**
	 * Gets the row coordinate.
	 *
	 * @return the row coordinate
	 */
	public int getI() {
		return i;
	}

	/**
	 * Sets the column coordinate.
	 *
	 * @param j the column coordinate
	 */
	public void setJ(int j) {
		this.j = j;
	}

	/**
	 * Gets the column coordinate.
	 *
	 * @return the row coordinate
	 */
	public int getJ() {
		return j;
	}
}
