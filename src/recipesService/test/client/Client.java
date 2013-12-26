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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Properties;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import communication.rmi.CommunicationData;

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

public class Client extends Thread{
	private String clientId;
	private Hosts hosts;
	private String leader = null;
//	http://docs.oracle.com/javase/tutorial/java/javaOO/innerclasses.html

	private long simulationDelay;
	private long simulationPeriod;
	
	private int seqnumber;
	
	private Properties properties;
	
	// 
	private double probCreate;
	private double probDel;
	private boolean deletionActivated;

	// recipes
	Recipes recipesSent;
	Recipes recipes;
	
	// phase
	String phase;
	
	public Client(int numClient, Hosts hosts, Recipes recipesSent, Recipes recipes, String phase){
		clientId = "Client"+numClient;
		seqnumber = 0;

		// Hosts
		this.hosts = hosts;
		
		// recipes
		this.recipesSent = recipesSent;
		this.recipes = recipes;
		
		//TODO: obtenir les dades d'una variable properties no del fitxer, sinó que sigui un paràmetre
		// properties
		properties = new Properties();

		try {
			properties.load(new FileInputStream("config.properties"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		simulationDelay = Long.parseLong((String) properties.get("simulationDelay"))*1000;
		simulationPeriod = Long.parseLong((String) properties.get("simulationPeriod"))*1000;
		probCreate = Double.parseDouble((String)properties.get("probCreate"));
		probDel = Double.parseDouble((String)properties.get("probDel"));		
		deletionActivated = (!(Double.parseDouble((String)properties.get("probDel")) == 0.0));
		
		// phase
		this.phase = phase;
	}
	
	public void run(){
		System.out.println("starting client: "+clientId);

		Random rnd = new Random();
		
		// delay to allow servers start
		long delay = 10000;
		try {
			Thread.sleep(delay);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		// set timer for activity generation
		Timer activityGenerationTimer = new Timer();
		activityGenerationTimer.schedule(
				new TimerTask(){

					@Override
					public void run() {
						// TODO Auto-generated method stub
						generateActivity();
					}
				},
				simulationDelay*(1+(long)(2*rnd.nextDouble())),
				simulationPeriod
				);

		// sleep until the end of the experiment
		try {
			// sleep for the expected duration of the experiment minus a delay to allow servers to start
			Thread.sleep(Long.parseLong((String) properties.get("simulationStop"))*1000-delay);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		activityGenerationTimer.cancel();

//		Random rnd = new Random();
//		System.out.println("simulationDelay: "+simulationDelay*(1+(long)10*rnd.nextDouble()));
//		try {
//			Thread.sleep(simulationDelay*(1+(long)(10*rnd.nextDouble())));
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		boolean end = false;
//		do{
//			generateActivity();
//			try {
//				Thread.sleep(simulationPeriod*(1+(long)(10*rnd.nextDouble())));
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}while(!end);
	}
	
	private void generateActivity(){
		// if not a phase that generates activity
		if (!(phase.equals("all") || phase.equals("3") || phase.equals("4"))){
//			System.out.println("This phase doesn't generate activity");
			return;
		}
		
		Random rnd = new Random();

		double a=rnd.nextDouble();
		//probability of creating a recipe
		if(a<probCreate){
			seqnumber++;
			System.out.println("\t["+clientId+"] Adds: "+clientId+"#"+seqnumber);

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

			//TODO: improve the generation of recipe titles, recipe content and authors
			Recipe recipe = new Recipe(clientId+"#"+seqnumber, clientId+"#"+seqnumber+"--"+String.valueOf(chars), clientId);
			
			recipesSent.add(recipe);
			if (send(new AddOperation(recipe, new Timestamp(clientId, seqnumber)))){
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
				seqnumber++;
				System.out.println("\t["+clientId+"] Removes: "+recipeTitle+" (timestamp: "+clientId+":"+seqnumber);
				send(new RemoveOperation(recipeTitle, new Timestamp(clientId, seqnumber))); // TODO: no timestamp because a remove operation can be applied many times (if recipe doesn't exist won't be applied)
			}
		}
	}

	/**
	 * Sends the operation to raft cluster
	 * @param operation
	 * return true: if operation was committed; false: otherwise
	 */
	private boolean send(Operation operation){
		int registryPort = 1099; // default value 				// TODO: que siguin paràmetres
		String registryHost = "localhost"; // default value		// TODO: que siguin paràmetres

		boolean sent = false;
		boolean stop = false;
		int numAttempts = 6; // TODO: parameterize the number of attempts of trying to send an operation
		String sendTo;
		RequestResponse response;
		do{
			sendTo = leader;
			if (sendTo == null){
				sendTo = hosts.getRandomHosts(1).get(0).getId();
			}
	    	Registry registry;
	 		try {
//				System.out.println("["+clientId+"] sendTo: "+sendTo);
				registry = LocateRegistry.getRegistry(registryHost,	registryPort);
				// RPC call
				// host.getAddress(), host.getPort());
				Raft stub = (Raft) registry.lookup(
						"rmi://"
								+ CommunicationData.getInstance().getRegistryHost()
								+ ':'
								+ CommunicationData.getInstance().getRegistryPort()
								+ "/Raft-"
								+ sendTo
						);
				response = stub.Request(operation);			
		 		sent = response.isSucceeded();
		 		leader = response.getLeader();
		 		if (response.getLeader() == null){
		 			// response.getLeader() is null when simulatingActivity is false (i.e. simulation activity phase is finished)
		 			stop = true;
		 		}
			} catch (RemoteException | NotBoundException e) {
				// TODO Auto-generated catch block
				//						e.printStackTrace();
				System.err.println("["+clientId+"] Destination host "+sendTo+" not reachable: a network error has occured or the testing environment simulates that the destination host is failed");
				leader = null;
			}
	 		numAttempts--;
		}while (!sent && numAttempts>0 && !stop);
		return sent;
	}
	
 }
