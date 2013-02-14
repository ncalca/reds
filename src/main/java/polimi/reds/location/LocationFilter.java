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

package polimi.reds.location;

import polimi.reds.Filter;
import polimi.reds.Message;

class LocationFilter implements Filter {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7204231586060566185L;
	protected Filter contentFilter;
	protected Zone zone;

	public LocationFilter(Filter contentFilter, Zone zone) {
		this.contentFilter = contentFilter;
		this.zone = zone;
	}

	public Filter getContentFilter() {
		return contentFilter;
	}

	public Zone getZone() {
		return zone;
	}

	public boolean matches(Message msg) {
		if (msg instanceof LocationMessage) {
			LocationMessage lmsg = (LocationMessage) msg;
			return contentFilter.matches(lmsg.getPayload()) && zone.includes(lmsg.sourceLocation);
		} else {
			return contentFilter.matches(msg);
		}
	}

	public boolean equals(Object o) {
		if (o instanceof LocationFilter) {
			LocationFilter f = (LocationFilter) o;
			return contentFilter.equals(f.contentFilter) && zone.equals(f.zone);
		} else
			return false;
	}

	public int hashCode() {
		return contentFilter.hashCode() + zone.hashCode();
	}

	public String toString() {
		return "Filter: " + contentFilter.toString() + " for zone: " + zone.toString();
	}
}
