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

import java.io.IOException;
import java.util.Properties;

/**
 * This class provides utilities for loading properties files.
 * 
 * @author Chris Neasbitt
 */
public class PropertiesUtils {

	/**
	 * Load local class properties.
	 *
	 * @param clazz the class of the corresponding properties file
	 * @return the loaded properties
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static Properties loadProperties(Class<?> clazz) throws IOException {
		Properties retval = new Properties();
		String propfilename = clazz.getSimpleName() + ".properties";
		retval.load(clazz.getResourceAsStream(propfilename));
		return retval;
	}
	
	/**
	 * Load application wide properties.
	 *
	 * @return the loaded properties from the fluxbuster.properties file.
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static Properties loadAppWideProperties() throws IOException {
	    Properties configFile = new Properties();
	    configFile.load(PropertiesUtils.class.getClassLoader()
	    		.getResourceAsStream("fluxbuster.properties"));
	    return configFile;
	}
}
