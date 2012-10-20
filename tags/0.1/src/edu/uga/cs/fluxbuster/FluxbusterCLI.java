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

package edu.uga.cs.fluxbuster;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.uga.cs.fluxbuster.analytics.ClusterSimilarityCalculator;
import edu.uga.cs.fluxbuster.classification.Classifier;
import edu.uga.cs.fluxbuster.clustering.ClusterGenerator;
import edu.uga.cs.fluxbuster.clustering.DomainCluster;
import edu.uga.cs.fluxbuster.features.FeatureCalculator;


//TODO add argument to use domains file
/**
 * This is the command line interface for fluxbuster.
 * 
 * usage: java -cp .:../lib/* edu.uga.cs.fluxbuster.FluxbusterCLI startTime endTime
 * Dates should be in yyyyMMdd format.
 */
public class FluxbusterCLI {

	private static Log log = LogFactory.getLog(FluxbusterCLI.class);
	
	/**
	 * Validates the arguments.
	 *
	 * @param args the program arguments.
	 * @return true, if valid, false otherwise.
	 */
	private static boolean validateArgs(String[] args) {
		Pattern ipregex = Pattern.compile("\\d{8}");
		if (args.length != 2) {
			return false;
		}
		for (String arg : args) {
			Matcher matcher = ipregex.matcher(arg);
			if (!matcher.matches()) {
				return false;
			}
		}

		return true;
	}

	/**
	 * The main method.
	 *
	 * @param args the command line arguments
	 */
	public static void main(String[] args){
		if (validateArgs(args)) {
			try{
				ClusterGenerator cg = new ClusterGenerator();
				SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
				Date logdate = df.parse(args[0]);
				
				long startTime = logdate.getTime() / 1000;
				long endTime = df.parse(args[1]).getTime() / 1000;
				
				List<DomainCluster> clusters = cg.generateClusters(startTime,
						endTime, true);
				cg.storeClusters(clusters, logdate);
				FeatureCalculator calc = new FeatureCalculator();
				ClusterSimilarityCalculator calc2 = new ClusterSimilarityCalculator();
				Classifier calc3 = new Classifier();
				calc.updateFeatures(logdate);
				calc2.updateClusterSimilarities(logdate);
				calc3.updateClusterClasses(logdate, 30);
					
			} catch (Exception e) {
				if(log.isFatalEnabled()){
					log.fatal("", e);
				}
			}
		} else {
			System.out.println("In the bin directory execute\n" + 
					"Usage: java -cp .:../lib/* " + FluxbusterCLI.class.getName() + 
					" <startTime> <endTime>");
			System.out.println("Dates should be int yyyyMMdd format.");
		}
	}
}
