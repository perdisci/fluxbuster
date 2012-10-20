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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.uga.cs.fluxbuster.clustering.hierarchicalclustering.Dendrogram;
import edu.uga.cs.fluxbuster.clustering.hierarchicalclustering.DistanceMatrix;
import edu.uga.cs.fluxbuster.clustering.hierarchicalclustering.HCluster;
import edu.uga.cs.fluxbuster.clustering.hierarchicalclustering.HierarchicalClustering;
import edu.uga.cs.fluxbuster.clustering.hierarchicalclustering.HierarchicalClustering.LinkageType;
import edu.uga.cs.fluxbuster.db.DBInterface;
import edu.uga.cs.fluxbuster.db.DBInterfaceFactory;
import edu.uga.cs.fluxbuster.utils.PropertiesUtils;
import edu.uga.cs.fluxbuster.utils.DomainNameUtils;

/**
 * This class initiates the hierarchical clustering process.
 * 
 * @author Chris Neasbitt
 */
public class ClusterGenerator {

	private ArrayList<String> domainWhitelist = null;
	
	private Properties properties = null;

	private static final String WHITELISTKEY = "WHITELIST_FILE";
	
	private static final String GAMMAKEY = "GAMMA";
	
	private static final String FLUXFILEREGEXKEY = "CANDIDATE_FLUX_FILE_REGEX";
	
	private static final String FLUXFILEPARSEREGEXKEY = "CANDIDATE_FLUX_FILE_PARSING_REGEX";
	
	private static final String FLUXDIRKEY = "CANDIDATE_FLUX_DIR";
	
	private static final String MINRRSETSIZEKEY = "MIN_TOTAL_RRSET_SIZE";
	
	private static final String MINDIVERSITYKEY = "MIN_TOTAL_DIVERSITY";
	
	private static final String SHORTTTLKEY = "VERY_SHORT_TTL";
	
	private static final String CANDIDATETHRESHKEY = "GOOD_CANDIDATE_THRESHOLD";
	
	private static final String MAXDOMAINSKEY = "MAX_CANDIDATE_DOMAINS";
	
	private static final String LINKAGETYPEKEY = "LINKAGE_TYPE";
	
	private static final String MAXCUTHEIGHTKEY = "MAX_CUT_HEIGHT";
	
	private static final String DISTMATRIXKEY = "DIST_MATRIX_MULTITHREADED";
	
	private static final String DISTNUMTHREADSKEY = "DIST_MATRIX_NUMTHREADS";

	private static final String SELECTEDCFDFILEKEY = "SELECTED_CFD_FILE";
	
	private static Log log = LogFactory.getLog(ClusterGenerator.class);
	
	/**
	 * Instantiates a new cluster generator.
	 *
	 * @throws IOException if the ClusterGenerator.properties file can
	 * 		not be read
	 */
	public ClusterGenerator() throws IOException {
		properties = PropertiesUtils.loadProperties(this.getClass());
		try {
			loadWhitelist();
		} catch (IOException e) {
			if(log.isErrorEnabled()){
				log.error("Error loading domain whitelist.", e);
			}
		}
	}

	/**
	 * Load the domain whitelist.
	 *
	 * @throws IOException if the whitelist file can not be read
	 */
	private void loadWhitelist() throws IOException {
		domainWhitelist = new ArrayList<String>();
		String whitelistfile = properties.getProperty(WHITELISTKEY);

		BufferedReader br = new BufferedReader(new FileReader(whitelistfile));
		String line;
		while ((line = br.readLine()) != null) {
			domainWhitelist.add(line.trim());
		}
		br.close();
	}

	/**
	 * Compute a distance matrix from a list of candidate flux domains.
	 *
	 * @param cfds the candidate flux domains
	 * @return the vector of values in the distance matrix in row major
	 * 		order
	 */
	private Vector<Float> computeDistanceMatrix(List<CandidateFluxDomain> cfds){
		boolean multithread = Boolean.parseBoolean(properties
				.getProperty(DISTMATRIXKEY));
		if (multithread) {
			int numthreads = Integer.parseInt(properties
					.getProperty(DISTNUMTHREADSKEY));
			if (numthreads < 1) {
				numthreads = 1;
			}
			return computeDistanceMatrixMultiThreaded(cfds, numthreads);
		} else {
			return computeDistanceMatrixMultiThreaded(cfds, 1);
		}

	}

	/**
	 * Compute a distance matrix from a list of candidate flux domains with
	 * a maximum number of calculation threads.
	 *
	 * @param cfds the list of candidate flux domains
	 * @param maxnumthreads the thread ceiling
	 * @return the vector of values in the distance matrix in row major
	 * 		order
	 */
	private Vector<Float> computeDistanceMatrixMultiThreaded(
			List<CandidateFluxDomain> cfds, int maxnumthreads){
		Vector<Float> retval = new Vector<Float>();
		ThreadFactory tf = Executors.defaultThreadFactory();
		double gamma = Double.parseDouble(properties.getProperty(GAMMAKEY));
		ArrayList<Thread> threads = new ArrayList<Thread>();
		ArrayList<HashSet<Integer>> threadrows = new ArrayList<HashSet<Integer>>();

		int interval = (int) Math.ceil((cfds.size() - 1)
				/ (double) maxnumthreads);
		int left = 0;
		int right = cfds.size() - 2;
		HashSet<Integer> curset = null;
		boolean addLeftFirst = true;

		while (left <= right) {
			if (curset == null) {
				curset = new HashSet<Integer>();
			}

			if (curset.size() == interval) {
				threadrows.add(curset);
				curset = null;
			} else {
				if (addLeftFirst) {
					curset.add(left++);
				} else {
					curset.add(right--);
				}
				addLeftFirst = !addLeftFirst;

				if (curset.size() == interval) {
					continue;
				}

				if (addLeftFirst) {
					curset.add(left++);
				} else {
					curset.add(right--);
				}
			}
		}
		if (curset != null && curset.size() > 0) {
			threadrows.add(curset);
		}

		ArrayList<Vector<Float>> resultsList = new ArrayList<Vector<Float>>(
				cfds.size());
		// this is necessary to make sure that the proper indexes exist in
		// resultsList before being accessed by the threads
		for (int i = 0; i < cfds.size() - 1; i++) {
			resultsList.add(null);
		}

		for (int i = 0; i < threadrows.size(); i++) {
			Thread t = tf.newThread(new DistanceMatrixCalculator(gamma,
					threadrows.get(i), cfds, resultsList));
			threads.add(t);
		}

		for (Thread t : threads) {
			t.start();
		}
		
		for (Thread t : threads) {
			try{
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		for (int i = 0; i < resultsList.size(); i++) {
			retval.addAll(resultsList.get(i));
		}

		return retval;
	}

	/**
	 * Determines if a domain name is in the whitelist.
	 *
	 * @param domainname the domain name
	 * @return true, if the domain name is on the whitelist
	 */
	private boolean isWhiteListable(String domainname) {
		for (String d : domainWhitelist) {
			if (domainname.endsWith(d)) {
				return true;
			}
		}
		return false;
	}

	// TODO improve the candidate score algorithm
	/**
	 * Calculates the candidate flux domains clustering potential.  This
	 * value is used to sort which candidate flux domains are the best
	 * candidates for clustering.
	 *
	 * @param cfd the candidate flux domain
	 * @return the candidate score
	 */
	public double calcCandidateScore(CandidateFluxDomain cfd) {
		int minTotalRrsetSize = Integer.parseInt(properties
				.getProperty(MINRRSETSIZEKEY));
		double minTotalDiversity = Double.parseDouble(properties
				.getProperty(MINDIVERSITYKEY));
		double veryShortTTL = Double.parseDouble(properties
				.getProperty(SHORTTTLKEY));

		double ipDiv = IPDiversityCalculator.ipDiversity(IPDiversityCalculator
				.getV4Ips(cfd.getIps()));

		if (cfd.getNumIPs() >= minTotalRrsetSize && ipDiv > minTotalDiversity) {
			return 1.0;
		} else if (cfd.getNumIPs() == 1 && cfd.getAvgTTL() <= veryShortTTL) {
			return 1.0;
		}
		return 0.0;
	}

	/**
	 * Load candidate flux domains from the data files for the time period
	 * between the start and end times.
	 *
	 * @param startTime the start time
	 * @param endTime the end time
	 * @param domainfile a file containing the list of domains that should
	 * 		be clustered regardless of the candidate score.  If null the list
	 * 		is ignored.
	 * @return the list of candidate flux domains
	 * @throws Exception if there is an error reading the ClusterGenerator.properties
	 * 		or data files
	 */
	public List<CandidateFluxDomain> loadCandidateFluxDomains(long startTime,
			long endTime, String domainfile) throws Exception {
		ArrayList<CandidateFluxDomain> retval = new ArrayList<CandidateFluxDomain>();
		HashMap<String, CandidateFluxDomain> seenDomains = new HashMap<String, CandidateFluxDomain>();
		List<String> recentFluxDomains = this.loadRecentFluxDomains();
		String dirPath = properties.getProperty(FLUXDIRKEY);
		double goodCandidateThreshold = Double.parseDouble(properties
				.getProperty(CANDIDATETHRESHKEY));
		int maxCandidateDomains = Integer.parseInt(properties
				.getProperty(MAXDOMAINSKEY));

		for (String filename : this.getFileNames(dirPath, startTime, endTime)) {
			BufferedReader br = null;
			try {
				GZIPInputStream gis = new GZIPInputStream(new FileInputStream(
						filename));
				br = new BufferedReader(new InputStreamReader(gis));
				String line;
				while ((line = br.readLine()) != null) {
					CandidateFluxDomain cfd = CandidateFluxDomain
							.parseFromLog(line);

					if (isWhiteListable(cfd.getDomainName())) {
						continue;
					}

					String domainname = cfd.getDomainName();
					if (seenDomains.containsKey(domainname)) {
						CandidateFluxDomain prev = seenDomains.get(domainname);
						seenDomains.put(domainname, prev.merge(cfd));
					} else {
						seenDomains.put(domainname, cfd);
					}

				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (br != null) {
					br.close();
				}
			}
		}
		
		//add all domains from a file
		if(domainfile != null){
			BufferedReader br = new BufferedReader(new FileReader(new File(domainfile)));
			String line = null;
			while((line = br.readLine()) != null){
				line = line.trim();
				if (retval.size() == maxCandidateDomains) {
					break;
				}
				CandidateFluxDomain d = seenDomains.get(line);
				if(d != null){
					if(log.isDebugEnabled()){
						log.debug("Adding domain " + line + " from domains file.");
					}
					retval.add(d);
					seenDomains.remove(line);
				} else {
					if(log.isDebugEnabled()){
						log.debug("Unable to load domain " + line + " from domains file.");
					}				
				}
			}
			br.close();
		}
		
		// add all domains from recently seen flux domains
		ArrayList<String> allDomains = new ArrayList<String>();
		allDomains.addAll(seenDomains.keySet());

		ArrayList<String> removeDomains = new ArrayList<String>();
		if (recentFluxDomains.size() > 0) {
			Collections.shuffle(allDomains); // this is probably not necessary
			for (String domainname : allDomains) {
				if (retval.size() == maxCandidateDomains) {
					break;
				}
				String temp = domainname;
				if (temp.endsWith(".")) {
					temp = temp.substring(0, temp.length() - 1);
				}
				String domainname2LD = DomainNameUtils.extractEffective2LD(temp);
				if (recentFluxDomains.contains(domainname2LD)) {
					retval.add(seenDomains.get(domainname));
					removeDomains.add(domainname);
				}
			}
		}
		allDomains.removeAll(removeDomains);
		removeDomains.clear();

		// then add the non-recent ones that meet the score threshold
		if (retval.size() < maxCandidateDomains) {
			ArrayList<CandidateFluxDomain> sortedDomains = new ArrayList<CandidateFluxDomain>();

			// get all cfd's whose score is over the threshold
			for (String domain : allDomains) {
				CandidateFluxDomain temp = seenDomains.get(domain);
				if (this.calcCandidateScore(temp) > goodCandidateThreshold) {
					sortedDomains.add(temp);
				}
			}

			// sort them in descending order by score
			Collections.sort(sortedDomains,
					new Comparator<CandidateFluxDomain>() {
						@Override
						public int compare(CandidateFluxDomain o1,
								CandidateFluxDomain o2) {
							Double o1score = calcCandidateScore(o1);
							Double o2score = calcCandidateScore(o2);
							return o2score.compareTo(o1score); // Descending
																// order
						}
					});

			for (CandidateFluxDomain cfd2 : sortedDomains) {
				if (retval.size() == maxCandidateDomains) {
					break;
				}
				retval.add(cfd2);
				removeDomains.add(cfd2.getDomainName());
			}
		}
		allDomains.removeAll(removeDomains);
		removeDomains.clear();

		// then fill the rest randomly from what's left over
		if (retval.size() < maxCandidateDomains) {
			Collections.shuffle(allDomains);
			for (String domainname : allDomains) {
				if (retval.size() == maxCandidateDomains) {
					break;
				}
				retval.add(seenDomains.get(domainname));
			}
		}

		return retval;
	}

	// TODO stub implementation
	/**
	 * Load recent flux domains.
	 *
	 * @return the list of recent flux domains
	 */
	private List<String> loadRecentFluxDomains() {
		ArrayList<String> retval = new ArrayList<String>();

		return retval;
	}

	/**
	 * Gets the names of the data input files from a specific 
	 * directory for the time period between the start and end times.
	 *
	 * @param dirPath the data file directory
	 * @param startTime the start time
	 * @param endTime the end time
	 * @return the list of input file names
	 */
	private List<String> getFileNames(String dirPath, long startTime,
			long endTime) {
		ArrayList<String> retval = new ArrayList<String>();
		ArrayList<File> selectedFiles = new ArrayList<File>();
		String fileregex = properties.getProperty(FLUXFILEREGEXKEY);
		String parseregx = properties.getProperty(FLUXFILEPARSEREGEXKEY);
		Pattern parsepattern = Pattern.compile(parseregx);
		File fluxdir = new File(dirPath);
		if (fluxdir.isDirectory()) {
			File[] posFluxFiles = fluxdir.listFiles();
			if (posFluxFiles != null) {
				for (File posFluxFile : posFluxFiles) {
					if (posFluxFile.getName().matches(fileregex)) {
						Matcher parsematcher = parsepattern.matcher(posFluxFile
								.getName());
						parsematcher.find();
						long timestamp = Long.parseLong(parsematcher.group(0));
						if (timestamp >= startTime && timestamp < endTime) {
							selectedFiles.add(posFluxFile);
						}
					}
				}
			}

		}

		// sorts in ascending order by filename
		Collections.sort(selectedFiles, new Comparator<File>() {
			@Override
			public int compare(File o1, File o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});

		for (File selectedFile : selectedFiles) {
			if(log.isDebugEnabled()){
				log.debug("Loading file: " + selectedFile.getName());
			}
			retval.add(selectedFile.getAbsolutePath());
		}
		
		return retval;
	}

	/**
	 * Runs the clustering process on the data files for the time period
	 * between the start and end times.  The linkage type and max cut height
	 * are read from the ClusterGenerator.properties file
	 *
	 * @param startTime the start time
	 * @param endTime the end time
	 * @param selcfds if true then the file with a list of domains to cluster regardless
	 * 		of candidate score is used for clustering
	 * @return the list of clusters
	 * @throws Exception if there is an error reading the ClusterGenerator.properties
	 * 		or data files
	 */
	public List<DomainCluster> generateClusters(long startTime, long endTime, boolean selcfds)
			throws Exception {
		if(selcfds){
			String selcfdfilepath = properties.getProperty(SELECTEDCFDFILEKEY);
			if(new File(selcfdfilepath).exists()){
				return this.generateClusters(startTime, endTime, selcfdfilepath);
			}
		}
		return this.generateClusters(startTime, endTime, null);
	}
	
	
	/**
	 * Runs the clustering process on the data files for the time period
	 * between the start and end times.  The linkage type and max cut height
	 * are read from the ClusterGenerator.properties file
	 *
	 * @param startTime the start time
	 * @param endTime the end time
	 * @param domainfile a list of domains to cluster regardless of candidate
	 * 		score
	 * @return the list of clusters
	 * @throws Exception if there is an error reading the ClusterGenerator.properties
	 * 		or data files
	 */
	public List<DomainCluster> generateClusters(long startTime, long endTime, 
			String domainfile) throws Exception{
		double maxCutHeight = Double.parseDouble(properties
				.getProperty(MAXCUTHEIGHTKEY));
		String linkageTypeStr = properties.getProperty(LINKAGETYPEKEY);
		LinkageType linkage = LinkageType.COMPLETE_LINKAGE;
		if (linkageTypeStr.toLowerCase().trim().equals("single")) {
			linkage = LinkageType.SINGLE_LINKAGE;
		}
		return this.generateClusters(startTime, endTime, domainfile, linkage, maxCutHeight);		
	}

	/**
	 * Runs the clustering process on the data files for the time period
	 * between the start and end times.
	 *
	 * @param startTime the start time
	 * @param endTime the end time
	 * @param domainfile a list of domains to cluster regardless of candidate
	 * 		score
	 * @param linkage the linkage type
	 * @param maxCutHeight the max cut height
	 * @return the list of clusters
	 * @throws Exception if there is an error reading the ClusterGenerator.properties
	 * 		or data files
	 */
	private List<DomainCluster> generateClusters(long startTime, long endTime, String domainfile,
			LinkageType linkage, double maxCutHeight) throws Exception{
		ArrayList<DomainCluster> retval = new ArrayList<DomainCluster>();
		if(log.isInfoEnabled()){
			log.info(this.getClass().getSimpleName() + " Started: " 
					+ Calendar.getInstance().getTime());
			log.info("Loading Candidate Flux Domains.");
		}
		List<CandidateFluxDomain> cfdList = loadCandidateFluxDomains(startTime,
				endTime, domainfile);
		
		if(log.isInfoEnabled()){
			log.info("Loaded " + cfdList.size() + " Candidate Flux Domains.");
		}
		if (cfdList.size() > 0) {
			if(log.isInfoEnabled()){
				log.info("Computing Distance Matrix.");
			}
			Vector<Float> utDistValues = this.computeDistanceMatrix(cfdList);
			DistanceMatrix distMatrix = new DistanceMatrix(utDistValues);
			if(log.isInfoEnabled()){
				log.info("Distance Matrix Calculated.");
			}
			HierarchicalClustering hc = new HierarchicalClustering(linkage);
			if(log.isInfoEnabled()){
				log.info("Running Clusterer.");
			}
			hc.runClusterer(distMatrix, maxCutHeight);
			if(log.isInfoEnabled()){
				log.info("Clustering Completed.");
			}
			Dendrogram dgram = hc.getDendrogram();
			if(log.isInfoEnabled()){
				log.info("Creating Domain Clusters.");
			}
			Vector<HCluster> hclusters = dgram.getClusters(maxCutHeight);
			for (HCluster hcluster : hclusters) {
				DomainCluster dm = new DomainCluster();
				for (int index : hcluster.getIndexes()) {
					dm.addCandidateFluxDomain(cfdList.get(index));
				}
				retval.add(dm);
			}
			if(log.isInfoEnabled()){
				for(DomainCluster d : retval){
					log.info(d.toString());
				}
				log.info("Created " + retval.size() + " Domain Clusters.");
				log.info(this.getClass().getSimpleName() + " Finished: " 
						+ Calendar.getInstance().getTime());
				
			}
		}

		return retval;
	}

	
	/**
	 * Store clusters through a db interface loaded by the DBInterfaceFactory.
	 *
	 * @param clusters the list of clusters to store.
	 * @param log_date the clustering run date
	 * @throws Exception if the database interface could not be loaded.
	 */
	public void storeClusters(List<DomainCluster> clusters, 
			Date log_date) throws Exception {
		DBInterface dbiface = DBInterfaceFactory.loadDBInterface();
		if (dbiface == null) {
			throw new Exception("Could not load DB interface.");
		}
		if(log.isInfoEnabled()){
			log.info(this.getClass().getSimpleName() + " Started: " 
					+ Calendar.getInstance().getTime());
			log.info("Storing " + clusters.size() + " Clusters.");
		}
		dbiface.storeBasicDnsFeatures(clusters, "SIE", log_date);
		if(log.isInfoEnabled()){
			log.info("Clusters stored.");
			log.info(this.getClass().getSimpleName() + " Finished: " 
					+ Calendar.getInstance().getTime());
		}
	}
	
	/**
	 * Prints each each cluster in the list to stdout.
	 *
	 * @param clusters the list clusters to print
	 */
	public void printClusters(List<DomainCluster> clusters){
		for(DomainCluster cluster : clusters){
			System.out.println(cluster);
		}
		
	}
}
