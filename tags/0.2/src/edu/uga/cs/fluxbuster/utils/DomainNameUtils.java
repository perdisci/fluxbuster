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

package edu.uga.cs.fluxbuster.utils;

import com.google.common.net.InternetDomainName;

/**
 * This class provides utilities for manipulating domain names.
 * 
 * @author Chris Neasbitt
 */
public class DomainNameUtils {
	
	/**
	 * Extracts the effective second level domain name.
	 *
	 * @param domainname the full domain name
	 * @return the second level domain name
	 */
	public static String extractEffective2LD(String domainname) 
	{
		String retval = null;
		InternetDomainName idn = InternetDomainName.from(domainname);
		InternetDomainName sld = idn.topPrivateDomain();
		retval = sld.name();
		return retval;
	}
	
	/**
	 * Reverses a domain name.
	 *
	 * @param domainName the domain name
	 * @return the domain name reversed at the periods
	 */
	public static String reverseDomainName(String domainName)
	{
		String reverse = "";
	
		String[] domainParts = domainName.split("\\.");
		
		for(int i=domainParts.length-1; i>=0; i--)
		{
			reverse += domainParts[i];
			if(i!=0)
				reverse+=".";
		}
		
		return reverse;
	}
}
