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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.uga.cs.fluxbuster.utils.DomainNameUtils;

/**
 * This class represents a single domain to be used as the input
 * for clustering.
 * 
 * @author Chris Neasbitt
 */
public class CandidateFluxDomain implements Serializable {

	private static final long serialVersionUID = -3413879070817131906L;
	
	private String domainName = null;
	// sample date to be parsed 2010-10-08 20:44:41
	private Date firstSeen = null, lastSeen = null, reportedAt = null;
	
	private double avgTTL;
	
	// need Double object to test for null if doesnt exist
	private Double lastGrowthRatioSingleEntry = null;
	
	private long numMessages, numQueries, minTTL, maxTTL,
			lastGrowthEntriesQueries;
	
	private HashSet<InetAddress> ips = null, lastGrowthEntriesIPs = null;

	/**
	 * Instantiates a new candidate flux domain.
	 *
	 * @param domainName the domain name
	 * @param firstSeen the date first seen in the input data
	 * @param lastSeen the date last seen in the input data
	 * @param reportedAt the date when the domain was reported in
	 * 		the input data
	 * @param numMessages the num messages
	 * @param numQueries the number of dns queries for the domain
	 * @param minTTL the min ttl of a dns query to the domain
	 * @param maxTTL the max ttl of a dns query to the domain
	 * @param avgTTL the avg ttl of a dns query to the domain
	 * @param lastGrowthRatioSingleEntry the last growth ratio single entry
	 * 		feature of the domain 
	 * @param ips the list of IP addresses to which the domain name
	 * 		resolved in the input data
	 * @param lastGrowthEntriesIPs the last growth entries IPs feature
	 * 		of the domain
	 * @param lastGrowthEntriesQueries the last growth entries queries feature
	 * 		of the domain
	 */
	public CandidateFluxDomain(String domainName, Date firstSeen,
			Date lastSeen, Date reportedAt, long numMessages, long numQueries,
			long minTTL, long maxTTL, double avgTTL,
			Double lastGrowthRatioSingleEntry, Set<InetAddress> ips,
			Set<InetAddress> lastGrowthEntriesIPs, long lastGrowthEntriesQueries) {
		this.ips = new HashSet<InetAddress>();
		this.lastGrowthEntriesIPs = new HashSet<InetAddress>();
		this.setDomainName(domainName);
		this.setFirstSeen(firstSeen);
		this.setLastSeen(lastSeen);
		this.setReportedAt(reportedAt);
		this.setNumMessages(numMessages);
		this.setNumQueries(numQueries);
		this.setMinTTL(minTTL);
		this.setMaxTTL(maxTTL);
		this.setAvgTTL(avgTTL);
		this.setLastGrowthRatioSingleEntry(lastGrowthRatioSingleEntry);
		this.setIps(ips);
		this.setLastGrowthEntriesIPs(lastGrowthEntriesIPs);
		this.setLastGrowthEntriesQueries(lastGrowthEntriesQueries);
	}

	/**
	 * Creates a CandidateFluxDomain object from a line in the input
	 * data.
	 *
	 * @param logline the log line
	 * @return the candidate flux domain
	 * @throws Exception if there is an error parsing the log line
	 */
	public static CandidateFluxDomain parseFromLog(String logline)
			throws Exception {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		String[] elems = logline.split(" ");

		String domainName = DomainNameUtils.stripDots(elems[0].trim());
		long numMessages = Long.parseLong(elems[1].trim());
		long numQueries = Long.parseLong(elems[2].trim());
		double avgTTL = Double.parseDouble(elems[3].trim());
		long minTTL = Long.parseLong(elems[4].trim());
		long maxTTL = Long.parseLong(elems[5].toString());

		Date firstSeen = df.parse(elems[6].trim() + " " + elems[7].trim());
		Date lastSeen = df.parse(elems[8].trim() + " " + elems[9].trim());

		String temp = elems[11].split("\\.")[0].trim();
		Date reportedAt = df.parse(elems[10] + " " + temp);

		long totalNumIps = Long.parseLong(elems[12].trim());
		long otherIps = 0;
		HashSet<InetAddress> ips = new HashSet<InetAddress>();
		Pattern ipregex = Pattern
				.compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
		int i;
		for (i = 13; i < 13 + totalNumIps; i++) {
			Matcher ipmatcher = ipregex.matcher(elems[i]);
			ipmatcher.find();
			String ipstr = ipmatcher.group(0);
			InetAddress addr = InetAddress
					.getByAddress(convertIPv4StringToByteArray(ipstr));

			if (isPublicIP((Inet4Address) addr)) {
				ips.add(addr);
			} else {
				otherIps++;
			}
		}

		double publicIPsRatio = ips.size() / (double) (ips.size() + otherIps);
		ArrayList<Double> growth = new ArrayList<Double>();
		Pattern digitregex = Pattern.compile("\\d+");
		for (; i < elems.length; i++) {
			Matcher digitmatcher = digitregex.matcher(elems[i]);
			digitmatcher.find();
			String digitstr = digitmatcher.group(0);
			growth.add(Integer.parseInt(digitstr) * publicIPsRatio);
		}

		Double lastGrowthRatioSingleEntry = null;
		if (numMessages > 1) {
			double avgQueriesPerMsg = numQueries / (double) numMessages;
			lastGrowthRatioSingleEntry = (growth.get(growth.size() - 1) - growth
					.get(growth.size() - 2)) / avgQueriesPerMsg;
		}

		return new CandidateFluxDomain(domainName, firstSeen, lastSeen,
				reportedAt, numMessages, numQueries, minTTL, maxTTL, avgTTL,
				lastGrowthRatioSingleEntry, ips, new HashSet<InetAddress>(), 0);
	}

	// the parameter succeeds the calling object
	/**
	 * Create a new CandidateFluxDomain object by merging two CandidateFluxDomain
	 * objects describing the same domain name.  
	 *
	 * @param cfd the CandidateFluxDomain object which succeeds in time
	 * 		this CandidateFluxDomain.
	 * @return the merged CandidateFluxDomain object
	 */
	public CandidateFluxDomain merge(CandidateFluxDomain cfd) {

		String domainname = this.getDomainName();
		long numMessages = this.getNumMessages() + cfd.getNumMessages();
		long numQueries = this.getNumQueries() + cfd.getNumQueries();
		long minTTL = Math.min(this.getMinTTL(), cfd.getMinTTL());
		long maxTTL = Math.max(this.getMaxTTL(), cfd.getMaxTTL());

		Date firstSeen = null;
		if (this.getFirstSeen().before(cfd.getFirstSeen())) {
			firstSeen = this.getFirstSeen();
		} else {
			firstSeen = cfd.getFirstSeen();
		}

		Date lastSeen = null;
		if (this.getLastSeen().after(cfd.getLastSeen())) {
			lastSeen = this.getLastSeen();
		} else {
			lastSeen = cfd.getLastSeen();
		}

		Date reportedAt = null;
		if (this.getReportedAt().after(cfd.getReportedAt())) {
			reportedAt = this.getReportedAt();
		} else {
			reportedAt = cfd.getReportedAt();
		}

		double avgTTL = (this.getAvgTTL() * this.getNumQueries() + cfd
				.getAvgTTL() * cfd.getNumQueries())
				/ numQueries;

		Double lastGrowthRatioSingleEntry = cfd.getLastGrowthRatioSingleEntry();

		HashSet<InetAddress> ips = new HashSet<InetAddress>();
		ips.addAll(this.getIps());
		ips.addAll(cfd.getIps());

		HashSet<InetAddress> lastGrowthEntriesIPs = new HashSet<InetAddress>();
		lastGrowthEntriesIPs.addAll(cfd.getIps());
		lastGrowthEntriesIPs.removeAll(this.getIps());

		long lastGrowthEntriesQueries = cfd.getNumQueries();

		return new CandidateFluxDomain(domainname, firstSeen, lastSeen,
				reportedAt, numMessages, numQueries, minTTL, maxTTL, avgTTL,
				lastGrowthRatioSingleEntry, ips, lastGrowthEntriesIPs,
				lastGrowthEntriesQueries);
	}

	// network byte order
	/**
	 * Convert a version 4 IP address string into a byte array
	 * in network byte order.
	 *
	 * @param ipstr the IP address string
	 * @return the byte array version of the address
	 * @throws Exception if the address is not a valid IPv4 address
	 */
	private static byte[] convertIPv4StringToByteArray(String ipstr)
			throws Exception {
		String[] parts = ipstr.split("\\.");
		if (parts.length != 4) {
			throw new Exception("Invalid IPv4 address");
		}
		byte[] bytes = new byte[4];
		for (int i = 0; i < bytes.length; i++) {
			bytes[i] = (byte) Integer.parseInt(parts[i]);
		}
		return bytes;
	}

	// IANA private address
	// 10.0.0.0 – 10.255.255.255 (Total Addresses: 16,777,216)
	// 172.16.0.0 – 172.31.255.255 (Total Addresses: 1,048,576)
	// 192.168.0.0 – 192.168.255.255 (Total Addresses: 65,536)
	/**
	 * Determine if the supplied version 4 IP address is in the
	 * public range.
	 *
	 * @param addr the IP address
	 * @return true, if it is a public ip
	 */
	private static boolean isPublicIP(Inet4Address addr) {
		boolean retval = true;
		byte[] ipbytes = addr.getAddress();
		if (ipbytes[0] == 10) {
			retval = false;
		}
		if (ipbytes[0] == 172 && ipbytes[1] >= 16 && ipbytes[1] <= 31) {
			retval = false;
		}
		if (ipbytes[0] == 192 && ipbytes[1] == 168) {
			retval = false;
		}
		return retval;
	}

	/**
	 * Gets the domain name.
	 *
	 * @return the domain name
	 */
	public String getDomainName() {
		return domainName;
	}

	/**
	 * Sets the domain name.
	 *
	 * @param domainName the domain name
	 */
	public void setDomainName(String domainName) {
		this.domainName = domainName;
	}

	/**
	 * Gets the first seen date.
	 *
	 * @return the first seen date
	 */
	public Date getFirstSeen() {
		return firstSeen;
	}

	/**
	 * Sets the first seen date.
	 *
	 * @param firstSeen the first seen
	 */
	public void setFirstSeen(Date firstSeen) {
		this.firstSeen = firstSeen;
	}

	/**
	 * Gets the last seen date.
	 *
	 * @return the last seen date
	 */
	public Date getLastSeen() {
		return lastSeen;
	}

	/**
	 * Sets the last seen date.
	 *
	 * @param lastSeen the last seen date
	 */
	public void setLastSeen(Date lastSeen) {
		this.lastSeen = lastSeen;
	}

	/**
	 * Gets the reported at date
	 *
	 * @return the reported at date
	 */
	public Date getReportedAt() {
		return reportedAt;
	}

	/**
	 * Sets the reported at date.
	 *
	 * @param reportedAt the reported at date
	 */
	public void setReportedAt(Date reportedAt) {
		this.reportedAt = reportedAt;
	}

	/**
	 * Gets the avg ttl.
	 *
	 * @return the avg ttl
	 */
	public double getAvgTTL() {
		return avgTTL;
	}

	/**
	 * Sets the avg ttl.
	 *
	 * @param avgTTL the new avg ttl
	 */
	public void setAvgTTL(double avgTTL) {
		this.avgTTL = avgTTL;
	}

	// NOTE this can be null;
	/**
	 * Gets the last growth ratio single entry feature.
	 *
	 * @return the last growth ratio single entry feature
	 */
	public Double getLastGrowthRatioSingleEntry() {
		return lastGrowthRatioSingleEntry;
	}

	/**
	 * Sets the last growth ratio single entry feature.
	 *
	 * @param lastGrowthRatioSingleEntry the last growth ratio 
	 * 		single entry feature
	 */
	public void setLastGrowthRatioSingleEntry(Double lastGrowthRatioSingleEntry) {
		this.lastGrowthRatioSingleEntry = lastGrowthRatioSingleEntry;
	}

	/**
	 * Gets the number of messages.
	 *
	 * @return the number of messages
	 */
	public long getNumMessages() {
		return numMessages;
	}

	/**
	 * Sets the number of messages.
	 *
	 * @param numMessages the number of messages
	 */
	public void setNumMessages(long numMessages) {
		this.numMessages = numMessages;
	}

	/**
	 * Gets the number of dns queries.
	 *
	 * @return the number of dns queries
	 */
	public long getNumQueries() {
		return numQueries;
	}

	/**
	 * Sets the number of dns queries.
	 *
	 * @param numQueries the number of dns queries
	 */
	public void setNumQueries(long numQueries) {
		this.numQueries = numQueries;
	}

	/**
	 * Gets the min ttl.
	 *
	 * @return the min ttl
	 */
	public long getMinTTL() {
		return minTTL;
	}

	/**
	 * Sets the min ttl.
	 *
	 * @param minTTL the new min ttl
	 */
	public void setMinTTL(long minTTL) {
		this.minTTL = minTTL;
	}

	/**
	 * Gets the max ttl.
	 *
	 * @return the max ttl
	 */
	public long getMaxTTL() {
		return maxTTL;
	}

	/**
	 * Sets the max ttl.
	 *
	 * @param maxTTL the new max ttl
	 */
	public void setMaxTTL(long maxTTL) {
		this.maxTTL = maxTTL;
	}

	/**
	 * Gets the number of IP addresses.
	 *
	 * @return the number of IP addresses
	 */
	public int getNumIPs() {
		return ips.size();
	}

	/**
	 * Gets the set of IP addresses.
	 *
	 * @return the set of IP addresses
	 */
	public Set<InetAddress> getIps() {
		HashSet<InetAddress> retval = new HashSet<InetAddress>();
		retval.addAll(ips);
		return retval;
	}

	/**
	 * Sets the IP addresses.
	 *
	 * @param ips the set of IP addresses
	 */
	public void setIps(Set<InetAddress> ips) {
		this.ips.clear();
		this.ips.addAll(ips);
	}

	/**
	 * Sets the last growth entries IPs.
	 *
	 * @param ips the last growth entries IPs
	 */
	public void setLastGrowthEntriesIPs(Set<InetAddress> ips) {
		this.lastGrowthEntriesIPs.clear();
		this.lastGrowthEntriesIPs.addAll(ips);
	}

	/**
	 * Gets the last growth entries IPs.
	 *
	 * @return the last growth entries IPs
	 */
	public Set<InetAddress> getLastGrowthEntriesIPs() {
		HashSet<InetAddress> retval = new HashSet<InetAddress>();
		retval.addAll(lastGrowthEntriesIPs);
		return retval;
	}

	/**
	 * Sets the last growth entries queries feature.
	 *
	 * @param lastGrowthEntriesQueries the new last growth entries 
	 * 		queries value
	 */
	public void setLastGrowthEntriesQueries(long lastGrowthEntriesQueries) {
		this.lastGrowthEntriesQueries = lastGrowthEntriesQueries;
	}

	/**
	 * Gets the last growth entries queries feature.
	 *
	 * @return the last growth entries queries value
	 */
	public long getLastGrowthEntriesQueries() {
		return this.lastGrowthEntriesQueries;
	}
	
	/**
	 * Returns a string representing this CandidateFluxDomain.
	 * 
	 * @return a string representing this CandidateFluxDomain.
	 */
	@Override
	public String toString(){
		String retval = "";
		retval += "Domain name: " + this.getDomainName() + "\n";
		retval += "First Seen: " + this.getFirstSeen() + "\n";
		retval += "Last Seen: " + this.getLastSeen() + "\n";
		retval += "Reported At: " + this.getReportedAt() + "\n";
		retval += "Min TTL: " + this.getMinTTL() + "\n";
		retval += "Max TTL: " + this.getMaxTTL() + "\n";
		retval += "Avg TTL: " + this.getAvgTTL() + "\n";
		retval += "Num of Messages: " + this.getNumMessages() + "\n";
		retval += "Num of Queries: " + this.getNumQueries() + "\n";
		retval += "Last Growth Ratio Single Entry: " + this.getLastGrowthRatioSingleEntry() + "\n";
		retval += "Last Growth Entries Queries: " + this.getLastGrowthEntriesQueries() + "\n";
		retval += "IP's :\n";
		for(InetAddress ip : this.getIps()){
			retval += "\t" + ip + "\n";
		}
		retval += "Last Growth IP's :\n";
		for(InetAddress ip : this.getLastGrowthEntriesIPs()){
			retval += "\t" + ip + "\n";
		}
		return retval;
	}
}
