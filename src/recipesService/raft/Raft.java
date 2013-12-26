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

package recipesService.raft;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import recipesService.data.Operation;
import recipesService.raft.dataStructures.LogEntry;
import recipesService.raftRPC.AppendEntriesResponse;
import recipesService.raftRPC.RequestVoteResponse;
import recipesService.test.client.RequestResponse;

/**
 * 
 * Raft Consensus Interface
 * 
 * @author Joan-Manuel Marques
 * June 2013
 *
 */
public interface Raft extends Remote{
	//
	// Raft consensus algorithm
	//
	
	 /**
	 * Invoked by candidates to gather votes 
	 * @param term: candidate's term
	 * @param candidateId: candidate requesting vote
	 * @param lastLogIndex: index of candidate's last log entry
	 * @param lastLogTerm: term of candidate's last log entry
	 * @return RequestVoteResponse: term (currentTerm, for candidate to update itself), voteGranted (true means candidate received vote)
	 * @throws RemoteException
	 */
	public RequestVoteResponse requestVote (long term, String candidateId, int lastLogIndex, long lastLogTerm) throws RemoteException;

	 /**
	 * Invoked by leader to replicate log entries; also used as heartbeat
	 * @param term: leader's term
	 * @param leaderId: so follower can redirect clients
	 * @param prevLogIndex: index of log entry immediately preceding new ones
	 * @param prevLogTerm: term of prevLogIndex entry 
	 * @param entries: log entries to store (empty for heartbeat: may send more than one for efficiency) 
	 * @param leaderCommit: leader's commitIndex 
	 * @return AppendEntriesResponse: term (currentTerm, for leader to update itself), success (true if follower contained entry matching prevLongIndex and prevLogTerm)
	 * @throws RemoteException
	 */
	public AppendEntriesResponse appendEntries (long term, String leaderId, int prevLogIndex, long prevLogTerm, List<LogEntry> entries, int leaderCommit) throws RemoteException;

	
	//
	// Server functionality
	//

	/**
	 * Invoked by a client to apply an operation to the recipes' service
	 * Operation may be: AddOpperation (to add a recipe) or RemoveOperation (to remove a recipe)
	 * @param Operation: operation to apply to recipes
	 * @return RequestResponse: the leaderId and a boolean that indicates if operation succeeded or not
	 * @throws RemoteException
	 */
	public RequestResponse Request(Operation operation) throws RemoteException;
}
