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

import polimi.reds.context.routing.ContextRange;

/**
 * Represents a set of <code>Property</code> in logic and.
 * 
 */
public class Context implements Iterable<Property>, Serializable {

	private static final long serialVersionUID = -8764192072039960911L;

	public static final Context nullContext = new Context();

	private HashMap<String, Property> properties;

	public Context() {
		properties = new HashMap<String, Property>();
	}

	/**
	 * Adds a property to this
	 * 
	 * @param p
	 *            the property to add
	 */
	public void addProperty( Property p ) {
		properties.put( p.getName() + p.getDataType(), p );
	}

	/**
	 * Returns the property specified by its name and datatype if exits,
	 * <code>null</code> else.
	 * 
	 * @param name
	 *            the name
	 * @param dataType
	 *            the data type
	 * @return the property or null
	 */
	public Property getProperty( String name, int dataType ) {
		return properties.get( name + dataType );
	}

	/**
	 * Returns an Iterator over the properties
	 * 
	 * @return an Iterator over the properties
	 */
	public Iterator<Property> iterator() {
		return properties.values().iterator();
	}

	/**
	 * Test if <code>this</code> is matched by the filter
	 * 
	 * @param filter
	 *            the context filter
	 * @return <code>true</code> is this is matched by the filter,
	 *         <code>false</code> otherwise
	 */
	public boolean isMatchedBy( ContextFilter filter ) {
		ContextRange contextRange = new ContextRange( this );
		return contextRange.isMatchedBy( filter );
	}

	@Override
	public String toString() {
		String result = "";
		for ( Property p : this.properties.values() ) {
			if ( p.getDataType() == Property.INTEGER ) {
				result += p.getName() + ": " + ( (Integer) p.getValue() ).intValue() + "\n";
			}
			else if ( p.getDataType() == Property.REAL ) {
				result += p.getName() + ": " + ( (Double) p.getValue() ).longValue() + "\n";
			}
			else if ( p.getDataType() == Property.STRING ) {
				result += p.getName() + ": " + (String) p.getValue() + "\n";
			}

		}

		return result;
	}

}
