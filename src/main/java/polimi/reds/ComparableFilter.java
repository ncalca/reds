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

package polimi.reds;

/**********************************************************************
 * A REDS filter which can be compared with others to determine the most generic
 * one.
 **********************************************************************/
public interface ComparableFilter extends Filter {
	/**
	 * Test whether this filter is covered by (i.e., is less generic than)
	 * another one.
	 * 
	 * @param filter
	 *            the filter to compare with.
	 * @return <tt>true</tt> if this filter is less generic than the given one.
	 */
	public boolean isCoveredBy(ComparableFilter filter);
}
