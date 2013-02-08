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

import polimi.reds.context.routing.PropertyRange;

public class Util {

	public static String getLongString( String s1, String s2 ) {
		if ( s1.length() <= s2.length() ) {
			return s2;
		}
		else {
			return s1;
		}
	}

	public static String getShortString( String s1, String s2 ) {
		if ( s1.length() <= s2.length() ) {
			return s1;
		}
		else {
			return s2;
		}
	}

	public static String getLongestCommonString( String s1, String s2 ) {
		String shortString = Util.getShortString( s1, s2 );
		String longString = Util.getLongString( s1, s2 );

		String substring = "";
		String maxSubstringFound = "";

		for ( int startIndex = 0; startIndex < shortString.length(); startIndex++ ) {
			for ( int endIndex = shortString.length(); endIndex > startIndex; endIndex-- ) {
				substring = shortString.substring( startIndex, endIndex );
				if ( longString.indexOf( substring ) > -1 ) {
					if ( substring.length() > maxSubstringFound.length() ) {
						maxSubstringFound = substring;
					}
				}
			}
		}

		return maxSubstringFound;

	}

	public static boolean hasSameOperator( PropertyRange pr1, PropertyRange pr2 ) {
		return pr1.getOperator() == pr2.getOperator();
	}
	
	public static boolean hasSameOperator( Condition c1, Condition c2 ) {
		return c1.getOperator() == c2.getOperator();
	}

	public static boolean isOneOperator( int typeOfOperator, PropertyRange pr1, PropertyRange pr2 ) {
		return ( ( pr1.getOperator() == typeOfOperator ) || ( pr2.getOperator() == typeOfOperator ) );
	}
	
	public static boolean isOneOperator( int typeOfOperator, Condition c1, Condition c2 ) {
		return ( ( c1.getOperator() == typeOfOperator ) || ( c2.getOperator() == typeOfOperator ) );
	}

	public static boolean areOperators( int typeA, int typeB, PropertyRange pr1, PropertyRange pr2 ) {
		boolean A1_B2 = ( pr1.getOperator() == typeA ) && ( pr2.getOperator() == typeB );
		boolean A2_B1 = ( pr2.getOperator() == typeA ) && ( pr1.getOperator() == typeB );

		return A1_B2 || A2_B1;
	}
	
	public static boolean areOperators( int typeA, int typeB, Condition c1, Condition c2 ) {
		boolean A1_B2 = ( c1.getOperator() == typeA ) && ( c2.getOperator() == typeB );
		boolean A2_B1 = ( c2.getOperator() == typeA ) && ( c1.getOperator() == typeB );

		return A1_B2 || A2_B1;
	}
	
	public static double getDoubleValue( Condition c ) {
		if ( c.getValue() instanceof Double ) {
			return ( (Double) c.getValue() ).doubleValue();
		}
		else if ( c.getValue() instanceof Integer ) {
			return ( (Integer) c.getValue() ).doubleValue();
		}
		else {
			throw new IllegalArgumentException();
		}
	}

	public static double getDoubleValue( PropertyRange range ) {
		if ( range.getValue() instanceof Double ) {
			return ( (Double) range.getValue() ).doubleValue();
		}
		else if ( range.getValue() instanceof Integer ) {
			return ( (Integer) range.getValue() ).doubleValue();
		}
		else {
			throw new IllegalArgumentException();
		}
	}

	public static boolean isOneANY( PropertyRange r1, PropertyRange r2 ) {
		boolean c1ANY = Util.isANY( r1 );
		boolean c2ANY = Util.isANY( r2 );
		return c1ANY || c2ANY;
	}
	
	public static boolean isOneANY( Condition c1, Condition c2 ) {
		boolean c1ANY = Util.isANY( c1 );
		boolean c2ANY = Util.isANY( c2 );
		return c1ANY || c2ANY;
	}

	public static boolean isANY( PropertyRange r ) {
		return ( r.getOperator() == PropertyRange.EQUALS ) && ( r.getValue() instanceof PropertyRange.Any );
	}
	
	public static boolean isANY( Condition c ) {
		return ( c.getOperator() == Condition.EQUALS ) && ( c.getValue() instanceof Condition.Any );
	}

}
