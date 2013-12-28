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

package recipesService.test.client;


import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import edu.uoc.dpcs.lsim.LSimFactory;
import edu.uoc.dpcs.lsim.exceptions.LSimExceptionMessage;
import edu.uoc.dpcs.lsim.utils.LSimParameters;

import lsim.LSimDispatcherHandler;
import lsim.application.ApplicationManager;
import lsim.application.handler.DummyHandler;
import lsim.worker.LSimWorker;
import lsimElement.recipesService.WorkerStartHandler;
import lsimElement.recipesService.client.WorkerInitClientHandler;

import recipesService.communication.Host;
import recipesService.communication.Hosts;
import recipesService.data.AddOperation;
import recipesService.data.Operation;
import recipesService.data.Recipe;
import recipesService.data.Recipes;
import recipesService.data.RemoveOperation;
import recipesService.data.Timestamp;
import recipesService.raft.Raft;

/**
 * @author Joan-Manuel Marques
 * July 2013
 *
 */

public class Clients implements ApplicationManager{

	private Hosts hosts; 
	private Recipes recipesSent; 
	private Recipes recipes;
	private String phase;

	private long simulationDelay;
	private long simulationPeriod;
	private long simulationStop;
	private double probCreate;
	private double probDel;
	private boolean deletionActivated;

	private int numClients;

	private long delay = 10000;
	private boolean endSimulation = false;

	Random rnd = new Random();

	private String recipeSurviveTitle;
	private String recipeDeadTitle;
	
	public Clients(){
	}

	public void setDataAndRunClients (
			Recipes recipesSent,
			Recipes recipes,
			List<String> params,
			Hosts hosts,
			String recipeSurviveTitle,
			String recipeDeadTitle
			){
		this.recipesSent = recipesSent;
		this.recipes = recipes;
		this.hosts = hosts;
		this.recipeSurviveTitle = recipeSurviveTitle;
		this.recipeDeadTitle = recipeDeadTitle;

		simulationStop = Long.parseLong((String) params.get(2))*1000;
		simulationDelay = Long.parseLong((String) params.get(4))*1000;
		simulationPeriod = Long.parseLong((String) params.get(5))*1000;
		probCreate = Double.parseDouble((String)params.get(8));
		probDel = Double.parseDouble((String)params.get(9));		
		deletionActivated = (!(Double.parseDouble((String)params.get(9)) == 0.0));
		numClients = Integer.parseInt((String) params.get(12));
		phase = (String)params.get(13);

		// run client instances
		runClients();
	}

	
	public int getCount() {
		return numClients;
	}

	private void runClients(){

		// delay to allow servers start
		try {
			Thread.sleep(delay);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// run clients
		for (int i=1; i<=numClients;i++){
			final int n = i;
			new Timer(true).schedule(
					new TimerTask(){
						private String clientId = "Client"+n;
						private int seqnumber = 0;

						private Host leader = null;

						@Override
						public void run() {
							if (endSimulation){
								this.cancel();
								}
	
							// generate activity

							// if not a phase that generates activity
							if (!(
									phase.equals("all")
									|| phase.equals("3")
									|| phase.equals("4")
									|| phase.equals("4.1")
									|| phase.equals("4.2")
									|| phase.equals("only4.1")
									)
								){
								//			System.out.println("This phase doesn't generate activity");
								return;
							}
							
							double a=rnd.nextDouble();
							
							// Synthetic simulation of adding and removal of recipes  
							if (phase.equals("3")
									|| phase.equals("all")
									|| phase.equals("4")
									|| phase.equals("4.2")
									|| phase.equals("4.1")
									) {
								//probability of creating a recipe
								if(a<probCreate){
									seqnumber++;
									System.out.println("\t["+clientId+"] Adds: "+clientId+"#"+seqnumber);

									Timestamp timestamp = newTimestamp();
									String recipeTitle = timestamp.toString();
									String recipeBody = recipeTitle +"-"+ getRandomRecipeBody();

									//TODO: improve the generation of recipe titles, recipe content and authors
									Recipe recipe = new Recipe(recipeTitle, recipeBody, clientId);

									recipesSent.add(recipe);
									if (send(new AddOperation(recipe, timestamp))){
										recipes.add(recipe);
										//				System.out.println("["+clientId+"] Recipes: "+recipes);
									}
								}
								//probability of deleting a recipe
								if(deletionActivated
										&& a>probCreate
										&& a<probCreate+probDel
										){
									// get a recipe among existing recipes
									String recipeTitle = recipes.getRandomRecipeTitle();
									if (recipeTitle != null){
										Timestamp timestamp = newTimestamp();
										System.out.println("\t["+clientId+"] Removes: "+recipeTitle+" (timestamp: "+timestamp);
										send(new RemoveOperation(recipeTitle, timestamp)); // TODO: no timestamp because a remove operation can be applied many times (if recipe doesn't exist won't be applied)
									}
								}
							}
							
							// Tests specific to phase 4.1
							if (phase.equals("only4.1")
									|| phase.equals("4.1")
									|| phase.equals("4")
									|| phase.equals("4.2")
									|| phase.equals("all")
									) {
								Timestamp timestamp0 = newTimestamp();
								Timestamp timestamp1 = newTimestamp();
								String recipeTitle = timestamp0.toString();
								String recipeBody = recipeTitle + getRandomRecipeBody();
								double probHalf = (probCreate + probDel) / 20;
//								double probHalf = (probCreate + probDel) / 2;
								
								if (0 <= a && a < probHalf) {
									Timestamp timestamp2 = newTimestamp();
									recipeTitle = recipeSurviveTitle+recipeTitle;
									Recipe recipe = new Recipe(recipeTitle, recipeBody, clientId);
									
									// Test 1
									// 	(First) a recipe is inserted by an AddOperation with timestamp timestamp0;
									//	(second) this recipe is removed by the RemoveOperation with timestamp1;
									//	(third) a new AddOperation with timestamp2 adds again the same recipe;
									//	(finally) the RemoveOperation with timesamp1 is re-issued again to remove the recipe. Raft shouldn't re-execute it
									//		(i.e. even though the recipeTile of the RemoveOperation with timestamp1 refers to the recipe inserted by the AddOpeartion 
									//		with timestamp2 it shouldn't be removed because the RemoveOperation with timestamp1 refers to AddOperation with timestamp0 
									//		and not to the AddOperation with timestamp2) 
									hardSend(new AddOperation(recipe, timestamp0));
									hardSend(new RemoveOperation(recipeTitle, timestamp1));
									hardSend(new AddOperation(recipe, timestamp2));
									hardSend(new RemoveOperation(recipeTitle, timestamp1));
								}
								if (probHalf <= a && a < probHalf + probHalf) {
									recipeTitle = recipeDeadTitle+recipeTitle;
									Recipe recipe = new Recipe(recipeTitle, recipeBody, clientId);
									
									// Test 2
									// Re-sending the AddOperation of a removed recipe. Second add shouldn't be executed.
									hardSend(new AddOperation(recipe, timestamp0));
									hardSend(new RemoveOperation(recipeTitle, timestamp1));
									hardSend(new AddOperation(recipe, timestamp0));
								}
							}
						}
						
						private String getRandomRecipeBody() {
							byte[] bytes=new byte[8];
							char[] chars=new char[8];
							byte mod=((byte)'z'-(byte)'a');
							rnd.nextBytes(bytes);
							for(int ii=0; ii<8; ii++){
								byte b=bytes[ii];
								if(b<0)
									b*=-1;
								b%=mod;
								chars[ii]=(char)((byte)'a'+b);
							}
							
							return String.valueOf(chars);
						}
						
						private Timestamp newTimestamp() {
							seqnumber++;
							return new Timestamp(clientId, seqnumber);
						}

						private void hardSend(Operation operation) {
							while (!endSimulation && !send(operation));
						}
						
						/**
						 * Sends the operation to raft cluster
						 * @param operation
						 * return true: if operation was committed; false: otherwise
						 */
						private boolean send(Operation operation){

							boolean sent = false;
							boolean stop = false;
							int numAttempts = 6; // TODO: parameterize the number of attempts of trying to send an operation
							Host sendTo;
							RequestResponse response;
							do{
								sendTo = leader;
								if (sendTo == null){
									sendTo = hosts.getRandomHosts(1).get(0);
								}
								Registry registry;
								try {
									//				System.out.println("["+clientId+"] sendTo: "+sendTo);
									registry = LocateRegistry.getRegistry(sendTo.getAddress(), sendTo.getPort());
									// RPC call
									// host.getAddress(), host.getPort());
									Raft stub = (Raft) registry.lookup(
											"rmi://"
													+ sendTo.getAddress()
													+ ':'
													+ sendTo.getPort()
													+ "/Raft-"
													+ sendTo.getId()
											);
									response = stub.Request(operation);		
									sent = response.isSucceeded();
									if (response.getLeader() == null){
										// response.getLeader() is null when simulatingActivity is false (i.e. simulation activity phase is finished)
										stop = true;
									}
									leader = hosts.getHost(response.getLeader());
								} catch (RemoteException | NotBoundException e) {
									// TODO Auto-generated catch block
									//						e.printStackTrace();
									System.err.println("["+clientId+"] Destination host "+sendTo+" not reachable: a network error has occured or the testing environment simulates that the destination host is failed");
//									leader = null;
									leader = hosts.getRandomHosts(1).get(0);
								}
								numAttempts--;
							}while (!sent && numAttempts>0 && !stop);
							return sent;
						}
					},
					simulationDelay*(1+(long)(2*rnd.nextDouble())),
					simulationPeriod
					);
		}

		// sleep until the end of the experiment
		try {
			// sleep for the expected duration of the experiment minus a delay to allow servers to start
			Thread.sleep(simulationStop-delay-delay);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		endSimulation=true;
	}

	//
	// LSim
	//

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
		// TODO Auto-generated method stub

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


//		// Recipes
		recipesSent = new Recipes(); // contains all recipes issued and sent to a RaftCluster
		recipes = new Recipes(); // contains all recipes committed by the raft cluster 

		// ------------------------------------------------
		// init
		// ------------------------------------------------

		WorkerInitClientHandler init = new WorkerInitClientHandler();
		//		WorkerInitHandler init = new WorkerInitHandler();
		lsim.init(init);

		// getting parameters
		LSimParameters params = init.getParameters();
		// 1. parameters from coordinator
		simulationDelay = Long.parseLong((String) ((LSimParameters)params.get("coordinatorLSimParameters")).get("simulationDelay"))*1000;
		simulationPeriod = Long.parseLong((String) ((LSimParameters)params.get("coordinatorLSimParameters")).get("simulationPeriod"))*1000;
		simulationStop = Long.parseLong((String) ((LSimParameters)params.get("coordinatorLSimParameters")).get("simulationStop"))*1000;
		probCreate = Double.parseDouble((String)((LSimParameters)params.get("coordinatorLSimParameters")).get("probCreate"));
		probDel = Double.parseDouble((String)((LSimParameters)params.get("coordinatorLSimParameters")).get("probDel"));		
		deletionActivated = (!(Double.parseDouble((String)((LSimParameters)params.get("coordinatorLSimParameters")).get("probDel")) == 0.0));
		phase = (String)((LSimParameters)params.get("coordinatorLSimParameters")).get("phase");
		recipeSurviveTitle = (String)params.get("recipeSurviveTitle");
		recipeDeadTitle = (String)params.get("recipeDeadTitle");
				
		// 2. parameters from "clients" worker in the xml
		numClients = Integer.parseInt((String) params.get("numClients"));


		LSimFactory.getWorkerInstance().log(
				"",
				"--- **** ---> worker ident: " + lsim.getIdent() +
				'\n' +
				"--- **** ---> lsim.getLSimElementAddress(\"Wclients0\")"+lsim.getLSimElementAddress("Wclients0") +
				'\n' +
				"--- **** ---> lsim.getLSimElementAddress(lsim.getIdent())"+lsim.getLSimElementAddress(lsim.getIdent()) +
				'\n' +
				"--- **** ---> lsim.getLSimElementAddress(lsim.getIdent())"+lsim.getLSimElementAddress("client")
				);
		//		System.out.println("--- **** ---> worker ident: " + lsim.getIdent());
		//		System.out.println("--- **** ---> lsim.getLSimElementAddress(\"Wapplication0\")"+lsim.getLSimElementAddress("Wapplication0"));
		//		System.out.println("--- **** ---> lsim.getLSimElementAddress(lsim.getIdent())"+lsim.getLSimElementAddress(lsim.getIdent()));
		//		System.out.println("--- **** ---> lsim.getLSimElementAddress(lsim.getIdent())"+lsim.getLSimElementAddress("client"));


		// ------------------------------------------------
		// start
		// ------------------------------------------------

		WorkerStartHandler start = new WorkerStartHandler();
		lsim.start(start);

		// get participating hosts
		hosts = start.getParticipants();

		LSimFactory.getWorkerInstance().log(
				"",
				"-- *** --> Client -- participants: "+hosts.getIds()
				);
		//		System.out.println(""-- *** --> Client -- participants: "+participants.getIds());


		// ------------------------------------------------
		// Clients
		// ------------------------------------------------
		// start client instances
		runClients();

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
