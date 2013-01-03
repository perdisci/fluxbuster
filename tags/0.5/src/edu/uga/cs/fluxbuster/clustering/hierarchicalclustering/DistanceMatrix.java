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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class implements an upper trianglar distance matrix.
 * 
 * @author Roberto Perdisci
 */
public class DistanceMatrix implements Serializable {

	static final long serialVersionUID = -1235809600124455376L;

	private Vector<Vector<Float>> distMatrix = null;

	private HierarchicalClustering.LinkageType linkMethod = 
			HierarchicalClustering.LinkageType.SINGLE_LINKAGE;
	
	private static Log log = LogFactory.getLog(DistanceMatrix.class);

	/**
	 * Loads the distance matrix from a file which contains the upper triangle
	 * values in a single row.
	 * 
	 * @param path
	 *            the path to the matrix file
	 * @throws IOException
	 *             if the matrix values file can not be read
	 */
	public DistanceMatrix(String path) throws IOException {
		distMatrix = new Vector<Vector<Float>>();
		BufferedReader br = new BufferedReader(new FileReader(path));
		String[] strvals = br.readLine().split("\\s");
		Vector<Float> vals = new Vector<Float>();
		for (String s : strvals) {
			vals.add(Float.valueOf(s));
		}
		br.close();
		populateDistanceMatrix(vals);
	}

	/**
	 * Loads the distance matrix from a file which contains the upper triangle
	 * values in a single row.
	 * 
	 * @param path
	 *            the path to the matrix file
	 * @param linkMethod
	 *            the linkage method to use when clustering
	 * @throws IOException
	 *             if the matrix values file can not be read
	 */
	public DistanceMatrix(String path,
			HierarchicalClustering.LinkageType linkMethod) throws IOException {
		this(path);
		this.linkMethod = linkMethod;

	}

	/**
	 * Instantiates a new distance matrix from a list of matrix values specified
	 * in row major order.
	 * 
	 * @param vals
	 *            the matrix values
	 */
	public DistanceMatrix(Vector<Float> vals) {
		distMatrix = new Vector<Vector<Float>>();
		populateDistanceMatrix(vals);
	}

	/**
	 * Instantiates a new distance matrix from a list of matrix values specified
	 * in row major order.
	 * 
	 * @param vals
	 *            the matrix values
	 * @param linkMethod
	 *            the linkage method to use when clustering
	 */
	public DistanceMatrix(Vector<Float> vals,
			HierarchicalClustering.LinkageType linkMethod) {
		this(vals);
		this.linkMethod = linkMethod;

	}

	/**
	 * Populate distance matrix from a list of matrix values specified in row
	 * major order.
	 * 
	 * @param vals
	 *            the matrix values
	 */
	private void populateDistanceMatrix(Vector<Float> vals) {
		int matrixdim = (int) Math.ceil(Math.sqrt(2 * vals.size()));

		int length = matrixdim - 1;
		int start = 0;
		for (int i = 0; i < matrixdim - 1; i++) {
			Vector<Float> row = new Vector<Float>();
			row.addAll(vals.subList(start, start + length));
			distMatrix.add(row);
			start += length;
			length--;
		}
	}

	/**
	 * Gets the num instances.
	 * 
	 * @return the num instances
	 */
	public int getNumInstances() {
		return distMatrix.size() + 1;
	}

	/**
	 * Return the value at coordinates (i,j). If i = j then 0 is returned. If j
	 * < i then the value at (j,i) is returned.
	 * 
	 * @param i
	 *            the row coordinate
	 * @param j
	 *            the column coordinate
	 * @return the value at the specified coordinates
	 */
	public float distance(int i, int j) {
		return distance(new IndexPair(i, j));
	}

	/**
	 * Get the distance between the clusters at the supplied indexes.
	 * 
	 * @param p
	 *            the indexes of the clusters
	 * @return the distance between the clusters, if the row == column then 0 is
	 *         returned, if the indexes are not within the bounds of the
	 *         distance matrix Float.MAX_VALUE is returned
	 */
	public float distance(IndexPair p) {
		if (p.getI() == p.getJ()) {
			return 0;
		} else {
			return distanceHelper(p);
		}
	}

	/**
	 * Retrieves the distance between the clusters at the supplied indexes
	 * 
	 * @param p
	 *            the indexes of the clusters
	 * @return the distance between the clusters, if the indexes are not within
	 *         the bounds of the distance matrix Float.MAX_VALUE is returned
	 */
	private float distanceHelper(IndexPair p) {
		int i = p.getI();
		int j = p.getJ();
		if (i >= 0 && i < distMatrix.size() && j >= 0 && j < distMatrix.size()) {
			try {
				Vector<Float> row = distMatrix.get(i);
				return row.get(j);

			} catch (Exception e) {
				if(log.isErrorEnabled()){
					log.error("Error retrieving distance.", e);
				}
			}
		}
		return Float.MAX_VALUE;
	}

	/**
	 * Find closest cluster pair in the distance matrix.
	 * 
	 * @return the closest cluster index pair
	 */
	public ClusterIndexPair findClosestClusterPair() {
		ClusterIndexPair p = new ClusterIndexPair(-1, -1, Float.MAX_VALUE);

		for (int k = 0; k < distMatrix.size(); k++) {
			for (int l = 0; l < distMatrix.get(k).size(); l++) {
				float d = distMatrix.get(k).get(l);
				if (d < p.getDist()) {
					p.setI(k);
					p.setJ(l + k + 1);
					p.setDist(d);
				} else if (d == p.getDist()) { // in this case chooses at random
					if (Math.random() < 0.5) {
						p.setI(k);
						p.setJ(l + k + 1);
						p.setDist(d);
					}
				}
			}
		}

		return p;
	}

	/**
	 * Updates the distance matrix with the clusters at the supplied indexes
	 * merged.
	 * 
	 * @param pair
	 *            the pair of clusters to merge.
	 */
	void update(ClusterIndexPair pair) {

		for (int m = 0; m < pair.getI(); m++) { // explores the rows until the
												// row pair.getI() that has to
												// be updated

			// System.out.println("0=================");
			// this.printDistMatrix();

			IndexPair p1 = translateCoordinates(m, pair.getI());
			IndexPair p2 = translateCoordinates(m, pair.getJ());

			float a = distMatrix.get(p1.getI()).get(p1.getJ());
			float b = distMatrix.get(p2.getI()).get(p2.getJ());

			float c = Math.min(a, b); // default is single linkage
			
			// this is redundant, it's here to make things clearer
			if (linkMethod == HierarchicalClustering.LinkageType.SINGLE_LINKAGE) 
				c = Math.min(a, b);
			else if (linkMethod == HierarchicalClustering.LinkageType.COMPLETE_LINKAGE)
				c = Math.max(a, b);

			distMatrix.get(p1.getI()).set(p1.getJ(), c);
			distMatrix.get(p2.getI()).remove(p2.getJ()); // removes the column
															// related to the
															// cluster that has
															// been merged
		}

		// System.out.println("1=================");
		// this.printDistMatrix();

		// updates row pair.getI()
		int numInst = this.getNumInstances();
		for (int h = pair.getI() + 1; h < numInst; h++) {
			// System.out.println(">>>  " + pair.getI() + "\t" + pair.getJ() +
			// "\t" + h + "\t");

			if (h != pair.getJ()) {
				IndexPair p1 = translateCoordinates(pair.getI(), h);
				IndexPair p2 = translateCoordinates(pair.getJ(), h);

				// System.out.println(p1.getI() + "\t" + p1.getJ() + "\t" +
				// p2.getI() + "\t" + p2.getJ());

				float a = distMatrix.get(p1.getI()).get(p1.getJ());
				float b = distMatrix.get(p2.getI()).get(p2.getJ());

				float c = Math.min(a, b); // default is single linkage
				
				// this is redundant, it's here to make things clearer
				if (linkMethod == HierarchicalClustering.LinkageType.SINGLE_LINKAGE) 
					c = Math.min(a, b);
				else if (linkMethod == HierarchicalClustering.LinkageType.COMPLETE_LINKAGE)
					c = Math.max(a, b);

				distMatrix.get(p1.getI()).set(p1.getJ(), c);
			}
		}

		// System.out.println("2=================");
		// this.printDistMatrix();

		IndexPair p = translateCoordinates(pair.getI(), pair.getJ());
		distMatrix.get(p.getI()).remove(p.getJ());

		for (int m = pair.getI() + 1; m < pair.getJ(); m++) { // removes the
																// columns
																// related to
																// the cluster
																// that has been
																// merged
			IndexPair p1 = translateCoordinates(m, pair.getJ());

			distMatrix.get(p1.getI()).remove(p1.getJ());
		}

		if (pair.getJ() < distMatrix.size())
			distMatrix.remove(pair.getJ()); // removes the row related to the
											// cluster that has been merged
		else
			distMatrix.remove(pair.getJ() - 1); // if pair.getJ() was the last
												// row, then row pair.getJ()-1
												// becomes useless (0 elements)

		// System.out.println("3=================");
		// this.printDistMatrix();

	}

	/**
	 * Translates coordinates from a square symmetric matrix to an upper
	 * triangular matrix. This method produces erronous results if i == j.
	 * 
	 * @param i
	 *            the row coordinate
	 * @param j
	 *            the column coordinate
	 * @return the translated coordinates
	 */
	private IndexPair translateCoordinates(int i, int j) {
		if (i > j) { // the distance matrix is symmetric, but we only store the
						// upper triangle
			int tmp = i;
			i = j;
			j = tmp;
		}

		return new IndexPair(i, j - i - 1);
	}

	/**
	 * Prints the distance matrix to stdout.
	 */
	public void printDistMatrix() {
		for (int i = 0; i < distMatrix.size() + 1; i++) {

			for (int h = 0; h < i + 1; h++) {
				System.out.print("x\t");
			}

			if (i < distMatrix.size()) {
				for (int j = 0; j < distMatrix.get(i).size(); j++)
					System.out.printf("%.2f\t", distMatrix.get(i).get(j));
			}

			System.out.println();
		}
	}
}
