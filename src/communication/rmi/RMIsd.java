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

package communication.rmi;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

import communication.DSException;

import recipesService.ServerData;
import recipesService.activitySimulation.ActivitySimulation;
import recipesService.communication.Host;
import recipesService.raft.Raft;
import recipesService.raft.dataStructures.LogEntry;
import recipesService.raftRPC.AppendEntriesResponse;
import recipesService.raftRPC.RequestVoteResponse;

/**
 * Package to encapsualte RPC communication
 * (uses java rmi)
 * @author Joan-Manuel Marques
 * July 2013
 *
 */
public class RMIsd {
	private static RMIsd rmiSD;

	private static int registryPort = 1099; // default value 1099			// TODO: que siguin paràmetres
	private String registryHost = "localhost"; // default value		// TODO: que siguin paràmetres
	
	public static RMIsd getInstance(){
		if (rmiSD == null){
			rmiSD = new RMIsd();
		}
		return rmiSD;
	}

	public int createRMIregistry(String address){
		return createRMIregistry(address, registryPort);
	}
	
	public int createRMIregistry(String address, int port){
		registryHost = address;
		boolean end = false;
		do {
			try {
				LocateRegistry.createRegistry(registryPort);
				end = true;
			} catch (RemoteException e1) {
				// TODO Auto-generated catch block
//				e1.printStackTrace();
				registryPort++;
			}
		}while (!end);

		return registryPort;
	}
	
	//
	// bind and unbind a remote reference (a host interface) 
	//
	
	public void connect(ServerData serverData) {
		if (ActivitySimulation.getInstance().isConnected()){
			return;
		}
//		System.out.println("|||||||||||||||||||||||||||||| connect: "+serverData.getId());
		
		// connect Raft API

		Raft stub;
		try {
			stub = (Raft) UnicastRemoteObject.exportObject(serverData, 0);

			Registry registry = LocateRegistry.getRegistry(registryHost, registryPort);
			//  Naming.rebind("rmi://localhost:1099/ServicioX", c);
			//		registry.bind("Raft"+localhost.getPort(), stub);
			
			
			registry.bind(
					"rmi://"
							+ registryHost
							+ ':'
							+ registryPort
							+ "/Raft-"
							+ serverData.getServerId(),
							stub
					);			
			} catch (RemoteException | AlreadyBoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

//		// connect Server API
//		ServerAPI stub2;
//		try {
//			stub2 = (ServerAPI) UnicastRemoteObject.exportObject(serverData, 0);
//
//			Registry registry = LocateRegistry.getRegistry(registryHost, registryPort);
//			//  Naming.rebind("rmi://localhost:1099/ServicioX", c);
//			//		registry.bind("Raft"+localhost.getPort(), stub);
//			
//			
//			registry.bind(
//					"rmi://"
//							+ registryHost
//							+ ':'
//							+ registryPort
//							+ "/ServerDataAPI-"
//							+ serverData.getId(),
//							stub2
//					);			
//			} catch (RemoteException | AlreadyBoundException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//
	
	}

	public void disconnect(ServerData serverData) {
		if (!ActivitySimulation.getInstance().isConnected()){
			return;
		}

//		System.out.println("|||||||||||||||||||||||||||||| disconnect: "+serverData.getId());
		
		// disconnect Raft API
		try {
			// unexports the object
			UnicastRemoteObject.unexportObject(serverData, true);

			// unbinds the registered application
			Registry registry = LocateRegistry.getRegistry(registryHost, registryPort);

			registry.unbind(
					"rmi://"
							+ registryHost
							+ ':'
							+ registryPort
							+ "/Raft-"
							+ serverData.getServerId()
					);
		} catch (NotBoundException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
		} catch (AccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

//		// disconnect Server API
//		try {
//			// unexports the object
//			UnicastRemoteObject.unexportObject(serverData, true);
//
//			// unbinds the registered application
//			Registry registry = LocateRegistry.getRegistry(registryHost, registryPort);
//
//			registry.unbind(
//					"rmi://"
//							+ registryHost
//							+ ':'
//							+ registryPort
//							+ "/ServerDataAPI-"
//							+ serverData.getId()
//					);
//		} catch (NotBoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (AccessException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (RemoteException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}

	}
	
	// *************
	// **** RAFT
	// *************
	public RequestVoteResponse requestVote(
			Host destination,
			long term,
			String id,
			int lastLogIndex,
			long lastLogTerm
			) throws Exception {
		
    	// TODO: posar un retard "realista" per la comunicació en un cluster (un temps variable)
		try {
			Thread.sleep(3); 	// TODO: !!!!! decide the amount of time to block !!!!!!!!!!!!!!!!!
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			System.out.println("RMIsd -- requestVote -- sleep cacth: "+e);
//			e.printStackTrace();
		}

		Registry registry;
    	if (!ActivitySimulation.getInstance().isConnected()){
    		throw new DSException("Sender is disconnected: Host tried to send a requestVote RPC while the testing environment simulates it is failed");
    	}
		try {
			registry = LocateRegistry.getRegistry(destination.getAddress(),	destination.getPort());
			// RPC call
			// host.getAddress(), host.getPort());
			Raft stub = (Raft) registry.lookup(
					"rmi://"
							+ destination.getAddress()
							+ ':'
							+ destination.getPort()
							+ "/Raft-"
							+ destination.getId()
					);
			return stub.requestVote(term, id, lastLogIndex, lastLogTerm);

		} catch (RemoteException | NotBoundException e) {
			// TODO Auto-generated catch block
			//						e.printStackTrace();
			throw new DSException("Destination host not reachable: a network error has occured or the testing environment simulates that the destination host is failed");
		}
	}
	
	
	public AppendEntriesResponse appendEntries (
			Host destination,
			long term,
			String leaderId,
			int prevLogIndex,
			long prevLogTerm,
			List<LogEntry> entries,
			int commitIndex
			) throws Exception{
		
    	if (!ActivitySimulation.getInstance().isConnected()){
    		throw new DSException("Sender is disconnected: Host tried to send a appendEntries RPC while the testing environment simulates it is failed");
    	}

    	// TODO: posar un retard "realista" per la comunicació en un cluster (un temps variable)
		try {
			Thread.sleep(3); 	// TODO: !!!!! decide the amount of time to block !!!!!!!!!!!!!!!!!
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			System.out.println("RMIsd -- AppendEntries -- sleep cacth: "+e);
//			e.printStackTrace();
		}

		Registry registry;
		try {
			registry = LocateRegistry.getRegistry(destination.getAddress(),	destination.getPort());
			Raft stub = (Raft) registry.lookup(
					"rmi://"
							+ destination.getAddress()
							+ ':'
							+ destination.getPort()
							+ "/Raft-"
							+ destination.getId()
					);
			return stub.appendEntries (term, leaderId, prevLogIndex, prevLogTerm, entries, commitIndex);
		} catch (RemoteException | NotBoundException e) {
			// TODO Auto-generated catch block
//						e.printStackTrace();
			throw new DSException("Destination host not reachable: a network error has occured or the testing environment simulates that the destination host is failed");
		}
	}
}