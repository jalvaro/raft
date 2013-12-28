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

package lsimElement.coordinator;

import java.util.Random;

import edu.uoc.dpcs.lsim.LSimFactory;
import edu.uoc.dpcs.lsim.utils.LSimParameters;
import lsim.LSimDispatcherHandler;
import lsim.application.ApplicationManager;
import lsim.application.handler.DummyHandler;
import lsim.coordinator.LSimCoordinator;

/*
* @author Joan-Manuel Marques   
* December 2012
*
*/
public class Coordinator implements ApplicationManager{

	@Override
	public boolean isAlive() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void start() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void start(LSimDispatcherHandler disp) {
		// TODO Auto-generated method stub

		LSimCoordinator lsim=LSimFactory.getCoordinatorInstance();
		lsim.setDispatcher(disp);

		//
		lsim.setExperimentTime(30);
		
		// Initial parameters		
		LSimParameters paramsServer = new LSimParameters();
		LSimParameters paramsClients = new LSimParameters();

		// add init params to server
		lsim.addInitParam("Wserver0","coordinatorLSimParameters",paramsServer);
		lsim.addInitParam("Wserver1","coordinatorLSimParameters",paramsServer);
		lsim.addInitParam("Wserver2","coordinatorLSimParameters",paramsServer);

		// add init params to serverSD
		lsim.addInitParam("WserverSD0","coordinatorLSimParameters",paramsServer);
		lsim.addInitParam("WserverSD1","coordinatorLSimParameters",paramsServer);

		// add init params to clients
		lsim.addInitParam("Wclients0","coordinatorLSimParameters",paramsClients);
		String recipeSurviveTitle = Math.abs(new Random().nextInt(10000)) + "-";
		lsim.addInitParam("Wclients0","recipeSurviveTitle",recipeSurviveTitle);
		String recipeDeadTitle = Math.abs(new Random().nextInt(10000)) + "-";
		lsim.addInitParam("Wclients0","recipeDeadTitle",recipeDeadTitle);

		
		// init coordinator and workers
		CoordinatorInitHandler init=new CoordinatorInitHandler(paramsServer, paramsClients);
//		InitHandler init=new InitHandler(lsim,30);
		lsim.init(init);

		// start workers
		CoordinatorStartHandler startHandler = new CoordinatorStartHandler();
		lsim.start(startHandler);
		
		// add parameter to evaluator parameters
		lsim.addInitParam("evaluator", "groupId", init.getGroupId());
		lsim.addInitParam("evaluator", "numServers", Integer.valueOf(startHandler.numWorkers()));
		lsim.addInitParam("evaluator", "phase", init.getPhase());
		lsim.addInitParam("evaluator", "recipeDeadTitle", recipeDeadTitle);

		// stop
		lsim.stop(new DummyHandler());
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		
	}


}
