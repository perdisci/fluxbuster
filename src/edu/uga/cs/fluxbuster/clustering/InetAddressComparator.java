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
import java.net.Inet4Address;
import java.util.Comparator;

/**
 * Provides for the sorting of numerical InetAddress objects in numerical order 
 * before textual InetAddress objects.
 * 
 * @author Chris Neasbitt
 */
public class InetAddressComparator implements Comparator<InetAddress> {
	
	/**
	 * Compares its two InetAddress arguments for order.
	 * 
	 * @return a negative integer, zero, or a positive integer as the first 
	 * 	argument is less than, equal to, or greater than the second.
	 */
	@Override
	public int compare(InetAddress o1, InetAddress o2) {
		String addr1 = null, addr2 = null;
		if(o1 instanceof Inet4Address){
			addr1 = formatInetv4((Inet4Address)o1);
		} else {
			addr1 = o1.getHostAddress();
		}
		
		if(o2 instanceof Inet4Address){
			addr2 = formatInetv4((Inet4Address)o2);
		} else {
			addr2 = o2.getHostAddress();
		}

		return addr1.compareTo(addr2);
	}
	
	
	/**
	 * Formats the Inet4Address for comparison 
	 *
	 * @param o1 the address
	 * @return the formatted comparison string
	 */
	private String formatInetv4(Inet4Address o1){
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
}
