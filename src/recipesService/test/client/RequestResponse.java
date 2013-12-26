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

import java.io.Serializable;


/**
 * 
 * Response message to a Request from a client
 * 
 * @author Joan-Manuel Marques
 * July 2013
 *
 */

public class RequestResponse implements Serializable{

	private static final long serialVersionUID = -7927077442043782163L;

	private String leader;
	private boolean success;
	
	/**
	 * RequestResponse is response to a client RPC
	 * @param leader: leader id. To inform the client about the last known leader in case the request was not sent to the current leader
	 * @param success: true if operation succeeded, false otherwise (e.g. server that received the request is not the leader)
	 */
	public RequestResponse (String leader, boolean success){
		this.leader = leader;
		this.success = success;
	}

	public boolean isSucceeded(){
		return success;
	}

	public String getLeader(){
		return leader;
	}

	@Override
	public String toString() {
		return "RequestResponse [leader=" + leader + ", success="
				+ success + "]";
	}
}
