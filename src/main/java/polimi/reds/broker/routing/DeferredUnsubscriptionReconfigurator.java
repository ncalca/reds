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
import java.util.logging.Level;
import java.util.logging.Logger;
import polimi.reds.Filter;
import polimi.reds.NodeDescriptor;
import polimi.reds.NotConnectedException;
import polimi.reds.broker.overlay.Overlay;

/**
 * This class provides mechanisms for reconfiguration of subscriptions routes in
 * face of neighbor removal using deferred forwarding of unsubscriptions.
 */
public class DeferredUnsubscriptionReconfigurator implements Reconfigurator {
	/**
	 * The local router.
	 */
	protected Router router;
	/**
	 * The logger
	 */
	protected Logger logger;
	private final static int DEFER_TIMEOUT = 3000;
	/**
	 * The overlay.
	 */
	protected Overlay overlay = null;

	/**
	 * Create a new <code>DeferredUnsubscriptionReconfigurator</code>.
	 * 
	 */
	public DeferredUnsubscriptionReconfigurator() {
		logger = Logger.getLogger("polimi.reds.Reconfigurator");
	}

	/**
	 * @see Reconfigurator#setRouter(Router)
	 */
	public void setRouter(Router router) {
		if (this.router == router)
			return;
		this.router = router;
	}

	/**
	 * Unsubscribe to all the subscriptions of the removed neighbor.
	 * 
	 * @see polimi.reds.broker.overlay.NeighborRemovedListener#signalNeighborRemoved(NodeDescriptor)
	 */
	public void signalNeighborRemoved(NodeDescriptor removedNeighbor) {
		logger.finest("Soft-disconnecting " + removedNeighbor.getID());
		// logger.fine("Soft-disconnecting "+neighborID);
		// Check if the neighbor exists
		if (!overlay.hasNeighbor(removedNeighbor)) {
			logger.warning(removedNeighbor.getID() + " is not known, ignoring call to softDisconnect.");
			return;
		}
		// Remove all the subscription of the neighbor
		router.unsubscribeAll(removedNeighbor);
	}

	/**
	 * If the new neighbor is a broker subscribe the new neighbor to all the
	 * necessary filters.
	 */
	public void signalNeighborAdded(NodeDescriptor newNeighbor) {
		logger.finest("Neighbor added: " + newNeighbor.toString());
		// Connects a new neighbor to this broker.
		if (newNeighbor.isBroker()) {
			// forward local subscriptions to the new neighboring broker
			Iterator it = router.getSubscriptionTable().getAllFiltersExcept(false, newNeighbor).iterator();
			while (it.hasNext())
				try {
					overlay.send(Router.SUBSCRIBE, (Filter) it.next(), newNeighbor);
				} catch (NotConnectedException e) {
					logger.warning("Error while connecting to " + newNeighbor.getID()
							+ ", the broker is now disconnected.");
				}
		}

	}

	/**
	 * Manages the brutal disconnection of the given neighbor.
	 * 
	 * @param deadNeighbor
	 *            the dead neighbor
	 */
	public void signalNeighborDead(NodeDescriptor deadNeighbor) {
		logger.finest("Hard-disconnecting " + deadNeighbor);
		// Check if the neighbor exists
		if (deadNeighbor == null)
			return;
		if (!overlay.hasNeighbor(deadNeighbor)) {
			logger.warning(deadNeighbor.getID() + " is not known, ignoring call to hardDisconnect.");
			return;
		}
		// Is this broker an end-point (a leaf) of the tree?
		if (overlay.numberOfBrokers() == 1) {
			// This disconnecting neighbor is a leaf: the hardDisconnect should
			// behave like a softDisconnect
			logger.fine(deadNeighbor.getID() + " is a leaf: hardDisconnect behaves like softDisconnect");
			signalNeighborRemoved(deadNeighbor);
			return;
		}
		// Locally unsubscription of the disconnected neighbor
		SubscriptionTable subscriptionTable = router.getSubscriptionTable();
		if (!subscriptionTable.isSubscribed(deadNeighbor)) {
			return;
		}
		// Store the filters in a local buffer for the deferred forwarding
		Collection tempBuffer = new ArrayList(subscriptionTable.getAllFilters(deadNeighbor));
		// Update the local subscription table, removing the above filters from
		// the
		// local subscription table
		subscriptionTable.removeAllSubscriptions(deadNeighbor);
		Object x = new Object();
		synchronized (x) {
			try {
				x.wait(DEFER_TIMEOUT);
			} catch (InterruptedException e) {
				logger.log(Level.SEVERE, "InterruptedException while waiting for deferred unsubscription.");
			}
		}
		// The timeout expires: forwards the unsubscription, using the temporary
		// buffer
		Iterator it = tempBuffer.iterator();
		while (it.hasNext()) {
			router.unsubscribe(deadNeighbor, (Filter) it.next());
		}
	}

	/**
	 * Get the router.
	 * 
	 * @return the local router
	 */
	public Router getRouter() {
		return router;
	}

	/**
	 * Set the overlay.
	 * 
	 * @param o
	 *            the overlay
	 */
	public void setOverlay(Overlay o) {
		overlay = o;
		overlay.addNeighborAddedListener(this);
		overlay.addNeighborRemovedListener(this);
		overlay.addNeighborDeadListener(this);
	}

	/**
	 * Get the overlay.
	 * 
	 * @return the overlay
	 */
	public Overlay getOverlay() {
		return overlay;
	}
}
