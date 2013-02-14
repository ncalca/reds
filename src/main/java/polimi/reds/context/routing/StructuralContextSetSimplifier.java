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

import java.util.ArrayList;
import java.util.Collection;

/**
 * This simplifier merges the ContextRange with the same structure
 * 
 */
public class StructuralContextSetSimplifier implements ContextSetSimplifier {
	PropertyRangeSimplifier propertyRangeSimplifier;

	public StructuralContextSetSimplifier(PropertyRangeSimplifier conditionSimplifier) {
		this.propertyRangeSimplifier = conditionSimplifier;
	}

	/**
	 * Set the condition simplifier used to simplify the conditions
	 * 
	 * @param conditionSimplifier
	 *            the condition simplifier
	 */
	public void setPropertyRangeSimplifier(PropertyRangeSimplifier conditionSimplifier) {
		this.propertyRangeSimplifier = conditionSimplifier;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * polimi.reds.context.definition.Simplifier#simplify(polimi.reds.context
	 * .definition.ContextSet)
	 */
	public ContextSet simplify(ContextSet originalContextSet) {
		if (originalContextSet == null) {
			return new ContextSet();
		}

		Collection<ContextSet> clusters = createCluster(originalContextSet);

		ContextSet result = new ContextSet();
		for (ContextSet set : clusters) {
			ContextSet csSemplificato = simplifyOmogeneousContextSet(set);
			result.addAll(csSemplificato);
		}

		return result;
	}

	private Collection<ContextSet> createCluster(ContextSet originalContextSet) {
		boolean added;
		Collection<ContextSet> clusters = new ArrayList<ContextSet>();

		for (ContextRange contextRange : originalContextSet) {
			added = false;
			for (ContextSet contextSetInCluster : clusters) {
				if (contextRange.isStructualEqualsTo(contextSetInCluster.iterator().next())) {
					contextSetInCluster.addContextRange(contextRange);
					added = true;
				}
			}

			if (!added) {
				ContextSet newContextSetToAdd = new ContextSet();
				newContextSetToAdd.addContextRange(contextRange);
				clusters.add(newContextSetToAdd);
			}
		}
		return clusters;
	}

	private ContextSet simplifyOmogeneousContextSet(ContextSet contextSet) {

		ContextRange rangeResult = contextSet.iterator().next();

		for (ContextRange range : contextSet) {
			rangeResult = simplify(rangeResult, range);
		}

		ContextSet result = new ContextSet();
		result.addContextRange(rangeResult);
		return result;

	}

	private ContextRange simplify(ContextRange cr1, ContextRange cr2) {
		ContextRange result = new ContextRange();

		for (PropertyRange pr1 : cr1) {
			PropertyRange pr2 = cr2.getPropertyRange(pr1.getName(), pr1.getDataType());
			result.addPropertyRange(propertyRangeSimplifier.merge(pr1, pr2));
		}

		return result;
	}

}
