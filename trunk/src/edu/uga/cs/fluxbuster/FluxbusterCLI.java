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

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.uga.cs.fluxbuster.analytics.ClusterSimilarityCalculator;
import edu.uga.cs.fluxbuster.classification.Classifier;
import edu.uga.cs.fluxbuster.clustering.ClusterGenerator;
import edu.uga.cs.fluxbuster.clustering.DomainCluster;
import edu.uga.cs.fluxbuster.db.DBInterfaceFactory;
import edu.uga.cs.fluxbuster.features.FeatureCalculator;


//TODO add argument to use domains file
/**
 * This is the command line interface for fluxbuster.
 * 
 * usage: java -cp .:../lib/* edu.uga.cs.fluxbuster.FluxbusterCLI [-gfsc] -d startTime -e endTime
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
	private static boolean validateDate(String date) {
		Pattern ipregex = Pattern.compile("\\d{8}");
		Matcher matcher = ipregex.matcher(date);
		if (!matcher.matches()) {
			return false;
		}
		return true;
	}

	@SuppressWarnings("static-access")
	private static Options initializeOptions(){
		Options retval = new Options();
		
		retval.addOption(
					OptionBuilder.isRequired(false)
					.withDescription("Print help message.")
					.withLongOpt("help")
					.create("?"))
				.addOption(
					OptionBuilder.hasArg(false)
					.isRequired(false)
					.withDescription("Generate clusters only. (Optional)")
					.withLongOpt("generate-clusters")
					.create("g"))
				.addOption(
					OptionBuilder.hasArg(false)
					.isRequired(false)
					.withDescription("Calculate cluster features. (Optional)")
					.withLongOpt("calc-features")
					.create("f"))
				.addOption(
					OptionBuilder.hasArg(false)
					.isRequired(false)
					.withDescription("Calculate cluster similarities. (Optional)")
					.withLongOpt("calc-similarity")
					.create("s"))
				.addOption(
					OptionBuilder.hasArg(false)
					.isRequired(false)
					.withDescription("Classify clusters. (Optional)")
					.withLongOpt("classify-clusters")
					.create("c"))
				.addOption(
					OptionBuilder.hasArg()
					.isRequired(true)
					.withDescription("The start date of the input data.  " +
							"Should be in yyyyMMdd format.")
					.withLongOpt("start-date")
					.create("d"))
				.addOption(
					OptionBuilder.hasArg()
					.isRequired(true)
					.withDescription("The end date of the input data.  " +
							"Should be in yyyyMMdd format.")
					.withLongOpt("end-date")
					.create("e"));
	
		   return retval;
		 
	}
	
	
	/**
	 * The main method.
	 *
	 * @param args the command line arguments
	 */
	public static void main(String[] args){
		GnuParser parser = new GnuParser();
		Options opts = FluxbusterCLI.initializeOptions();
		CommandLine cli;
		try {
			cli = parser.parse(opts, args);
			
			if(cli.hasOption('?')){
				throw new ParseException(null);
			}
		
			if (validateDate(cli.getOptionValue('d')) && 
					validateDate(cli.getOptionValue('e'))) {
				
				if(log.isInfoEnabled()){
					StringBuffer arginfo = new StringBuffer("\n");
					arginfo.append("generate-clusters: " + cli.hasOption('g') + "\n");
					arginfo.append("calc-features: " + cli.hasOption('f') + "\n");
					arginfo.append("calc-similarity: " + cli.hasOption('s') + "\n");
					arginfo.append("classify-clusters: " + cli.hasOption('c') + "\n");
					arginfo.append("start-date: " + cli.getOptionValue('d') + "\n");
					arginfo.append("end-date: " + cli.getOptionValue('e') + "\n");
					log.info(arginfo.toString());
				}
				
				try{
					boolean clus = true, feat = true, simil = true, clas = true;
					if(cli.hasOption('g') || cli.hasOption('f') 
							|| cli.hasOption('s') || cli.hasOption('c')){
						if(!cli.hasOption('g')){
							clus = false;
						}
						if(!cli.hasOption('f')){
							feat = false;
						}
						if(!cli.hasOption('s')){
							simil = false;
						}
						if(!cli.hasOption('c')){
							clas = false;
						}
					}
					
					DBInterfaceFactory.init();
					SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
					Date logdate = df.parse(cli.getOptionValue('d'));
					long startTime = logdate.getTime() / 1000;
					long endTime = df.parse(cli.getOptionValue('e')).getTime() / 1000;
					
					if(clus){
						ClusterGenerator cg = new ClusterGenerator();
						List<DomainCluster> clusters = cg.generateClusters(startTime,
								endTime, true);
						cg.storeClusters(clusters, logdate);
					}
					if(feat){
						FeatureCalculator calc = new FeatureCalculator();
						calc.updateFeatures(logdate);
					}
					if(simil){
						ClusterSimilarityCalculator calc2 = new ClusterSimilarityCalculator();
						calc2.updateClusterSimilarities(logdate);
					}
					if(clas){
						Classifier calc3 = new Classifier();
						calc3.updateClusterClasses(logdate, 30);
					}	
				} catch (Exception e) {
					if(log.isFatalEnabled()){
						log.fatal("", e);
					}
				} finally {
					DBInterfaceFactory.shutdown();
				}
			} else {
				throw new ParseException(null);
			}
		} catch (ParseException e1){
			PrintWriter writer = new PrintWriter(System.out);  
			HelpFormatter usageFormatter = new HelpFormatter();
			usageFormatter.printHelp(writer,
                    80,
                    "fluxbuster",
                    "If none of the options g, f, s, c are specified " +
                    "then the program will execute as if all of them " +
                    "have been specified.  Otherwise, the program will " +
                    "only execute the options specified.",
                    opts,
                    0,
                    2,
                    "");
			writer.close();
		}
	}
}
