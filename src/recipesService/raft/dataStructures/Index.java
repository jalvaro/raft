/*
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

package recipesService.raft.dataStructures;

import java.util.HashMap;
import java.util.List;

import recipesService.communication.Host;

/**
 * 
 * Index: contains an index for each server
 * 
 * @author Joan-Manuel Marques
 * September 2013
 *
 */
public class Index {

	private HashMap<String, Integer> index;
	
	public Index(List<Host> servers, int intialIndex){
		index = new HashMap<String, Integer>();
		for (Host host : servers){
			index.put(host.getId(), new Integer(intialIndex));
		}
	}
	
	public int getIndex(String id){
		return index.get(id);
	}

	public void setIndex(String id, int currentIndex){
		index.put(id, new Integer(currentIndex));
	}
	
	public void decrease(String id){
		int indexAux = index.get(id);
		if (indexAux > 1 ) {
			indexAux--;
			index.put(id, new Integer(indexAux));
		}
	}
	
	public void inc(String id){
		index.put(id, new Integer(index.get(id)+1));
	}
	
	// majority of index[i] >= N
	public boolean majorityHigherOrEqual(int n){
		int count = 0;
		for (Integer i : index.values()){
			if (Integer.valueOf(i) >= n){
				count++;
			}
		}
		//TODO: majority enlloc de tots!!!
		return (count >= (index.size()/2 +1));
	}
	
	@Override
	public String toString() {
		return "Index [" + index + "]";
	}
}
