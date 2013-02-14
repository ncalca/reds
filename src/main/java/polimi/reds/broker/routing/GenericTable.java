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

package polimi.reds.broker.routing;

import polimi.reds.*;

import java.util.*;

/*******************************************************************************
 * A <code>SubscriptionTable</code> is used by the REDS broker to store
 * information about the subscriptions (i.e., filters) it received from its
 * neighbors. The <code>GenericTable</code> does not make any assumption about
 * the format of filters and messages it can store and manage. It implements the
 * <code>SubscriptionTable</code> interface by keeping a simple list of filters
 * for each neighbor, i.e., the list of filters received from that neighbor
 * through subscription messages, and by using extensively the method
 * <code>Message.match</code> to determine the list of neighbors subscibed to a
 * given message. As a consequence of this choice, it is not particularly
 * efficient (but its "generic", as its name suggests).
 ******************************************************************************/
public class GenericTable implements SubscriptionTable {
	/**
	 * The <code>Map</code> that stores information about received
	 * subscriptions. In particular, neighbors are used as keys and values are
	 * the set of filters received by the corresponding neighbor.
	 */
	protected Map data;

	/**
	 * Builds an empty <code>GenericTable</code>.
	 */
	public GenericTable() {
		data = Collections.synchronizedMap(new HashMap());
	}

	public void addSubscription(NodeDescriptor n, Filter f) {
		List filters = (List) data.get(n);
		if (filters == null) {
			filters = new ArrayList();
			filters.add(f);
			data.put(n, filters);
		} else {
			if (!filters.contains(f))
				filters.add(f);
		}
	}

	public void removeSubscription(NodeDescriptor n, Filter f) {
		List filters = (List) data.get(n);
		if (filters != null) {
			filters.remove(f);
		}
	}

	public void removeAllSubscriptions(NodeDescriptor n) {
		data.remove(n);
	}

	public void clear() {
		data.clear();
	}

	public boolean isSubscribed(NodeDescriptor n) {
		List filters = (List) data.get(n);
		return filters != null && !filters.isEmpty();
	}

	public Collection getAllFilters(NodeDescriptor n) {
		return (List) data.get(n);
	}

	public Collection getAllFilters(boolean duplicate) {
		return getAllFiltersExcept(duplicate, null);
	}

	public Collection getAllFiltersExcept(boolean duplicate, NodeDescriptor n) {
		Collection result = new ArrayList();
		Set keys = data.keySet();
		synchronized (data) {
			Iterator it = keys.iterator();
			NodeDescriptor currentNeighbor;
			while (it.hasNext()) {
				currentNeighbor = (NodeDescriptor) it.next();
				if (currentNeighbor.equals(n))
					continue; // skip neighbor n
				Collection filters = (Collection) data.get(currentNeighbor);
				if (duplicate)
					result.addAll(filters);
				else {
					Iterator it1 = filters.iterator();
					while (it1.hasNext()) {
						Filter f = (Filter) it1.next();
						if (!result.contains(f))
							result.add(f);
					}
				}
			}
		}
		return result;
	}

	public boolean isFilterInTable(Filter filter) {
		Collection val = data.values();
		synchronized (data) {
			Iterator it = val.iterator();
			Collection c;
			while (it.hasNext()) {
				c = (Collection) it.next();
				if (c.contains(filter))
					return true;
			}
		}
		return false;
	}

	public NodeDescriptor getSingleSubscribedBroker(Filter filter) {
		NodeDescriptor foundBroker = null;
		// iterates over the subscribed neighbors
		Set keys = data.keySet();
		synchronized (data) {
			Iterator it = keys.iterator();
			while (it.hasNext()) {
				NodeDescriptor currentNeighbor = (NodeDescriptor) it.next();
				if (((Collection) data.get(currentNeighbor)).contains(filter)) {
					if (foundBroker != null || !currentNeighbor.isBroker()) { // more
																				// than
																				// one
																				// broker
																				// found
																				// or
																				// a
																				// client
																				// found
						return null;
					}
					foundBroker = currentNeighbor;
				}
			}
		}
		return foundBroker;
	}

	public Collection matches(Message message) {
		return matches(message, null);
	}

	public Collection matches(Message message, NodeDescriptor senderID) {
		List matchingNeighbors = new ArrayList();
		// iterates over the subscribed neighbors
		Set keys = data.keySet();
		synchronized (data) {
			Iterator it = keys.iterator();
			NodeDescriptor currentNeighbor;
			while (it.hasNext()) {
				currentNeighbor = (NodeDescriptor) it.next();
				// Check if currentNeighbor is subscribed to the message
				// iterates over the set of filters for currentNeighbor
				if (senderID.equals(currentNeighbor))
					continue;
				Iterator it1 = ((Collection) data.get(currentNeighbor)).iterator();
				while (it1.hasNext()) {
					Filter f = (Filter) it1.next();
					if (f.matches(message)) {
						matchingNeighbors.add(currentNeighbor);
						break;
					}
				}
			}
		}
		return matchingNeighbors;
	}

	public Collection getSubscribedNeighbors(Filter f) {
		List matchingNeighbors = new ArrayList();
		// iterates over the subscribed neighbors
		Set keys = data.keySet();
		synchronized (data) {
			Iterator it = keys.iterator();
			NodeDescriptor currentNeighbor;
			while (it.hasNext()) {
				currentNeighbor = (NodeDescriptor) it.next();
				// Check if currentNeighbor is subscribed to the message
				if (((Collection) data.get(currentNeighbor)).contains(f)) {
					matchingNeighbors.add(currentNeighbor);
				}
			}
		}
		return matchingNeighbors;
	}

	// *** For debug purposes
	public String toString() {
		StringBuffer result = new StringBuffer(super.toString() + "\n");
		// iterates over the subscribed neighbors
		Set keys = data.keySet();
		synchronized (data) {
			Iterator it = keys.iterator();
			NodeDescriptor currentNeighbor;
			while (it.hasNext()) {
				currentNeighbor = (NodeDescriptor) it.next();
				result.append("   " + currentNeighbor + " is subscribed to:\n");
				// iterates over the set of filters for currentNeighbor
				Iterator it1 = ((Collection) data.get(currentNeighbor)).iterator();
				Filter f;
				while (it1.hasNext()) {
					f = (Filter) it1.next();
					result.append("      " + f + "\n");
				}
			}
		}
		return result.toString();
	}
}
