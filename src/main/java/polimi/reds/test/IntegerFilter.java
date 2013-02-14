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

package polimi.reds.test;

import polimi.reds.ComparableFilter;
import polimi.reds.Message;

public class IntegerFilter implements ComparableFilter {

	private int min = Integer.MIN_VALUE;
	private int max = Integer.MAX_VALUE;
	private static final long serialVersionUID = -7421568140770155321L;

	public IntegerFilter() {

	}

	public IntegerFilter(int min, int max) {
		this.min = min;
		this.max = max;
	}

	public boolean isCoveredBy(ComparableFilter filter) {
		if (!(filter instanceof IntegerFilter))
			return false;
		if (((IntegerFilter) filter).getMin() <= min && ((IntegerFilter) filter).getMin() >= max)
			return true;
		return false;
	}

	public boolean matches(Message msg) {
		if (!(msg instanceof IntegerMessage))
			return false;
		if (((IntegerMessage) msg).getValue() >= min && ((IntegerMessage) msg).getValue() <= max)
			return true;
		return false;
	}

	public int getMin() {
		return min;
	}

	public int getMax() {
		return max;
	}

}
