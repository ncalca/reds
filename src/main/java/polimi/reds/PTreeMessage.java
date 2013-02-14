/***
 * * REDS - REconfigurable Dispatching System
 * * Copyright (C) 2003 Politecnico di Milano
 * * <mailto: cugola@elet.polimi.it> <mailto: picco@elet.polimi.it>
 * *
 * * This library is free software; you can redistribute it and/or modify it
 * * under the terms of the GNU Lesser General Public License as published by
 * * the Free Software Foundation; either version 2.1 of the License, or (at
 * * your option) any later version.
 * *
 * * This library is distributed in the hope that it will be useful, but
 * * WITHOUT ANY WARRANTY; without even the implied warranty of
 * * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * * General Public License for more details.
 * *
 * * You should have received a copy of the GNU Lesser General Public License
 * * along with this library; if not, write to the Free Software Foundation,
 * * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 ***/

package polimi.reds;

import java.util.HashMap;

/**
 * This class implements a message to be used with the <code>PTreeTable</code>.
 */
public class PTreeMessage extends Message {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8848959575126630707L;
	// The set of variables defined in this message with their respective
	// values.
	private HashMap variableValues;

	public PTreeMessage() {
		variableValues = new HashMap();
	}

	public void addValue(String key, String value) {
		variableValues.put(key, value);
	}

	public Object getValue(String key) {
		return variableValues.get(key);
	}

	// For testing purposes...
	public String toString() {
		return variableValues.toString();
	}
}
