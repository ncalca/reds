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

package polimi.reds.context.routing;

import java.io.Serializable;

import polimi.reds.context.ComparisonResult;
import polimi.reds.context.Condition;
import polimi.reds.context.IllegalDataTypeException;
import polimi.reds.context.IllegalOperatorException;
import polimi.reds.context.IllegalValueException;
import polimi.reds.context.Interval;
import polimi.reds.context.Property;
import polimi.reds.context.Util;

public class PropertyRange extends Property {

	private static final long serialVersionUID = 0L;

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
		public boolean equals( Object obj ) {
			if ( obj == null ) {
				return false;
			}
			if ( !( this.getClass().equals( obj.getClass() ) ) ) {
				return false;
			}
			
			return true;
			
		}
		
	}
	
	
	protected int operator;
	
	
	/**
	 * Returns the operator of this attribute
	 * 
	 * @return the operator of this attribute
	 */
	public int getOperator() {
		return this.operator;
	}

	

	private void init( String name, int dataType, int operator, Object value ) {
		this.name = name;
		this.dataType = dataType;
		this.operator = operator;
		this.value = value;
	}

	/**
	 * Clone the condition c
	 * 
	 * @param p
	 *            the condition to clone
	 * @return the cloned condition
	 */
	public PropertyRange ( PropertyRange p ) {
		this( p.name, p.dataType, p.operator, p.value );
	}
	
	/**
	 * Clone the condition c
	 * 
	 * @param p
	 *            the condition to clone
	 * @return the cloned condition
	 */
	public PropertyRange( Property p ) {
		this( p.getName(), p.getDataType(), EQUALS, p.getValue() );
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
	public PropertyRange( String name, int dataType, int operator, Object value ) {

		if ( ( operator == EQUALS ) && ( PropertyRange.ANY.equals( value ) ) ) {
			this.init( name, dataType, operator, value );
			return;
		}

		switch ( dataType ) {
			case ( Property.INTEGER          ): {
				if ( operator > 10 ) {
					throw new IllegalOperatorException();
				}

				if ( ( operator <= 3 ) || ( operator == 10 ) ) {
					if ( !( value instanceof Integer ) ) {
						throw new IllegalValueException();
					}
				}

				else if ( operator > 3 ) {
					if ( !( value instanceof Interval ) ) {
						throw new IllegalValueException();
					}
				}
				break;
			}

			case ( Property.REAL         ): {
				if ( operator > 10 ) {
					throw new IllegalOperatorException();
				}

				if ( ( operator <= 3 ) || ( operator == 10 ) ) {
					if ( !( value instanceof Double ) ) {
						throw new IllegalValueException();
					}
				}

				else if ( operator > 3 ) {
					if ( !( value instanceof Interval ) ) {
						throw new IllegalValueException();
					}
				}
				break;
			}

			case ( Property.STRING       ): {
				if ( operator < 10 ) {
					throw new IllegalOperatorException();
				}

				if ( !( value instanceof String ) ) {
					throw new IllegalValueException();
				}
				break;
			}

			default: {
				throw new IllegalDataTypeException();
			}
		}

		this.init( name, dataType, operator, value );

	}



	/**
	 * Creates a new PropertyRange with ANY as value
	 * 
	 * @param name
	 *            the name
	 * @param dataType
	 *            the data type
	 * @return
	 */
	public static PropertyRange CreatePropertyRangeANY ( String name, int dataType ) {
		return new PropertyRange( name, dataType, EQUALS, ANY );
	}

	/**
	 * Test is c2 overlaps this
	 * 
	 * @param condition
	 *            the other condition
	 * @return true is this overlaps c2
	 */
	public boolean isSatifiedBy( Condition condition ) {
		
		String conditionName = condition.getName();
		int conditionDataType = condition.getDataType();
		int conditionOperator = condition.getOperator();
		Object conditionValue = condition.getValue();
		
		if ( ( conditionOperator == Condition.EQUALS ) && ( Condition.ANY.equals( conditionValue  ) ) ) {
			conditionValue = PropertyRange.ANY;			
		}
		
		
		PropertyRange otherPropertyRange = new PropertyRange(conditionName, conditionDataType, conditionOperator, conditionValue);
		
		if ( !this.name.equals( otherPropertyRange.name ) ) {
			return false;
		}

		if ( this.dataType != otherPropertyRange.dataType ) {
			return false;
		}

		if ( Util.isOneANY( this, otherPropertyRange ) ) {
			return true;
		}

		if ( this.dataType == Property.STRING ) {
			return overlapsStrings( otherPropertyRange );
		}
		if ( ( this.dataType == Property.REAL ) || ( this.dataType == Property.INTEGER ) ) {
			return overlapsNumbers( otherPropertyRange );
		}

		throw new IllegalArgumentException();

	}

	private boolean overlapsNumbers( PropertyRange c2 ) {

		if ( Util.isOneOperator( EQUALS, this, c2 ) ) {

			PropertyRange conditionWithEQUALS;
			PropertyRange otherPropertyRange;

			if ( this.operator == EQUALS ) {
				conditionWithEQUALS = this;
				otherPropertyRange = c2;
			}
			else {
				conditionWithEQUALS = c2;
				otherPropertyRange = this;
			}

			double valueEquals = Util.getDoubleValue( conditionWithEQUALS );

			switch ( otherPropertyRange.operator ) {

				case ( EQUALS ): {
					if ( valueEquals != Util.getDoubleValue( otherPropertyRange ) )
						return false;
					break;
				}

				case ( LOWER ): {
					if ( valueEquals >= Util.getDoubleValue( otherPropertyRange ) )
						return false;
					break;
				}

				case ( GREATER ): {
					if ( valueEquals <= Util.getDoubleValue( otherPropertyRange ) )
						return false;
					break;
				}

				case ( NOT_EQUALS ): {
					if ( valueEquals == Util.getDoubleValue( otherPropertyRange ) )
						return false;
					break;
				}

				case ( INNER ): {
					double lower = ( (Interval<Double>) otherPropertyRange.value ).getLowerDouble();
					double upper = ( (Interval<Double>) otherPropertyRange.value ).getUpperDouble();

					if ( ( valueEquals < lower ) || ( valueEquals > upper ) )
						return false;
					break;
				}

				case ( NOT_INNER ): {
					double lower = ( (Interval<Double>) otherPropertyRange.value ).getLowerDouble();
					double upper = ( (Interval<Double>) otherPropertyRange.value ).getUpperDouble();

					if ( ( lower < valueEquals ) && ( valueEquals < upper ) )
						return false;
					break;
				}
			}
		}

		else if ( Util.isOneOperator( LOWER, this, c2 ) ) {
			PropertyRange conditionWithLOWER;
			PropertyRange otherPropertyRange;

			if ( this.operator == LOWER ) {
				conditionWithLOWER = this;
				otherPropertyRange = c2;
			}
			else {
				conditionWithLOWER = c2;
				otherPropertyRange = this;
			}

			double doubleLower = Util.getDoubleValue( conditionWithLOWER );

			switch ( otherPropertyRange.operator ) {

				case ( GREATER ): {
					if ( doubleLower <= Util.getDoubleValue( otherPropertyRange ) )
						return false;
					break;
				}

				case ( INNER ): {
					double lower = ( (Interval<Double>) otherPropertyRange.value ).getLowerDouble();

					if ( doubleLower <= lower )
						return false;
					break;
				}

			}
		}

		else if ( Util.isOneOperator( GREATER, this, c2 ) ) {
			PropertyRange conditionWithGREATER;
			PropertyRange otherPropertyRange;

			if ( this.operator == GREATER ) {
				conditionWithGREATER = this;
				otherPropertyRange = c2;
			}
			else {
				conditionWithGREATER = c2;
				otherPropertyRange = this;
			}

			double doubleGreater = Util.getDoubleValue( conditionWithGREATER );

			if ( otherPropertyRange.operator == INNER ) {
				double upper = ( (Interval<Double>) otherPropertyRange.value ).getUpperDouble();

				if ( doubleGreater >= upper )
					return false;
			}

		}

		else if ( Util.isOneOperator( NOT_EQUALS, this, c2 ) ) {
			PropertyRange conditionNotEquals;
			PropertyRange otherPropertyRange;

			if ( this.operator == NOT_EQUALS ) {
				conditionNotEquals = this;
				otherPropertyRange = c2;
			}
			else {
				conditionNotEquals = c2;
				otherPropertyRange = this;
			}

			double doubleNotEquals = Util.getDoubleValue( conditionNotEquals );

			switch ( otherPropertyRange.operator ) {
				case ( INNER ): {
					double lower = ( (Interval<Double>) otherPropertyRange.value ).getLowerDouble();
					double upper = ( (Interval<Double>) otherPropertyRange.value ).getUpperDouble();

					if ( ( doubleNotEquals == lower ) && ( doubleNotEquals == upper ) )
						return false;
					break;
				}
			}
		}

		else if ( Util.isOneOperator( INNER, this, c2 ) ) {
			PropertyRange conditionWithINTERNO_A;
			PropertyRange otherPropertyRange;

			if ( this.operator == INNER ) {
				conditionWithINTERNO_A = this;
				otherPropertyRange = c2;
			}
			else {
				conditionWithINTERNO_A = c2;
				otherPropertyRange = this;
			}
			double lower = ( (Interval<Double>) conditionWithINTERNO_A.value ).getLowerDouble();
			double upper = ( (Interval<Double>) conditionWithINTERNO_A.value ).getUpperDouble();

			switch ( otherPropertyRange.operator ) {
				case ( INNER ): {
					double otherLower = ( (Interval<Double>) otherPropertyRange.value ).getLowerDouble();
					double otherUpper = ( (Interval<Double>) otherPropertyRange.value ).getUpperDouble();

					if ( ( upper < otherLower ) || ( lower > otherUpper ) )
						return false;
					break;

				}
				case ( NOT_INNER ): {
					double otherLower = ( (Interval<Double>) otherPropertyRange.value ).getLowerDouble();
					double otherUpper = ( (Interval<Double>) otherPropertyRange.value ).getUpperDouble();

					if ( ( lower > otherLower ) && ( upper < otherUpper ) )
						return false;
					break;
				}

			}
		}

		return true;
	}

	private boolean overlapsStrings( PropertyRange c2 ) {
		String s1 = (String) this.value;
		String s2 = (String) c2.value;

		if ( Util.hasSameOperator( this, c2 ) ) {
			int sameOperator = this.operator;

			String shortString = Util.getShortString( s1, s2 );
			String longString = Util.getLongString( s1, s2 );

			switch ( sameOperator ) {
				case ( STARTS_WITH ): {
					if ( !longString.startsWith( shortString ) )
						return false;
					break;
				}
				case ( ENDS_WITH ): {
					if ( !longString.endsWith( shortString ) )
						return false;
					break;
				}
				case ( EQUALS ): {
					if ( !longString.equals( shortString ) )
						return false;
					break;
				}
			}
		}

		else if ( Util.isOneOperator( EQUALS, this, c2 ) ) {

			PropertyRange conditionWithEQUALS;
			PropertyRange otherPropertyRange;

			if ( this.operator == EQUALS ) {
				conditionWithEQUALS = this;
				otherPropertyRange = c2;
			}
			else {
				conditionWithEQUALS = c2;
				otherPropertyRange = this;
			}

			String stringEquals = (String) conditionWithEQUALS.value;
			String otherString = (String) otherPropertyRange.value;

			switch ( otherPropertyRange.operator ) {
				case ( STARTS_WITH ): {
					if ( !stringEquals.startsWith( otherString ) )
						return false;
					break;
				}
				case ( ENDS_WITH ): {
					if ( !stringEquals.endsWith( otherString ) )
						return false;
					break;
				}
				case ( CONTAINS ): {
					if ( !stringEquals.contains( otherString ) )
						return false;
					break;
				}
			}
		}

		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals( Object o ) {
		if ( o == null ) {
			return false;
		}
		if ( !( this.getClass().equals( o.getClass() ) ) ) {
			return false;
		}

		PropertyRange other = (PropertyRange) o;
		if ( !this.name.equals( other.name ) ) {
			return false;
		}
		if ( this.dataType != other.dataType ) {
			return false;
		}
		if ( !this.value.equals( other.value ) ) {
			return false;
		}
		if ( this.operator != other.operator ) {
			return false;
		}

		return true;

	}

	/**
	 * Test is c has the same structure of this (ie same name and data type)
	 * 
	 * @param r
	 * @return true is this has the same structure o
	 */
	public boolean isStructuralEqualsTo( PropertyRange r ) {
		return ( ( this.dataType == r.dataType ) && ( this.name.equals( r.name ) ) );
	}

	@Override
	public String toString() {
		return "Nome: " + name + " - Tipo: " + dataType + " - Operatore: " + operator + " - Valore: "
				+ value.toString();
	}

	/**
	 * Test is this is bigger, smaller, equal or not compatible with the param
	 * 
	 * @param oldPropertyRange
	 *            the other condition
	 * @return the relation beetween this and the other condition
	 */
	public ComparisonResult compareTo( PropertyRange oldPropertyRange ) {
		if ( !this.name.equals( oldPropertyRange.name ) ) {
			return ComparisonResult.NOT_COMPARABLE;
		}

		if ( this.dataType != oldPropertyRange.dataType ) {
			return ComparisonResult.NOT_COMPARABLE;
		}

		if ( Util.isANY( oldPropertyRange ) ) {
			if ( Util.isANY( this ) ) {
				return ComparisonResult.EQUALS;
			}
			else {
				return ComparisonResult.SMALLER;
			}
		}

		if ( Util.isANY( this ) && !Util.isANY( oldPropertyRange ) ) {
			return ComparisonResult.BIGGER;
		}

		if ( this.dataType == Property.STRING ) {
			return compareString( oldPropertyRange );
		}

		if ( this.dataType == Property.REAL ) {
			return compareReal( this, oldPropertyRange );
		}

		if ( this.dataType == Property.INTEGER ) {

			PropertyRange thisPropertyRange = transformIntegerToRealPropertyRange( this );
			PropertyRange otherPropertyRange = transformIntegerToRealPropertyRange( oldPropertyRange );

			return compareReal( thisPropertyRange, otherPropertyRange );
		}

		throw new IllegalArgumentException();
	}

	private PropertyRange transformIntegerToRealPropertyRange( PropertyRange integerPropertyRange ) {
		PropertyRange otherPropertyRange = null;

		if ( ( integerPropertyRange.operator == INNER ) || ( integerPropertyRange.operator == NOT_INNER ) ) {
			Interval<Integer> integerBounds = (Interval<Integer>) integerPropertyRange.value;
			Interval<Double> otherDoubleValue = new Interval<Double>( integerBounds.getLower().doubleValue(), integerBounds.getUpper().doubleValue() );
			otherPropertyRange = new PropertyRange( integerPropertyRange.name, Property.REAL, integerPropertyRange.operator, otherDoubleValue );
		}
		else {
			Double otherDoubleValue = new Double( ( (Integer) integerPropertyRange.value ).intValue() );
			otherPropertyRange = new PropertyRange( integerPropertyRange.name, Property.REAL, integerPropertyRange.operator, otherDoubleValue );
		}
		return otherPropertyRange;
	}

	private static ComparisonResult compareReal( PropertyRange newPropertyRange, PropertyRange oldPropertyRange ) {

		if ( newPropertyRange.operator == EQUALS ) {
			double newValue = ( (Double) newPropertyRange.value ).doubleValue();

			if ( oldPropertyRange.operator == EQUALS ) {
				double oldValue = ( (Double) oldPropertyRange.value ).doubleValue();

				if ( oldValue == newValue ) {
					return ComparisonResult.EQUALS;
				}
				else {
					return ComparisonResult.NOT_COMPARABLE;
				}
			}
			if ( oldPropertyRange.operator == LOWER ) {
				double oldValue = ( (Double) oldPropertyRange.value ).doubleValue();

				if ( oldValue > newValue ) {
					return ComparisonResult.SMALLER;
				}
				else {
					return ComparisonResult.NOT_COMPARABLE;
				}
			}
			if ( oldPropertyRange.operator == GREATER ) {
				double oldValue = ( (Double) oldPropertyRange.value ).doubleValue();

				if ( oldValue < newValue ) {
					return ComparisonResult.SMALLER;
				}
				else {
					return ComparisonResult.NOT_COMPARABLE;
				}
			}
			if ( oldPropertyRange.operator == NOT_EQUALS ) {
				double oldValue = ( (Double) oldPropertyRange.value ).doubleValue();
				if ( newValue != oldValue ) {
					return ComparisonResult.SMALLER;
				}
				else {
					return ComparisonResult.NOT_COMPARABLE;
				}
			}
			if ( oldPropertyRange.operator == INNER ) {
				Interval<Double> oldValue = (Interval<Double>) oldPropertyRange.value;
				if ( ( oldValue.getLower() <= newValue ) && ( newValue <= oldValue.getUpper() ) ) {
					return ComparisonResult.SMALLER;
				}
				else {
					return ComparisonResult.NOT_COMPARABLE;
				}
			}
			if ( oldPropertyRange.operator == NOT_INNER ) {
				Interval<Double> oldValue = (Interval<Double>) oldPropertyRange.value;

				if ( ( oldValue.getLower() <= newValue ) && ( newValue <= oldValue.getUpper() ) ) {
					return ComparisonResult.NOT_COMPARABLE;
				}
				else {
					return ComparisonResult.SMALLER;
				}
			}

		}

		if ( newPropertyRange.operator == LOWER ) {
			double newValue = ( (Double) newPropertyRange.value ).doubleValue();
			if ( oldPropertyRange.operator == EQUALS ) {
				double oldValue = ( (Double) oldPropertyRange.value ).doubleValue();

				if ( newValue > oldValue ) {
					return ComparisonResult.BIGGER;
				}
				else {
					return ComparisonResult.NOT_COMPARABLE;
				}

			}
			if ( oldPropertyRange.operator == LOWER ) {
				double oldValue = ( (Double) oldPropertyRange.value ).doubleValue();

				if ( oldValue == newValue ) {
					return ComparisonResult.EQUALS;
				}
				else if ( oldValue < newValue ) {
					return ComparisonResult.BIGGER;
				}
				else if ( oldValue > newValue ) {
					return ComparisonResult.SMALLER;
				}

			}
			if ( oldPropertyRange.operator == GREATER ) {
				return ComparisonResult.NOT_COMPARABLE;
			}
			if ( oldPropertyRange.operator == NOT_EQUALS ) {
				double oldValue = ( (Double) oldPropertyRange.value ).doubleValue();
				if ( newValue <= oldValue ) {
					return ComparisonResult.SMALLER;
				}
				else {
					return ComparisonResult.NOT_COMPARABLE;
				}

			}
			if ( oldPropertyRange.operator == INNER ) {
				Interval<Double> oldValue = (Interval<Double>) oldPropertyRange.value;
				if ( newValue >= oldValue.getUpper() ) {
					return ComparisonResult.BIGGER;
				}
				else {
					return ComparisonResult.NOT_COMPARABLE;
				}
			}
			if ( oldPropertyRange.operator == NOT_INNER ) {
				Interval<Double> oldValue = (Interval<Double>) oldPropertyRange.value;
				if ( newValue <= oldValue.getLower() ) {
					return ComparisonResult.SMALLER;
				}
				else {
					return ComparisonResult.NOT_COMPARABLE;
				}
			}
		}

		if ( newPropertyRange.operator == GREATER ) {
			double newValue = ( (Double) newPropertyRange.value ).doubleValue();
			if ( oldPropertyRange.operator == EQUALS ) {
				double oldValue = ( (Double) oldPropertyRange.value ).doubleValue();
				if ( newValue < oldValue ) {
					return ComparisonResult.BIGGER;
				}
				else {
					return ComparisonResult.NOT_COMPARABLE;
				}
			}
			if ( oldPropertyRange.operator == LOWER ) {
				return ComparisonResult.NOT_COMPARABLE;
			}
			if ( oldPropertyRange.operator == GREATER ) {
				double oldValue = ( (Double) oldPropertyRange.value ).doubleValue();
				if ( newValue == oldValue ) {
					return ComparisonResult.EQUALS;
				}
				else if ( newValue > oldValue ) {
					return ComparisonResult.SMALLER;
				}
				else {
					return ComparisonResult.BIGGER;
				}
			}
			if ( oldPropertyRange.operator == NOT_EQUALS ) {
				double oldValue = ( (Double) oldPropertyRange.value ).doubleValue();
				if ( newValue >= oldValue ) {
					return ComparisonResult.SMALLER;
				}
				else {
					return ComparisonResult.NOT_COMPARABLE;
				}
			}
			if ( oldPropertyRange.operator == INNER ) {
				Interval<Double> oldValue = (Interval<Double>) oldPropertyRange.value;
				if ( newValue < oldValue.getLower() ) {
					return ComparisonResult.BIGGER;
				}
				else {
					return ComparisonResult.NOT_COMPARABLE;
				}
			}
			if ( oldPropertyRange.operator == NOT_INNER ) {
				Interval<Double> oldValue = (Interval<Double>) oldPropertyRange.value;
				if ( newValue >= oldValue.getUpper() ) {
					return ComparisonResult.SMALLER;
				}
				else {
					return ComparisonResult.NOT_COMPARABLE;
				}
			}
		}

		if ( newPropertyRange.operator == NOT_EQUALS ) {
			double newValue = ( (Double) newPropertyRange.value ).doubleValue();
			if ( oldPropertyRange.operator == EQUALS ) {
				double oldValue = ( (Double) oldPropertyRange.value ).doubleValue();
				if ( oldValue == newValue ) {
					return ComparisonResult.NOT_COMPARABLE;
				}
				else {
					return ComparisonResult.BIGGER;
				}
			}
			if ( oldPropertyRange.operator == LOWER ) {
				double oldValue = ( (Double) oldPropertyRange.value ).doubleValue();
				if ( newValue >= oldValue ) {
					return ComparisonResult.BIGGER;
				}
				else {
					return ComparisonResult.NOT_COMPARABLE;
				}
			}
			if ( oldPropertyRange.operator == GREATER ) {
				double oldValue = ( (Double) oldPropertyRange.value ).doubleValue();
				if ( newValue <= oldValue ) {
					return ComparisonResult.BIGGER;
				}
				else {
					return ComparisonResult.NOT_COMPARABLE;
				}
			}
			if ( oldPropertyRange.operator == NOT_EQUALS ) {
				double oldValue = ( (Double) oldPropertyRange.value ).doubleValue();
				if ( oldValue == newValue ) {
					return ComparisonResult.EQUALS;
				}
				else {
					return ComparisonResult.NOT_COMPARABLE;
				}
			}
			if ( oldPropertyRange.operator == INNER ) {
				Interval<Double> oldValue = (Interval<Double>) oldPropertyRange.value;
				if ( ( newValue < oldValue.getLower() ) || ( newValue > oldValue.getUpper() ) ) {
					return ComparisonResult.BIGGER;
				}
				else {
					return ComparisonResult.NOT_COMPARABLE;
				}
			}
			if ( oldPropertyRange.operator == NOT_INNER ) {
				Interval<Double> oldValue = (Interval<Double>) oldPropertyRange.value;
				if ( ( oldValue.getLower() <= newValue ) && ( newValue <= oldValue.getUpper() ) ) {
					return ComparisonResult.BIGGER;
				}
				else {
					return ComparisonResult.NOT_COMPARABLE;
				}
			}
		}

		if ( newPropertyRange.operator == INNER ) {
			Interval<Double> newValue = (Interval<Double>) newPropertyRange.value;
			if ( oldPropertyRange.operator == EQUALS ) {
				double oldValue = ( (Double) oldPropertyRange.value ).doubleValue();
				if ( ( newValue.getLower() <= oldValue ) && ( oldValue <= newValue.getUpper() ) ) {
					return ComparisonResult.BIGGER;
				}
				else {
					{
						return ComparisonResult.NOT_COMPARABLE;
					}
				}
			}
			if ( oldPropertyRange.operator == LOWER ) {
				double oldValue = ( (Double) oldPropertyRange.value ).doubleValue();
				if ( oldValue >= newValue.getUpper() ) {
					return ComparisonResult.SMALLER;
				}
				else {
					return ComparisonResult.NOT_COMPARABLE;
				}
			}
			if ( oldPropertyRange.operator == GREATER ) {
				double oldValue = ( (Double) oldPropertyRange.value ).doubleValue();
				if ( oldValue <= newValue.getLower() ) {
					return ComparisonResult.SMALLER;
				}
				else {
					return ComparisonResult.NOT_COMPARABLE;
				}
			}
			if ( oldPropertyRange.operator == NOT_EQUALS ) {
				double oldValue = ( (Double) oldPropertyRange.value ).doubleValue();
				if ( ( oldValue < newValue.getLower() ) || ( newValue.getUpper() < oldValue ) ) {
					return ComparisonResult.SMALLER;
				}
				else {
					return ComparisonResult.NOT_COMPARABLE;
				}
			}
			if ( oldPropertyRange.operator == INNER ) {
				Interval<Double> oldValue = (Interval<Double>) oldPropertyRange.value;
				if ( oldValue.equals( newValue ) ) {
					return ComparisonResult.EQUALS;
				}
				else if ( ( oldValue.getLower() <= newValue.getLower() )
							&& ( oldValue.getUpper() >= newValue.getUpper() ) ) {
					return ComparisonResult.SMALLER;
				}
				else if ( ( oldValue.getLower() >= newValue.getLower() )
							&& ( oldValue.getUpper() <= newValue.getUpper() ) ) {
					return ComparisonResult.BIGGER;
				}
				else {
					return ComparisonResult.NOT_COMPARABLE;
				}
			}
			if ( oldPropertyRange.operator == NOT_INNER ) {
				Interval<Double> oldValue = (Interval<Double>) oldPropertyRange.value;
				if ( ( newValue.getUpper() <= oldValue.getLower() || ( newValue.getLower() >= newValue.getUpper() ) ) ) {
					return ComparisonResult.SMALLER;
				}
				else {
					return ComparisonResult.NOT_COMPARABLE;
				}
			}
		}
		if ( newPropertyRange.operator == NOT_INNER ) {
			Interval<Double> newValue = (Interval<Double>) newPropertyRange.value;
			if ( oldPropertyRange.operator == EQUALS ) {
				double oldValue = ( (Double) oldPropertyRange.value ).doubleValue();
				if ( ( oldValue >= newValue.getUpper() ) || ( oldValue <= newValue.getLower() ) ) {
					return ComparisonResult.BIGGER;
				}
				else {
					return ComparisonResult.NOT_COMPARABLE;
				}
			}
			if ( oldPropertyRange.operator == LOWER ) {
				double oldValue = ( (Double) oldPropertyRange.value ).doubleValue();
				if ( oldValue <= newValue.getLower() ) {
					return ComparisonResult.BIGGER;
				}
				else {
					return ComparisonResult.NOT_COMPARABLE;
				}
			}
			if ( oldPropertyRange.operator == GREATER ) {
				double oldValue = ( (Double) oldPropertyRange.value ).doubleValue();
				if ( oldValue >= newValue.getUpper() ) {
					return ComparisonResult.BIGGER;
				}
				else {
					return ComparisonResult.NOT_COMPARABLE;
				}
			}
			if ( oldPropertyRange.operator == NOT_EQUALS ) {
				double oldValue = ( (Double) oldPropertyRange.value ).doubleValue();
				if ( ( newValue.getLower() <= oldValue ) && ( oldValue <= newValue.getUpper() ) ) {
					return ComparisonResult.SMALLER;
				}
				else {
					return ComparisonResult.NOT_COMPARABLE;
				}
			}
			if ( oldPropertyRange.operator == INNER ) {
				Interval<Double> oldValue = (Interval<Double>) oldPropertyRange.value;
				if ( ( oldValue.getUpper() <= newValue.getLower() ) || ( oldValue.getLower() >= newValue.getUpper() ) ) {
					return ComparisonResult.BIGGER;
				}
				return ComparisonResult.NOT_COMPARABLE;
			}
			if ( oldPropertyRange.operator == NOT_INNER ) {
				Interval<Double> oldValue = (Interval<Double>) oldPropertyRange.value;
				if ( oldValue.equals( newValue ) ) {
					return ComparisonResult.EQUALS;
				}
				else if ( ( newValue.getLower() <= oldValue.getLower() )
							&& ( newValue.getUpper() >= oldValue.getUpper() ) ) {
					return ComparisonResult.SMALLER;
				}
				else if ( ( newValue.getLower() >= oldValue.getLower() )
							&& ( newValue.getUpper() <= oldValue.getUpper() ) ) {
					return ComparisonResult.BIGGER;
				}
				else {
					return ComparisonResult.NOT_COMPARABLE;
				}
			}
		}
		throw new IllegalArgumentException();
	}

	private ComparisonResult compareString( PropertyRange oldPropertyRange ) {
		String newString = (String) this.value;
		String oldString = (String) oldPropertyRange.value;

		if ( oldPropertyRange.operator == EQUALS ) {
			switch ( this.operator ) {
				case ( EQUALS ): {
					if ( newString.equals( oldString ) ) {
						return ComparisonResult.EQUALS;
					}
					else
						return ComparisonResult.NOT_COMPARABLE;
				}
				case ( STARTS_WITH ): {
					if ( oldString.startsWith( newString ) ) {
						return ComparisonResult.BIGGER;
					}
					else
						return ComparisonResult.NOT_COMPARABLE;
				}

				case ( ENDS_WITH ): {
					if ( oldString.endsWith( newString ) ) {
						return ComparisonResult.BIGGER;
					}
					else
						return ComparisonResult.NOT_COMPARABLE;
				}

				case ( CONTAINS ): {
					if ( oldString.contains( newString ) ) {
						return ComparisonResult.BIGGER;
					}
					else
						return ComparisonResult.NOT_COMPARABLE;
				}

			}
		}

		else if ( oldPropertyRange.operator == STARTS_WITH ) {
			switch ( this.operator ) {
				case ( EQUALS ): {
					if ( newString.startsWith( oldString ) ) {
						return ComparisonResult.SMALLER;
					}
					else
						return ComparisonResult.NOT_COMPARABLE;
				}
				case ( STARTS_WITH ): {
					if ( newString.equals( oldString ) ) {
						return ComparisonResult.EQUALS;
					}
					else if ( newString.startsWith( oldString ) ) {
						return ComparisonResult.SMALLER;
					}
					else if ( oldString.startsWith( newString ) ) {
						return ComparisonResult.BIGGER;
					}
					else
						return ComparisonResult.NOT_COMPARABLE;
				}

				case ( ENDS_WITH ): {
					return ComparisonResult.NOT_COMPARABLE;
				}

				case ( CONTAINS ): {
					if ( oldString.contains( newString ) ) {
						return ComparisonResult.BIGGER;
					}
					else
						return ComparisonResult.NOT_COMPARABLE;

				}

			}
		}

		else if ( oldPropertyRange.operator == ENDS_WITH ) {
			switch ( this.operator ) {
				case ( EQUALS ): {
					if ( newString.endsWith( oldString ) ) {
						return ComparisonResult.SMALLER;
					}
					else
						return ComparisonResult.NOT_COMPARABLE;
				}
				case ( STARTS_WITH ): {
					return ComparisonResult.NOT_COMPARABLE;
				}

				case ( ENDS_WITH ): {
					if ( newString.equals( oldString ) ) {
						return ComparisonResult.EQUALS;
					}
					else if ( newString.endsWith( oldString ) ) {
						return ComparisonResult.SMALLER;
					}
					else if ( oldString.endsWith( newString ) ) {
						return ComparisonResult.BIGGER;
					}
					else
						return ComparisonResult.NOT_COMPARABLE;
				}

				case ( CONTAINS ): {
					if ( oldString.contains( newString ) ) {
						return ComparisonResult.BIGGER;
					}
					else
						return ComparisonResult.NOT_COMPARABLE;
				}

			}
		}

		else if ( oldPropertyRange.operator == CONTAINS ) {
			switch ( this.operator ) {
				case ( EQUALS ): {
					if ( newString.contains( oldString ) ) {
						return ComparisonResult.SMALLER;
					}
					else
						return ComparisonResult.NOT_COMPARABLE;
				}
				case ( STARTS_WITH ): {
					if ( newString.contains( oldString ) ) {
						return ComparisonResult.SMALLER;
					}
					else if ( oldString.contains( newString ) ) {
						return ComparisonResult.SMALLER;
					}
					else
						return ComparisonResult.NOT_COMPARABLE;
				}

				case ( ENDS_WITH ): {
					if ( newString.contains( oldString ) ) {
						return ComparisonResult.SMALLER;
					}
					else if ( oldString.contains( newString ) ) {
						return ComparisonResult.SMALLER;
					}
					else
						return ComparisonResult.NOT_COMPARABLE;
				}

				case ( CONTAINS ): {
					if ( oldString.contains( newString ) ) {
						return ComparisonResult.BIGGER;
					}
					else if ( newString.contains( oldString ) ) {
						return ComparisonResult.SMALLER;
					}
					else
						return ComparisonResult.NOT_COMPARABLE;
				}

			}
		}

		throw new IllegalArgumentException();
	}

}

	

