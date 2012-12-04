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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
	
	private static final String WEKA_CLASSPATHKEY = "WEKA_CLASSPATH";
	
	private static final String FEATURES_PATHKEY = "FEATURES_PATH";
	
	private static final String MODEL_PATHKEY = "MODEL_PATH";
	
	private String classpath, featuresfile, modelfile;
	
	private DBInterface dbi;
	
	private Properties localprops = null, appprops = null;
	
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
		if(appprops == null){
			appprops = PropertiesUtils.loadAppWideProperties();
		}
		this.classpath = localprops.getProperty(WEKA_CLASSPATHKEY);
		this.featuresfile = appprops.getProperty(FEATURES_PATHKEY);
		this.modelfile = localprops.getProperty(MODEL_PATHKEY);
		this.dbi = dbi;
	}
	
	/**
	 * Instantiates a new classifier.
	 *
	 * @param classpath the class path for running weka
	 * @param featuresfile the path to store the features file
	 * @param modelfile the path to the classification model file
	 */
	public Classifier(String classpath, String featuresfile, String modelfile){
		this(classpath, featuresfile, modelfile, DBInterfaceFactory.loadDBInterface());
	}
	
	/**
	 * Instantiates a new feature calculator with a specific database
	 * interface.
	 *
	 * @param classpath the class path for running weka
	 * @param featuresfile the path to store the features file
	 * @param modelfile the path to the classification model file
	 * @param dbi the database interface
	 */
	public Classifier(String classpath, String featuresfile, String modelfile, DBInterface dbi){
		this.classpath = classpath;
		this.featuresfile = featuresfile;
		this.modelfile = modelfile;
		this.dbi = dbi;
	}
	
	/**
	 * Executes the classifier and stores the results in the database.
	 *
	 * @param logDate the clustering run date
	 * @param minCardinality the minimum network cardinality of clusters
	 * 		to classify
	 * @throws IOException if there is an error creating the features file
	 */
	public void updateClusterClasses(Date logDate, int minCardinality) throws IOException{
		List<List<String>> features = dbi.getDnsFeatures(logDate, minCardinality);
		this.prepareFeaturesFile(featuresfile, features);
		String output = this.callClassifier(classpath, featuresfile, modelfile);
		if(log.isDebugEnabled()){
			log.debug("Classifier output\n" + output);
		}
		Map<String, List<Integer>> clusterClasses = 
				this.parseClassifierOutput(output, features);
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
	}
	
	/**
	 * Prepares the features file from the clustering data.
	 *
	 * @param filepath the path to store the prepared features file
	 * @param features the feature values to use in classification
	 * @throws IOException if there is an error writing the features
	 * 		file
	 */
	private void prepareFeaturesFile(String filepath, 
			List<List<String>> features) throws IOException{
		PrintWriter writer = new PrintWriter(new FileWriter(new File(filepath)));
		writer.write(featuresHeader);
		for(List<String> featurerow : features){
			for(int i = 2; i < featurerow.size(); i++){
				writer.write(Double.parseDouble(featurerow.get(i)) + ", ");
			}
			writer.write("NOT_Flux\n");
		}
		writer.close();
	}
	
	/**
	 * Executes the classifier in a separate process.
	 *
	 * @param classpath the class path for running weka
	 * @param featuresfile the path to store the features file
	 * @param modelfile the path to the classification model file
	 * @return the output of the classifier
	 */
	private String callClassifier(String classpath, String featuresfile, String modelfile){
		String cmdline = this.getJavaBin() + " -cp " + classpath + " weka.classifiers.trees.J48 -T " +
				featuresfile + " -l " + modelfile + " -p 0";
		return this.execToString(cmdline);
	}
	
	/**
	 * Finds the path of the currently executing jvm.
	 * 
	 * @return the path to the jvm
	 */
	private String getJavaBin(){
		String filesep = System.getProperty("file.separator");
		return System.getProperty("java.home") + filesep + "bin" + 
				filesep + "java";
	}
	
	/**
	 * Parses the classifier output.
	 *
	 * @param output the output of the classifier
	 * @param features the feature values for each cluster
	 * @return a map of classified clusters the keys are the class names
	 * 		and the values are the list of cluster ids in the class
	 */
	private Map<String, List<Integer>> parseClassifierOutput(String output, 
			List<List<String>> features){
		Map<String, List<Integer>> retval = 
				new HashMap<String, List<Integer>>();
		retval.put("Flux", new ArrayList<Integer>());
		retval.put("NOT_Flux", new ArrayList<Integer>());
		
		Pattern validatorRegex = Pattern.compile("^\\s+\\d");
		int feature_index = 0;
		String[] lines = output.split("\n");
		if(log.isDebugEnabled()){
			log.debug("Features size " + features.size());
		}
		for(int i = 0; i < lines.length; i++){
			Matcher validator = validatorRegex.matcher(lines[i]);
			if(validator.find()){
				List<String> sample = features.get(feature_index++);
				String clusterid = sample.get(0);
				if(log.isDebugEnabled()){
					log.debug("Cluster id " + clusterid + " log line " + lines[i]);
				}
				if(lines[i].contains(":Flux")){
					if(log.isDebugEnabled()){
						log.debug("Adding " + clusterid + " to Flux class.");
					}
					retval.get("Flux").add(Integer.parseInt(clusterid));
				} else {
					if(log.isDebugEnabled()){
						log.debug("Adding " + clusterid + " to NOT_Flux class.");
					}
					retval.get("NOT_Flux").add(Integer.parseInt(clusterid));
				}
			}
		}
		return retval;
	}
	
	//http://stackoverflow.com/questions/6295866/how-can-i-capture-the-output-of-a-command-as-a-string-with-commons-exec
	/**
	 * Executes a command in a separate process and returns the output
	 * of the command.
	 *
	 * @param command the command
	 * @return the output of the command
	 */
	public String execToString(String command){
		try{
		    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		    CommandLine commandline = CommandLine.parse(command);
		    DefaultExecutor exec = new DefaultExecutor();
		    PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
		    exec.setStreamHandler(streamHandler);
		    if(log.isDebugEnabled()){
		    	log.debug("Executing " + commandline);
		    }
		    exec.execute(commandline);
		    String retval = outputStream.toString();	    
		    return(retval);
		} catch (IOException e) {
			log.error("Error executing command " + command,e);
		}
		return new String();
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
