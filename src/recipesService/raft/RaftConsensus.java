/*
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


import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;


import communication.DSException;
import communication.rmi.RMIsd;

import recipesService.CookingRecipes;
import recipesService.activitySimulation.SimulationData;
import recipesService.communication.Host;
import recipesService.data.AddOperation;
import recipesService.data.Operation;
import recipesService.data.RemoveOperation;
import recipesService.raft.dataStructures.Index;
import recipesService.raft.dataStructures.LogEntry;
import recipesService.raft.dataStructures.PersistentState;
import recipesService.raftRPC.AppendEntriesResponse;
import recipesService.raftRPC.RequestVoteResponse;
import recipesService.test.client.RequestResponse;

/**
 * 
 * Raft Consensus
 * 
 * @author Joan-Manuel Marques
 * May 2013
 *
 */

public abstract class RaftConsensus extends CookingRecipes implements Raft{

	// current server
	private Host localHost;
	
	//
	// STATE
	//
	
	// raft persistent state state on all servers
	protected PersistentState persistentState;  

	// raft volatile state on all servers
	private int commitIndex; // index of highest log entry known to be committed (initialized to 0, increases monotonically) 
	private int lastApplied; // index of highest log entry applied to state machine (initialized to 0, increases monotonically) 
	
	// other 
	private RaftState state = RaftState.FOLLOWER;
	
	// Leader
	private String leader; 
	
	// Leader election
	private Timer electionTimeoutTimer; // timer for election timeout
	private long electionTimeout; // period of time that a follower receives no communication.
									// If a timeout occurs, it assumes there is no viable leader.
	private Set<Host> receivedVotes; // contains hosts that have voted for this server as candidate in a leader election
	
	//
	// LEADER
	//

	// Volatile state on leaders
	private Index nextIndex; // for each server, index of the next log entry to send to that server (initialized to leader last log index + 1)
	private Index matchIndex; // for each server, index of highest log known to be replicated on server (initialized to 0, increases monotonically)
	
	// Heartbeats on leaders
	private Timer leaderHeartbeatTimeoutTimer;
	private long leaderHeartbeatTimeout;
	
	//
	// CLUSTER
	//
	
	// general
	private int numServers; // number of servers in a Raft cluster.
							// 5 is a typical number, which allows the system to tolerate two failures 
	// partner servers
	private List<Host> otherServers; // list of partner servers (localHost not included in the list)

	//
	// UTILS
	//
	
	static Random rnd = new Random();
	
	// =======================
	// === IMPLEMENTATION
	// =======================
	
	public RaftConsensus(long electionTimeout){ // electiontimeout is a parameter in config.properties file
		// set electionTimeout
		this.electionTimeout = electionTimeout;
		
		//set leaderHeartbeatTimeout
		this.leaderHeartbeatTimeout = electionTimeout / 3; //TODO: Cal revisar-ne el valor 
	}
	
	// sets localhost and other servers participating in the cluster
	protected void setServers(
			Host localHost,
			List<Host> otherServers
			){
		
		this.localHost = localHost; 

		// initialize persistent state  on all servers
		persistentState = new PersistentState();
		
		// set servers list
		this.otherServers = otherServers;
		numServers = otherServers.size() + 1;	
	}

	// connect
	public void connect(){
		/*
		 *  ACTIONS TO DO EACH TIME THE SERVER CONNECTS (i.e. when it starts or after a failure or disconnection)
		 */
	}
	
	public void disconnect(){
		electionTimeoutTimer.cancel();
		if (localHost.getId().equals(leader)){
			leaderHeartbeatTimeoutTimer.cancel();
		}
	}
	

	//
	// LEADER ELECTION
	//

	/*
	 *  Leader election
	 */

	

	//
	// LOG REPLICATION
	//

	/*
	 *  Log replication
	 */

	
	//
	// API 
	//

	@Override
	public RequestVoteResponse requestVote(long term, String candidateId,
			int lastLogIndex, long lastLogTerm) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AppendEntriesResponse appendEntries(long term, String leaderId,
			int prevLogIndex, long prevLogTerm, List<LogEntry> entries,
			int leaderCommit) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RequestResponse Request(Operation operation) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	
	//
	// Other methods
	//
	public String getServerId(){
		return localHost.getId();
	}

	public synchronized List<LogEntry> getLog(){
		return persistentState.getLog();
	}
}
