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

import edu.uoc.dpcs.lsim.utils.LSimParameters;

import lsim.application.handler.Handler;

/**
 * @author Joan-Manuel Marques
 * July 2013
 *
 */
public class CoordinatorInitHandler implements Handler {
	
	LSimParameters params;
	
	LSimParameters paramsServer;
	LSimParameters paramsClients;
	
	public CoordinatorInitHandler(LSimParameters paramsServer, LSimParameters paramsClients){
		this.paramsServer = paramsServer;
		this.paramsClients = paramsClients;
	}
	
	@Override
	public Object execute(Object obj) {
		params = (LSimParameters) obj;

		paramsServer.put("groupId",params.get("groupId"));
		paramsServer.put("electionTimeout",params.get("electionTimeout"));
		paramsServer.put("simulationStop",params.get("simulationStop"));
		paramsServer.put("executionStop",params.get("executionStop"));
		paramsServer.put("simulationDelay",params.get("simulationDelay"));
		paramsServer.put("simulationPeriod",params.get("simulationPeriod"));
		paramsServer.put("probDisconnect",params.get("probDisconnect"));
		paramsServer.put("probReconnect",params.get("probReconnect"));
		paramsServer.put("probCreate",params.get("probCreate"));
		paramsServer.put("probDel",params.get("probDel"));
		paramsServer.put("samplingTime",params.get("samplingTime"));
		paramsServer.put("executionMode",params.get("executionMode"));
		paramsServer.put("phase",params.get("phase"));

		paramsClients.put("simulationStop",params.get("simulationStop"));
		paramsClients.put("simulationDelay",params.get("simulationDelay"));
		paramsClients.put("simulationPeriod",params.get("simulationPeriod"));
		paramsClients.put("probCreate",params.get("probCreate"));
		paramsClients.put("probDel",params.get("probDel"));
		paramsClients.put("phase",params.get("phase"));
		return null;
	}
	
	public String getGroupId(){
		return (String) params.get("groupId");
	}
	
	public String getPhase(){
		return (String) params.get("phase");
	}
}