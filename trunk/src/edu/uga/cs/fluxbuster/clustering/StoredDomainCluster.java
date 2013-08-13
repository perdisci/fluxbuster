/*
* Copyright (C) 2013 Chris Neasbitt
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

import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import edu.uga.cs.fluxbuster.classification.ClusterClass;

/**
 * This class represents a domain cluster that has been stored in the database.
 * Note: Changes to the cluster in the database are not synchronized with its
 * object representation once the object has been created.
 * 
 * @author Chris Neasbitt
 */
public class StoredDomainCluster {

	private final Set<String> domains; 
	private final Set<InetAddress> ips;
	private final Date logDate;
	private final int clusterId;
	private final ClusterClass clusterClass;
	private final double networkCardinality, ipDiversity, domainsPerNetwork,
		numberOfDomains, ttlPerDomain, ipGrowthRatio;
	

	/**
	 * Instantiates a new stored domain cluster.
	 *
	 * @param clusterId the cluster id
	 * @param logDate the generation date of the cluster
	 * @param domains the cluster domains
	 * @param ips the cluster ips
	 * @param clusterClass the cluster's classification
	 * @param networkCardinality the cluster's network cardinality
	 * @param ipDiversity the cluster's ip diversity
	 * @param domainsPerNetwork the cluster's domains per network
	 * @param numberOfDomains the cluster's number of domains
	 * @param ttlPerDomain the cluster's ttl per domain
	 * @param ipGrowthRatio the cluster's ip growth ratio
	 */
	public StoredDomainCluster(int clusterId, Date logDate, Set<String> domains, 
			Set<InetAddress> ips, ClusterClass clusterClass, double networkCardinality, 
			double ipDiversity, double domainsPerNetwork, double numberOfDomains, 
			double ttlPerDomain, double ipGrowthRatio) {
		this.domains = domains;
		this.ips = ips;
		this.logDate = logDate;
		this.clusterId = clusterId;
		this.clusterClass = clusterClass;
		this.networkCardinality = networkCardinality;
		this.ipDiversity = ipDiversity;
		this.domainsPerNetwork = domainsPerNetwork;
		this.numberOfDomains = numberOfDomains;
		this.ttlPerDomain = ttlPerDomain;
		this.ipGrowthRatio = ipGrowthRatio;
	}
	
	/**
	 * Compares this StoredDomainCluster to the specified object.
	 * 
	 * @return true of the objects are equal, false otherwise.
	 */
	@Override
	public boolean equals(Object obj){
		if(obj instanceof StoredDomainCluster){
			StoredDomainCluster clus = (StoredDomainCluster)obj;
			return clusterClass.equals(clus.getClusterClass()) && 
					clusterId == clus.getClusterId() && 
					logDate.equals(clus.getLogDate()) &&
					domains.equals(clus.getDomains()) &&
					ips.equals(clus.getIps());
		}
		return false;
	}

	/**
	 * Gets the cluster's classification
	 *
	 * @return the cluster classification
	 */
	public ClusterClass getClusterClass() {
		return clusterClass;
	}

	/**
	 * Gets the cluster's id.
	 *
	 * @return the cluster id
	 */
	public int getClusterId() {
		return clusterId;
	}

	/**
	 * Gets the cluster's domains.
	 *
	 * @return the domains
	 */
	public Set<String> getDomains() {
		return domains;
	}

	/**
	 * Gets the cluster's domains per network.
	 *
	 * @return the domains per network
	 */
	public double getDomainsPerNetwork() {
		return domainsPerNetwork;
	}
	
	/**
	 * Gets the cluster's ip diversity.
	 *
	 * @return the ip diversity
	 */
	public double getIpDiversity() {
		return ipDiversity;
	}
	
	/**
	 * Gets the cluster's ip growth ratio.
	 *
	 * @return the ip growth ratio
	 */
	public double getIpGrowthRatio() {
		return ipGrowthRatio;
	}

	/**
	 * Gets the cluster's ips.
	 *
	 * @return the ips
	 */
	public Set<InetAddress> getIps() {
		return ips;
	}

	/**
	 * Gets the cluster's date of generation.
	 *
	 * @return the log date
	 */
	public Date getLogDate() {
		return logDate;
	}

	/**
	 * Gets the cluster's network cardinality.
	 *
	 * @return the network cardinality
	 */
	public double getNetworkCardinality() {
		return networkCardinality;
	}

	/**
	 * Gets the cluster's number of domains.
	 *
	 * @return the number of domains
	 */
	public double getNumberOfDomains() {
		return numberOfDomains;
	}

	/**
	 * Gets the cluster's ttl per domain.
	 *
	 * @return the ttl per domain
	 */
	public double getTtlPerDomain() {
		return ttlPerDomain;
	}

	/**
	 * Returns a string representing this StoredDomainCluster.
	 * 
	 * @return a string representing this StoredDomainCluster.
	 */
	@Override
	public String toString(){
		StringBuffer buf = new StringBuffer();
		SimpleDateFormat dateFormatStr = new SimpleDateFormat("yyyy-MM-dd");
		
		List<String> domainslst = new ArrayList<String>(domains);
		List<InetAddress> ipslst = new ArrayList<InetAddress>(ips);
		Collections.sort(domainslst);
		Collections.sort(ipslst, new InetAddressComparator());
		
		buf.append("Cluster ID: " + clusterId + "\n");
		buf.append("Log Date: " + dateFormatStr.format(logDate) + "\n");
		buf.append("Cluster Class: " + clusterClass + "\n");
		
		buf.append("Domains: \n");
		for(String domain : domainslst){
			buf.append("\t" + domain + "\n");			
		}
		
		buf.append("IP's: \n");
		for(InetAddress ip : ipslst){
			buf.append("\t" + ip.getHostAddress() + "\n");
		}
		
		return buf.toString();
	}
}
