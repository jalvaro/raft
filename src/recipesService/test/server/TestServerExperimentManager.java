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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Vector;

import recipesService.communication.Host;
import recipesService.communication.Hosts;
import recipesService.data.AddOperation;
import recipesService.data.OperationType;
import recipesService.data.Recipes;
import recipesService.raft.dataStructures.LogEntry;
import recipesService.test.client.Clients;
import util.Serializer;

/**
 * @author Joan-Manuel Marques
 * February 2013
 *
 */

public class TestServerExperimentManager extends Thread{
	private ServerSocket serverSocket;
	private ExperimentData experimentData;
	private boolean logResults;
	private String path;

	// to run student and teacher solutions together
	final int NUM_TEACHER_SERVERS = 2;
	final int NUM_STUDENT_SERVERS = 3;
	private boolean instantiateServers;
	private int testServerPort= 20000;
	
	//
	// START
	//
	public TestServerExperimentManager(boolean instantiateServers, int testServerPort){
		this.instantiateServers = instantiateServers;
		this.testServerPort = testServerPort;
	}
	
	public void setServerSocket(ServerSocket serverSocket) {
		this.serverSocket = serverSocket;
	}

	public void setExperimentData(ExperimentData experimentData) {
		this.experimentData = experimentData;
	}

	public void setLogResults(boolean logResults) {
		this.logResults = logResults;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public void run() {
		//
		// Configuration of participating servers
		//

		// participating hosts
		List<Object> participants = new Vector<Object>();
		Hosts hosts = new Hosts();
		
		// receives info of participating servers
		// sends initial values for the algorithm and simulation		
		Socket clientSocket = null;
		ObjectInputStream in = null;
		ObjectOutputStream out = null;

		final List<String> params = experimentData.getParams();
		System.out.println("TestServerExperimentManager -- params: "+params);

		int numNodes = experimentData.getNumNodes();
		String phase = (String) params.get(13);
		
		// in case of running together student's implementation and teacher's implementation,
		// check if the number of student's implementation is NUM_STUDENT_SERVERS 
		String groupId = params.get(0);
		if (instantiateServers){
			if (numNodes != NUM_STUDENT_SERVERS){
				FileWriter outputStream = null;
				String result = "Error. The number of servers running the student's implementation should be: "+NUM_STUDENT_SERVERS;
				if (logResults){
					File file = new File(path, groupId);
					try {
						//				outputStream = new FileWriter(results.get(0).getGroupId(),true);
						outputStream = new FileWriter(file,true);
						DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
						outputStream.append(groupId
								+ '\t' + (dateFormat.format(new java.util.Date())).toString() 
								+ '\t' + result
								+ '\n');
						outputStream.close();
					} catch (IOException e) {
						e.printStackTrace();
					} finally{
						if (outputStream != null) {
							try {
								outputStream.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
				}
				System.out.println("\n\n");
				System.out.println("*********** "+result);
				System.out.println("\n\n");
				return;
			}
			numNodes += NUM_TEACHER_SERVERS;

			// Execute script that runs NUM_TEACHER_SERVERS
			try {
				Runtime.getRuntime().exec("./startServers.sh "+NUM_TEACHER_SERVERS+" -p "+testServerPort+" -g "+params.get(0));
//				Runtime.getRuntime().exec("/home/marques/eclipseProjects/SD/2013t-SD/2013t/scripts/startServers.sh "+NUM_TEACHER_SERVERS+" -p "+20000);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		
		// send params to student's implementation and receive its id
		for (int i = 0 ; i<numNodes ; i++){
			try {
				serverSocket.setSoTimeout(45000);// sets a timeout. A read() call on the InputStream associated with this Socket will block for only this amount of time (milliseconds) 
				clientSocket = serverSocket.accept();
				out = new ObjectOutputStream(clientSocket.getOutputStream());
				in = new ObjectInputStream(clientSocket.getInputStream());

				// send initialization parameters
				out.writeObject(params);

				// obtain the address of the remote participant server
				// ** method Serializer.serialize() is used to serialize node information
				// ** used to maintain compatibility with LSim, that requires a serialization
				Host host = (Host) in.readObject();
				participants.add(Serializer.serialize(host));
				hosts.add(host);

				in.close();
				out.close();
				clientSocket.close();
			} catch (SocketTimeoutException acceptException) {
				System.out.println("Less than "+ numNodes+" Serveres asked the initialization parameters");
				if (logResults){
					File file = new File(path, experimentData.getGroupId());
					try {
						//				outputStream = new FileWriter(results.get(0).getGroupId()+".data",true);
						FileWriter outputStream = new FileWriter(file,true);
						DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
						outputStream.append(experimentData.getGroupId()
								+ '\t' + (dateFormat.format(new java.util.Date())).toString() 
								+ "\tLess than "+ numNodes+" Serveres asked the initialization parameters"
								+ '\n');
						outputStream.close();
					} catch (IOException e) {
						e.printStackTrace();
					} 
				}
				System.exit(50);
			} catch (IOException e) {
				System.err.println("TestServerExperimentManager -- Accept failed.");
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}

		// Calculate the number of required results
		int numRequiredResults = ( (numNodes * experimentData.getPercentageRequiredResults()) / 100 + 1 );
		if (experimentData.getPercentageRequiredResults() == 100){
			numRequiredResults = numNodes;
		}
		
		// sends the list of participating servers to servers
		for (int i = 0 ; i<numNodes ; i++){
			try {
				serverSocket.setSoTimeout(45000);// sets a timeout. A read() call on the InputStream associated with this Socket will block for only this amount of time (milliseconds) 
				clientSocket = serverSocket.accept();
				out = new ObjectOutputStream(clientSocket.getOutputStream());
				out.writeObject(participants);
				out.close();
				clientSocket.close();
			} catch (SocketTimeoutException acceptException) {
				System.out.println("Less than "+ numNodes+" Servers asked the list of participants");
				if (logResults){
					File file = new File(path, experimentData.getGroupId());
					try {
						//				outputStream = new FileWriter(results.get(0).getGroupId()+".data",true);
						FileWriter outputStream = new FileWriter(file,true);
						DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
						outputStream.append(experimentData.getGroupId()
								+ '\t' + (dateFormat.format(new java.util.Date())).toString() 
								+ "\tLess than "+ numNodes+" Serveres asked the list of participants"
								+ '\n');
						outputStream.close();
					} catch (IOException e) {
						e.printStackTrace();
					} 
				}
				System.exit(50);
			} catch (IOException e) {
				System.err.println("Accept failed.");
				e.printStackTrace();
			}
		}

		// ************
		// ** Activity Generation
		// ************

		// Recipes
		Recipes recipesSent = new Recipes(); // contains all recipes issued and sent to a RaftCluster
		Recipes recipes = new Recipes(); // contains all recipes committed by the raft cluster 
		
		// Create and run clients
		String recipeSurviveTitle = Math.abs(new Random().nextInt(10000)) + "-";
		String recipeDeadTitle    = Math.abs(new Random().nextInt(10000)) + "-";
		Clients clients = new Clients();
		clients.setDataAndRunClients(
				recipesSent,
				recipes,
				params,
				hosts,
				recipeSurviveTitle,
				recipeDeadTitle
				);
		
		// ************
		// ** Results
		// ************

		// receives results from nodes 
		List<ServerResult> finalResults = new Vector<ServerResult>();
		boolean end = false;
		HashMap<Integer, List<ServerResult>> allResults = new HashMap<Integer, List<ServerResult>>();
		
		
		try {
			serverSocket.setSoTimeout(900000);// sets a timeout. A read() call on the InputStream associated with this Socket will block for only this amount of time (milliseconds) 
			do{
				clientSocket = serverSocket.accept();
				in = new ObjectInputStream(clientSocket.getInputStream());
				try {
					ResultBase result = (ResultBase)in.readObject();
					switch(result.type()){
					case PARTIAL:
						Integer iteration = ((PartialResult)result).getIteration();
						List<ServerResult> results = null;
						if (allResults.containsKey(iteration)){
							results = allResults.get(iteration);
						} else{
							results = new Vector<ServerResult>();
						}
						results.add(result.getServerResult());
						allResults.put(iteration, results);
						System.out.println("##### [iteration: "+iteration
								+"] partial result from server: " + result.getServerResult().getHostId());
						break;
					case FINAL:
						finalResults.add(result.getServerResult());
						System.out.println("##### Final result from server: " + result.getServerResult().getHostId());
						if (finalResults.size() == numNodes){
//						if (finalResults.size() == numRequiredResults){
							end = true;
						}
						break;
					}
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
				in.close();
				clientSocket.close();
				serverSocket.setSoTimeout(45000);// sets a timeout. A read() call on the InputStream associated with this Socket will block for only this amount of time (milliseconds) 
			}while(!end);
		} catch (SocketTimeoutException acceptException) {
			System.out.println("*********** Accept timeout");
		} catch (IOException e){
			e.printStackTrace();
		}
		
		
		if (finalResults.size() < numRequiredResults){
			String result = "Unable to evaluate results due to: Not enough Servers where connected at the moment of finishing the Activity Simulation phase."
					+ "\tRecieved Results: "+finalResults.size()
					+ "\tnumRequiredResults: "+numRequiredResults
					;
			if (logResults){
				File file = new File(path, groupId);
				FileWriter outputStream = null;
				try {
					//				outputStream = new FileWriter(results.get(0).getGroupId()+".data",true);
					outputStream = new FileWriter(file,true);
					DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
					outputStream.append(groupId
							+ '\t' + (dateFormat.format(new java.util.Date())).toString() 
							+ '\t' + result
							+ '\n');
					outputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				} 
			}
			return;
//			System.exit(30);
		}

		if (instantiateServers && !resultsFromTwoDifferentServers(finalResults)){
			String result = "Unable to evaluate results due to: Only results from student's server. Evalution requires receiving results from teacher's Server. Check config.properties of Teacher's implementation to be sure that groupId field is the teacher's Id"
					;
			if (logResults){
				File file = new File(path, groupId);
				FileWriter outputStream = null;
				try {
					//				outputStream = new FileWriter(results.get(0).getGroupId()+".data",true);
					outputStream = new FileWriter(file,true);
					DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
					outputStream.append(groupId
							+ '\t' + (dateFormat.format(new java.util.Date())).toString() 
							+ '\t' + result
							+ '\n');
					outputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				} 
			}
			System.err.println(">>>>>>>>>>>>>>>>>>>>>>> "+result);
			return;
		}
			
		try {
			serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("\n\n");
		System.out.println("================================================");
		System.out.println("END OF EVALUATION");
		System.out.println("\n");
		System.out.println("RESULTS");
		System.out.println("=======");

		// evaluate final results
		boolean equal = false;

		System.out.println("##### [" + finalResults.get(0).getHostId() + "] Result:\n " + finalResults.get(0));

		FileWriter outputStream = null;
		if (logResults){
			File file = new File(path, groupId+".data");
			try {
				//				outputStream = new FileWriter(results.get(0).getGroupId()+".data",true);
				outputStream = new FileWriter(file,true);
				DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
				outputStream.append("\n##### " + finalResults.get(0).getGroupId() 
						+ '\t' + (dateFormat.format(new java.util.Date())).toString()
						);
				outputStream.append("\n----- [" + finalResults.get(0).getHostId() + "] Result:\n " + finalResults.get(0));
			} catch (IOException e) {
				e.printStackTrace();
			} 
		}
		equal = true;
		for (int i = 1 ; i<finalResults.size() && equal; i++){
			equal = equal && finalResults.get(0).equals(finalResults.get(i));
			if (!equal){
				System.out.println("##### ["+finalResults.get(i).getHostId()+"] Result:\n " + finalResults.get(i));
				if (logResults){
					try {
						DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
						outputStream.append("\n##### " + finalResults.get(i).getGroupId() 
								+ '\t' + (dateFormat.format(new java.util.Date())).toString());
						outputStream.append("\n----- ["+finalResults.get(i).getHostId()+"] Result:\n " + finalResults.get(i));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}

		// calculate in which iteration nodes converged
		boolean converged = false;
		int convergenceIteration = -1;
		for (int it = 0; allResults.containsKey(Integer.valueOf(it))&& !converged; it++){
			List<ServerResult> results = allResults.get(Integer.valueOf(it));
			converged = (allResults.size() >= finalResults.size());
			for (int i = 1 ; i<results.size() && converged; i++){
				converged = converged && results.get(0).equals(results.get(i));
			}
			if (converged){
				convergenceIteration = it;
			}
		}

		if (phase.equals("4.1")) {
			int deadCount = 0;
			int liveCount = 0;
			for (ServerResult serverResult : finalResults) {
				undoLastAdds(serverResult);
				deadCount = deadCount + countRecipes(serverResult, recipeDeadTitle);
				liveCount = liveCount + countRecipes(serverResult, recipeSurviveTitle);
			}
			equal = equal && (deadCount == 0) && (liveCount > 0); 
//			System.out.println("\n\t" + 
//					"Having "+deadCount+" dead results <<< "+
//					(finalResults.size() * clients.getCount()) + " dead-recipes:" + recipeDeadTitle + "*");			
//			result += "\n\t" + 
//					"Having "+deadCount+" dead results <<< "+
//					(finalResults.size() * clients.getCount()) + " dead-recipes:" + recipeDeadTitle + "*";
			if (logResults){
				try {
					DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
					outputStream.append("\n\n##### Phase 4.1" 
//							+ finalResults.get(i).getGroupId() 
							+ '\t' + (dateFormat.format(new java.util.Date())).toString()
							);
					if (deadCount == 0 && liveCount > 0){
						outputStream.append('\n' + "Phase 4.1 is correct (live: "+liveCount+")");
					} else {
						outputStream.append(
							'\n' + "ERROR in Phase 4.1: Raft re-executed an operation multiple times"
							+ '\n' + "i.e. it has received:"
							+ '\n' + '\t' + "(a) AddOperation(recipeN, timestampX)"
							+ '\n' + '\t' + "(b) RemoveOperation(recipeN.title,timestampY)"
							+ '\n' + '\t' + "(c) AddOperation(recipeN, timestampX)"
							+ '\n' + "It shouldn't have executed the second AddOperation(recipeN, timestampX)"
							);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		if (logResults){
			try {
				outputStream.append("\n================================================\n");
			} catch (IOException e) {
				e.printStackTrace();
			} finally{
				if (outputStream != null) {
					try { outputStream.close(); }
					catch (IOException e) { e.printStackTrace(); }
				}
			}
		}
		
		// write final result
		System.out.println("\n\n");
		String result = "phase " + phase;
		if (equal){
			result += '\t' + "Correct";
			if (convergenceIteration == -1){
				//				result += '\n' + '\t' + "Nodes converged at the last iteration ";
			} else{
				//				result += '\n' + '\t' + "Nodes converged at the iteration " + convergenceIteration;
			}
		} else{
			result += '\t' + "Servers DON'T have coherent data";
		}

		
		System.out.println(result);
		
		System.out.println("\n\n");
		System.out.println("================================================");
		System.out.println("\n\n");

		if (logResults){
			File file = new File(path, groupId);
			try {
				//				outputStream = new FileWriter(results.get(0).getGroupId(),true);
				outputStream = new FileWriter(file,true);
				DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
				outputStream.append(groupId
						+ '\t' + (dateFormat.format(new java.util.Date())).toString() 
						+ '\t' + result
						+ '\n');
				outputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			} finally{
				if (outputStream != null) {
					try { outputStream.close();	} 
					catch (IOException e) {	e.printStackTrace(); }
				}
			}
		}
		
		System.out.println("\n\n");
		System.out.println("*********** num received results: "+finalResults.size());
		System.out.println("*********** % received results: "+(finalResults.size()*100)/numNodes);
		System.out.println("*********** minimal required number of results: "+numRequiredResults);
		System.out.println("\n\n");

//		if (equal){
//			System.exit(10);
//		} else{
//			System.exit(20);
//		}
	}

	private static void undoLastAdds(ServerResult serverResult) {
		Set<String> seenClients = new HashSet<>();
		List<LogEntry> reverseLog = new ArrayList<LogEntry>(serverResult.getLog()); Collections.reverse(reverseLog);
		for (LogEntry entry : reverseLog) {
			String clientId = entry.getCommand().getTimestamp().getHostId();
			if (!seenClients.contains(clientId)) {
				seenClients.add(clientId);
				if (entry.getCommand().getType().equals(OperationType.ADD)) {
					String recipeTitle = ((AddOperation)entry.getCommand()).getRecipe().getTitle();
					serverResult.getRecipes().remove(recipeTitle);
					//System.err.println("UNDO::"+serverResult.getHostId()+"::"+'_'+"::"+recipeTitle);
				}
			}
		}
	}
	private static int countRecipes(ServerResult serverResult, String recipeTitleHead) {
		int count = 0;
		for (String title : serverResult.getRecipes().getTitles()) {			
			if (title.startsWith(recipeTitleHead)) {
				count = count + 1;
			}
		}
		return count;
	}


	private boolean resultsFromTwoDifferentServers(List<ServerResult> finalresults){
		boolean resultsFromTwoDifferentServers = false;
		String oneGroupId = finalresults.get(0).getGroupId();
		for (int i = 1; (i < finalresults.size()) && !resultsFromTwoDifferentServers; i++){
			resultsFromTwoDifferentServers = ! oneGroupId.equals(finalresults.get(i).getGroupId());
		}
		return resultsFromTwoDifferentServers;
	}
}
