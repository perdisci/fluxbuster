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

package edu.uga.cs.fluxbuster.db;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.uga.cs.fluxbuster.utils.PropertiesUtils;

/**
 * A factory for creating DBInterface objects based on a configured properties.
 * 
 * @author Chris Neasbitt
 */
public final class DBInterfaceFactory {

	private static Properties properties = null;
	
	private static final String DBCLASSKEY = "DBINTERFACE_CLASS";
	
	private static final String DBCONNECTKEY = "DBINTERFACE_CONNECTINFO";
	
	private static Log log = LogFactory.getLog(DBInterfaceFactory.class);
	
	/**
	 * Instantiates a new database interface factory.
	 */
	protected DBInterfaceFactory(){}
	
	/**
	 * Load properties.
	 *
	 * @throws IOException if the properties file can not be loaded.
	 */
	private static void loadProperties() throws IOException{
		if(properties == null){
			properties = PropertiesUtils.loadProperties(DBInterfaceFactory.class);
		}
	}
	
	/**
	 * Creates a database interface.
	 *
	 * @return the database interface
	 */
	@SuppressWarnings("rawtypes")
	public static DBInterface loadDBInterface() {
		DBInterface retval = null;
		try {
			loadProperties();
			
			String dbclassname = properties.getProperty(DBCLASSKEY);
			String dbconnect = properties.getProperty(DBCONNECTKEY);
			
			Class cls = Class.forName(dbclassname);
			Constructor[] constructors = cls.getConstructors();
			Constructor con = null;
			for (Constructor constructor : constructors) {
				Class[] paramtypes = constructor.getParameterTypes();
				if (paramtypes.length == 1
						&& paramtypes[0].isInstance(new String())) {
					con = constructor;
				}
			}
			Object obj = con.newInstance(dbconnect);
			retval = (DBInterface) obj;
		} catch (Exception e) {
			if(log.isErrorEnabled()){
				log.error("Error loading db interface.", e);
			}
		}
		return retval;
	}
}
