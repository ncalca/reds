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

import java.io.Serializable;
import java.rmi.dgc.VMID;

/**
 * It represents the Universal Unique ID of a message.
 * 
 * @author Alessandro Monguzzi
 */
public class MessageID implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2999945363215343435L;
	private VMID id;

	/**
	 * Base constructor.
	 * 
	 */
	public MessageID() {
		id = new VMID();
	}

	/**
	 * Return a <code>String</code> representation of the id.
	 * 
	 * @return a <code>String</code> representing the id
	 */
	public String toString() {
		return id.toString();
	}

	/**
	 * Check whether the given object has the same id of <code>this</code>.
	 * 
	 * @return true iff the two object have the same id
	 */
	public boolean equals(Object o) {
		if (o instanceof MessageID) {
			return ((MessageID) o).id.equals(this.id);
		}
		return false;
	}

	/**
	 * Get a hashcode of <code>this</code>
	 */
	public int hashCode() {
		return id.hashCode();
	}
}