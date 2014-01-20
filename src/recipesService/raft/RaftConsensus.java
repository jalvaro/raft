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
import recipesService.activitySimulation.ActivitySimulation;
import recipesService.communication.Host;
import recipesService.data.AddOperation;
import recipesService.data.Operation;
import recipesService.data.Recipe;
import recipesService.data.RemoveOperation;
import recipesService.data.Timestamp;
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
 *         May 2013
 * 
 */

public abstract class RaftConsensus extends CookingRecipes implements Raft {

	// current server
	private Host localHost;

	//
	// STATE
	//

	// raft persistent state state on all servers
	protected PersistentState persistentState;

	// raft volatile state on all servers
	private int commitIndex; // index of highest log entry known to be committed
								// (initialized to 0, increases monotonically)
	private int lastApplied; // index of highest log entry applied to state
								// machine (initialized to 0, increases
								// monotonically)

	// other
	private RaftState state = RaftState.FOLLOWER;

	// Leader
	private String leader;

	// Leader election
	private Timer electionTimeoutTimer; // timer for election timeout
	private long electionTimeout; // period of time that a follower receives no
									// communication.
									// If a timeout occurs, it assumes there is
									// no viable leader.
	private Set<Host> receivedVotes; // contains hosts that have voted for this
										// server as candidate in a leader
										// election

	//
	// LEADER
	//

	// Volatile state on leaders
	private Index nextIndex; // for each server, index of the next log entry to
								// send to that server (initialized to leader
								// last log index + 1)
	private Index matchIndex; // for each server, index of highest log known to
								// be replicated on server (initialized to 0,
								// increases monotonically)

	// Heartbeats on leaders
	private Timer leaderHeartbeatTimeoutTimer;
	private long leaderHeartbeatTimeout;

	//
	// CLUSTER
	//

	// general
	private int numServers; // number of servers in a Raft cluster.
							// 5 is a typical number, which allows the system to
							// tolerate two failures
	// partner servers
	private List<Host> otherServers; // list of partner servers (localHost not
										// included in the list)

	//
	// UTILS
	//

	private Executor executorQueue;
	private Timer requestVoteRetryTimeoutTimer;
	private Timer appendEntriesRetryTimeoutTimer;
	// AtomicBoolean isleaderAlive;
	static Random rnd = new Random();
	private Vector<Timestamp> operationTimestamps;

	// =======================
	// === IMPLEMENTATION
	// =======================

	public RaftConsensus(long electionTimeout) { // electiontimeout is a
													// parameter in
													// config.properties file
		// set electionTimeout
		this.electionTimeout = electionTimeout;

		// set leaderHeartbeatTimeout
		this.leaderHeartbeatTimeout = electionTimeout / 3; // TODO: Cal
															// revisar-ne el
															// valor
	}

	// sets localhost and other servers participating in the cluster
	protected void setServers(Host localHost, List<Host> otherServers) {

		this.localHost = localHost;

		// initialize persistent state on all servers
		persistentState = new PersistentState();

		// set servers list
		this.otherServers = otherServers;
		numServers = otherServers.size() + 1;

		// Create a pool of threads
		executorQueue = Executors.newCachedThreadPool();

		// Initialize

		// volatile state on all servers (initialized to 0, increases
		// monotonically)
		commitIndex = 0;
		lastApplied = 0;
		operationTimestamps = new Vector<Timestamp>();
	}

	// connect
	public void connect() {
		/*
		 * ACTIONS TO DO EACH TIME THE SERVER CONNECTS (i.e. when it starts or
		 * after a failure or disconnection)
		 */

		long rndTimeout = getRandomizedElectionTimeout();
		electionTimeoutTimer = new Timer();

		electionTimeoutTimer.schedule(new TimerTask() {

			@Override
			public void run() {
				startNewElection();
			}
		}, rndTimeout, rndTimeout);
	}

	public void disconnect() {
		electionTimeoutTimer.cancel();
		if (localHost.getId().equals(leader)) {
			leaderHeartbeatTimeoutTimer.cancel();
		}
	}

	//
	// LEADER ELECTION
	//

	/*
	 * Leader election
	 */
	private void setFollowerState(long currentTerm, String newLeader) {
		state = RaftState.FOLLOWER;
		persistentState.setCurrentTerm(currentTerm);

		disconnect();
		connect();
		if (newLeader != null) {
			leader = newLeader;
		}
	}

	private long getRandomizedElectionTimeout() {
		return (long) rnd.nextInt((int) electionTimeout + 1) + electionTimeout;
	}

	private boolean hasMajorityOfVotes() {
		return receivedVotes.size() > (numServers / 2 + 1);
	}

	private void startNewElection() {
		synchronized (this) {
			// Increment current term
			persistentState.nextTerm();
			// Change to Candidate state
			state = RaftState.CANDIDATE;
			// Vote for self
			persistentState.setVotedFor(localHost.getId());
			receivedVotes = new HashSet<Host>();
			receivedVotes.add(localHost);

			if (requestVoteRetryTimeoutTimer != null) {
				requestVoteRetryTimeoutTimer.cancel();
				requestVoteRetryTimeoutTimer = null;
			}
		}

		/*
		 * Send RequestVote RPCs to all the servers in the cluster, retry until
		 * either:
		 * 1. Receive votes from the majority of the servers:
		 * - Become leader.
		 * - Send AppendEntries heartbeats to all other servers
		 * 2. Receive RPC from valid leader:
		 * - Return to follower state.
		 * 3. No-one wins election (election timeout elapses):
		 * - Increment term, start new election.
		 */

		for (final Host server : otherServers) {
			executorQueue.execute(createRequestVoteRunnable(server));
		}
	}

	private void sendHeartBeats() {
		leaderHeartbeatTimeoutTimer = new Timer();

		leaderHeartbeatTimeoutTimer.schedule(new TimerTask() {

			@Override
			public void run() {
				// RequestVote RPC is sent to all the servers in the cluster
				for (Host server : otherServers) {
					executorQueue.execute(createAppendEntriesRunnable(server, true));
				}
			}
		}, 0, leaderHeartbeatTimeout);
	}

	private Runnable createRequestVoteRunnable(final Host s) {
		return new Runnable() {

			@Override
			public void run() {
				final RaftConsensus rc = RaftConsensus.this;

				final long currentTerm;
				final String candidateId;
				final int currLastLogIndex;
				final long currLastLogTerm;
				synchronized (rc) {
					currentTerm = persistentState.getCurrentTerm();
					candidateId = localHost.getId();
					currLastLogIndex = persistentState.getLastLogIndex();
					currLastLogTerm = persistentState.getLastLogTerm();
				}

				try {
					RequestVoteResponse rvr = RMIsd.getInstance().requestVote(s, currentTerm, candidateId, currLastLogIndex,
							currLastLogTerm);

					synchronized (rc) {
						if (rvr != null && rvr.isVoteGranted() && state == RaftState.CANDIDATE) {
							receivedVotes.add(s);

							if (hasMajorityOfVotes()) {
								disconnect();
								leader = localHost.getId();
								state = RaftState.LEADER;
								initializeLeaderIndexes();
								sendHeartBeats();
								sendNewEntries();
							}
						} else if (rvr != null && !rvr.isVoteGranted()) {
							if (rvr.getTerm() > persistentState.getCurrentTerm()) {
								setFollowerState(rvr.getTerm(), null);
							}
						}
						if (requestVoteRetryTimeoutTimer != null) {
							requestVoteRetryTimeoutTimer.cancel();
							requestVoteRetryTimeoutTimer = null;
						}
					}
				} catch (Exception e) {
					synchronized (rc) {
						if (state == RaftState.CANDIDATE && requestVoteRetryTimeoutTimer == null) {
							requestVoteRetryTimeoutTimer = new Timer();

							requestVoteRetryTimeoutTimer.schedule(new TimerTask() {

								@Override
								public void run() {
									executorQueue.execute(createRequestVoteRunnable(s));
								}
							}, leaderHeartbeatTimeout / 2, leaderHeartbeatTimeout / 2);
						} else if (state != RaftState.CANDIDATE && requestVoteRetryTimeoutTimer != null) {
							requestVoteRetryTimeoutTimer.cancel();
							requestVoteRetryTimeoutTimer = null;
						}
					}
				}
			}
		};
	}

	private Runnable createAppendEntriesRunnable(final Host s, final boolean isHeartBeat) {
		return new Runnable() {

			@Override
			public void run() {
				final RaftConsensus rc = RaftConsensus.this;

				final long currentTerm;
				final String currentLeaderId;
				final int currPrevLogIndex;
				final long currPrevLogTerm;
				final List<LogEntry> currEntries;
				final int currCommitIndex;
				final int auxNextIndex;
				final int currLastLogIndex;

				synchronized (rc) {
					auxNextIndex = nextIndex.getIndex(s.getId());
					currentTerm = persistentState.getCurrentTerm();
					currentLeaderId = localHost.getId();
					currPrevLogIndex = auxNextIndex - 1;
					currPrevLogTerm = persistentState.getTerm(currPrevLogIndex);
					if (isHeartBeat) {
						currEntries = null;
					} else {
						currEntries = persistentState.getLogEntries(auxNextIndex);
					}
					tryToUpdateCommitIndex();
					currCommitIndex = commitIndex;
					currLastLogIndex = persistentState.getLastLogIndex();
				}

				try {
					AppendEntriesResponse aer = RMIsd.getInstance().appendEntries(s, currentTerm, currentLeaderId, currPrevLogIndex,
							currPrevLogTerm, currEntries, currCommitIndex);

					boolean retryAppendEntries = false;
					synchronized (rc) {
						if (state == RaftState.LEADER && aer != null) {
							if (!aer.isSucceeded() && persistentState.getCurrentTerm() < aer.getTerm()) {
								setFollowerState(aer.getTerm(), null);
							} else if (!isHeartBeat) {
								if (aer.isSucceeded()) {
									nextIndex.setIndex(s.getId(), currLastLogIndex + 1);
									matchIndex.setIndex(s.getId(), currLastLogIndex);
								} else {
									nextIndex.decrease(s.getId());
									retryAppendEntries = true;
								}
							}
						}
					}
					if (retryAppendEntries) {
						executorQueue.execute(createAppendEntriesRunnable(s, isHeartBeat));
					}

				} catch (Exception e) {
					synchronized (rc) {
						if (!isHeartBeat) {
							if (state == RaftState.LEADER && appendEntriesRetryTimeoutTimer == null) {
								appendEntriesRetryTimeoutTimer = new Timer();

								appendEntriesRetryTimeoutTimer.schedule(new TimerTask() {

									@Override
									public void run() {
										executorQueue.execute(createAppendEntriesRunnable(s, false));
									}
								}, leaderHeartbeatTimeout / 2, leaderHeartbeatTimeout / 2);
							} else if (state != RaftState.LEADER && appendEntriesRetryTimeoutTimer != null) {
								appendEntriesRetryTimeoutTimer.cancel();
								appendEntriesRetryTimeoutTimer = null;
							}
						}
					}
				}
			}
		};
	}

	//
	// LOG REPLICATION
	//

	/*
	 * Log replication
	 */

	private void initializeLeaderIndexes() {
		// initialized to leader last log index + 1
		nextIndex = new Index(otherServers, persistentState.getLastLogIndex() + 1);

		// initialized to 0, increases monotonically
		matchIndex = new Index(otherServers, 0);
	}

	private void sendNewEntries() {
		synchronized (this) {
			if (appendEntriesRetryTimeoutTimer != null) {
				appendEntriesRetryTimeoutTimer.cancel();
				appendEntriesRetryTimeoutTimer = null;
			}
		}

		new Timer().schedule(new TimerTask() {

			@Override
			public void run() {
				// RequestVote RPC is sent to all the servers in the cluster
				for (Host server : otherServers) {
					executorQueue.execute(createAppendEntriesRunnable(server, false));
				}
			}
		}, 0);
	}

	private void tryToUpdateCommitIndex() {
		int i = 0;
		int n;
		for (LogEntry entry : persistentState.getLogEntries(commitIndex + 1)) {
			n = commitIndex + 1 + i;
			if (myMajorityHigherOrEqual(n) && entry.getTerm() == persistentState.getCurrentTerm()) {
				commitIndex = n;
			}
			i++;
		}
		tryToApplyNewEntriesToStateMachine();
	}

	private void tryToApplyNewEntriesToStateMachine() {
		while (commitIndex > lastApplied) {
			lastApplied++;
			LogEntry entry = persistentState.getLogEntry(lastApplied);
			Timestamp ts = entry.getCommand().getTimestamp();

			if (entry.getCommand() instanceof AddOperation) {
				Recipe recipe = ((AddOperation) entry.getCommand()).getRecipe();
				if (!operationTimestamps.contains(ts)) {
					addRecipe(recipe);
					operationTimestamps.add(ts);
				}
			} else {
				String recipeTitle = ((RemoveOperation) entry.getCommand()).getRecipeTitle();
				if (!operationTimestamps.contains(ts)) {
					removeRecipe(recipeTitle);
					operationTimestamps.add(ts);
				}
			}
		}
	}

	private boolean myMajorityHigherOrEqual(int n) {
		int count = 1;
		for (Host server : otherServers) {
			int val = matchIndex.getIndex(server.getId());
			if (val >= n) {
				count++;
			}
		}
		return (count >= (numServers / 2 + 1));
	}

	//
	// API
	//

	@Override
	public RequestVoteResponse requestVote(long term, String candidateId, int lastLogIndex, long lastLogTerm) throws RemoteException {
		RequestVoteResponse rvr = null;
		// It is required to *guard* read, apply and create response actions
		// Test parameters (including term)
		// Read the current state
		// Apply action to the state
		// Create a response
		final long currentTerm;
		final String currentVotedFor;
		final int currentLastLogIndex;
		final long currentLastLogTerm;
		synchronized (this) {
			currentTerm = persistentState.getCurrentTerm();
			currentVotedFor = persistentState.getVotedFor();
			currentLastLogIndex = persistentState.getLastLogIndex();
			currentLastLogTerm = persistentState.getLastLogTerm();
		}
		boolean grantVote;

		grantVote = checkRequest(term, candidateId, lastLogIndex, lastLogTerm, currentTerm, currentVotedFor, currentLastLogIndex,
				currentLastLogTerm);

		if (grantVote) {
			rvr = new RequestVoteResponse(currentTerm, true);
			synchronized (this) {
				setFollowerState(term, null);
				persistentState.setVotedFor(candidateId);
			}
		} else {
			rvr = new RequestVoteResponse(currentTerm, false);
		}

		return rvr;
	}

	@Override
	public AppendEntriesResponse appendEntries(long term, String leaderId, int prevLogIndex, long prevLogTerm, List<LogEntry> entries,
			int leaderCommit) throws RemoteException {
		AppendEntriesResponse aer;

		final long currentTerm;
		final int lastLogIndex;
		final int currentCommitIndex;
		final LogEntry prevLogEntry;
		synchronized (this) {
			currentTerm = persistentState.getCurrentTerm();
			lastLogIndex = persistentState.getLastLogIndex();
			currentCommitIndex = commitIndex;
			prevLogEntry = persistentState.getLogEntry(prevLogIndex);
		}
		if (term < currentTerm) {
			aer = new AppendEntriesResponse(currentTerm, false);
		} else if (entries == null) {
			// It is a heartbeat
			aer = new AppendEntriesResponse(currentTerm, true);

			synchronized (this) {
				setFollowerState(term, leaderId);

				if (leaderCommit > currentCommitIndex) {
					commitIndex = (leaderCommit < lastLogIndex) ? leaderCommit : lastLogIndex;
					tryToApplyNewEntriesToStateMachine();
				}
			}

		} else if (prevLogIndex > 0 && (prevLogEntry == null || prevLogEntry.getTerm() != prevLogTerm)) {
			aer = new AppendEntriesResponse(currentTerm, false);
			synchronized (this) {
				setFollowerState(term, leaderId);

				if (leaderCommit > currentCommitIndex) {
					commitIndex = (leaderCommit < lastLogIndex) ? leaderCommit : lastLogIndex;
					tryToApplyNewEntriesToStateMachine();
				}
			}
		} else {
			aer = new AppendEntriesResponse(currentTerm, true);

			synchronized (this) {
				setFollowerState(term, leaderId);

				int i = 1;
				for (final LogEntry entry : entries) {
					final LogEntry auxLogEntry = persistentState.getLogEntry(prevLogIndex + i);
					if (auxLogEntry == null) {
						persistentState.appendEntry(entry);
					} else if (!auxLogEntry.equals(entry)) {
						persistentState.deleteEntries(prevLogIndex + i);
						persistentState.appendEntry(entry);
					}
					i++;
				}
				if (leaderCommit > currentCommitIndex) {
					commitIndex = (leaderCommit < lastLogIndex) ? leaderCommit : lastLogIndex;
					tryToApplyNewEntriesToStateMachine();
				}
			}
		}
		return aer;
	}

	@Override
	public RequestResponse Request(Operation operation) throws RemoteException {

		RequestResponse rr;
		synchronized (this) {
			if (state == RaftState.LEADER) {
				persistentState.addEntry(operation);
				sendNewEntries();
				rr = new RequestResponse(leader, true);
			} else {
				// Redirect to leader
				rr = new RequestResponse(leader, false);
			}
		}

		return rr;
	}

	/*
	 * API - other methods
	 */

	private boolean checkRequest(long term, String candidateId, int lastLogIndex, long lastLogTerm, long currentTerm,
			String currentVotedFor, int currentLastLogIndex, long currentLastLogTerm) {
		boolean grantVote;

		if (candidateId != null && term > 0) {
			if (term < currentTerm) {
				grantVote = false;
			} else if (term == currentTerm) {
				if (candidateId.equals(currentVotedFor)) {
					grantVote = checkLogTermAndIndex(lastLogIndex, lastLogTerm, currentLastLogIndex, currentLastLogTerm);
				} else {
					grantVote = false;
				}
			} else {
				grantVote = checkLogTermAndIndex(lastLogIndex, lastLogTerm, currentLastLogIndex, currentLastLogTerm);
			}
		} else {
			grantVote = false;
		}

		return grantVote;
	}

	private boolean checkLogTermAndIndex(int lastLogIndex, long lastLogTerm, int currentLastLogIndex, long currentLastLogTerm) {
		boolean grantVote;

		if ((currentLastLogTerm > lastLogTerm || (currentLastLogTerm == lastLogTerm && currentLastLogIndex > lastLogIndex))) {
			grantVote = false;
		} else {
			grantVote = true;
		}

		return grantVote;
	}

	//
	// Other methods
	//
	public String getServerId() {
		return localHost.getId();
	}

	public synchronized List<LogEntry> getLog() {
		return persistentState.getLog();
	}

	public long getCurrentTerm() {
		return persistentState.getCurrentTerm();
	}

	public String getLeaderId() {
		return ((leader == null) ? "" : leader);
	}
}
