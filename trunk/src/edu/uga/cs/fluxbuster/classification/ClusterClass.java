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

package edu.uga.cs.fluxbuster.classification;

/**
 * This enum represents the different classifications of a cluster.
 * 
 * @author Chris Neasbitt
 */
public enum ClusterClass {
	
	/** Flux classification */
	FLUX{
		/**
		 * Returns the string version of the classification.  This method can be
		 * used to convert the enum into a value that can be stored in the database.
		 * 
		 * @return the string version of the classification
		 */
        @Override
        public String toString() {
            return "Flux";
        }
	},
	
	/** NOT_Flux classification */
	NOT_FLUX{
		/**
		 * Returns the string version of the classification.  This method can be
		 * used to convert the enum into a value that can be stored in the database.
		 * 
		 * @return the string version of the classification
		 */
        @Override
        public String toString() {
            return "NOT_Flux";
        }
	},
	
	/** No classification */
	NONE
}
