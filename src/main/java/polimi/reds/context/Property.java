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

/**
 * Represents an attribute that describes a point in the Context space. It is
 * represented by a name, a data type and a value
 * 
 */
public class Property implements Serializable {

	private static final long serialVersionUID = -3037694531102962808L;

	/**
	 * Integer data type
	 */
	public static final int INTEGER = 1;

	/**
	 * Real data type
	 */
	public static final int REAL = 2;

	/**
	 * String data type
	 */
	public static final int STRING = 3;

	protected String name;

	protected Object value;

	protected int dataType;

	/**
	 * Returns the property name
	 * 
	 * @return the property name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Returns the property data type
	 * 
	 * @return the property data type
	 */
	public int getDataType() {
		return this.dataType;
	}

	/**
	 * Returns the property value
	 * 
	 * @return the property value
	 */
	public Object getValue() {
		return this.value;
	}
	
	protected Property(){
		
	}

	private void init( String name, int dataType, Object value ) {
		this.name = name;
		this.value = value;
		this.dataType = dataType;
	}

	/**
	 * Creates a string property with the selected name and value
	 * 
	 * @param name
	 *            the name of the property
	 * @param value
	 *            the value of the property
	 * @return
	 */
	public  Property ( String name, String value ) {
		this( name, STRING, value );
	}

	/**
	 * Creates a real property with the selected name and value
	 * 
	 * @param name
	 *            the name of the property
	 * @param value
	 *            the value of the property
	 * @return
	 */
	public Property( String name, Double value ) {
		this( name, REAL, value );
	}

	/**
	 * Creates an integer property with the selected name and value
	 * 
	 * @param name
	 *            the name of the property
	 * @param value
	 *            the value of the property
	 * @return
	 */
	public  Property ( String name, Integer value ) {
		 this( name, INTEGER, value );
	}

	/**
	 * Creates a property with the selected name, datatype and value
	 * 
	 * @param name
	 *            the name of the property
	 * @param dataType
	 *            the data type of the property
	 * @param value
	 *            the value of the property
	 * @return
	 */
	public Property ( String name, int dataType, Object value ) {

		try {
			switch ( dataType ) {
				case ( INTEGER ): {
					value = (Integer) value;
					break;
				}

				case ( REAL ): {
					value = (Double) value;
					break;
				}

				case ( STRING ): {
					value = (String) value;
					break;
				}

				default: {
					throw new IllegalDataTypeException();
				}
			}
		}
		catch ( ClassCastException e ) {
			throw new IllegalValueException();
		}

		this.init( name, dataType, value );

	}

	public boolean equals( Object o ) {
		if ( o == null ) {
			return false;
		}
		if ( !( this.getClass().equals( o.getClass() ) ) ) {
			return false;
		}

		Property other = (Property) o;
		if ( !this.name.equals( other.name ) ) {
			return false;
		}
		if ( this.dataType != other.dataType ) {
			return false;
		}
		if ( !this.value.equals( other.value ) ) {
			return false;
		}

		return true;

	}

	public String toString() {
		return "Property: " + this.name + ", " + this.dataType + ", " + this.value.toString();
	}

}
