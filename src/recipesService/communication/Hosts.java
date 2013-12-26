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

package recipesService.communication;

import java.util.List;
import java.util.Random;
import java.util.Vector;



/**
 * Hosts contains: (a) a list of all hosts; (b) information about the localHost 
 * @author Joan-Manuel Marques
 * December 2012, July 2013
 *
 */
public class Hosts {
	private List<Host> listHosts;
	private Host localHost;
	
	private List<String> listIds;

	static Random rnd = new Random();

	public Hosts(Host localNode){
		this.listHosts = new Vector<Host>();
		this.localHost = localNode;
		
		this.listIds = new Vector<String>();
	}

	public Hosts(){
		this.listHosts = new Vector<Host>();
		this.listIds = new Vector<String>();
	}

	public void add(Host node){
		this.listHosts.add(node);
		this.listIds.add(node.getId());
	}
	
	public int size(){
		return listHosts.size();
	}
	/**
	 * Returns the list of partners
	 * @param num
	 * @return List<Host>
	 */
	public List<Host> getAllHosts(){
		return this.listHosts;
	}

	/**
	 * Returns a list of num random hosts
	 * (in case a local host value is different from null)
	 * @param num
	 * @return
	 */
	public List<Host> getRandomHosts(int num){
		List<Host> v = new Vector<Host>();

		if (listHosts.size() == 1 || num < 1){
			return v;
		}

		num = Math.min(num, listHosts.size()-1);

		@SuppressWarnings("unchecked")
		List<Host> auxNodes=(Vector<Host>)((Vector<Host>) listHosts).clone();

		auxNodes.remove(localHost);

		int n;
		while(v.size()<num){
			n= (((int)(rnd.nextDouble() *10000))%auxNodes.size());
			v.add(auxNodes.get(n));
			auxNodes.remove(n);
		}
		return v;		
	}
	
	public Host getLocalHost(){
		return localHost;
	}
	
	public List<String> getIds(){
		return listIds;
	}
	public String toString(){
		return localHost + "-" + listHosts.toString();
	}
}
