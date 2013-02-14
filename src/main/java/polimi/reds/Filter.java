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

/**********************************************************************
 * A REDS filter. Filters are used to subscribe to specific classes of messages,
 * namely messages that match the specified filter. This is the most general
 * interface. See classes implementing this interface for specific type of
 * filters.
 **********************************************************************/
public interface Filter extends java.io.Serializable {
	/**
	 * Test whether this filter matches the given message.
	 * 
	 * @param msg
	 *            The message to match.
	 * @return <tt>true</tt> if the filter matches the given message.
	 */
	public boolean matches(Message msg);

	/**
	 * Test if this filter is equal to another filter
	 * 
	 * @param o
	 *            The filter to compare against.
	 * @return <tt>true</tt> if the two filters are equal.
	 */
	public boolean equals(Object o);

	/**
	 * Returns the hash code of this object. You MUST redefine this method if
	 * you redefine method <code>equals</code>.
	 * 
	 * @return the hash code of this object.
	 */
	public int hashCode();
}
