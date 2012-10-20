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

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * This class contains a set of utility methods for manipulating
 * IP addresses.
 * 
 * @author Chris Neasbitt
 */
public class IPDiversityCalculator {

	/**
	 * Calculates the Kullback–Leibler divergence of a set of 
	 * version 4 IP addresses.
	 *
	 * @param addrs the set of addresses
	 * @return the metric value
	 */
	public static double ipDiversity(Set<Inet4Address> addrs) {
		double retval = 0.0;
		HashMap<String, Long> ipGroups = new HashMap<String, Long>();

		if (addrs.size() < 2) {
			return retval;
		}

		for (Inet4Address addr : addrs) {
			byte[] ipbytes = addr.getAddress();
			String temp = ipbytes[0] + "." + ipbytes[1];
			if (ipGroups.containsKey(temp)) {
				ipGroups.put(temp, ipGroups.get(temp) + 1);
			} else {
				ipGroups.put(temp, 1L);
			}
		}

		ArrayList<Double> probs = new ArrayList<Double>();
		for (String ipGroup : ipGroups.keySet()) {
			probs.add(((double) ipGroups.get(ipGroup)) / addrs.size());
		}

		double entropy = 0.0;
		for (double prob : probs) {
			entropy -= prob * (Math.log(prob) / Math.log(2)); // lg prob
		}

		retval = entropy / (Math.log(addrs.size()) / Math.log(2)); // lg addrs.size()

		return retval;
	}

	/**
	 * Extracts the version 4 IP addresses of a list of InetAddress
	 * objects.
	 *
	 * @param ips the set of IP addresses
	 * @return a set of version 4 IP addresses
	 */
	public static Set<Inet4Address> getV4Ips(Set<InetAddress> ips) {
		HashSet<Inet4Address> retval = new HashSet<Inet4Address>();
		for (InetAddress ip : ips) {
			if (ip instanceof Inet4Address) {
				retval.add((Inet4Address) ip);
			}
		}
		return retval;
	}
}
