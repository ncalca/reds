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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import polimi.reds.context.ComparisonResult;
import polimi.reds.context.ContextFilter;

/**
 * Represents a set of <code>ContextRange</code> in logic or
 * 
 * @author Migh
 * 
 */
public class ContextSet implements Iterable<ContextRange>, Serializable {

	private static final long serialVersionUID = -3955456637910863612L;

	private Collection<ContextRange> contextRanges;

	public ContextSet() {
		contextRanges = new ArrayList<ContextRange>();
	}

	public ContextSet( ContextSet contextSet ) {
		contextRanges = new ArrayList<ContextRange>();
		for ( ContextRange range : contextSet ) {
			contextRanges.add( new ContextRange( range ) );
		}
	}

	/**
	 * Add a ContextRange to this
	 * 
	 * @param c
	 *            the context range to add
	 */
	public void addContextRange( ContextRange c ) {
		contextRanges.add( c );
	}

	/**
	 * Adds all context ranges of cs to this
	 * 
	 * @param cs
	 *            the context set whose context range are to add to this
	 */
	public void addAll( ContextSet cs ) {
		for ( ContextRange range : cs ) {
			contextRanges.add( range );
		}
	}

	/**
	 * Returns the Iterator over the context ranges
	 * 
	 * @return the Iterator over the context ranges
	 */
	public Iterator<ContextRange> iterator() {
		return contextRanges.iterator();
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
		if ( filter.equals( ContextFilter.ANY ) ) {
			return true;
		}

		for ( ContextRange contextRange : contextRanges ) {
			if ( contextRange.isMatchedBy( filter ) == true ) {
				return true;
			}
		}

		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals( Object o ) {

		if ( o == null ) {
			return false;
		}

		if ( !( this.getClass().equals( o.getClass() ) ) ) {
			return false;
		}

		ContextSet cs = (ContextSet) o;

		if ( this.contextRanges.size() != cs.contextRanges.size() ) {
			return false;
		}

		for ( ContextRange cr : cs ) {
			if ( !this.contextRanges.contains( cr ) ) {
				return false;
			}
		}

		return true;

	}

	public String toString() {
		String toString = "ContextSet\n";
		for ( ContextRange cr : this.contextRanges ) {
			toString += cr.toString() + "\n";
		}
		return toString;
	}

	/**
	 * Test is this is bigger, smaller, equal or not compatible with the param
	 * 
	 * @param oldCond
	 *            the other ContextSet
	 * @return the relation beetween this and the other context set
	 */
	public ComparisonResult compareTo( ContextSet other ) {
		if ( other == null ) {
			return ComparisonResult.NOT_COMPARABLE;
		}
		if ( this.equals( other ) ) {
			return ComparisonResult.EQUALS;
		}

		if ( this.isCompareSmaller( other ) ) {
			return ComparisonResult.SMALLER;
		}

		if ( other.isCompareSmaller( this ) ) {
			return ComparisonResult.BIGGER;
		}

		return ComparisonResult.NOT_COMPARABLE;

	}

	private boolean isCompareSmaller( ContextSet other ) {

		ComparisonResult comparison = null;

		for ( ContextRange myRange : this ) {

			boolean smallerFound = false;
			boolean equalsFound = false;

			Iterator otherRangeIterator = other.iterator();

			while ( otherRangeIterator.hasNext() && ( !smallerFound ) ) {
				ContextRange otherRange = (ContextRange) otherRangeIterator.next();

				comparison = myRange.compareTo( otherRange );

				if ( comparison == ComparisonResult.SMALLER ) {
					smallerFound = true;
				}
				else if ( comparison == ComparisonResult.EQUALS ) {
					equalsFound = true;
				}
			}

			if ( ( !smallerFound ) && ( !equalsFound ) ) {
				return false;
			}

		}

		return true;
	}

}
