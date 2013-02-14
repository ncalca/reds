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

public class Interval<T extends Number> implements Serializable {

	private T upper;
	private T lower;

	public Interval(T lower, T upper) {

		if (lower.doubleValue() > upper.doubleValue()) {
			throw new IllegalArgumentException();
		}

		this.lower = lower;
		this.upper = upper;
	}

	public T getLower() {
		return lower;
	}

	public T getUpper() {
		return upper;
	}

	public double getLowerDouble() {
		return lower.doubleValue();
	}

	public double getUpperDouble() {
		return upper.doubleValue();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (!(this.getClass().equals(obj.getClass()))) {
			return false;
		}

		Interval<T> other = (Interval<T>) obj;

		return ((other.lower.equals(this.lower)) && (other.upper.equals(this.upper)));
	}

	@Override
	public String toString() {
		return "[ " + lower.doubleValue() + ", " + upper.doubleValue() + " ]";
	}
}
