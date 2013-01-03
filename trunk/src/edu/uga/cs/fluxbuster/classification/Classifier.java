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

package edu.uga.cs.fluxbuster.classification;


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import weka.classifiers.trees.J48;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

import edu.uga.cs.fluxbuster.db.DBInterface;
import edu.uga.cs.fluxbuster.db.DBInterfaceFactory;
import edu.uga.cs.fluxbuster.utils.PropertiesUtils;

/**
 * This class runs the classifier on the clusters and stores
 * the derived classes in the database.
 * 
 * @author Chris Neasbitt
 */
public class Classifier {

	private static final String featuresHeader = "@RELATION FastFlux\n\n"+
			"@ATTRIBUTE Network_Cardinality NUMERIC\n"+
			"@ATTRIBUTE Network_Prefixes NUMERIC\n" +
			"@ATTRIBUTE Domains_Per_Network NUMERIC\n" +
			"@ATTRIBUTE Number_of_Domains NUMERIC\n" +
			"@ATTRIBUTE TTL_Per_Domain NUMERIC\n" +
			"@ATTRIBUTE IP_Growth_Ratio NUMERIC\n" +
			"@ATTRIBUTE class {Flux, NOT_Flux}\n\n" +
			"@DATA\n";
	
	
	private static final String MODEL_PATHKEY = "MODEL_PATH";
	
	private String modelfile;
	
	private DBInterface dbi;
	
	private Properties localprops = null;
	
	private static Log log = LogFactory.getLog(Classifier.class);
	
	/**
	 * Instantiates a new classifier. The object is configured via
	 * the properties file.
	 *
	 * @throws IOException if there is an error reading the
	 * 		Classifer.properties file
	 */
	public Classifier() throws IOException{
		this(DBInterfaceFactory.loadDBInterface());
	}
	
	/**
	 * Instantiates a new feature calculator with a specific database
	 * interface.  The object is configured via the properties file.
	 *
	 * @param dbi the database interface
	 * @throws IOException if there is an error reading the
	 * 		Classifer.properties file
	 */
	public Classifier(DBInterface dbi) throws IOException{
		if(localprops == null){
			localprops = PropertiesUtils.loadProperties(this.getClass());
		}
		this.setModelPath(new File(localprops.getProperty(MODEL_PATHKEY))
			.getCanonicalPath());
		this.dbi = dbi;
	}
		
	/**
	 * Instantiates a new classifier.
	 *
	 * @param modelfile the path to the classification model file
	 */
	public Classifier(String modelfile){
		this(modelfile, DBInterfaceFactory.loadDBInterface());
	}
	
	/**
	 * Instantiates a new feature calculator with a specific database
	 * interface.
	 *
	 * @param modelfile the absolute path to the classification model file
	 * @param dbi the database interface
	 */
	public Classifier(String modelfile, DBInterface dbi){
		this.modelfile = modelfile;
		this.dbi = dbi;
	}
	
	/**
	 * Sets the path to the trained J48 decision tree.
	 * 
	 * @param modelfile the path to the serialized model
	 */
	public void setModelPath(String modelfile){
		this.modelfile = modelfile;
	}
	
	/**
	 * Gets the path to the trained J48 decision tree.
	 */
	public String getModelPath(){
		return this.modelfile;
	}
	
	/**
	 * Prepares the features from the db, executes the classifier, and 
	 * stores the results in the database.
	 *
	 * @param logDate the clustering run date
	 * @param minCardinality the minimum network cardinality of clusters
	 * 		to classify
	 * @throws IOException if there is an error creating the features file
	 */
	public void updateClusterClasses(Date logDate, int minCardinality) throws IOException{
		String simplename = null;
		if(log.isInfoEnabled()){
			simplename = this.getClass().getSimpleName();
			log.info(simplename + " Started: " 
					+ Calendar.getInstance().getTime());
		}
		dbi.initClassificationTables(logDate);
		Map<String, List<Integer>> clusterClasses = 
				this.classifyClusters(logDate, minCardinality);
		if(log.isDebugEnabled()){
			String retval = "";
			for(String cls : clusterClasses.keySet()){
				retval += "Cluster Class: " + cls + "\n";
				for(int clusterid : clusterClasses.get(cls)){
					retval += "\t" + clusterid + "\n";
				}
			}
			log.debug(retval);
		}
		this.storeClusterClasses(logDate, clusterClasses);
		if(log.isInfoEnabled()){
			log.info(simplename + " Finished: " 
					+ Calendar.getInstance().getTime());
		}
	}
	
	/**
	 * Prepares the features from the db and executes the classifier.
	 * 
	 * @param logDate the run date of the clusters
	 * @param minCardinality the minimum network cardinality for a cluster
	 * 		to be consider for classification
	 * @return a map of the classified clusters, the keys are the classes
	 * 		and the values are lists of cluster id's belonging to those classes
	 * @throws IOException
	 */
	public Map<String, List<Integer>> classifyClusters(Date logDate, 
			int minCardinality) throws IOException{
		Map<String, List<Integer>> retval = null;
		if(log.isDebugEnabled()){
			log.debug("Retrieving features from db.");
		}
		List<List<String>> features = dbi.getDnsFeatures(logDate, minCardinality);
		if(log.isDebugEnabled()){
			log.debug("Features retrieved.");
			log.debug("Preparing features file.");
		}
		String prepfeatures = this.prepareFeatures(features);
		if(log.isDebugEnabled()){
			log.debug("File prepared.");
			log.debug("Executing J48 classifier.");
		}
		retval = this.executeClassifier(prepfeatures, modelfile, features);
		if(log.isDebugEnabled()){
			log.debug("J48 execution complete.");
		}
		return retval;
	}
	
	/**
	 * Generates a String of the features in arff format
	 * 
	 * @param features the raw features
	 * @return the arff format version of the features
	 */
	private String prepareFeatures(List<List<String>> features){
		StringBuffer buf = new StringBuffer();
		buf.append(featuresHeader);
		for(List<String> featurerow : features){
			for(int i = 2; i < featurerow.size(); i++){
				buf.append(Double.parseDouble(featurerow.get(i)) + ", ");
			}
			buf.append("NOT_Flux\n");
		}
		return buf.toString();	
	}
	
	
	/**
	 * Executes the classifier.
	 * 
	 * @param prepfeatures the prepared features in arff format
	 * @param modelfile the path to the serialized model
	 * @param rawfeatures the raw features retrieved from the database
	 * @return a map of the classified clusters, the keys are the classes
	 * 		and the values are lists of cluster id's belonging to those classes
	 */
	private Map<String, List<Integer>> executeClassifier(String prepfeatures, String modelfile, 
			List<List<String>> rawfeatures){
		Map<String, List<Integer>> retval = new HashMap<String, List<Integer>>();
		try{
			DataSource source = new DataSource(new ByteArrayInputStream(prepfeatures.getBytes()));
			Instances data = source.getDataSet();
			if (data.classIndex() == -1){
				data.setClassIndex(data.numAttributes() - 1);
			}
			String[] options = weka.core.Utils.splitOptions("-p 0");
			J48 cls = (J48)weka.core.SerializationHelper.read(modelfile);
			cls.setOptions(options);
			for(int i = 0; i < data.numInstances(); i++){
				Integer clusterid = Integer.parseInt(rawfeatures.get(i).get(0));
				double pred = cls.classifyInstance(data.instance(i));
				String iclass = data.classAttribute().value((int)pred);
				if(!retval.containsKey(iclass)){
					retval.put(iclass, new ArrayList<Integer>());
				}
				retval.get(iclass).add(clusterid);
			}
		} catch (Exception e) {
			if(log.isErrorEnabled()){
				log.error("Error executing classifier.", e);
			}
		}
		return retval;
	}
	
	/**
	 * Store cluster classes in the database.
	 *
	 * @param logDate the clustering run date
	 * @param clusterClasses the map of classified clusters
	 */
	private void storeClusterClasses(Date logDate, Map<String, List<Integer>> clusterClasses){
		dbi.storeDnsClusterClasses(logDate, clusterClasses, false);
	}

}
