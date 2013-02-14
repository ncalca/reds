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

package polimi.reds.location;

/**
 * A location (i.e., a point in space) used to provide location-based
 * publish-subscribe services.
 * 
 * @see polimi.reds.location.LocationDispatchingService
 */
public interface Location extends java.io.Serializable {
	/**
	 * Tests if two objects (in most of the cases two locations) matches.
	 * 
	 * @param o
	 *            the object (i.e., location) to be compared with
	 *            <code>this</code>.
	 * @return <code>true</code> if the two objects (i.e., locations) equals,
	 */
	public boolean equals(Object o);

	/**
	 * Returns the hash code of this object. You MUST redefine this method if
	 * you redefine method <code>equals</code>. Indeed, the Java library
	 * prescribes that if two objects equals they MUST have the same hash code.
	 * 
	 * @return the hash code of this object.
	 */
	public int hashCode();
}
