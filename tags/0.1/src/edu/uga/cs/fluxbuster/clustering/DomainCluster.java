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

import java.io.Serializable;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class represents a cluster of CandidateFluxDomains.  DomainClusters
 * are the results of hierarchical clustering of CandidateFluxDomains
 * 
 * @author Chris Neasbitt
 */
public class DomainCluster implements Serializable {

	private static final long serialVersionUID = 7189396121418308685L;

	private ArrayList<CandidateFluxDomain> candidateDomains = null;
	
	private HashSet<String> domains = null;
	
	private HashSet<InetAddress> ips = null;
	
	private double ipDiversity = 0;
	
	private long queries = 0;
	
	private ArrayList<Double> avgTTLs = null;
	
	private ArrayList<Double> growthRatios = null;
	
	private ArrayList<Double> lastGrowthRatioSingleEntries = null;

	private ArrayList<Set<InetAddress>> lastGrowthEntriesIPs = null;
	
	private ArrayList<Long> lastGrowthEntriesQueries = null;

	private HashSet<InetAddress> lastGrowthClusterIPs = null;
	
	private long lastGrowthClusterQueries = 0;
	
	private static Log log = LogFactory.getLog(DomainCluster.class);

	/**
	 * Instantiates a new empty domain cluster.
	 */
	public DomainCluster() {
		candidateDomains = new ArrayList<CandidateFluxDomain>();
		domains = new HashSet<String>();
		ips = new HashSet<InetAddress>();
		avgTTLs = new ArrayList<Double>();
		growthRatios = new ArrayList<Double>();
		lastGrowthRatioSingleEntries = new ArrayList<Double>();
		lastGrowthClusterIPs = new HashSet<InetAddress>();
		lastGrowthEntriesIPs = new ArrayList<Set<InetAddress>>();
		lastGrowthEntriesQueries = new ArrayList<Long>();
	}

	/**
	 * Adds the candidate flux domain to the cluster
	 *
	 * @param cfd the candidate flux domain to add
	 */
	public void addCandidateFluxDomain(CandidateFluxDomain cfd) {
		this.candidateDomains.add(cfd);
		this.domains.add(cfd.getDomainName());
		this.ips.addAll(cfd.getIps());

		// NOTE bases diversity solely on IPv4 addresses
		this.setIpDiversity(IPDiversityCalculator
				.ipDiversity(IPDiversityCalculator.getV4Ips(ips)));

		this.queries += cfd.getNumQueries();
		this.avgTTLs.add(cfd.getAvgTTL());
		this.growthRatios.add((double) cfd.getNumIPs()
				/ (double) cfd.getNumQueries());

		if (cfd.getLastGrowthRatioSingleEntry() != null) {
			this.lastGrowthRatioSingleEntries.add(cfd
					.getLastGrowthRatioSingleEntry());
		}

		if (cfd.getLastGrowthEntriesIPs().size() > 0) {
			this.lastGrowthEntriesIPs.add(cfd.getLastGrowthEntriesIPs());
			this.lastGrowthEntriesQueries
					.add(cfd.getLastGrowthEntriesQueries());
		}

		if (this.candidateDomains.size() > 1) {
			Collections.sort(this.candidateDomains,
					new Comparator<CandidateFluxDomain>() {
						@Override
						public int compare(CandidateFluxDomain arg0,
								CandidateFluxDomain arg1) {
							return arg0.getLastSeen().compareTo(
									arg1.getLastSeen());
						}
					});

			HashSet<InetAddress> prevIps = new HashSet<InetAddress>();
			for (int i = 0; i < this.candidateDomains.size() - 1; i++) {
				prevIps.addAll(this.candidateDomains.get(i).getIps());
			}
			CandidateFluxDomain lastCFD = this.candidateDomains
					.get(this.candidateDomains.size() - 1);
			HashSet<InetAddress> temp = new HashSet<InetAddress>();

			temp.addAll(lastCFD.getIps());
			temp.removeAll(prevIps);

			this.lastGrowthClusterIPs = temp;
			this.lastGrowthClusterQueries = lastCFD.getNumQueries();
		}
	}
	
	/**
	 * Gets the queries per domain of the cluster.
	 *
	 * @return the queries per domain
	 */
	public double getQueriesPerDomain(){
		return this.queries / (double)this.domains.size();
	}
	
	/**
	 * Gets the last growth prefix ratio of the cluster.
	 *
	 * @return the last growth prefix ratio or null if the
	 * 		feature can not be calculated
	 */
	public Double getLastGrowthPrefixRatioCluster(){
		if(lastGrowthClusterIPs.size() > 0){
			return getPrefixes24(lastGrowthClusterIPs).size() / (double)lastGrowthClusterQueries;
		} else {
			return null;
		}		
	}
	
	/**
	 * Gets the last growth ratio of the cluster.
	 *
	 * @return the last growth ratio or null if the feature
	 * 		can not be calculated
	 */
	public Double getLastGrowthRatioCluster(){
		if(lastGrowthClusterIPs.size() > 0){
			return lastGrowthClusterIPs.size() / (double)lastGrowthClusterQueries;
		} else {
			return null;
		}
	}
	
	/**
	 * Gets the avg last growth ratio single entry feature 
	 * of the cluster
	 *
	 * @return the avg last growth ratio single entry or null
	 * 		if the feature can not be calculated
	 */
	public Double getAvgLastGrowthRatioSingleEntry(){
		if(lastGrowthRatioSingleEntries.size() > 0){
			double temp = 0;
			for(double d : lastGrowthRatioSingleEntries){
				temp += d;
			}
			return temp/lastGrowthRatioSingleEntries.size();
		} else {
			return null;
		}
	}
	
	/**
	 * Gets the last growth ratio entries list of the cluster.
	 *
	 * @return the list of last growth ratio entries
	 */
	private List<Double> getLastGrowthRatioEntriesList(){
		ArrayList<Double> retval = new ArrayList<Double>();
		if(lastGrowthEntriesIPs.size() > 0){
			for(int i = 0; i < lastGrowthEntriesIPs.size(); i++){
				retval.add(lastGrowthEntriesIPs.get(i).size() / (double)lastGrowthEntriesQueries.get(i));
			}
		}
		return retval;
	}
	
	/**
	 * Gets the last growth prefix ratio entries list of the cluster.
	 *
	 * @return the list of last growth prefix ratio entries
	 */
	private List<Double> getLastGrowthPrefixRatioEntriesList(){
		ArrayList<Double> retval = new ArrayList<Double>();
		if(lastGrowthEntriesIPs.size() > 0){
			for(int i = 0; i < lastGrowthEntriesIPs.size(); i++){
				retval.add(getPrefixes24(lastGrowthEntriesIPs.get(i)).size() / (double)lastGrowthEntriesQueries.get(i));
			}			
		}
		return retval;
	}
	
	/**
	 * Gets the cluster's average last growth ratio entries.
	 *
	 * @return the average last growth ratio entries
	 */
	public Double getAvgLastGrowthRatioEntries(){
		List<Double> list = getLastGrowthRatioEntriesList();
		if(list.size() > 0){
			double temp = 0;
			for(double d : list){
				temp += d;
			}
			return temp / list.size();
		} else {
			return null;
		}
	}

	/**
	 * Gets the cluster's average last growth prefix ratio entries.
	 *
	 * @return the average last growth prefix ratio entries
	 */
	public Double getAvgLastGrowthPrefixRatioEntries(){
		List<Double> list = getLastGrowthPrefixRatioEntriesList();
		if(list.size() > 0){
			double temp = 0;
			for(double d : list){
				temp += d;
			}
			return temp / list.size();
		} else {
			return null;
		}
	}

	/**
	 * Gets the candidate flux domains in the cluster.
	 *
	 * @return the list of candidate flux domains
	 */
	public List<CandidateFluxDomain> getCandidateDomains() {
		ArrayList<CandidateFluxDomain> retval = new ArrayList<CandidateFluxDomain>();
		retval.addAll(candidateDomains);
		return retval;
	}

	/**
	 * Sets the cluster's candidate flux domains.
	 *
	 * @param candidateDomains the list of candidate flux domains
	 */
	public void setCandidateDomains(List<CandidateFluxDomain> candidateDomains) {
		this.candidateDomains.clear();
		this.candidateDomains.addAll(candidateDomains);
	}

	/**
	 * Gets the domain names in the cluster.
	 *
	 * @return the set of domain names
	 */
	public Set<String> getDomains() {
		HashSet<String> retval = new HashSet<String>();
		retval.addAll(this.domains);
		return retval;
	}

	/**
	 * Sets the domain names in the cluster.
	 *
	 * @param domains the set of domains
	 */
	public void setDomains(Set<String> domains) {
		this.domains.clear();
		this.domains.addAll(domains);
	}

	/**
	 * Gets the set of IP addresses in the cluster.
	 *
	 * @return the set of the IP addresses
	 */
	public Set<InetAddress> getIps() {
		HashSet<InetAddress> retval = new HashSet<InetAddress>();
		retval.addAll(this.ips);
		return retval;
	}

	/**
	 * Sets the IP addresses in the cluster.
	 *
	 * @param ips the set of the IP addresses
	 */
	public void setIps(Set<InetAddress> ips) {
		this.ips.clear();
		this.ips.addAll(ips);
	}

	/**
	 * Gets the ip diversity of the cluster.
	 *
	 * @return the ip diversity
	 */
	public double getIpDiversity() {
		return ipDiversity;
	}

	/**
	 * Sets the ip diversity of the cluster.
	 *
	 * @param ipDiversity the ip diversity
	 */
	public void setIpDiversity(double ipDiversity) {
		this.ipDiversity = ipDiversity;
	}

	/**
	 * Gets the number of dns queries in the cluster.
	 *
	 * @return the number of dns queries
	 */
	public long getQueries() {
		return queries;
	}

	/**
	 * Sets the number of dns queries in the cluster.
	 *
	 * @param queries the number of dns queries
	 */
	public void setQueries(long queries) {
		this.queries = queries;
	}

	/**
	 * Gets the list of average ttls in the cluster.
	 *
	 * @return the list average ttls
	 */
	public List<Double> getAvgTTLs() {
		return avgTTLs;
	}
	
	/**
	 * Gets the average ttl per domain of the cluster.
	 * 
	 * @return the avergae ttl
	 */
	
	public double getAvgTTLPerDomain(){
		List<Double> ttls = this.getAvgTTLs();
        double ttl_per_domain = 0;
        if(ttls.size() > 0){
            double temp = 0.0;
            for(double ttl : ttls){
            	temp += ttl;
            }
            ttl_per_domain = temp/ttls.size();
        }
        return ttl_per_domain;
	}

	/**
	 * Sets the list of average ttls in the cluster.
	 *
	 * @param avgTTLs the cluster's average ttls
	 */
	public void setAvgTTLs(ArrayList<Double> avgTTLs) {
		this.avgTTLs = avgTTLs;
	}

	/**
	 * Gets the growth ratios for the cluster.
	 *
	 * @return the list of growth ratios
	 */
	public List<Double> getGrowthRatios() {
		ArrayList<Double> retval = new ArrayList<Double>();
		retval.addAll(this.growthRatios);
		return retval;
	}
	
	public double getIpGrowthRatio(){
        List<Double> ratios = this.getGrowthRatios();
        double ip_growth_ratio = 0;
        if(ratios.size() > 0){
        	double temp = 0.0;
            for(double ratio : ratios){
            	temp += ratio;
            }
            ip_growth_ratio = temp/ratios.size();
        }
        return ip_growth_ratio;
	}

	/**
	 * Sets the growth ratios for the cluster.
	 *
	 * @param growthRatios the list growth ratios
	 */
	public void setGrowthRatios(List<Double> growthRatios) {
		this.growthRatios.clear();
		this.growthRatios.addAll(growthRatios);
	}

	/**
	 * Gets the last growth ratio single entries feature for
	 * this cluster.
	 *
	 * @return the list of last growth ratio single entries
	 */
	public List<Double> getLastGrowthRatioSingleEntries() {
		ArrayList<Double> retval = new ArrayList<Double>();
		retval.addAll(this.lastGrowthRatioSingleEntries);
		return retval;
	}

	/**
	 * Sets the last growth ratio single entries feature for
	 * this cluster.
	 *
	 * @param lastGrowthRatioSingleEntries the list of last growth 
	 * 		ratio single entries
	 */
	public void setLastGrowthRatioSingleEntries(
			List<Double> lastGrowthRatioSingleEntries) {
		this.lastGrowthRatioSingleEntries.clear();
		this.lastGrowthRatioSingleEntries.addAll(lastGrowthRatioSingleEntries);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();

		ArrayList<String> domains = new ArrayList<String>();
		HashSet<InetAddress> ipset = new HashSet<InetAddress>();
		ArrayList<InetAddress> ips = new ArrayList<InetAddress>();
		for (CandidateFluxDomain cfd : this.getCandidateDomains()) {
			domains.add(cfd.getDomainName());
			ipset.addAll(cfd.getIps());
		}
		ips.addAll(ipset);
		Collections.sort(domains);
		Collections.sort(ips, new Comparator<InetAddress>(){
			@Override
			public int compare(InetAddress o1, InetAddress o2) {
				String addr1 = null, addr2 = null;
				if(o1 instanceof Inet4Address){
					addr1 = formatInetv4(o1);
				} else {
					addr1 = o1.getHostAddress();
				}
				
				if(o2 instanceof Inet4Address){
					addr2 = formatInetv4(o2);
				} else {
					addr2 = o2.getHostAddress();
				}

				return addr1.compareTo(addr2);
			}
			
			
			private String formatInetv4(InetAddress o1){
				StringBuffer addr1str = new StringBuffer();
				byte[] addr1 = o1.getAddress();
				for(byte b : addr1){
                    int val = 0;
                    if(b < 0){
                            val = 256 + b;
                    } else {
                            val = b;
                    }
                    addr1str.append(String.format("%03d" , val));
				}
				return addr1str.toString();
			}
		});
		
		buf.append("Domains: \n");
		for(String domain : domains){
			buf.append("\t" + domain + "\n");			
		}
		
		buf.append("IP's: \n");
		for(InetAddress ip : ips){
			buf.append("\t" + ip.getHostAddress() + "\n");
		}
		
		buf.append("Query Volume: " + this.getQueries() + "\n");
		buf.append("Distinct IPs: " + this.getIps().size() + "\n");
		buf.append("IP Diversity: " + this.getIpDiversity() + "\n");

		double sumAvgTTL = 0.0;
		for (double avgTTL : this.getAvgTTLs()) {
			sumAvgTTL += avgTTL;
		}
		buf.append("Average TTL: " + sumAvgTTL / this.getAvgTTLs().size()
				+ "\n");

		double sumGrowthRatios = 0.0;
		for (double growthRatio : this.getGrowthRatios()) {
			sumGrowthRatios += growthRatio;
		}
		buf.append("Average Growth Ratio: " + sumGrowthRatios
				/ this.getGrowthRatios().size() + "\n");

		buf.append("Candidate Flux Domains:\n");
		for(CandidateFluxDomain d : this.getCandidateDomains()){
			buf.append(d.toString());
		}
		
		return buf.toString();
	}
	
	
	/**
	 * Gets the CIDR /24 prefixes of a set of IPv4 addresses
	 *
	 * @param ips the set of ip addresses
	 * @return the set of /24 prefixes
	 */
	private Set<InetAddress> getPrefixes24(Set<InetAddress> ips){
		HashSet<InetAddress> retval = new HashSet<InetAddress>();
		Set<Inet4Address> ipsv4 = IPDiversityCalculator.getV4Ips(ips);
		for(Inet4Address ip : ipsv4){
			byte[] temp = ip.getAddress();
			temp[3] = 0;
			try {
				retval.add(InetAddress.getByAddress(temp));
			} catch (UnknownHostException e) {
				if(log.isErrorEnabled()){
					log.error("Error getting CIDR /24 prefix", e);
				}
			}
		}
		return retval;
	}

}
