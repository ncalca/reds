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
 * A filter for text messages. It can match text messages exactly, or messages
 * that begin with a given string, or messages that end with a given string, or
 * messages that contain a given string.
 * 
 * @see polimi.reds.TextMessage
 **********************************************************************/
public class TextFilter implements Filter {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7329825719776541651L;

	/** A constant to specify exact matching. */
	public final static int EXACT = 0;

	/** A constant to specify containment matching. */
	public final static int CONTAINS = 1;

	/**
	 * A constant to specify that the filter matches messages that begins with a
	 * given string.
	 */
	public final static int BEGINS = 2;

	/**
	 * A constant to specify that the filter matches messages that ends with a
	 * given string.
	 */
	public final static int ENDS = 3;

	/**
	 * The type of match. Specifies the matching strategy (e.g., EXACT,
	 * CONTAINS, ...)
	 */
	protected int typeOfMatch;

	/**
	 * The filtering string that characterizes this filter. Together with the
	 * matching strategy determines which messages are matched.
	 */
	protected String filter;

	/**
	 * Builds a new <tt>TextFilter</tt> with the given filtering string and
	 * matching strategy.
	 * 
	 * @param filter
	 *            The filtering string that identifies this filter.
	 * @param typeOfMatch
	 *            The matching strategy (e.g. exact, begins, ...)
	 */
	public TextFilter(String filter, int typeOfMatch) {
		this.filter = filter;
		this.typeOfMatch = typeOfMatch;
	}

	/**
	 * Builds a new <tt>TextFilter</tt> with the given filtering string and an
	 * exact matching strategy.
	 * 
	 * @param filter
	 *            The filtering string that identifies this filter.
	 * @see #TextFilter(String,int)
	 */
	public TextFilter(String filter) {
		this(filter, EXACT);
	}

	/**
	 * Builds a new <tt>TextFilter</tt> with a <tt>null</tt> filtering string
	 * and an exact matching strategy.
	 * 
	 * @see #TextFilter(String,int)
	 */
	public TextFilter() {
		this(null, EXACT);
	}

	/**
	 * Get the value of the filtering string.
	 * 
	 * @return The value of filtering string.
	 */
	public String getFilter() {
		return filter;
	}

	/**
	 * Set the value of the filtering string.
	 * 
	 * @param v
	 *            Value to assign to the filtering string
	 */
	public void setFilter(String v) {
		this.filter = v;
	}

	/**
	 * Get the type of match.
	 * 
	 * @return The type of match.
	 */
	public int getTypeOfMatch() {
		return typeOfMatch;
	}

	/**
	 * Set the type of match.
	 * 
	 * @param v
	 *            The type of match.
	 */
	public void setTypeOfMatch(int v) {
		this.typeOfMatch = v;
	}

	/**
	 * Test whether this filter matches the given message. A preconditions to
	 * obtain a positive answer is the message being an instance of the
	 * <tt>TextMessage</tt> class. The result depends on the filtering string
	 * and the type of match.
	 * 
	 * @return <tt>true</tt> if the filter matches the given message.
	 */
	public boolean matches(Message msg) {
		if (!(msg instanceof TextMessage))
			return false;

		TextMessage mess = (TextMessage) msg;

		switch (typeOfMatch) {
		case EXACT:
			return filter.equals(mess.getData());
		case CONTAINS:
			return mess.getData().indexOf(filter) != -1;
		case BEGINS:
			return mess.getData().startsWith(filter);
		case ENDS:
			return mess.getData().endsWith(filter);
		}

		return false;
	}

	/**
	 * Test if this filter is equal to an other filter. There are two conditions
	 * to obtain a positive answer: <code>Object</code> o is an instance of the
	 * <tt>TextFilter</tt> class, the the filtering string that characterizes
	 * the filters equals, and they use the same matching strategy.
	 * 
	 * @param o
	 *            The filter to compare against.
	 * @return <tt>true</tt> if the two filters are equal.
	 */
	public boolean equals(Object o) {
		if (!(o instanceof TextFilter))
			return false;
		TextFilter compare = (TextFilter) o;
		return filter.equals(compare.filter) && typeOfMatch == compare.typeOfMatch;
	}

	/**
	 * Returns the hash code of this object. You MUST redefine this method if
	 * you redefine method <code>equals</code>.
	 * 
	 * @return the hash code of this object.
	 */
	public int hashCode() {
		return filter.hashCode() + typeOfMatch;
	}

	// For debug purposes only
	public String toString() {
		return "Class TextFilter: " + typeOfMatch + " " + filter;
	}
}
