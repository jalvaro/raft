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

import java.util.List;
import java.util.Vector;

import recipesService.data.Operation;

/**
 * Persitent State
 * 
 * @author Joan-Manuel Marques
 * May 2013
 *
 */
public class PersistentState {
	private long currentTerm = 0; // latest term server has seen (initialized to 0)
	private String votedFor; // candidateId that received vote in current term (or null if none)
	private List<LogEntry> log; // log entries

	public PersistentState(){
		currentTerm = 0; // latest term server has seen (initialized to 0)
		votedFor = null;
		log = new Vector<LogEntry>();
	}
	
	public long getCurrentTerm() {
		return currentTerm;
	}
	public void setCurrentTerm(long term) {
		currentTerm = term;
		votedFor = null;
	}
	public void nextTerm() {
		currentTerm++;
		removeVotedFor();
	}
	public String getVotedFor() {
		return votedFor;
	}
	public void setVotedFor(String votedFor) {
		this.votedFor = votedFor;
	}
	public void removeVotedFor() {
		setVotedFor(null);
	}
	public List<LogEntry> getLog() {
		return log;
	}
	public LogEntry getLogEntry(int index){
		if (index < 1 || index > log.size()){
			return null;
		}
		return log.get(--index);
	}
	public long getTerm(int index){
		if (index < 1 || index > log.size()){
			return -1;
		}
		return log.get(--index).getTerm();
	}
	public List<LogEntry> getLogEntries(int index){
		List<LogEntry> sublist = new Vector<LogEntry>();
		for (index--;index < log.size(); index++){
			sublist.add(log.get(index));
		}
		return sublist;
	}
	public void addEntry(Operation operation){
		LogEntry logEntry = new LogEntry(currentTerm, operation);
		appendEntry(logEntry);
	}
	public void appendEntry(LogEntry entry){
		if (!log.contains(entry)){
			log.add(entry);
		} else{
			System.out.println("[Possible error] PersistentState -- appendEntry -- entry already in the log: "+entry);
		}
	}
	public long getLastLogTerm(){
		if (log.size()>0){
			return log.get(log.size()-1).getTerm();
		}
		return -1;
	}
	public int getLastLogIndex(){
		return log.size();
	}
	public void deleteEntries(int index){
		if (index < 1){
			System.out.println("[Possible error] PersistentState -- deleteEntries -- index (lower than 1): "+index);
			return;
		}
		for (int i = log.size() - index; i>=0 ; i--){
			log.remove(log.size()-1);			
		}
	}
	@Override
	public String toString() {
		return "PersistentState [currentTerm=" + currentTerm + ", votedFor="
				+ votedFor + ", log=" + log + "]";
	}
}
