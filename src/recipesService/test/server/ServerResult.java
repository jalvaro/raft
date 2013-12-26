/*
* Copyright (c) Joan-Manuel Marques 2013. All rights reserved.
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
*
* This file is part of the practical assignment of Distributed Systems course.
*
* This code is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This code is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this code.  If not, see <http://www.gnu.org/licenses/>.
*/

package recipesService.test.server;

import java.io.Serializable;
import java.util.List;
import java.util.Vector;

import recipesService.data.Recipes;
import recipesService.raft.dataStructures.LogEntry;

/**
 * @author Joan-Manuel Marques
 * December 2012
 *
 */

public class ServerResult implements Serializable{

	private static final long serialVersionUID = 1334487840616410385L;
	private String groupId;
	private String hostId;
	private Recipes recipes;
	private List<LogEntry> log;
	
	public ServerResult (String groupId, String hostId, Recipes recipes, List<LogEntry> log){
		this.groupId = groupId;
		this.hostId = hostId;
		this.recipes = recipes.clone();
		this.log = (List<LogEntry>) ((Vector<LogEntry>) log).clone();
	}
	
	public String getGroupId(){
		return this.groupId;
	}
	public String getHostId(){
		return this.hostId;
	}
	public Recipes getRecipes() {
		return recipes;
	}
	
//	public String toString(){
//		return "Group id: " + groupId + "\nNode id: " + hostId + "\nRecipes: " + recipes.toString();
//	}

	@Override
	public String toString() {
		return "ServerResult [\ngroupId=" + groupId + "\nhostId=" + hostId
				+ "\nrecipes=" + recipes + "\nlog=" + log + "\n]";
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ServerResult other = (ServerResult) obj;
		if (log == null) {
			if (other.log != null)
				return false;
		} else if (!log.equals(other.log))
			return false;
		if (recipes == null) {
			if (other.recipes != null)
				return false;
		} else if (!recipes.equals(other.recipes))
			return false;
		return true;
	}

	
	
//	@Override
//	public boolean equals(Object obj) {
//		if (this == obj)
//			return true;
//		if (obj == null)
//			return false;
//		if (getClass() != obj.getClass())
//			return false;
//		ServerResult other = (ServerResult) obj;
//		if (recipes == null) {
//			if (other.recipes != null)
//				return false;
//		} else if (!recipes.equals(other.recipes)){
////			System.out.println("ServerResult --- equals: recipes are not equals");
//			return false;
//		}
//		return true;
//	}
}
