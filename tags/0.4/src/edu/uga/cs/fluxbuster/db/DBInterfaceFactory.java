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

import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;

import edu.uga.cs.fluxbuster.utils.PropertiesUtils;

/**
 * A factory for creating DBInterface objects based on a configured properties.
 * 
 * @author Chris Neasbitt
 */
public final class DBInterfaceFactory {

	private static Properties properties = null;
	
	@SuppressWarnings("rawtypes")
	private static Class dbifaceClass = null;
	
	private static final String DBCLASSKEY = "DBINTERFACE_CLASS";
	
	private static final String DBDRIVERKEY = "DBINTERFACE_DRIVER";
	
	private static final String DBCONNECTKEY = "DBINTERFACE_CONNECTINFO";
	
	private static final String DBPARTKEY = "DBINTERFACE_PARTITIONS";
	
	private static final String DBMINCONKEY = "DBINTERFACE_MIN_CON_PER_PART";
	
	private static final String DBMAXCONKEY = "DBINTERFACE_MAX_CON_PER_PART";
	
	private static final String DBRETRYATTEMPTSKEY = "DBINTERFACE_RETRY_ATTEMPTS";
	
	private static final String DBRETRYDELAYKEY = "DBINTERFACE_RETRY_DELAY";
	
	private static BoneCP connectionPool = null;
	
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
			properties = PropertiesUtils.loadAppWideProperties();
		}
	}
	
	/**
	 * Initializes the factory.  This must be called once before use.
	 * 
	 * @throws Exception if an error occurs initializing the factory
	 */
	public static void init() throws Exception{
		loadProperties();
		DBInterfaceFactory.init(properties.getProperty(DBCONNECTKEY), 
				properties.getProperty(DBCLASSKEY));
	}
	
	/**
	 * Initializes the factory with the supplied jdbc url and 
	 * DBInterface implementation.  This must be called once before use.
	 * 
	 * @param dbconnect the JDBC url to use in creating connections
	 * @param dbclassname
	 * @throws Exception if an error occurs initializing the factory
	 */
	public static void init(String dbconnect, String dbclassname) throws Exception{
		loadProperties();
		if(connectionPool == null){
			dbifaceClass = Class.forName(dbclassname);
			Class.forName(properties.getProperty(DBDRIVERKEY));
			BoneCPConfig config = new BoneCPConfig();
			config.setJdbcUrl(dbconnect);
			config.setMinConnectionsPerPartition(
					Integer.parseInt(properties.getProperty(DBMINCONKEY)));
			config.setMaxConnectionsPerPartition(
					Integer.parseInt(properties.getProperty(DBMAXCONKEY)));
			config.setAcquireRetryAttempts(
					Integer.parseInt(properties.getProperty(DBRETRYATTEMPTSKEY)));
			config.setAcquireRetryDelayInMs(
					Long.parseLong(properties.getProperty(DBRETRYDELAYKEY)));
			config.setLogStatementsEnabled(true);
			config.setPartitionCount(
					Integer.parseInt(properties.getProperty(DBPARTKEY)));
			connectionPool = new BoneCP(config);
		}
	}
	
	/**
	 * 
	 */
	public static void shutdown(){
		if(connectionPool != null){
			connectionPool.shutdown();
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
			DBInterfaceFactory.init();
			
			Constructor[] constructors = dbifaceClass.getConstructors();
			Constructor con = null;
			for (Constructor constructor : constructors) {
				Class[] paramtypes = constructor.getParameterTypes();
				if (paramtypes.length == 1
						&& paramtypes[0].isInstance(DBInterfaceFactory.connectionPool)) {
					con = constructor;
				}
			}
			Object obj = con.newInstance(DBInterfaceFactory.connectionPool);
			retval = (DBInterface) obj;
		} catch (Exception e) {
			if(log.isErrorEnabled()){
				log.error("Error loading db interface.", e);
			}
		}
		return retval;
	}
}
