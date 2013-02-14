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
import java.util.HashMap;
import java.util.Iterator;

import polimi.reds.context.ComparisonResult;
import polimi.reds.context.Condition;
import polimi.reds.context.Context;
import polimi.reds.context.ContextFilter;
import polimi.reds.context.Property;

/**
 * Represents a set of <code>Condition</code>s in logic and
 * 
 */
public class ContextRange implements Iterable<PropertyRange>, Serializable {

	private static final long serialVersionUID = -7443361810239960727L;

	private HashMap<String, PropertyRange> properties;

	public ContextRange() {
		properties = new HashMap<String, PropertyRange>();
	}

	/**
	 * Clone the context range
	 * 
	 * @param contextRange
	 *            the context range to clone
	 */
	public ContextRange(ContextRange contextRange) {
		properties = new HashMap<String, PropertyRange>();
		for (PropertyRange propertyRange : contextRange) {
			this.addPropertyRange(new PropertyRange(propertyRange));
		}
	}

	/**
	 * Create a contextRange starting from a <code>Context</code>
	 * 
	 * @param c
	 *            the Context
	 */
	public ContextRange(Context c) {
		this();

		Iterator contextIterator = c.iterator();

		while (contextIterator.hasNext()) {
			Property p = (Property) contextIterator.next();
			this.addPropertyRange(new PropertyRange(p));
		}
	}

	/**
	 * Returns the Iterator over the conditions
	 * 
	 * @return the Iterator over the conditions
	 */
	public Iterator<PropertyRange> iterator() {
		return properties.values().iterator();
	}

	/**
	 * Add a <code>Condition</code> to this
	 * 
	 * @param condition
	 *            the condition to add
	 */
	public void addPropertyRange(PropertyRange propertyRange) {
		properties.put(getPropertyRangeID(propertyRange), propertyRange);
	}

	/**
	 * Test if <code>this</code> is matched by the filter
	 * 
	 * @param filter
	 *            the context filter
	 * @return <code>true</code> is this is matched by the filter,
	 *         <code>false</code> otherwise
	 */
	public boolean isMatchedBy(ContextFilter filter) {
		Iterator filterCoonditionsIterator = filter.iterator();

		while (filterCoonditionsIterator.hasNext()) {
			Condition filterCondition = (Condition) filterCoonditionsIterator.next();

			String conditionID = getConditionID(filterCondition);
			if (!this.properties.containsKey(conditionID))
				return false;

			PropertyRange myPropertyRange = this.properties.get(getConditionID(filterCondition));

			if (myPropertyRange.isSatifiedBy(filterCondition) == false) {
				return false;
			}
		}

		return true;
	}

	private String getPropertyRangeID(PropertyRange pr) {
		return pr.getName() + pr.getDataType();
	}

	private String getConditionID(Condition c) {
		return c.getName() + c.getDataType();
	}

	/**
	 * Test is this is bigger, smaller, equal or not compatible with the param
	 * 
	 * @param oldCond
	 *            the other contextRange
	 * @return the relation beetween this and the other context range
	 */
	public ComparisonResult compareTo(ContextRange other) {
		if (this.equals(other)) {
			return ComparisonResult.EQUALS;
		}

		if (this.isCompareSmallerOrEquals(other)) {
			return ComparisonResult.SMALLER;
		}

		if (other.isCompareSmallerOrEquals(this)) {
			return ComparisonResult.BIGGER;
		}

		return ComparisonResult.NOT_COMPARABLE;
	}

	/**
	 * Check is this filter is smaller or equal to other
	 * 
	 * @param other
	 * @return <tt>true</tt> if <tt>this</tt> is smaller or equal to
	 *         <tt>other</tt>
	 */
	private boolean isCompareSmallerOrEquals(ContextRange other) {

		ComparisonResult comparison = null;

		for (PropertyRange myPropertyRange : this) {
			String conditionID = getPropertyRangeID(myPropertyRange);
			if (!other.properties.containsKey(conditionID)) {
				return false;
			}

			PropertyRange otherPropertyRange = other.properties.get(getPropertyRangeID(myPropertyRange));
			comparison = myPropertyRange.compareTo(otherPropertyRange);

			if ((comparison == ComparisonResult.BIGGER) || (comparison == ComparisonResult.NOT_COMPARABLE)) {
				return false;
			}
		}
		return true;
	}

	public PropertyRange getPropertyRange(String name, int dataType) {
		return properties.get(name + dataType);
	}

	public boolean isStructualEqualsTo(ContextRange cr) {

		if (this.properties.size() != cr.properties.size()) {
			return false;
		}

		for (PropertyRange propertyRange : cr) {
			if (!containsAConditionWithSameStructure(this, propertyRange)) {
				return false;
			}
		}

		return true;
	}

	private boolean containsAConditionWithSameStructure(ContextRange cr, PropertyRange propertyRange) {
		return (cr.getPropertyRange(propertyRange.getName(), propertyRange.getDataType()) != null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object o) {

		if (o == null) {
			return false;
		}

		if (!(this.getClass().equals(o.getClass()))) {
			return false;
		}

		ContextRange otherCr = (ContextRange) o;

		for (PropertyRange propertyRange : otherCr) {
			if (!this.properties.containsValue(propertyRange)) {
				return false;
			}
		}

		for (PropertyRange propertyRange : this) {
			if (!otherCr.properties.containsValue(propertyRange)) {
				return false;
			}
		}

		return true;

	}

	@Override
	public String toString() {
		return "Context range: \n" + properties.toString();
	}

}
