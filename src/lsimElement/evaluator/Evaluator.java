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

package lsimElement.evaluator;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import edu.uoc.dpcs.lsim.LSimFactory;
import edu.uoc.dpcs.lsim.exceptions.LSimExceptionMessage;

import recipesService.data.AddOperation;
import recipesService.data.OperationType;
import recipesService.raft.dataStructures.LogEntry;
import recipesService.test.server.PartialResult;
import recipesService.test.server.ResultBase;
import recipesService.test.server.ServerResult;
import lsim.LSimDispatcherHandler;
import lsim.application.ApplicationManager;
import lsim.application.handler.DummyHandler;
import lsim.evaluator.DefaultResultHandler;
import lsim.evaluator.GetResultTimeoutException;
import lsim.evaluator.LSimEvaluator;

/**
 * @author Joan-Manuel Marques
 * December 2012
 *
 */

public class Evaluator implements ApplicationManager {

	private boolean allResultsReceived = false;

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
	public void start(LSimDispatcherHandler disp) {
		LSimFactory.getEvaluatorInstance().setDispatcher(disp);
		try{
			process(disp);
		}catch(RuntimeException e){
			LSimFactory.getEvaluatorInstance().logException(new LSimExceptionMessage("", e, null));
		}
	}

	public void process(LSimDispatcherHandler hand) {
		LSimEvaluator lsim = LSimFactory.getEvaluatorInstance();
		
		// set maximum time (minutes) that is expected the evaluation of experiment will last
		lsim.startTimer(30);
		
		// Sets the action to perform if the evaluator timeout expires
		EvaluatorTimeOutAction timeOutHandler = new EvaluatorTimeOutAction();
		lsim.setTimeOutAction(timeOutHandler);


		System.out.println("========= EVALUATOR");
		EvaluatorInitHandler init = new EvaluatorInitHandler();

		lsim.init(init);
		int percentageRequiredResults = init.getPercentageRequiredResults();
		int numNodes = init.getNumNodes();
		String groupId = init.getGroupId();
		String phase = init.getPhase();
		String recipeDeadTitle = init.getRecipeDeadTitle(); 
		String recipeSurviveTitle = init.getRecipeSurviveTitle(); 
				
		
		// Set result handler to standard (receive results and return one result
		// a time)
		DefaultResultHandler resultHandler = new DefaultResultHandler();
		// Must be done before start
		lsim.setResultHandler(resultHandler);
		
		lsim.start(new DummyHandler());

		ObjectSerializableHandler<ResultBase> getResultHandler = new ObjectSerializableHandler<ResultBase>();
		
		// Calculate the number of required results
		int numRequiredResults = ( (numNodes * percentageRequiredResults) / 100 + 1 );
		if (percentageRequiredResults == 100){
			numRequiredResults = numNodes;
		}

        List<ServerResult> finalResults = new Vector<ServerResult>();
		HashMap<Integer, List<ServerResult>> partialResults = new HashMap<Integer, List<ServerResult>>();
		try{
//			int i = 0;
			do{
				lsim.getResult(getResultHandler);
				ResultBase result = (ResultBase)getResultHandler.value();
				switch(result.type()){
				case PARTIAL:
					Integer iteration = ((PartialResult)result).getIteration();
					List<ServerResult> results = null;
					if (partialResults.containsKey(iteration)){
						results = partialResults.get(iteration);
					} else{
						results = new Vector<ServerResult>();
					}
					results.add(result.getServerResult());
					partialResults.put(iteration, results);
//					System.out.println("##### [iteration: "+iteration
//							+"] partial result from server: " + result.getServerResult().getNodeId());
					lsim.log("",
							"##### [iteration: "+iteration
							+"] partial result from server: " + result.getServerResult().getHostId()
							);
					break;
				case FINAL:
					finalResults.add(result.getServerResult());
//					System.out.println("##### Final result from server: " + result.getServerResult().getNodeId());
					lsim.log("",
							"##### Final result from server: " + result.getServerResult().getHostId()
							);
					if (finalResults.size() == numNodes){
						allResultsReceived = true;
					}
					break;
				}

				// We don't know how many nodes will send results.
				// Timer is reset at each iteration. When timer finishes 
				// the number of nodes to evaluate will be the nodes that
				// had send results
				resultHandler.setGetResultTimeout(15000);
			}while(!allResultsReceived);
		}catch (GetResultTimeoutException e){
//			System.out.println(e.getMessage());
			lsim.logException(new LSimExceptionMessage(e.getMessage(), e, null));
		}
		
		if (finalResults.size() < numRequiredResults){
//			System.out.println("Unable to evaluate results due to: Not enough Servers where connected at the moment of finishing the Activity Simulation phase.");
//			System.out.println("Partial results received: "+partialResults.size());
//			System.out.println("Final results received: "+finalResults.size());
//			System.out.println("numRequiredResults: "+numRequiredResults);
			lsim.logException(new LSimExceptionMessage(
					"Unable to evaluate results due to: Not enough Servers where connected at the moment of finishing the Activity Simulation phase."
					+ "\nReceived Results: "+finalResults.size()
					+ "\nnumRequiredResults: "+numRequiredResults,
					null,
					null
					)
			);
//			lsim.logException(new LSimExceptionMessage("Unable to evaluate results due to: Not enough Servers where connected at the moment of finishing the Activity Simulation phase.", null, null));
//			lsim.logException(new LSimExceptionMessage("Received Results: "+finalResults.size(), null, null));
//			lsim.logException(new LSimExceptionMessage("numRequiredResults: "+numRequiredResults, null, null));
			DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			lsim.store( groupId
					+ '\t' + (dateFormat.format(new java.util.Date())).toString()
					+ '\t' + "Unable to evaluate results due to: Not enough Servers where connected at the moment of finishing the Activity Simulation phase."
					+ '\t' + "Recieved Results: "+finalResults.size()
					+ '\t' + "numRequiredResults: "+numRequiredResults
					+ '\n'
					);

			lsim.stop(new DummyHandler());
		}
		
		if (!resultsFromTwoDifferentServers(finalResults)){
			lsim.logException(new LSimExceptionMessage(
					"Unable to evaluate results due to: Only results from student's server. Evalution requires receiving results from teacher's Server. Check the xml file that specifies the experiment to make sure you haven't changed groupId of worker ServerSD"
					, null
					, null
					)
			);
			
			DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			lsim.store( groupId
					+ '\t' + (dateFormat.format(new java.util.Date())).toString()
					+ '\t' + "Unable to evaluate results due to: Only results from student's server. Evalution requires receiving results from teacher's Server. Check the xml file that specifies the experiment to make sure you haven't changed groupId of worker ServerSD"
					+ '\n'
					);

			lsim.stop(new DummyHandler());			
		}

		// evaluate final results
		String resultDetail = "##### ["+finalResults.get(0).getHostId()+"] Result:\n " + finalResults.get(0);
//		System.out.println("##### ["+finalResults.get(0).getNodeId()+"] Result:\n " + finalResults.get(0));
		boolean equal = true;
		
		for (int i = 1 ; i<finalResults.size() && equal; i++){
			equal = equal && finalResults.get(0).equals(finalResults.get(i));
			if (!equal){
//				System.out.println("##### ["+finalResults.get(i).getNodeId()+"] Result:\n " + finalResults.get(i));
	           	resultDetail += "\n##### (different) ["+finalResults.get(i).getHostId()+"] Result:\n " + finalResults.get(i);
			}
		}
		
		// calculate in which iteration nodes converged
		boolean converged = false;
		int convergenceIteration = -1;
		for (int it = 0; partialResults.containsKey(Integer.valueOf(it))&& !converged; it++){
			List<ServerResult> results = partialResults.get(Integer.valueOf(it));
			converged = (partialResults.size() >= finalResults.size());
			for (int i = 1 ; i<results.size() && converged; i++){
				converged = converged && results.get(0).equals(results.get(i));
			}
			if (converged){
				convergenceIteration = it;
			}
		}
 
        // ------------------------------------------------
        // final results
        // ------------------------------------------------
		if (phase.equals("4.1")) {
			int deadCount = 0;
			int liveCount = 0;
			for (ServerResult serverResult : finalResults) {
				undoLastAdds(serverResult);
				deadCount = deadCount + countRecipes(serverResult, recipeDeadTitle);
				liveCount = liveCount + countRecipes(serverResult, recipeSurviveTitle);
			}
			equal = equal && (deadCount == 0) && liveCount > 0; 
//			System.out.println("\n\t" + 
//					"Having "+deadCount+" dead results <<< "+
//					(finalResults.size() * clients.getCount()) + " dead-recipes:" + recipeDeadTitle + "*");			
//			result += "\n\t" + 
//					"Having "+deadCount+" dead results <<< "+
//					(finalResults.size() * clients.getCount()) + " dead-recipes:" + recipeDeadTitle + "*";

			DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			resultDetail += "\n##### Phase 4.1" 
					+ '\t' + (dateFormat.format(new java.util.Date())).toString()
					;
			if (deadCount == 0 && liveCount > 0){
				resultDetail += '\n' + "Phase 4.1 is correct (live: "+liveCount+")";
			} else {
				resultDetail += 
						'\n' + "ERROR in Phase 4.1: Raft re-executed an operation multiple times"
						+ '\n' + "i.e. it has received:"
						+ '\n' + '\t' + "(a) AddOperation(recipeN, timestampX)"
						+ '\n' + '\t' + "(b) RemoveOperation(recipeN.title,timestampY)"
						+ '\n' + '\t' + "(c) AddOperation(recipeN, timestampX)"
						+ '\n' + "It shouldn't have executed the second AddOperation(recipeN, timestampX)"
						;
			}
		}

//		System.out.println("\n\n");
//		System.out.println("*********** num received results: "+finalResults.size());
//		System.out.println("*********** % received results: "+(finalResults.size()*100/numNodes));
//		System.out.println("*********** minimal required number of results: "+numRequiredResults);
//		System.out.println("\n\n");
//		lsim.store("\n\n");
//		lsim.store("*********** num received results: "+finalResults.size());
//		lsim.store("*********** % received results: "+(finalResults.size()*100/numNodes));
//		lsim.store("*********** minimal required number of results: "+numRequiredResults);
//		lsim.store("\n\n");

		resultDetail += "\n\n"
				+ '\n' + "*********** num received results: "+finalResults.size()
				+ '\n' + "*********** % received results: "+(finalResults.size()*100/numNodes)
				+ '\n' + "*********** minimal required number of results: "+numRequiredResults
				+ "\n\n"
				;

		
		// write final result
//		System.out.println("\n\n");
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		String result = lsim.getIdent().split("]")[0].concat("]")
				+ '\t' + groupId
				+ '\t' + (dateFormat.format(new java.util.Date())).toString()
				+ '\t' + "phase " + phase
				;
		if (equal){
			result += '\t' + "Correct";
			if (convergenceIteration == -1){
//				result += '\t' + "Nodes converged at the last iteration ";
			} else{
//				result += '\t' + "Nodes converged at the iteration " + convergenceIteration;
			}
		} else{
			result += '\t' + "Servers DON'T have coherent data";
		}
		result += '\n';
		lsim.store(
				result
				+ "::"
				+ resultDetail
				);
//    	System.out.println(result);
//      System.out.println("\n\n");
//		System.out.println("================================================");
//		System.out.println("\n\n");
                
        // ------------------------------------------------
        // stop
        // ------------------------------------------------

        lsim.stop(new DummyHandler());
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

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}
}