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

import polimi.reds.Filter;
import polimi.reds.Message;
import polimi.reds.context.ContextFilter;

/*******************************************************************************
 * The filter used in the context aware system to subscribe. it contains a
 * content filter and a contextFilter for the sender context
 * 
 */
public class CAFilter implements Filter {

	private static final long serialVersionUID = -7206049226480333674L;

	private Filter contentFilter;

	private ContextFilter contextFilter;

	/*
	 * (non-Javadoc)
	 * 
	 * @see polimi.reds.Filter#matches(polimi.reds.Message)
	 */
	public boolean matches( Message msg ) {

		if ( msg instanceof CAMessage ) {
			CAMessage contextMessage = (CAMessage) msg;
			if ( !this.contentFilter.matches( contextMessage.getMessage() ) ) {
				return false;
			}
			if ( !contextMessage.getSourceContext().isMatchedBy( this.contextFilter ) ) {
				return false;
			}
			return true;
		}
		else {
			return false;
		}

	}

	public CAFilter( Filter contentFilter, ContextFilter contextFilter ) {
		super();
		this.contentFilter = contentFilter;
		this.contextFilter = contextFilter;
	}

	/***************************************************************************
	 * This method returns the content filter contained in this CAFilter
	 * 
	 * @return the content filter
	 */
	public Filter getContentFilter() {
		return this.contentFilter;
	}

	/***************************************************************************
	 * This method returns the context filter contained in this CAFilter
	 * 
	 * @return the context filter
	 */
	public ContextFilter getContextFilter() {
		return this.contextFilter;
	}

	@Override
	public String toString() {
		String result = "\nFilter: Content " + this.contentFilter.toString() + " / Context "
						+ this.contextFilter.toString();
		return result;
	}

	@Override
	public boolean equals( Object other ) {
		if ( other == null ) {
			return false;
		}

		if ( !( other.getClass().equals( this.getClass() ) ) ) {
			return false;
		}

		CAFilter otherContextFilter = (CAFilter) other;

		boolean contentEquals = otherContextFilter.contentFilter.equals( this.contentFilter );
		boolean contextEquals = otherContextFilter.contextFilter.equals( this.contextFilter );

		return ( contentEquals && contextEquals );
	}

}
