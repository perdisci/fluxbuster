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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class represents the result of hierarchical clustering represented
 * as a dendrogram
 * 
 * @author Roberto Perdisci
 */
public class Dendrogram implements Serializable {

	static final long serialVersionUID = -1235809600124455376L;

	private Map<Double, Vector<HCluster>> heights = 
			new TreeMap<Double, Vector<HCluster>>();
	
	private static Log log = LogFactory.getLog(Dendrogram.class);

	/**
	 * Adds the clusters at a specific height in the dendrogram.
	 *
	 * @param height the height
	 * @param clusters the clusters
	 */
	@SuppressWarnings("unchecked")
	public void addClusters(double height, Vector<HCluster> clusters) {
		// Makes a deep copy
		Vector<HCluster> c = (Vector<HCluster>) deepCopy(clusters);
		getHeights().put(height, c);
	}

	/**
	 * Creates a deep copy of any object.
	 *
	 * @param o the object to copy
	 * @return the copy
	 */
	public Object deepCopy(Object o) {
		try {
			ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
			ObjectOutputStream out = new ObjectOutputStream(byteOut);
			out.writeObject(o);
			out.close();

			byte[] data = byteOut.toByteArray();
			ObjectInputStream in = new ObjectInputStream(
					new ByteArrayInputStream(data));
			Object copy = in.readObject();
			in.close();

			return copy;
		} catch (Exception e) {
			if(log.isErrorEnabled()){
				log.error("Error performing deep copy.", e);
			}
		}

		return null;
	}

	/**
	 * Gets the clusters at a particular cut height.
	 *
	 * @param cutHeight the cut height
	 * @return the clusters
	 */
	public Vector<HCluster> getClusters(double cutHeight) {
		double height = 0;
		for (double h : getHeights().keySet()) {
			if (h > cutHeight)
				break;

			height = h;
		}

		return getHeights().get(height);
	}

	/**
	 * Prints the dendrogram to stdout.
	 */
	public void print() {
		for (double h : getHeights().keySet()) {
			System.out.println("h=" + h + " ---> ");
			for (HCluster v : getHeights().get(h)) {
				System.out.println(" " + v);
			}
		}
	}

	/**
	 * Prints the clusters at a cut height.
	 *
	 * @param h the cut height.
	 */
	public void print(double h) {
		System.out.println("h=" + h + " ---> ");
		for (HCluster v : getHeights().get(h)) {
			System.out.print(" " + v + " ");
		}
		System.out.println("Num of clusters = " + getHeights().get(h).size());
	}

	/**
	 * Sets the dendrogram with the list clusters at specific heights.
	 *
	 * @param heights the heights, clusters map
	 */
	public void setHeights(Map<Double, Vector<HCluster>> heights) {
		this.heights = heights;
	}

	/**
	 * Gets all of the clusters at each height.
	 *
	 * @return the heights, clusters map
	 */
	public Map<Double, Vector<HCluster>> getHeights() {
		return heights;
	}
}
