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

import polimi.reds.context.DataTypeMismatchException;
import polimi.reds.context.IllegalDataTypeException;
import polimi.reds.context.Interval;
import polimi.reds.context.NameMismatchException;
import polimi.reds.context.Property;
import polimi.reds.context.Util;

/**
 * This class merges the conditions following the Bellati and Della Torre rules
 * 
 */
public class SimplePropertyRangeSimplifier implements PropertyRangeSimplifier {

	public SimplePropertyRangeSimplifier() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * polimi.reds.context.definition.PropertyRangeSimplifier#merge(polimi.reds
	 * .context.definition.PropertyRange,
	 * polimi.reds.context.definition.PropertyRange)
	 */
	public PropertyRange merge(PropertyRange pr1, PropertyRange pr2) {

		if (!pr1.getName().equals(pr2.getName())) {
			throw new NameMismatchException("Cannot merge condition with different names");
		}

		if (pr1.getDataType() != pr2.getDataType()) {
			throw new DataTypeMismatchException("Cannot merge condition with different data types");
		}

		if (Util.isOneANY(pr1, pr2)) {
			return PropertyRange.CreatePropertyRangeANY(pr1.getName(), pr1.getDataType());
		}

		if (pr1.getDataType() == Property.INTEGER) {
			return simplifyInteger(pr1, pr2);
		} else if (pr1.getDataType() == Property.REAL) {
			return simplifyReal(pr1, pr2);
		} else if (pr1.getDataType() == Property.STRING) {
			return simplifyString(pr1, pr2);
		} else {
			throw new IllegalDataTypeException("Data type not recognized");
		}
	}

	private static PropertyRange simplifyString(PropertyRange pr1, PropertyRange pr2) {
		String conditionName = pr1.getName();
		int conditionDataType = pr1.getDataType();

		String shortString = Util.getShortString((String) pr1.getValue(), (String) pr2.getValue());
		String longString = Util.getLongString((String) pr1.getValue(), (String) pr2.getValue());

		if (Util.areOperators(PropertyRange.STARTS_WITH, PropertyRange.STARTS_WITH, pr1, pr2)) {
			if (longString.indexOf(shortString) > -1) {
				return new PropertyRange(conditionName, conditionDataType, PropertyRange.STARTS_WITH, shortString);
			} else {
				return PropertyRange.CreatePropertyRangeANY(conditionName, conditionDataType);
			}
		}

		else if (Util.areOperators(PropertyRange.STARTS_WITH, PropertyRange.ENDS_WITH, pr1, pr2)) {
			return PropertyRange.CreatePropertyRangeANY(conditionName, conditionDataType);
		}

		else if (Util.areOperators(PropertyRange.STARTS_WITH, PropertyRange.CONTAINS, pr1, pr2)) {
			if (longString.indexOf(shortString) > -1) {
				return new PropertyRange(conditionName, conditionDataType, PropertyRange.CONTAINS, shortString);
			} else {
				return PropertyRange.CreatePropertyRangeANY(conditionName, conditionDataType);
			}
		}

		else if (Util.areOperators(PropertyRange.STARTS_WITH, PropertyRange.EQUALS, pr1, pr2)) {
			if (longString.indexOf(shortString) > -1) {
				return new PropertyRange(conditionName, conditionDataType, PropertyRange.STARTS_WITH, shortString);
			} else {
				return PropertyRange.CreatePropertyRangeANY(conditionName, conditionDataType);
			}
		}

		else if (Util.areOperators(PropertyRange.ENDS_WITH, PropertyRange.ENDS_WITH, pr1, pr2)) {
			if (longString.indexOf(shortString) > -1) {
				return new PropertyRange(conditionName, conditionDataType, PropertyRange.ENDS_WITH, shortString);
			} else {
				return PropertyRange.CreatePropertyRangeANY(conditionName, conditionDataType);
			}
		}

		else if (Util.areOperators(PropertyRange.ENDS_WITH, PropertyRange.CONTAINS, pr1, pr2)) {
			if (longString.indexOf(shortString) > -1) {
				return new PropertyRange(conditionName, conditionDataType, PropertyRange.CONTAINS, shortString);
			} else {
				return PropertyRange.CreatePropertyRangeANY(conditionName, conditionDataType);
			}
		}

		else if (Util.areOperators(PropertyRange.ENDS_WITH, PropertyRange.EQUALS, pr1, pr2)) {
			if (longString.indexOf(shortString) > -1) {
				return new PropertyRange(conditionName, conditionDataType, PropertyRange.ENDS_WITH, shortString);
			} else {
				return PropertyRange.CreatePropertyRangeANY(conditionName, conditionDataType);
			}
		}

		else if (Util.areOperators(PropertyRange.CONTAINS, PropertyRange.CONTAINS, pr1, pr2)) {
			if (longString.indexOf(shortString) > -1) {
				return new PropertyRange(conditionName, conditionDataType, PropertyRange.CONTAINS, shortString);
			} else {
				return PropertyRange.CreatePropertyRangeANY(conditionName, conditionDataType);
			}
		}

		else if (Util.areOperators(PropertyRange.CONTAINS, PropertyRange.EQUALS, pr1, pr2)) {
			if (longString.indexOf(shortString) > -1) {
				return new PropertyRange(conditionName, conditionDataType, PropertyRange.CONTAINS, shortString);
			} else {
				return PropertyRange.CreatePropertyRangeANY(conditionName, conditionDataType);
			}
		}

		else if (Util.areOperators(PropertyRange.EQUALS, PropertyRange.EQUALS, pr1, pr2)) {

			String maxCommonString = Util.getLongestCommonString(shortString, longString);
			if (maxCommonString.length() > 0) {
				if (shortString.startsWith(maxCommonString) && longString.startsWith(maxCommonString)) {
					return new PropertyRange(conditionName, conditionDataType, PropertyRange.STARTS_WITH,
							maxCommonString);
				} else if (shortString.endsWith(maxCommonString) && longString.endsWith(maxCommonString)) {
					return new PropertyRange(conditionName, conditionDataType, PropertyRange.ENDS_WITH, maxCommonString);
				} else {
					return new PropertyRange(conditionName, conditionDataType, PropertyRange.CONTAINS, maxCommonString);
				}
			}

			return PropertyRange.CreatePropertyRangeANY(conditionName, conditionDataType);
		}

		return null;
	}

	private static PropertyRange simplifyReal(PropertyRange pr1, PropertyRange pr2) {

		if (Util.isOneOperator(PropertyRange.EQUALS, pr1, pr2)) {

			PropertyRange propertyRangeWithEQUALS;
			PropertyRange otherPropertyRange;

			if (pr1.getOperator() == PropertyRange.EQUALS) {
				propertyRangeWithEQUALS = pr1;
				otherPropertyRange = pr2;
			} else {
				propertyRangeWithEQUALS = pr2;
				otherPropertyRange = pr1;
			}

			switch (otherPropertyRange.getOperator()) {

			case (PropertyRange.EQUALS): {
				return createPropertyRangeRealEqualsEquals(propertyRangeWithEQUALS, otherPropertyRange);
			}
			case (PropertyRange.LOWER): {
				return createPropertyRangeRealEqualsLower(propertyRangeWithEQUALS, otherPropertyRange);
			}
			case (PropertyRange.GREATER): {
				return createPropertyRangeRealEqualsGreater(propertyRangeWithEQUALS, otherPropertyRange);
			}
			case (PropertyRange.NOT_EQUALS): {
				return createPropertyRangeRealEqualsNotEquals(propertyRangeWithEQUALS, otherPropertyRange);
			}
			case (PropertyRange.INNER): {
				return createPropertyRangeRealEqualsInner(propertyRangeWithEQUALS, otherPropertyRange);
			}
			case (PropertyRange.NOT_INNER): {
				return createPropertyRangeRealEqualsNotInner(propertyRangeWithEQUALS, otherPropertyRange);
			}

			}

		} else if (Util.isOneOperator(PropertyRange.LOWER, pr1, pr2)) {
			PropertyRange propertyRangeWithLOWER;
			PropertyRange otherPropertyRange;

			if (pr1.getOperator() == PropertyRange.LOWER) {
				propertyRangeWithLOWER = pr1;
				otherPropertyRange = pr2;
			} else {
				propertyRangeWithLOWER = pr2;
				otherPropertyRange = pr1;
			}

			switch (otherPropertyRange.getOperator()) {

			case (PropertyRange.LOWER): {
				return createPropertyRangeRealLowerLower(propertyRangeWithLOWER, otherPropertyRange);
			}
			case (PropertyRange.GREATER): {
				return createPropertyRangeRealLowerGreater(propertyRangeWithLOWER, otherPropertyRange);
			}
			case (PropertyRange.NOT_EQUALS): {
				return createPropertyRangeRealLowerNotEquals(propertyRangeWithLOWER, otherPropertyRange);
			}
			case (PropertyRange.INNER): {
				return createPropertyRangeRealLowerInner(propertyRangeWithLOWER, otherPropertyRange);
			}
			case (PropertyRange.NOT_INNER): {
				return createPropertyRangeRealLowerNotInner(propertyRangeWithLOWER, otherPropertyRange);
			}
			}
		} else if (Util.isOneOperator(PropertyRange.GREATER, pr1, pr2)) {
			PropertyRange propertyRangeWithGreater;
			PropertyRange otherPropertyRange;

			if (pr1.getOperator() == PropertyRange.GREATER) {
				propertyRangeWithGreater = pr1;
				otherPropertyRange = pr2;
			} else {
				propertyRangeWithGreater = pr2;
				otherPropertyRange = pr1;
			}

			switch (otherPropertyRange.getOperator()) {

			case (PropertyRange.GREATER): {
				return createPropertyRangeRealGreaterGreater(propertyRangeWithGreater, otherPropertyRange);
			}
			case (PropertyRange.NOT_EQUALS): {
				return createPropertyRangeRealGreaterNotEquals(propertyRangeWithGreater, otherPropertyRange);
			}
			case (PropertyRange.INNER): {
				return createPropertyRangeRealGreaterInner(propertyRangeWithGreater, otherPropertyRange);
			}
			case (PropertyRange.NOT_INNER): {
				return createPropertyRangeRealGreaterNotInner(propertyRangeWithGreater, otherPropertyRange);
			}
			}
		} else if (Util.isOneOperator(PropertyRange.NOT_EQUALS, pr1, pr2)) {
			PropertyRange propertyRangeWithNotEquals;
			PropertyRange otherPropertyRange;

			if (pr1.getOperator() == PropertyRange.NOT_EQUALS) {
				propertyRangeWithNotEquals = pr1;
				otherPropertyRange = pr2;
			} else {
				propertyRangeWithNotEquals = pr2;
				otherPropertyRange = pr1;
			}

			switch (otherPropertyRange.getOperator()) {

			case (PropertyRange.NOT_EQUALS): {
				return createPropertyRangeRealNotEqualsNotEquals(propertyRangeWithNotEquals, otherPropertyRange);
			}
			case (PropertyRange.INNER): {
				return createPropertyRangeRealNotEqualsInner(propertyRangeWithNotEquals, otherPropertyRange);
			}
			case (PropertyRange.NOT_INNER): {
				return createPropertyRangeRealNotEqualsNotInner(propertyRangeWithNotEquals, otherPropertyRange);
			}
			}
		} else if (Util.isOneOperator(PropertyRange.INNER, pr1, pr2)) {
			PropertyRange propertyRangeWithInner;
			PropertyRange otherPropertyRange;

			if (pr1.getOperator() == PropertyRange.INNER) {
				propertyRangeWithInner = pr1;
				otherPropertyRange = pr2;
			} else {
				propertyRangeWithInner = pr2;
				otherPropertyRange = pr1;
			}

			switch (otherPropertyRange.getOperator()) {

			case (PropertyRange.INNER): {
				return createPropertyRangeRealInnerInner(propertyRangeWithInner, otherPropertyRange);
			}
			case (PropertyRange.NOT_INNER): {
				return createPropertyRangeRealInnerNotInner(propertyRangeWithInner, otherPropertyRange);
			}
			}
		} else if (Util.isOneOperator(PropertyRange.NOT_INNER, pr1, pr2)) {
			PropertyRange propertyRangeWithNotInner;
			PropertyRange otherPropertyRange;

			if (pr1.getOperator() == PropertyRange.NOT_INNER) {
				propertyRangeWithNotInner = pr1;
				otherPropertyRange = pr2;
			} else {
				propertyRangeWithNotInner = pr2;
				otherPropertyRange = pr1;
			}

			switch (otherPropertyRange.getOperator()) {

			case (PropertyRange.NOT_INNER): {
				return createPropertyRangeRealNotInnerNotInner(propertyRangeWithNotInner, otherPropertyRange);
			}
			}
		}

		return null;
	}

	private static PropertyRange createPropertyRangeRealEqualsLower(PropertyRange pr1, PropertyRange pr2) {
		double max = Math.max(Util.getDoubleValue(pr1), Util.getDoubleValue(pr2));

		return new PropertyRange(pr1.getName(), pr1.getDataType(), PropertyRange.LOWER, new Double(max));
	}

	private static PropertyRange createPropertyRangeRealEqualsGreater(PropertyRange c1, PropertyRange c2) {
		double min = Math.min(Util.getDoubleValue(c1), Util.getDoubleValue(c2));

		return new PropertyRange(c1.getName(), c1.getDataType(), PropertyRange.GREATER, new Double(min));
	}

	private static PropertyRange createPropertyRangeRealEqualsNotEquals(PropertyRange propertyRangeWithEquals,
			PropertyRange propertyRangeWithNotEquals) {
		if (Util.getDoubleValue(propertyRangeWithEquals) == Util.getDoubleValue(propertyRangeWithNotEquals)) {
			return PropertyRange.CreatePropertyRangeANY(propertyRangeWithEquals.getName(),
					propertyRangeWithEquals.getDataType());
		} else {
			return new PropertyRange(propertyRangeWithNotEquals);
		}

	}

	private static PropertyRange createPropertyRangeRealEqualsInner(PropertyRange conditionEquals,
			PropertyRange conditionInner) {
		double max = Math.max(Util.getDoubleValue(conditionEquals),
				((Interval<Double>) conditionInner.getValue()).getUpper());

		double min = Math.min(Util.getDoubleValue(conditionEquals),
				((Interval<Double>) conditionInner.getValue()).getLower());

		return new PropertyRange(conditionEquals.getName(), conditionEquals.getDataType(), PropertyRange.INNER,
				new Interval<Double>(min, max));
	}

	private static PropertyRange createPropertyRangeRealEqualsNotInner(PropertyRange propertyRangeWithEquals,
			PropertyRange propertyRangeWithNotInner) {

		double lower = ((Interval<Double>) propertyRangeWithNotInner.getValue()).getLower();
		double upper = ((Interval<Double>) propertyRangeWithNotInner.getValue()).getUpper();
		double meanPoint = (lower + upper) / 2.0;

		double equals = Util.getDoubleValue(propertyRangeWithEquals);

		Interval<Double> bounds = null;

		if (equals > meanPoint) {
			bounds = new Interval<Double>(lower, Math.min(upper, equals));
		} else {
			bounds = new Interval<Double>(Math.max(lower, equals), upper);
		}

		return new PropertyRange(propertyRangeWithEquals.getName(), propertyRangeWithEquals.getDataType(),
				PropertyRange.NOT_INNER, bounds);

	}

	private static PropertyRange createPropertyRangeRealEqualsEquals(PropertyRange c1, PropertyRange c2) {

		double max = Math.max(Util.getDoubleValue(c1), Util.getDoubleValue(c2));

		double min = Math.min(Util.getDoubleValue(c1), Util.getDoubleValue(c2));

		return new PropertyRange(c1.getName(), c1.getDataType(), PropertyRange.INNER, new Interval<Double>(min, max));
	}

	private static PropertyRange createPropertyRangeRealLowerLower(PropertyRange propertyRangeWithLOWER,
			PropertyRange otherPropertyRange) {
		double max = Math.max(Util.getDoubleValue(propertyRangeWithLOWER), Util.getDoubleValue(otherPropertyRange));

		return new PropertyRange(propertyRangeWithLOWER.getName(), propertyRangeWithLOWER.getDataType(),
				PropertyRange.LOWER, new Double(max));
	}

	private static PropertyRange createPropertyRangeRealLowerGreater(PropertyRange propertyRangeWithLOWER,
			PropertyRange otherPropertyRange) {
		double lower = Util.getDoubleValue(propertyRangeWithLOWER);
		double greater = Util.getDoubleValue(otherPropertyRange);

		if (lower > greater) {
			return PropertyRange.CreatePropertyRangeANY(propertyRangeWithLOWER.getName(),
					propertyRangeWithLOWER.getDataType());
		} else if (lower < greater) {
			return new PropertyRange(propertyRangeWithLOWER.getName(), propertyRangeWithLOWER.getDataType(),
					PropertyRange.NOT_INNER, new Interval<Double>(lower, greater));
		} else { // sono uguali
			return new PropertyRange(propertyRangeWithLOWER.getName(), propertyRangeWithLOWER.getDataType(),
					PropertyRange.NOT_EQUALS, new Double(lower));
		}
	}

	private static PropertyRange createPropertyRangeRealLowerNotEquals(PropertyRange propertyRangeWithLOWER,
			PropertyRange otherPropertyRange) {
		double lower = Util.getDoubleValue(propertyRangeWithLOWER);
		double notEquals = Util.getDoubleValue(otherPropertyRange);

		if (lower > notEquals) {
			return PropertyRange.CreatePropertyRangeANY(propertyRangeWithLOWER.getName(),
					propertyRangeWithLOWER.getDataType());
		} else {
			return new PropertyRange(propertyRangeWithLOWER.getName(), propertyRangeWithLOWER.getDataType(),
					PropertyRange.NOT_EQUALS, new Double(notEquals));
		}
	}

	private static PropertyRange createPropertyRangeRealLowerInner(PropertyRange propertyRangeWithLOWER,
			PropertyRange otherPropertyRange) {
		double max = Math.max(Util.getDoubleValue(propertyRangeWithLOWER),
				((Interval<Double>) otherPropertyRange.getValue()).getUpper());

		return new PropertyRange(propertyRangeWithLOWER.getName(), propertyRangeWithLOWER.getDataType(),
				PropertyRange.LOWER, new Double(max));
	}

	private static PropertyRange createPropertyRangeRealLowerNotInner(PropertyRange propertyRangeWithLOWER,
			PropertyRange otherPropertyRange) {

		double lowerBound = ((Interval<Double>) otherPropertyRange.getValue()).getLower();
		double upperBound = ((Interval<Double>) otherPropertyRange.getValue()).getUpper();
		double lower = Util.getDoubleValue(propertyRangeWithLOWER);

		if (lower < upperBound) {
			return new PropertyRange(propertyRangeWithLOWER.getName(), propertyRangeWithLOWER.getDataType(),
					PropertyRange.NOT_INNER, new Interval<Double>(Math.max(lower, lowerBound), upperBound));
		} else {
			return PropertyRange.CreatePropertyRangeANY(propertyRangeWithLOWER.getName(),
					propertyRangeWithLOWER.getDataType());
		}
	}

	private static PropertyRange createPropertyRangeRealGreaterGreater(PropertyRange propertyRangeWithGreater,
			PropertyRange otherPropertyRange) {
		double min = Math.min(Util.getDoubleValue(propertyRangeWithGreater), Util.getDoubleValue(otherPropertyRange));

		return new PropertyRange(propertyRangeWithGreater.getName(), propertyRangeWithGreater.getDataType(),
				PropertyRange.GREATER, new Double(min));
	}

	private static PropertyRange createPropertyRangeRealGreaterNotEquals(PropertyRange propertyRangeWithGreater,
			PropertyRange otherPropertyRange) {
		double greater = Util.getDoubleValue(propertyRangeWithGreater);
		double notEquals = Util.getDoubleValue(otherPropertyRange);

		if (greater < notEquals) {
			return PropertyRange.CreatePropertyRangeANY(propertyRangeWithGreater.getName(),
					propertyRangeWithGreater.getDataType());
		} else {
			return new PropertyRange(propertyRangeWithGreater.getName(), propertyRangeWithGreater.getDataType(),
					PropertyRange.NOT_EQUALS, new Double(notEquals));
		}
	}

	private static PropertyRange createPropertyRangeRealGreaterInner(PropertyRange propertyRangeWithGreater,
			PropertyRange otherPropertyRange) {
		double min = Math.min(Util.getDoubleValue(propertyRangeWithGreater),
				((Interval<Double>) otherPropertyRange.getValue()).getLower());

		return new PropertyRange(propertyRangeWithGreater.getName(), propertyRangeWithGreater.getDataType(),
				PropertyRange.GREATER, new Double(min));
	}

	private static PropertyRange createPropertyRangeRealGreaterNotInner(PropertyRange propertyRangeWithGreater,
			PropertyRange otherPropertyRange) {
		double lowerBound = ((Interval<Double>) otherPropertyRange.getValue()).getLower();
		double upperBound = ((Interval<Double>) otherPropertyRange.getValue()).getUpper();
		double greater = Util.getDoubleValue(propertyRangeWithGreater);

		if (greater > lowerBound) {
			return new PropertyRange(propertyRangeWithGreater.getName(), propertyRangeWithGreater.getDataType(),
					PropertyRange.NOT_INNER, new Interval<Double>(lowerBound, Math.min(upperBound, greater)));
		} else {
			return PropertyRange.CreatePropertyRangeANY(propertyRangeWithGreater.getName(),
					propertyRangeWithGreater.getDataType());
		}
	}

	private static PropertyRange createPropertyRangeRealNotEqualsNotEquals(PropertyRange propertyRangeWithNotEquals,
			PropertyRange otherPropertyRange) {
		double firstNotEquals = Util.getDoubleValue(propertyRangeWithNotEquals);
		double secondNotEquals = Util.getDoubleValue(otherPropertyRange);

		if (firstNotEquals == secondNotEquals) {
			return new PropertyRange(propertyRangeWithNotEquals);
		} else {
			return PropertyRange.CreatePropertyRangeANY(propertyRangeWithNotEquals.getName(),
					propertyRangeWithNotEquals.getDataType());
		}
	}

	private static PropertyRange createPropertyRangeRealNotEqualsInner(PropertyRange propertyRangeWithNotEquals,
			PropertyRange otherPropertyRange) {
		double lowerBound = ((Interval<Double>) otherPropertyRange.getValue()).getLower();
		double upperBound = ((Interval<Double>) otherPropertyRange.getValue()).getUpper();
		double notEquals = Util.getDoubleValue(propertyRangeWithNotEquals);

		if ((lowerBound < notEquals) && (notEquals < upperBound)) {
			return new PropertyRange(propertyRangeWithNotEquals);
		} else {
			return PropertyRange.CreatePropertyRangeANY(propertyRangeWithNotEquals.getName(),
					propertyRangeWithNotEquals.getDataType());
		}
	}

	private static PropertyRange createPropertyRangeRealNotEqualsNotInner(PropertyRange propertyRangeWithNotEquals,
			PropertyRange otherPropertyRange) {
		double lowerBound = ((Interval<Double>) otherPropertyRange.getValue()).getLower();
		double upperBound = ((Interval<Double>) otherPropertyRange.getValue()).getUpper();
		double notEquals = Util.getDoubleValue(propertyRangeWithNotEquals);

		if ((notEquals < lowerBound) || (upperBound < lowerBound)) {
			return PropertyRange.CreatePropertyRangeANY(propertyRangeWithNotEquals.getName(),
					propertyRangeWithNotEquals.getDataType());
		} else {
			return new PropertyRange(propertyRangeWithNotEquals);
		}
	}

	private static PropertyRange createPropertyRangeRealInnerInner(PropertyRange propertyRangeWithInner,
			PropertyRange otherPropertyRange) {
		double firstLowerBound = ((Interval<Double>) otherPropertyRange.getValue()).getLower();
		double firstUpperBound = ((Interval<Double>) otherPropertyRange.getValue()).getUpper();

		double secondLowerBound = ((Interval<Double>) propertyRangeWithInner.getValue()).getLower();
		double secondUpperBound = ((Interval<Double>) propertyRangeWithInner.getValue()).getUpper();

		Interval<Double> bounds = new Interval<Double>(Math.min(firstLowerBound, secondLowerBound), Math.max(
				firstUpperBound, secondUpperBound));

		return new PropertyRange(propertyRangeWithInner.getName(), propertyRangeWithInner.getDataType(),
				PropertyRange.INNER, bounds);
	}

	private static PropertyRange createPropertyRangeRealInnerNotInner(PropertyRange propertyRangeWithInner,
			PropertyRange otherPropertyRange) {
		double notInnerLowerBound = ((Interval<Double>) otherPropertyRange.getValue()).getLower();
		double notInnerUpperBound = ((Interval<Double>) otherPropertyRange.getValue()).getUpper();

		double innerLowerBound = ((Interval<Double>) propertyRangeWithInner.getValue()).getLower();
		double innerUpperBound = ((Interval<Double>) propertyRangeWithInner.getValue()).getUpper();

		if ((innerLowerBound >= notInnerUpperBound) || (innerUpperBound <= notInnerLowerBound)) {
			return new PropertyRange(otherPropertyRange);
		} else if ((innerLowerBound <= notInnerLowerBound) && (innerUpperBound < notInnerUpperBound)) {
			return new PropertyRange(propertyRangeWithInner.getName(), propertyRangeWithInner.getDataType(),
					PropertyRange.NOT_INNER, new Interval<Double>(innerUpperBound, notInnerUpperBound));
		} else if ((innerLowerBound > notInnerLowerBound) && (innerUpperBound >= notInnerUpperBound)) {
			return new PropertyRange(propertyRangeWithInner.getName(), propertyRangeWithInner.getDataType(),
					PropertyRange.NOT_INNER, new Interval<Double>(notInnerLowerBound, innerLowerBound));
		} else {
			return PropertyRange.CreatePropertyRangeANY(propertyRangeWithInner.getName(),
					propertyRangeWithInner.getDataType());
		}

	}

	private static PropertyRange createPropertyRangeRealNotInnerNotInner(PropertyRange propertyRangeWithNotInner,
			PropertyRange otherPropertyRange) {

		double firstLowerBound = ((Interval<Double>) otherPropertyRange.getValue()).getLower();
		double firstUpperBound = ((Interval<Double>) otherPropertyRange.getValue()).getUpper();

		double secondLowerBound = ((Interval<Double>) propertyRangeWithNotInner.getValue()).getLower();
		double secondUpperBound = ((Interval<Double>) propertyRangeWithNotInner.getValue()).getUpper();

		double newLowerBound = Math.max(firstLowerBound, secondLowerBound);
		double newUpperBound = Math.min(firstUpperBound, secondUpperBound);

		if (newLowerBound > newUpperBound) {
			return PropertyRange.CreatePropertyRangeANY(propertyRangeWithNotInner.getName(),
					propertyRangeWithNotInner.getDataType());
		} else {
			return new PropertyRange(propertyRangeWithNotInner.getName(), propertyRangeWithNotInner.getDataType(),
					PropertyRange.NOT_INNER, new Interval<Double>(newLowerBound, newUpperBound));
		}
	}

	private static PropertyRange simplifyInteger(PropertyRange pr1, PropertyRange pr2) {
		PropertyRange equivalentRealPropertyRange1 = convertIntegerToReal(pr1);
		PropertyRange equivalentRealPropertyRange2 = convertIntegerToReal(pr2);

		PropertyRange result = simplifyReal(equivalentRealPropertyRange1, equivalentRealPropertyRange2);

		return convertRealToInteger(result);
	}

	private static PropertyRange convertRealToInteger(PropertyRange propertyRange) {

		if (Util.isANY(propertyRange)) {
			return PropertyRange.CreatePropertyRangeANY(propertyRange.getName(), Property.INTEGER);
		}

		if ((propertyRange.getOperator() == PropertyRange.EQUALS)
				|| (propertyRange.getOperator() == PropertyRange.GREATER)
				|| (propertyRange.getOperator() == PropertyRange.LOWER)
				|| (propertyRange.getOperator() == PropertyRange.NOT_EQUALS)) {

			return new PropertyRange(propertyRange.getName(), Property.INTEGER, propertyRange.getOperator(),
					new Integer((int) Util.getDoubleValue(propertyRange)));
		} else {

			int intLowerBound = (int) ((Interval<Double>) propertyRange.getValue()).getLowerDouble();
			int intUpperBound = (int) ((Interval<Double>) propertyRange.getValue()).getUpperDouble();

			Interval<Integer> bounds = new Interval<Integer>(intLowerBound, intUpperBound);

			return new PropertyRange(propertyRange.getName(), Property.INTEGER, propertyRange.getOperator(), bounds);
		}
	}

	private static PropertyRange convertIntegerToReal(PropertyRange propertyRange) {

		if (Util.isANY(propertyRange)) {
			return PropertyRange.CreatePropertyRangeANY(propertyRange.getName(), Property.REAL);
		}

		if ((propertyRange.getOperator() == PropertyRange.EQUALS)
				|| (propertyRange.getOperator() == PropertyRange.GREATER)
				|| (propertyRange.getOperator() == PropertyRange.LOWER)
				|| (propertyRange.getOperator() == PropertyRange.NOT_EQUALS)) {

			return new PropertyRange(propertyRange.getName(), Property.REAL, propertyRange.getOperator(), new Double(
					Util.getDoubleValue(propertyRange)));
		} else {

			double doubleLowerBound = ((Interval<Integer>) propertyRange.getValue()).getLower();
			double doubleUpperBound = ((Interval<Integer>) propertyRange.getValue()).getUpper();

			Interval<Double> bounds = new Interval<Double>(doubleLowerBound, doubleUpperBound);

			return new PropertyRange(propertyRange.getName(), Property.REAL, propertyRange.getOperator(), bounds);
		}
	}

}
