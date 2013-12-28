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

package recipesService.activitySimulation;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Timer;

import communication.rmi.RMIsd;

import edu.uoc.dpcs.lsim.LSimFactory;

import recipesService.ServerData;

/**
 * @author Joan-Manuel Marques
 * December 2012, July 2013
 *
 */
public class ActivitySimulation {
	private static ActivitySimulation data;

	// true when creating synthetic activity and connections/desconnections; false otherwise
	private boolean activitySimulation=false;

	// activity simulation timer
	private static Timer activitySimulationTimer;

	// Activity simulation phases
	private int simulationStop; // duration of activity simulation phase
	private int executionStop; // duration of convergence phase (Raft consensus algorithm. Disconnected nodes won't reconnect)
	
	private boolean connected = false;
	private boolean deletion=false;

	// Activity simulation
	private long simulationDelay;
	private long simulationPeriod;
	
	//
	private double probDisconnect;
	private double probReconnect;
	private double probCreate;
	private double probDel;

	//
	private int samplingTime;
	
	// 
	private ServerData serverData;
	
	// to distinguish between an execution with all Servers running in a single computer
	// and an execution with Servers running in different computers (or more than one 
	// Server in a single computer but this computer having the same internal and external IP address)
	// * true: all Server run in a single computer
	// * false: Servers running in different computers (or more than one Server in a single computer but
	//         this computer having the same internal and external IP address) 
	private boolean localExecution = true;
	
	
	public static ActivitySimulation getInstance(){
		if (data == null){
			data = new ActivitySimulation();
		}
		return data;
	}
	
	public void startSimulation(ServerData serverData){
		this.activitySimulation = true;
		this.serverData = serverData;
		
		// sets a timer for activity generation phase:
		// ACTIVITY GENERATION PHASE: (synthetic) activity generation + TSAE sessions
		javax.swing.Timer timerSimulationStop = new javax.swing.Timer(simulationStop, new ActionListener (){
			public void actionPerformed(ActionEvent e){
				if (connected){
//					LSimFactory.getWorkerInstance().log(
//							"",
//							"Server " +
//									SimulationData.getInstance().serverData.getId() +
//									" finishes Activity Simulation"
//							);
					System.out.println("Server " +
							ActivitySimulation.getInstance().serverData.getServerId() +
							" finishes Activity Simulation"
							);
				} else{
//					LSimFactory.getWorkerInstance().log(
//							"",
//							"Server " + 
//									SimulationData.getInstance().serverData.getId() +
//									" finishes Activity Simulation. It will stop because is not connected"
//							);
					System.out.println("Server " + 
							ActivitySimulation.getInstance().serverData.getServerId() +
							" finishes Activity Simulation. It will stop because is not connected"
							);
					System.exit(1);
				}
				activitySimulation = false;

				// stop synthetic activity generation
				activitySimulationTimer.cancel();

				// sets a timer for convergence phase:
				// CONVERGENCE PHASE: only raft consensus
				javax.swing.Timer timerExecutionStop = new javax.swing.Timer(executionStop, new ActionListener (){
					public void actionPerformed(ActionEvent e){
//						LSimFactory.getWorkerInstance().log(
//								"",
//								"Server " + 
//										SimulationData.getInstance().serverData.getId() +
//										" Ends Execution"
//								);
						System.out.println("Server " +
								ActivitySimulation.getInstance().serverData.getServerId() +
								" Ends Execution"
								);
						ActivitySimulation.getInstance().serverData.disconnect();
						ActivitySimulation.getInstance().serverData.setEnd();
//						endSimulation = true;
					}
				});
				timerExecutionStop.setRepeats(false);
				timerExecutionStop.start();
			}
		});
		timerSimulationStop.setRepeats(false);
		timerSimulationStop.start();		

		// Sets the period of synthetic activity 
		DynamicitySimulation activity = new DynamicitySimulation(serverData);
		activitySimulationTimer = new Timer();
		activitySimulationTimer.scheduleAtFixedRate(activity, simulationDelay, simulationPeriod);                                         
	}

	public boolean isSimulatingActivity() {
		return activitySimulation;
	}

	public synchronized boolean isConnected() {
		return connected;
	}

	public synchronized void connect() {
//		System.out.println("|||||||||||||||||||||||||||||| (simulationData) connect: "+serverData.getId());
		RMIsd.getInstance().connect(serverData);
		serverData.connect();
		this.connected = true;
	}

	public synchronized void disconnect() {
//		System.out.println("|||||||||||||||||||||||||||||| disconnect: "+serverData.getId());
		RMIsd.getInstance().disconnect(serverData);
		serverData.disconnect();
		this.connected = false; //TODO: dubte: posar-ho abans? ?????????????????????????????????????????????????
	}

	public boolean deletionActivated() {
		return deletion;
	}

	public void setDeletion(boolean deletion) {
		this.deletion = deletion;
	}

	public void setSimulationStop(int simulationStop) {
		this.simulationStop = simulationStop;
	}

	public int getExecutionStop() {
		return this.executionStop;
	}

	public void setExecutionStop(int executionStop) {
		this.executionStop = executionStop;
	}

	public void setSimulationDelay(int simulationDelay) {
		this.simulationDelay = simulationDelay;
	}

	public void setSimulationPeriod(int simulationPeriod) {
		this.simulationPeriod = simulationPeriod;
	}

	public double getProbDisconnect() {
		return probDisconnect;
	}

	public void setProbDisconnect(double probDisconnect) {
		this.probDisconnect = probDisconnect;
	}

	public double getProbCreate() {
		return probCreate;
	}

	public void setProbCreate(double probCreate) {
		this.probCreate = probCreate;
	}

	public double getProbDel() {
		return probDel;
	}

	public void setProbDel(double probDel) {
		this.probDel = probDel;
	}

	public double getProbReconnect() {
		return probReconnect;
	}

	public void setProbReconnect(double probReconnect) {
		this.probReconnect = probReconnect;
	}
	
	public int getSetSamplingTime(){
		return this.samplingTime;
	}

	public void setSamplingTime(int samplingTime){
		this.samplingTime = samplingTime;
	}

	// to distinguish between an execution with all Servers running in a single computer
	// and an execution with Servers running in different computers (or more than one 
	// Server in a single computer but this computer having the same internal and external IP address)
	// * true: all Server run in a single computer
	// * false: Servers running in different computers (or more than one Server in a single computer but
	//         this computer having the same internal and external IP address) 
	public boolean localExecution(){
		return this.localExecution;
	}
	public void setLocalExecution(boolean localExecution){
		this.localExecution = localExecution;
	}
}
