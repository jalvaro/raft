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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import recipesService.communication.Host;
import util.Serializer;

import edu.uoc.dpcs.lsim.LSimFactory;

import lsim.application.handler.Handler;

/**
 * @author Joan-Manuel Marques
 * December 2012
 *
 */
public class CoordinatorStartHandler implements Handler {
	
	private List<Object> workers;

	@Override
	public Object execute(Object obj) {
		List<Object> workersAux = (List<Object>) obj;
//		System.out.println("-- ** --> CoordinatorStartHandler -- values: " + values);
		LSimFactory.getCoordinatorInstance().log(
				"",
				"-- ** --> CoordinatorStartHandler -- values: " + workersAux
				);
		workers = new ArrayList<Object>();
		for (Object object: workersAux){
			if (object != null){
				try {
					workers.add((Host) Serializer.deserialize((byte []) object));
				} catch (ClassNotFoundException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return workers;
	}

	public int numWorkers(){
		return workers.size();
	}
}
