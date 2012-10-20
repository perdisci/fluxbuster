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
 * The class represents a pair of (row, column) i.e (i,j) coordinates.
 * 
 * @author Roberto Perdisci
 */
public class IndexPair implements Serializable {

	static final long serialVersionUID = -1235809600124455376L;

	private int i;
	
	private int j;

	/**
	 * Instantiates a new index pair.
	 *
	 * @param i the row coordinate
	 * @param j the column coordinate
	 */
	public IndexPair(int i, int j) {
		this.setI(i);
		this.setJ(j);
	}

	/**
	 * Sets the row coordinate.
	 *
	 * @param i the row coordinate
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
	 * @return the column coordinate
	 */
	public int getJ() {
		return j;
	}
}
