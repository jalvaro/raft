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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import edu.uoc.dpcs.lsim.LSimFactory;
import edu.uoc.dpcs.lsim.exceptions.LSimExceptionMessage;

import recipesService.activitySimulation.ActivitySimulation;
import recipesService.communication.Host;
import recipesService.communication.Hosts;
import recipesService.test.server.FinalServerResult;
import recipesService.test.server.PartialResult;
import recipesService.test.server.ServerResult;
import recipesService.test.server.TestServerMessage;
import recipesService.test.server.TestServerMsgType;
import util.Serializer;

import lsim.LSimDispatcherHandler;
import lsim.application.ApplicationManager;
import lsim.application.handler.DummyHandler;
import lsim.application.handler.ResultHandler;
import lsim.worker.LSimWorker;
import lsimElement.recipesService.WorkerInitHandler;
import lsimElement.recipesService.WorkerInitHandlerLSim;
import lsimElement.recipesService.WorkerStartHandler;


/**
 * @authors Joan-Manuel Marques
 * February, July 2013
 *
 */
public class Server implements ApplicationManager {

	// Data to store recipes and other information
	private ServerData serverData;
	
//	private String id;	
	String testServerAddress = "localhost";
	int testServerPort;
	
	/**
	 * Initialization operations
	 */
	
	public Server(){
		
	}
	
	
	/**
	 * Method to start a client.
	 * Connects to a group.
	 * Obtains list of hosts that form the group
	 * The timers for activity simulation
	 * are set.
	 * @param args
	 */

	public static void main(String[] args){

		// properties
		Properties properties = new Properties();

		int portTestServer = 20000;
		
		Server server = new Server();

		String groupId = "DefaultId--ChangeIt";
		//
		List<String> argsList = Arrays.asList(args);
		try {
              //load a properties file
    		properties.load(new FileInputStream("config.properties"));

			server.testServerAddress = properties.getProperty("testServerAddress");
			if (argsList.contains("-h")){
				int i = argsList.indexOf("-h");
				server.testServerAddress = args[i+1];
			}

			portTestServer = Integer.parseInt(properties.getProperty("testServerPort"));
			if (argsList.contains("-p")){
				int i = argsList.indexOf("-p");
				portTestServer = Integer.parseInt(args[i+1]);
			}
			
			groupId = properties.getProperty("groupId");
			if (argsList.contains("-g")){
				int i = argsList.indexOf("-g");
				groupId = args[i+1];
			}
			
		} catch (Exception e){
			System.err.println("-- Server error ---> "+e.getMessage());			
			System.err.println("Server error. Incorrect arguments");
			System.err.println("Args:");
			System.err.println("\t-p <port of TestServer>: TestServer port");
			System.err.println("\t-h <IP address of TestServer>: IP Address of TestServer");
			System.err.println("\t-g <groupId>: groupId");
			System.exit(1);
		}
			// ------------------------------------------------
	        // Initialize and start
			// ------------------------------------------------
			
		// init
		server.initializeAndStart(portTestServer, groupId);
		// simulated mode
		try{
			server.simulatedMode();
		}catch (Exception e){
			System.err.println(e.getMessage());			
			e.printStackTrace();			
			System.exit(1);
		}
	}
	
	private void initializeAndStart(int port, String groupId){
		System.out.println("Server -- Initializing ... TestServer port: "+port+" TestServer host: "+testServerAddress);
//		System.out.println("Server -- Initializing ...");
		
        // connect to TestServer
        Host localHost = null;
        Hosts participants = null;
        try {
          	Socket socket = new Socket(testServerAddress, port);
        	ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());

        	//
        	// 1.initialize Raft data structures and Simulation data 
        	//
        	        	
        	out.writeObject(new TestServerMessage(TestServerMsgType.GET_PORT, groupId, null));

        	ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        	testServerPort = (int) in.readObject();
        	// get initialization information from TestServer
        	// (initialization is done using a WorkerInitHandler to maintain consistency with LSim mode of execution)
 
    		in.close();
        	out.close();
        	socket.close();
        	
           	socket = new Socket(testServerAddress, testServerPort);
        	out = new ObjectOutputStream(socket.getOutputStream());
        	in = new ObjectInputStream(socket.getInputStream());

         	WorkerInitHandler init = new WorkerInitHandler();
    		init.execute(in.readObject());

    		// get references to serverData and local host information
    		serverData = init.getServerData();
    		localHost = init.getLocalHost();

     		// send localHost to TestServer
        	out.writeObject(localHost);

        	in.close();
        	out.close();
        	socket.close();
        	//
        	// 2. sleep (some time) to give time to all servers to get initialization data and send localHost information 
        	Thread.sleep(10000); // 10 seconds
        	//
        	
        	//
        	// 3. obtain the list of participating servers
        	// 
        	
        	// connect to TestServer
        	socket = new Socket(testServerAddress, testServerPort);
           	in = new ObjectInputStream(socket.getInputStream());

         	// get list of participating servers
           	WorkerStartHandler start = new WorkerStartHandler();
        	start.execute(in.readObject());

        	participants = start.getParticipants(localHost);

        	in.close();
        	socket.close();
        } catch (ClassNotFoundException e) {
        	// TODO Auto-generated catch block
        	e.printStackTrace();
        }catch (UnknownHostException e) {
        	System.err.println("Unknown server: " + testServerAddress);
        	System.exit(1);
        } catch (IOException e) {
        	System.err.println("Server -- initialize and obtain list of participants -- Couldn't get I/O for "
        			+ "the connection to: " + testServerAddress);
        	System.exit(1);
        } catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    
    	System.out.println("-- *** --> Server -- local host: "+ localHost);
    	System.out.println("-- *** --> Server -- participants: "+participants.getIds());
    	
       	// 4. start Raft protocol
    	serverData.start(participants);

	}
	
	private void endAndSendResults(){
		// sleep a time to finish current connections 
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// print final results
//		System.out.println("Final Result ");
//		System.out.println("============ ");
//		System.out.println("[" + serverData.getId() + "]" + serverData.getRecipes().toString());
//		System.out.println("[" + serverData.getId() + "]" + serverData.getLog().toString());
//		System.out.println("[" + serverData.getId() + "]" + "summary: " + serverData.getSummary().toString());
//		System.out.println("[" + serverData.getId() + "]" + "ack: " + serverData.getAck().toString());

		
		// ------------------------------------------------
		// send final results to TestServer
		// ------------------------------------------------
		
		// create a result's object that contains the data structures of this server
		ServerResult sr = new ServerResult(
				serverData.getGroupId(),
				serverData.getServerId(),
				serverData.getRecipes(),
				serverData.getLog(),
				serverData.getCurrentTerm(),
				serverData.getLeaderId()
				);

		// send final result to localTestServer
		try {
			Socket socket = new Socket(testServerAddress, testServerPort);
			ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.writeObject(new FinalServerResult(sr));
            
            out.close();
            socket.close();
        } catch (UnknownHostException e) {
            System.err.println("Unknown server: " + testServerAddress);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Server -- sending final results -- Couldn't get I/O for "
                               + "the connection to: " + testServerAddress);
            System.exit(1);
        }
	}

	private void simulatedMode(){
		// ------------------------------------------------
		// ------------------------------------------------
		// Recipes Service
		
		// sleep and print data structures until the end of the simulation 
		do{
			try {
				Thread.sleep(500); //120000
//				System.out.println("[" + serverData.getId() + "]" + serverData.getRecipes().toString());
//				System.out.println("[" + serverData.getId() + "]" + serverData.getLog().toString());
//				System.out.println("[" + serverData.getId() + "]" + "summary: " + serverData.getSummary().toString());
//				System.out.println("[" + serverData.getId() + "]" + "ack: " + serverData.getAck().toString());
			} catch (InterruptedException e) { 
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}while (ActivitySimulation.getInstance().isSimulatingActivity());
	
		
		// ------------------------------------------------
		// Send partial results
		// ------------------------------------------------

//		int numIterations = SimulationData.getInstance().getExecutionStop() / SimulationData.getInstance().getSetSamplingTime();;
//		for (int iteration = 0; iteration < numIterations; iteration++){
//			// create a result's object that contains the data structures of this server
//			ServerResult sr =
//					new ServerResult(
//							serverData.getGroupId(), 
//							serverData.getServerId(),
//							serverData.getRecipes(),
//							serverData.getLog(),
//							serverData.getCurrentTerm(),
//							serverData.getLeaderId()
//					);
//
//			try {
//				Socket socket = new Socket(testServerAddress, testServerPort);
//				ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
//				out.writeObject(new PartialResult(iteration, sr));
//
//				out.close();
//				socket.close();
//			} catch (UnknownHostException e) {
//				System.err.println("Unknown server: " + testServerAddress);
//				System.exit(1);
//			} catch (IOException e) {
//				System.err.println( "--- Server -- send partial results --->"
//						+ "Couldn't get I/O for "
//						+ "the connection to: " + testServerAddress
//						+ " Server: " + serverData.getServerId()
//						+ " iteration: " + iteration
//						);
//				System.exit(1);
//			}
//			try {
//				Thread.sleep(SimulationData.getInstance().getSetSamplingTime());
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
		 
		// ------------------------------------------------
		// End simulation and send final results
		// ------------------------------------------------

		// waits some time before sending final results
		try {
			Thread.sleep((ActivitySimulation.getInstance().getExecutionStop() / 2));
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// end and send final results
		endAndSendResults();
		
		// waits some time before finishing
		try {
			Thread.sleep((ActivitySimulation.getInstance().getExecutionStop() / 2));
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.exit(0);
	}

	
	// ****************************************************
	// ****************************************************
	// ******** LSIM methods
	// ****************************************************
	// ****************************************************
	
	@Override
	public boolean isAlive() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public void start() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void start(LSimDispatcherHandler dispatcher) {
//		LSimFactory.getEvaluatorInstance().setDispatcher(dispatcher);
		try{
			process(dispatcher);
		}catch(RuntimeException e){
			LSimFactory.getWorkerInstance().logException(
					new LSimExceptionMessage(
							"",
							e,
							null
							)
					);
		}
	}
	
	private void process(LSimDispatcherHandler dispatcher){
		LSimWorker lsim = LSimFactory.getWorkerInstance();
		lsim.setDispatcher(dispatcher);

		// set maximum time (minutes) that is expected the experiment will last
		lsim.startTimer(30);
		
		
		// ------------------------------------------------
		// init
		// ------------------------------------------------
		
		WorkerInitHandlerLSim init = new WorkerInitHandlerLSim();
//		WorkerInitHandler init = new WorkerInitHandler();
		lsim.init(init);

		// getting parameters
		serverData = init.getServerData();
		Host localHost = init.getLocalHost();
		LSimFactory.getWorkerInstance().log(
				"",
				"--- **** ---> worker ident: " + lsim.getIdent() +
					'\n' +
					"--- **** ---> lsim.getLSimElementAddress(\"Wserver0\")"+lsim.getLSimElementAddress("Wserver0") +
					'\n' +
					"--- **** ---> lsim.getLSimElementAddress(lsim.getIdent())"+lsim.getLSimElementAddress(lsim.getIdent()) +
					'\n' +
					"--- **** ---> lsim.getLSimElementAddress(lsim.getIdent())"+lsim.getLSimElementAddress("server")
				);
//		System.out.println("--- **** ---> worker ident: " + lsim.getIdent());
//		System.out.println("--- **** ---> lsim.getLSimElementAddress(\"Wapplication0\")"+lsim.getLSimElementAddress("Wapplication0"));
//		System.out.println("--- **** ---> lsim.getLSimElementAddress(lsim.getIdent())"+lsim.getLSimElementAddress(lsim.getIdent()));
//		System.out.println("--- **** ---> lsim.getLSimElementAddress(lsim.getIdent())"+lsim.getLSimElementAddress("server"));

	
		// ------------------------------------------------
		// start
		// ------------------------------------------------
		
		WorkerStartHandler start = new WorkerStartHandler();
		lsim.start(start);
		
		// get participating hosts
		Hosts participants = start.getParticipants(localHost);
		
		LSimFactory.getWorkerInstance().log(
				"",
				"-- *** --> Server -- local host: "+ localHost +
					'\n' +
					"-- *** --> Server -- participants: "+participants
				);
//		System.out.println("-- *** --> Server -- local host: "+ localHost);
//		System.out.println("-- *** --> Server -- participants: "+participants);
		
		// start
		serverData.start(participants);
			
		
		// ----------------------------------------------- -
		// Recipes Service
		// ------------------------------------------------
		
		// sleep and print data structures until the end of the simulation 
		do{
			try {
				Thread.sleep(500); //120000
//				Thread.sleep(60000); //120000
//				System.out.println(serverData.getRecipes().toString());
//				System.out.println(serverData.getLog().toString());
//				System.out.println("Summary: " + serverData.getSummary().toString());
//				System.out.println("Ack: " + serverData.getAck().toString());
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}while (ActivitySimulation.getInstance().isSimulatingActivity());

		
		// ------------------------------------------------
		// Send partial results
		// ------------------------------------------------

//		int numIterations = SimulationData.getInstance().getExecutionStop() / SimulationData.getInstance().getSetSamplingTime();
//		for (int iteration = 0; iteration < numIterations; iteration++){
////			serverData.updateLocalSummaryWithCurrentTimestamp();
//			// create a result's object that contains the data structures of this server
//			ServerResult sr = new ServerResult(
//					serverData.getGroupId(),
//					serverData.getId(),
//					serverData.getRecipes()
//					);
//
//			try {
//				lsim.sendResult(new ResultHandler(Serializer.serialize(new PartialResult(iteration, sr))));
//			} catch (IOException e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			}
//
//			try {
//				Thread.sleep(SimulationData.getInstance().getSetSamplingTime());
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//		
//		// print final data structures of this server
//		LSimFactory.getWorkerInstance().log(
//				"",
//				"Final Result " +
//					'\n' +
//					"============ " +
//					'\n' +
//					"-- *** --> Server: "+ serverData.getId() +
//					'\n' +
//					serverData.getRecipes().toString() +
//					'\n' +
//					serverData.getLog().toString() +
//					'\n' +
//					"Summary: " + serverData.getSummary().toString() +
//					'\n' +
//					"Ack: " + serverData.getAck().toString()
//				);
////		System.out.println("Final Result ");
////		System.out.println("============ ");
////		System.out.println(serverData.getRecipes().toString());
////		System.out.println(serverData.getLog().toString());
////		System.out.println("Summary: " + serverData.getSummary().toString());
////		System.out.println("Ack: " + serverData.getAck().toString());
		
		
		// ------------------------------------------------
		// send final results
		// ------------------------------------------------
		
		// waits some time before sending final results
		try {
			Thread.sleep((ActivitySimulation.getInstance().getExecutionStop() / 2) + 3000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// create a result's object that contains the data structures of this server
		ServerResult sr = new ServerResult(
				serverData.getGroupId(),
				serverData.getServerId(),
				serverData.getRecipes(),
				serverData.getLog(),
				serverData.getCurrentTerm(),
				serverData.getLeaderId()
				);
		
		// send result's object to the evaluator
		try {
			lsim.sendResult(new ResultHandler(Serializer.serialize(new FinalServerResult(sr))));
		} catch (IOException e) {
			// TODO Auto-generated catch block
//			 e.printStackTrace();
			lsim.logException(new LSimExceptionMessage("", e, null));
		}
		
		// waits some time before finishing
		try {
			Thread.sleep((ActivitySimulation.getInstance().getExecutionStop() / 2));
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// ------------------------------------------------
		// stop
		// ------------------------------------------------
		
		lsim.stop(new DummyHandler());
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		
	}
}