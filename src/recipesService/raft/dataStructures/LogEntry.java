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

package recipesService.raft.dataStructures;

import java.io.Serializable;

import recipesService.data.Operation;

/**
 * Log Entry
 * 
 * @author Joan-Manuel Marques
 * May 2013
 *
 */

public class LogEntry implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 4197410153405370507L;
	
	
	private long term; // term when entry was received by leader
	private Operation command; // command for state machine
	
	public LogEntry(long term, Operation command){
		this.term = term;
		this.command = command;
	}

	public long getTerm() {
		return term;
	}

	public Operation getCommand() {
		return command;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LogEntry other = (LogEntry) obj;
		if (command == null) {
			if (other.command != null)
				return false;
		} else if (!command.equals(other.command))
			return false;
		if (term != other.term)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "LogEntry [term=" + term + ", command=" + command + "]";
	}
}
