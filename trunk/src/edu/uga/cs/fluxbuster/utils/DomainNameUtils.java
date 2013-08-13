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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.net.InternetDomainName;

/**
 * This class provides utilities for manipulating domain names.
 * 
 * @author Chris Neasbitt
 */
public class DomainNameUtils {
	
	private static Log log = LogFactory.getLog(DomainNameUtils.class);
	
	/**
	 * Extracts the effective second level domain name.
	 *
	 * @param domainname the full domain name
	 * @return the second level domain name or null on error
	 */
	public static String extractEffective2LD(String domainname) 
	{
		String retval = null;
		try{
			InternetDomainName idn = InternetDomainName.from(domainname);
			InternetDomainName sld = idn.topPrivateDomain();
			retval = sld.name();
		} catch (Exception e) {
			if(log.isDebugEnabled()){
				log.debug("Unable to extract 2LD.", e);
			}
		}
		return retval;
	}
	
	/**
	 * Returns a copy of the domain name with leading and trailing
	 * dots removed.
	 * 
	 * @param domainname the original domain name
	 * @return the stripped of dots
	 */
	public static String stripDots(String domainname){
		String retval = domainname;
		if (retval.endsWith(".")) {
			retval = retval.substring(0, retval.length() - 1);
		}
		if(retval.startsWith(".")){
			retval = retval.substring(1, retval.length());
		}
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
