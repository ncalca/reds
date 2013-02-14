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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import polimi.reds.Filter;
import polimi.reds.Message;
import polimi.reds.NodeDescriptor;
import polimi.reds.NotConnectedException;
import polimi.reds.broker.overlay.Overlay;

/**
 * This class implements a subscription forwarding routing strategy among a set
 * of brokers connected in an unrooted tree.
 */
public class SubscriptionForwardingRoutingStrategy implements RoutingStrategy {
	/**
	 * Reference to the router. Must be set before starting the broker (use the
	 * <code>setRouter</code> method.
	 */
	protected Router router = null;
	private Logger logger;
	private Overlay overlay = null;

	public SubscriptionForwardingRoutingStrategy() {
		logger = Logger.getLogger("polimi.reds.Router");
	}

	public void subscribe(NodeDescriptor neighbor, Filter filter) {
		logger.finest("Subscribing " + neighbor + " to " + filter);
		NodeDescriptor d;
		SubscriptionTable subscriptionTable = router.getSubscriptionTable();
		// Forward the subscription.
		if (!subscriptionTable.isFilterInTable(filter)) {
			// This is a new filter, all neighbors except the sender should
			// receive it
			List l = overlay.getAllNeighborsExcept(neighbor);
			if (l != null) {
				Iterator it = l.iterator();
				while (it.hasNext()) {
					d = (NodeDescriptor) it.next();
					if (d.isBroker())
						try {
							overlay.send(Router.SUBSCRIBE, filter, d);
						} catch (NotConnectedException e) {
							logger.warning("Error while forwarding subscription: neighbor " + neighbor.getID()
									+ " is now disconnected.");
						}
				}
			}
		} else {
			// There is another subscriber; is it the only one?
			d = (NodeDescriptor) subscriptionTable.getSingleSubscribedBroker(filter);
			if (d != null && !d.equals(neighbor))
				try {
					overlay.send(Router.SUBSCRIBE, filter, d);
				} catch (NotConnectedException e) {
					logger.warning("Error while forwarding subscription: neighbor " + neighbor.getID()
							+ " is now disconnected.");
				}
		}
		// Update the local subscription table
		subscriptionTable.addSubscription(neighbor, filter);
	}

	public void unsubscribe(NodeDescriptor neighbor, Filter filter) {
		logger.finest("Unsubscribing " + neighbor.getID() + " from " + filter);
		NodeDescriptor d;
		SubscriptionTable subscriptionTable = router.getSubscriptionTable();
		// Locally unsubscribe
		subscriptionTable.removeSubscription(neighbor, filter);
		// Forward the unsubscription
		if (!subscriptionTable.isFilterInTable(filter)) {
			// No more subscribers: forward the unsubscription to all other
			// brokers
			List l = overlay.getAllNeighborsExcept(neighbor);
			if (l != null) {
				Iterator it = l.iterator();
				while (it.hasNext()) {
					d = (NodeDescriptor) it.next();
					if (d.isBroker())
						try {
							overlay.send(Router.UNSUBSCRIBE, filter, d);
						} catch (NotConnectedException e) {
							logger.warning("Error while forwarding unsubscription: neighbor " + neighbor.getID()
									+ " is now disconnected.");
						}
				}
			}
		} else {
			// There is an other subscriber; is this the only one?
			d = (NodeDescriptor) subscriptionTable.getSingleSubscribedBroker(filter);
			if (d != null)
				try {
					overlay.send(Router.UNSUBSCRIBE, filter, d);
				} catch (NotConnectedException e) {
					logger.warning("Error while forwarding subscription: neighbor " + neighbor.getID()
							+ " is now disconnected.");
				}
		}
	}

	public void unsubscribeAll(NodeDescriptor neighbor) {
		logger.finest("Unsubscribing " + neighbor.getID() + " from all filters");
		SubscriptionTable subscriptionTable = router.getSubscriptionTable();
		// use a copy of the Collection returned by
		// subscriptiontable.getAllFilters
		// since the subscription table may change during unsubscription
		Collection c = subscriptionTable.getAllFilters(neighbor);
		if (c != null && !c.isEmpty()) {
			Iterator it = new ArrayList(c).iterator();
			while (it.hasNext())
				unsubscribe(neighbor, (Filter) it.next());
		}
	}

	/**
	 * @see RoutingStrategy#publish(NodeDescriptor, Message)
	 */
	public FutureInt publish(NodeDescriptor sourceID, Message message) {
		logger.finest("Publishing " + message + " coming from " + sourceID);
		SubscriptionTable subscriptionTable = router.getSubscriptionTable();
		NodeDescriptor d;
		// Iterate over the collection of subscribed neighbors, forwarding them
		// the message
		Iterator it = subscriptionTable.matches(message, sourceID).iterator();
		// Counts the number of neighbors that receive the message.
		int numNeighbor = 0;
		while (it.hasNext()) {
			d = (NodeDescriptor) it.next();
			if (d.equals(sourceID))
				continue;
			try {
				overlay.send(Router.PUBLISH, message, d);
				numNeighbor++;
			} catch (NotConnectedException e) {
				logger.warning("Error while forwarding message: neighbor " + sourceID.getID() + " is now disconnected.");
			}
		}
		return new FutureInt(numNeighbor);
	}

	public void setRouter(Router router) {
		if (this.router == router)
			return;
		this.router = router;
	}

	public void setOverlay(Overlay o) {
		overlay = o;

	}

	public Overlay getOverlay() {
		return overlay;
	}
}
