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

package recipesService.data;

import java.io.Serializable;

/**
 * @author Joan-Manuel Marques
 * February 2013
 *
 */
public class RemoveOperation extends Operation implements Serializable{

	private static final long serialVersionUID = 6662533950228454468L;
	
	String recipeTitle;
	
	public RemoveOperation(String recipeTitle, Timestamp ts){
		super(ts); 
		this.recipeTitle = recipeTitle;
	}

	public OperationType getType(){
		return OperationType.REMOVE;
	}
	public String getRecipeTitle() {
		return recipeTitle;
	}
	@Override
	public String toString() {
		return "RemoveOperation [recipeTitle=" + recipeTitle + ", timestamp=" + timestamp +
				"]";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		RemoveOperation other = (RemoveOperation) obj;
		if (recipeTitle == null) {
			if (other.recipeTitle != null)
				return false;
		} else if (!recipeTitle.equals(other.recipeTitle))
			return false;
		return true;
	}
}
