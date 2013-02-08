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

package polimi.reds.context;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Represents a filter used to select context entities like ContextSet and
 * ContextRange
 * 
 */
public class ContextFilter implements Iterable<Condition>, Serializable {

	private static final long serialVersionUID = 3830316759905749179L;

	private HashMap<String, Condition> conditions;

	public static ContextFilter ANY = new ContextFilter();

	public ContextFilter() {
		conditions = new HashMap<String, Condition>();
	}


	/**
	 * Returns an iterator over the Conditions of this filter
	 */
	public Iterator<Condition> iterator() {
		return conditions.values().iterator();
	}




	/**
	 * Adds a condition to this
	 * 
	 * @param condition
	 *            a condition
	 */
	public void addCondition( Condition condition ) {
		conditions.put( getConditionID( condition ), condition );
	}

	/**
	 * Returns a condition with the name and data type specified
	 * 
	 * @param name
	 *            the required name
	 * @param dataType
	 *            the required data type
	 * @return <tt>null</tt> if the condition doesn't exits else returns the
	 *         condition
	 */
	public Condition getCondition( String name, int dataType ) {
		return conditions.get( name + dataType );
	}

	private String getConditionID( Condition c ) {
		return c.getName() + c.getDataType();
	}

	/**
	 * Checks if <tt>this</tt> contains a condition c
	 * 
	 * @param c
	 *            the condition to check
	 * @return <tt>true</tt> is <tt>this</tt> contains c
	 */
	public boolean contains( Condition c ) {

		Condition c1 = this.getCondition( c.getName(), c.getDataType() );

		return c.equals( c1 );
	}

	/**
	 * Returns the number of condition in this filter
	 * 
	 * @return the number of condition in this filter
	 */
	public int size() {
		return conditions.size();
	}

	@Override
	public String toString() {
		String result = "ContextFilter:";

		for ( Condition condition : conditions.values() ) {
			result += "\n" + condition.toString();
		}

		return result;
	}

	@Override
	public boolean equals( Object o ) {
		if ( o == null ) {
			return false;
		}
		if ( !( this.getClass().equals( o.getClass() ) ) ) {
			return false;
		}

		ContextFilter other = (ContextFilter) o;

		for ( Condition condition : conditions.values() ) {
			if ( !other.contains( condition ) ) {
				return false;
			}
		}

		return true;

	}

}
