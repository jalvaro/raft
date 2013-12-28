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
	private long term;
	private String leaderId;	
	
	public ServerResult (String groupId, String hostId, Recipes recipes, List<LogEntry> log, long term, String leaderId){
		this.groupId = groupId;
		this.hostId = hostId;
		this.recipes = recipes.clone();
		this.log = (List<LogEntry>) ((Vector<LogEntry>) log).clone();
		this.term = term;
		this.leaderId = leaderId;
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
	public List<LogEntry> getLog() {
		return log;
	}
	
	@Override
	public String toString() {
		return "ServerResult [\ngroupId=" + groupId + "\n"+
				"hostId=" + hostId	+ "\n"+
				"recipes=" + recipes + "\n"+
				"log=" + log + "\n"+
				"term=" + term + "\n"+
				"leaderId=" + leaderId + "]";
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
		if (term != other.term)
			return false;
		if (leaderId == null) {
			if (other.leaderId != null)
				return false;
		} else if (!leaderId.equals(other.leaderId))
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
