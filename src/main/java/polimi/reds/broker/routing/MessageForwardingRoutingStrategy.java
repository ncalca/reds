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

import java.util.Iterator;
import java.util.logging.Logger;
import polimi.reds.Filter;
import polimi.reds.Message;
import polimi.reds.NodeDescriptor;
import polimi.reds.NotConnectedException;
import polimi.reds.broker.overlay.Overlay;

/**
 * This class implements a message forwarding routing strategy among a set of
 * brokers connected in an unrooted tree.
 */
public class MessageForwardingRoutingStrategy implements RoutingStrategy {
	/**
	 * Reference to the router. Must be set before starting the broker (use the
	 * <code>setRouter</code> method.
	 */
	protected Router router = null;
	private Logger logger;
	private Overlay overlay = null;

	public MessageForwardingRoutingStrategy() {
		logger = Logger.getLogger("polimi.reds.router");
	}

	public void subscribe(NodeDescriptor neighbor, Filter filter) {
		logger.fine("Subscribing " + neighbor + " to " + filter);
		// If subscribing neighbor is a broker something strange happened.
		if (neighbor.isBroker()) {
			logger.warning(neighbor + " is a broker. Brokers are not expected to forward subscriptions!");
			return;
		}
		// Record filter in table
		router.getSubscriptionTable().addSubscription(neighbor, filter);
	}

	public void unsubscribe(NodeDescriptor neighbor, Filter filter) {
		logger.fine("Unsubscribing " + neighbor + " from " + filter);
		// If unsubscribing neighbor is a broker something strange happened.
		if (neighbor.isBroker()) {
			logger.warning(neighbor + " is a broker. Brokers are not expected to forward unsubscriptions!");
			return;
		}
		// Remove filter from table
		router.getSubscriptionTable().removeSubscription(neighbor, filter);
	}

	public void unsubscribeAll(NodeDescriptor neighbor) {
		logger.fine("Unsubscribing " + neighbor + " from all previously sent filters");
		// If unsubscribing neighbor is a broker something strange happened.
		if (neighbor.isBroker()) {
			logger.warning(neighbor + " is a broker. Brokers are not expected to forward unsubscriptions!");
			return;
		}
		// Remove all subscriptions from table
		router.getSubscriptionTable().removeAllSubscriptions(neighbor);
	}

	public FutureInt publish(NodeDescriptor neighbor, Message message) {
		logger.fine("Publishing " + message + " coming from " + neighbor);
		SubscriptionTable subscriptionTable = router.getSubscriptionTable();
		// Forward the message to subscribed peers
		Iterator it = subscriptionTable.matches(message, neighbor).iterator();
		NodeDescriptor d = null;
		int numNeighbor = 0; // counts the number of neighbors to which the
								// message is sent.
		while (it.hasNext()) {
			d = (NodeDescriptor) it.next();
			try {
				overlay.send(Router.PUBLISH, message, d);
				numNeighbor++;
			} catch (NotConnectedException e) {
				logger.warning("Error while publishing event: neighbor " + d.getID() + " is now disconnected.");
			}

		}
		// Forward the message to other neighboring brokers except the one
		// from which the message was received
		it = overlay.getAllNeighborsExcept(neighbor).iterator();
		while (it.hasNext()) {
			d = (NodeDescriptor) it.next();
			if (d.isBroker()) {
				try {
					overlay.send(Router.PUBLISH, message, d);
					numNeighbor++;
				} catch (NotConnectedException e) {
					logger.warning("Error while propagating event: neighbor " + neighbor.getID()
							+ " is now disconnected.");
				}
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
