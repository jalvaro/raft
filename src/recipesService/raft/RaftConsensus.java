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
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


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
	
	Executor executorQueue;
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
		System.out.println("JORDI - RaftConsensus.java - setServers(): " + otherServers.size() + ", " + otherServers.get(0));
		this.otherServers = otherServers;
		numServers = otherServers.size() + 1;	

		// Create a pool of threads
		executorQueue = Executors.newCachedThreadPool();
	}

	// connect
	public void connect(){
		/*
		 *  ACTIONS TO DO EACH TIME THE SERVER CONNECTS (i.e. when it starts or after a failure or disconnection)
		 */
		// System.out.println("JORDI - RaftConsensus.java - connect()");
		
		long rndTimeout = getRandomizedElectionTimeout();
		//System.out.println("JORDI - new timer with timeout = " + rndTimeout + " (electionTimeout: " + electionTimeout + ").");
		electionTimeoutTimer = new Timer();
		
		electionTimeoutTimer.schedule(new TimerTask() {
			
			@Override
			public void run() {
				System.out.println("JORDI - TimeOut!!!!!!" + ", - localhost: " + localHost);
				startNewElection();
			}
		// First approximation, must be thought again
		}, rndTimeout, rndTimeout);
	}
	
	public void disconnect(){
		// System.out.println("JORDI - RaftConsensus.java - disconnect()");
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
	private void setFollowerState(long currentTerm) {
		state = RaftState.FOLLOWER;
		persistentState.setCurrentTerm(currentTerm);
		disconnect();
		connect();
	}
	
	private long getRandomizedElectionTimeout() {
		return (long) rnd.nextInt((int) electionTimeout + 1) + electionTimeout;
	}
	
	private synchronized void startNewElection() {
		System.out.println("JORDI - startNewElection() - localhost: " + localHost.getId());
		// Increment current term
		persistentState.nextTerm();
		// Change to Candidate state
		state = RaftState.CANDIDATE;
		// Vote for self
		persistentState.setVotedFor(localHost.getId());
		receivedVotes = new HashSet<Host>();
		receivedVotes.add(localHost);
		
		// Reset leader??????????
		leader = null;
		
		/* Send RequestVote RPCs to all the servers in the cluster, retry until either:
		 * 1. Receive votes from the majority of the servers:
		 * 		- Become leader.
		 * 		- Send AppendEntries heartbeats to all other servers
		 * 2. Receive RPC from valid leader:
		 * 		- Return to follower state.
		 * 3. No-one wins election (election timeout elapses):
		 * 		- Increment term, start new election.
		 */
		
		for (Host server : otherServers) {
			executorQueue.execute(createRequestVoteRunnable(server));
		}
	}
	
	private Runnable createRequestVoteRunnable(final Host s) {
		return new Runnable() {
		
			@Override
			public void run() {
				final RaftConsensus rc = RaftConsensus.this;
				boolean hasResponded = false;
				while (!hasResponded && state == RaftState.CANDIDATE) {
					try {
						RequestVoteResponse rvr = RMIsd.getInstance().requestVote(s.getId(), persistentState.getCurrentTerm(), localHost.getId(),
								persistentState.getLastLogIndex(), persistentState.getLastLogTerm());
						// System.out.println("JORDI - createRequestVoteRunnable() - rvr: " + rvr + ", - localhost: " + localHost);
						
						hasResponded = true;
						synchronized(rc) {
							if (rvr != null && rvr.isVoteGranted() && state == RaftState.CANDIDATE) {
								receivedVotes.add(s);
								System.out.println("JORDI - createRequestVoteRunnable() - votes: " + receivedVotes.size() + ", - localhost: " + localHost);
								
								if ((double) receivedVotes.size() > ((double) numServers)/2) {
									System.out.println("JORDI - ************New LEADER**************" + " - term: " + persistentState.getCurrentTerm() + ", - localhost: " + localHost);
									rc.disconnect();
									leader = localHost.getId();
									state = RaftState.LEADER;
									rc.sendHeartBeats();
								}
							} else if (rvr != null && !rvr.isVoteGranted()) {
								if (rvr.getTerm() > persistentState.getCurrentTerm()) {
									setFollowerState(rvr.getTerm());
								}
							}
						}
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		};
	}
	
	private Runnable createHeartBeatRunnable(final Host s) {
		return new Runnable() {
		
			@Override
			public void run() {
				final RaftConsensus rc = RaftConsensus.this;
				boolean hasResponded = false;
				
				while (!hasResponded && state == RaftState.LEADER) {
					try {
						AppendEntriesResponse aer = RMIsd.getInstance().appendEntries(s.getId(), persistentState.getCurrentTerm(), leader, -2, -2, null, -2);
						// System.out.println("JORDI - createHeartBeatRunnable() - aer: " + aer + ", - localhost: " + localHost);
						
						hasResponded = true;
						synchronized(rc) {
							if (aer != null && !aer.isSucceeded() && state == RaftState.LEADER) {
								setFollowerState(aer.getTerm());
							}
						}
						
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		};
	}
	
	private synchronized void sendHeartBeats() {
		System.out.println("JORDI - sendHeartBeats()");
		leaderHeartbeatTimeoutTimer = new Timer();
		
		leaderHeartbeatTimeoutTimer.schedule(new TimerTask() {
			
			@Override
			public void run() {
				// RequestVote RPC is sent to all the servers in the cluster
				for (Host server : otherServers) {
					executorQueue.execute(createHeartBeatRunnable(server));
				}
			}
		}, 0, leaderHeartbeatTimeout);
	}

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
	public synchronized RequestVoteResponse requestVote(long term, String candidateId,
			int lastLogIndex, long lastLogTerm) throws RemoteException {
		//System.out.println("JORDI - requestVote: {" + term + ", " + candidateId + ", " + lastLogIndex + ", " + lastLogTerm + "}," +
		//		" - localhost: " + localHost);
		RequestVoteResponse rvr = null;
		// It is required to *guard* read, apply and create response actions
		// Test parameters (including term)
		long currentTerm = persistentState.getCurrentTerm();
		boolean grantVote;
		
		grantVote = checkRequest(term, candidateId, lastLogIndex, lastLogTerm);
		
		if (grantVote) {
			// Que es retorna???? lastlogterm????? term???
			rvr = new RequestVoteResponse(1, true);
			setFollowerState(term);
			persistentState.setVotedFor(candidateId);
			// System.out.println("JORDI - requestVote: {votedFor: " + votedFor + ", candidateId: " + candidateId + "}, - localhost: " + localHost);
		} else {
			rvr = new RequestVoteResponse(currentTerm, false);
		}
		
		// Read the current state
		// Apply action to the state
		// Create a response
		System.out.println("JORDI - requestVote: {term: " + term + ", candidateId: " + candidateId + ", " + rvr + "}, - localhost: " + localHost);
		return rvr;
	}
	
	private boolean checkRequest(long term, String candidateId, int lastLogIndex, long lastLogTerm) {
		boolean grantVote;
		String votedFor = persistentState.getVotedFor();
		long currentTerm = persistentState.getCurrentTerm();
		
		if (candidateId != null && term > 0) {
			if (term < currentTerm) {
				grantVote = false;
			} else if (term == currentTerm) {
				// if (votedFor == null || candidateId.equals(votedFor)) {
				if (candidateId.equals(votedFor)) {
					grantVote = checkLogTermAndIndex(term, candidateId, lastLogIndex, lastLogTerm);
				} else {
					grantVote = false;
				}
			} else {
				grantVote = checkLogTermAndIndex(term, candidateId, lastLogIndex, lastLogTerm);
			}
		} else {
			grantVote = false;
		}
		
		return grantVote;
	}
	
	private boolean checkLogTermAndIndex(long term, String candidateId, int lastLogIndex, long lastLogTerm) {
		boolean grantVote;
		
		if ( (persistentState.getLastLogTerm() > lastLogTerm || 
				(persistentState.getLastLogTerm() == lastLogTerm && persistentState.getLastLogIndex() > lastLogIndex)) ) {
			grantVote = false;
		} else {
			grantVote = true;
		}
		
		return grantVote;
	}

	@Override
	public synchronized AppendEntriesResponse appendEntries(long term, String leaderId,
			int prevLogIndex, long prevLogTerm, List<LogEntry> entries,
			int leaderCommit) throws RemoteException {
		// System.out.println("JORDI - appendEntries: {" + term + ", " + leaderId + ", " + prevLogIndex + ", " + prevLogTerm + ", [" + entries + "], "
		// + leaderCommit + "}");
		AppendEntriesResponse aer;
		if (term < persistentState.getCurrentTerm()) {
			aer = new AppendEntriesResponse(persistentState.getCurrentTerm(), false);
		} else {
			aer = new AppendEntriesResponse(prevLogTerm, true);
			leader = leaderId;
			setFollowerState(term);
		}
		
		return aer;
	}

	@Override
	public RequestResponse Request(Operation operation) throws RemoteException {
		// TODO Auto-generated method stub
		System.out.println("JORDI - Request: {" + operation.toString() + "}");
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
