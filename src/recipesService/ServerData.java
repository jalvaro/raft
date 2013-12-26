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

package recipesService;

import java.rmi.RemoteException;
import java.util.List;

import recipesService.activitySimulation.SimulationData;
import recipesService.communication.Host;
import recipesService.communication.Hosts;
import recipesService.data.Operation;
import recipesService.raft.RaftConsensus;
import recipesService.raft.dataStructures.LogEntry;
import recipesService.raftRPC.AppendEntriesResponse;
import recipesService.raftRPC.RequestVoteResponse;
import recipesService.test.client.RequestResponse;

/**
 * @author Joan-Manuel Marques
 * July 2012
 *
 */
public class ServerData extends RaftConsensus {

	// Students' id
	private String groupId;

	// end: true when program should end; false otherwise
	private boolean end = false;
	
	public ServerData(
			String groupId,
			long electionTimeout
			){
		super(electionTimeout);
		this.groupId = groupId;
	}
	
	/**
	 * Starts the execution
	 * @param hosts: contains information about all hosts + the localhost
	 */
	public void start(Hosts hosts){
		System.out.println("start serverData: "+hosts.getLocalHost().getId());
		List<Host> servers = hosts.getAllHosts();
		
		// remove oneself from the list of servers
		servers.remove(hosts.getLocalHost());

		// set localhost and other servers participating in the cluster
		setServers(hosts.getLocalHost(),servers);
		
		// start simulation
		//	* start activity simulation timers
		// 	* set connected state on simulation data
		//  * connect Server and raftConsensus
		SimulationData.getInstance().startSimulation(this);
		SimulationData.getInstance().connect(); // this call includes starting raft consensus
	}

	public boolean end(){
		return this.end;
	}
	
	public void setEnd(){
		this.end = true;
	}

	// ******************************
	// *** getters and setters
	// ******************************
	public String getGroupId(){
		return this.groupId;
	}
}
