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
 * Represents an attribute that describes an area in the Context space. It is
 * represented by a name, a data type, and operator and a value
 * 
 */
public class Condition implements Serializable {

	private static final long serialVersionUID = 8902108789320156602L;

	/**
	 * Greater than operator
	 */
	public static final int GREATER = 1;

	/**
	 * Lower than operator
	 */
	public static final int LOWER = 2;

	/**
	 * Not equals operator
	 */
	public static final int NOT_EQUALS = 3;

	/**
	 * Inner operator
	 */
	public static final int INNER = 4;

	/**
	 * Not inner operator
	 */
	public static final int NOT_INNER = 5;

	/**
	 * Equals operator
	 */
	public static final int EQUALS = 10;

	/**
	 * Starts with operator (for strings)
	 */
	public static final int STARTS_WITH = 11;

	/**
	 * Ends with operator (for strings)
	 */
	public static final int ENDS_WITH = 12;

	/**
	 * Contains operator (for strings)
	 */
	public static final int CONTAINS = 13;

	/**
	 * The value used to represent "any value"
	 */
	public static final Any ANY = new Any();

	public static class Any implements Serializable {
		private static final long serialVersionUID = -8152335918750606677L;

		private Any() {
		}

		@Override
		public String toString() {
			return "ANY";
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			if (!(this.getClass().equals(obj.getClass()))) {
				return false;
			}

			return true;

		}
	}

	private String name;

	private int dataType;

	private int operator;

	private Object value;

	/**
	 * Returns the name of this attribute
	 * 
	 * @return the name of this attribute
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Returns the data type of this attribute
	 * 
	 * @return the data type of this attribute
	 */
	public int getDataType() {
		return this.dataType;
	}

	/**
	 * Returns the value of this attribute
	 * 
	 * @return the value of this attribute
	 */
	public Object getValue() {
		return this.value;
	}

	/**
	 * Returns the operator of this attribute
	 * 
	 * @return the operator of this attribute
	 */
	public int getOperator() {
		return this.operator;
	}

	private void init(String name, int dataType, int operator, Object value) {
		this.name = name;
		this.dataType = dataType;
		this.operator = operator;
		this.value = value;
	}

	/**
	 * Clone the condition c
	 * 
	 * @param c
	 *            the condition to clone
	 * @return the cloned condition
	 */
	public Condition(Condition c) {
		this(c.name, c.dataType, c.operator, c.value);
	}

	/**
	 * Create a new condition with the specified params
	 * 
	 * @param name
	 *            the name
	 * @param dataType
	 *            the data type
	 * @param operator
	 *            the operator
	 * @param value
	 *            the value
	 * @return a new condition with the specified params
	 */
	public Condition(String name, int dataType, int operator, Object value) {

		if ((operator == EQUALS) && (value == Condition.ANY)) {
			this.init(name, dataType, operator, value);
			return;
		}

		switch (dataType) {
		case (Property.INTEGER): {
			if (operator > 10) {
				throw new IllegalOperatorException();
			}

			if ((operator <= 3) || (operator == 10)) {
				if (!(value instanceof Integer)) {
					throw new IllegalValueException();
				}
			}

			else if (operator > 3) {
				if (!(value instanceof Interval)) {
					throw new IllegalValueException();
				}
			}
			break;
		}

		case (Property.REAL): {
			if (operator > 10) {
				throw new IllegalOperatorException();
			}

			if ((operator <= 3) || (operator == 10)) {
				if (!(value instanceof Double)) {
					throw new IllegalValueException();
				}
			}

			else if (operator > 3) {
				if (!(value instanceof Interval)) {
					throw new IllegalValueException();
				}
			}
			break;
		}

		case (Property.STRING): {
			if (operator < 10) {
				throw new IllegalOperatorException();
			}

			if (!(value instanceof String)) {
				throw new IllegalValueException();
			}
			break;
		}

		default: {
			throw new IllegalDataTypeException();
		}
		}

		this.init(name, dataType, operator, value);

	}

	/**
	 * Creates a new Condition with ANY as value
	 * 
	 * @param name
	 *            the name
	 * @param dataType
	 *            the data type
	 * @return
	 */
	public static Condition CreateANYCondition(String name, int dataType) {
		return new Condition(name, dataType, EQUALS, ANY);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}
		if (!(this.getClass().equals(o.getClass()))) {
			return false;
		}

		Condition other = (Condition) o;
		if (!this.name.equals(other.name)) {
			return false;
		}
		if (this.dataType != other.dataType) {
			return false;
		}
		if (!this.value.equals(other.value)) {
			return false;
		}
		if (this.operator != other.operator) {
			return false;
		}

		return true;

	}

	/**
	 * Test is c has the same structure of this (ie same name and data type)
	 * 
	 * @param c
	 * @return true is this has the same structure o
	 */
	public boolean isStructuralEqualsTo(Condition c) {
		return ((this.dataType == c.dataType) && (this.name.equals(c.name)));
	}

	@Override
	public String toString() {
		return "Nome: " + name + " - Tipo: " + dataType + " - Operatore: " + operator + " - Valore: "
				+ value.toString();
	}

}
