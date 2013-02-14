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

import polimi.reds.broker.overlay.Overlay;
import polimi.reds.broker.overlay.PacketListener;
import polimi.reds.broker.routing.Reconfigurator;
import polimi.reds.broker.routing.Router;
import polimi.reds.broker.routing.FutureInt;
import polimi.reds.broker.routing.RoutingStrategy;
import polimi.reds.broker.routing.SubscriptionTable;
import polimi.reds.*;

import java.io.Serializable;
import java.util.logging.Logger;
import java.util.*;

/**
 * A router which implements the location forwarding protocol as defined in:<br>
 * G. Cugola and J.E. Munoz de Cote, "Introducing Location Awareness in
 * Publish-Subscribe Middleware". Proc. of DEBS05, June 2005.
 */
public class LocationForwardingRoutingStrategy implements RoutingStrategy, PacketListener, Reconfigurator {

	public static final String UPDATE_ZONE = "update_zone";
	public static final String UPDATE_LOCATION = "update_location";
	protected Router router = null;
	private Logger logger;
	protected LocationTable locTable;
	protected ZoneMerger merger;
	protected Overlay overlay = null;

	public LocationForwardingRoutingStrategy(ZoneMerger merger) {
		this.merger = merger;
		this.locTable = new LocationTable();
		logger = Logger.getLogger("polimi.reds.router");
	}

	public void subscribe(NodeDescriptor neighborID, Filter filter) {
		logger.fine("Subscribing " + neighborID.getID() + " to " + filter);
		NodeDescriptor neighbor;
		SubscriptionTable subscriptionTable = router.getSubscriptionTable();
		// Determine which neighbors are interested in this subscription based
		// on
		// their location.
		Collection managingNeighbors;
		if (filter instanceof LocationFilter) {
			LocationFilter lf = (LocationFilter) filter;
			managingNeighbors = locTable.getManagingNeighbors(lf.getZone());
		} else
			managingNeighbors = router.getOverlay().getNeighbors();
		if (managingNeighbors != null) {
			// Forward the subscription.
			if (!subscriptionTable.isFilterInTable(filter)) {
				// This is a new filter, all neighbors except the sender should
				// receive
				// it
				Iterator it = router.getOverlay().getAllNeighborsExcept(neighborID).iterator();
				while (it.hasNext()) {
					neighbor = (NodeDescriptor) it.next();
					if (neighbor.isBroker() && managingNeighbors.contains(neighbor)) {
						try {
							router.getOverlay().send(Router.SUBSCRIBE, filter, neighbor);
						} catch (NotConnectedException e) {
							logger.warning("Error while forwarding subscription: neighbor " + neighbor.getID()
									+ " is now disconnected.");
						}
					}
				}
			} else {
				// There is another subscriber; is it the only one?
				neighbor = (NodeDescriptor) subscriptionTable.getSingleSubscribedBroker(filter);
				if (neighbor != null && !neighbor.getID().equals(neighborID) && managingNeighbors.contains(neighbor)) {
					try {
						router.getOverlay().send(Router.SUBSCRIBE, filter, neighbor);
					} catch (NotConnectedException e) {
						logger.warning("Error while forwarding subscription: neighbor " + neighbor.getID()
								+ " is now disconnected.");
					}
				}
			}
		}
		// Update the local subscription table
		subscriptionTable.addSubscription(neighborID, filter);
	}

	public void unsubscribe(NodeDescriptor neighborID, Filter filter) {
		logger.fine("Unsubscribing " + neighborID.getID() + " from " + filter);
		NodeDescriptor neighbor;
		SubscriptionTable subscriptionTable = router.getSubscriptionTable();
		// Locally unsubscribe
		subscriptionTable.removeSubscription(neighborID, filter);
		// Forward the unsubscription
		if (!subscriptionTable.isFilterInTable(filter)) {
			// No more subscribers: forward the unsubscription to all other
			// brokers
			Iterator it = router.getOverlay().getAllNeighborsExcept(neighborID).iterator();
			while (it.hasNext()) {
				neighbor = (NodeDescriptor) it.next();
				if (neighbor.isBroker()) {
					try {
						router.getOverlay().send(Router.UNSUBSCRIBE, filter, neighbor);
					} catch (NotConnectedException e) {
						logger.warning("Error while forwarding unsubscription: neighbor " + neighbor.getID()
								+ " is now disconnected.");
					}
				}
			}
		} else {
			// There is an other subscriber; is this the only one?
			neighbor = (NodeDescriptor) subscriptionTable.getSingleSubscribedBroker(filter);
			if (neighbor != null) {
				// forward the unsubscription to this neighbor
				try {
					router.getOverlay().send(Router.UNSUBSCRIBE, filter, neighbor);
				} catch (NotConnectedException e) {
					logger.warning("Error while forwarding subscription: neighbor " + neighbor.getID()
							+ " is now disconnected.");
				}
			}
		}
	}

	public void unsubscribeAll(NodeDescriptor neighborID) {
		logger.fine("Unsubscribing " + neighborID.getID() + " from all filters");
		SubscriptionTable subscriptionTable = router.getSubscriptionTable();
		Collection filters = subscriptionTable.getAllFilters(neighborID);
		if (filters == null || filters.isEmpty())
			return;
		// use a copy of the Collection returned by
		// subscriptiontable.getAllFilters
		// since the subscription table may change during unsubscription
		Iterator it = new ArrayList(filters).iterator();
		while (it.hasNext()) {
			unsubscribe(neighborID, (Filter) it.next()); // Remove the filter
		}
	}

	public FutureInt publish(NodeDescriptor sourceID, Message message) {
		logger.fine("Publishing " + message + " coming from " + sourceID.getID());
		SubscriptionTable subscriptionTable = router.getSubscriptionTable();
		NodeDescriptor neighbor;
		// Determine which neighbors manage a zone that overlaps the
		// dest)ination
		// of this message (if this message does not hold a NodeDescriptor
		// behave has
		// if it was addressed all over the net).
		Collection managingNeighbors;
		if (message instanceof LocationMessage) {
			LocationMessage lm = (LocationMessage) message;
			managingNeighbors = locTable.getManagingNeighbors(lm.getDestinationZone());
			if (managingNeighbors == null) {
				return new FutureInt(0);
			}
		} else
			managingNeighbors = router.getOverlay().getNeighbors();
		// Iterate over the collection of subscribed neighbors, forwarding them
		// the
		// message only if they belong to the previous collection, also.
		Iterator it = subscriptionTable.matches(message, sourceID).iterator();
		// Counts the number of neighbors that receive the message.
		int numNeighbor = 0;
		while (it.hasNext()) {
			neighbor = (NodeDescriptor) it.next();
			if (!managingNeighbors.contains(neighbor))
				continue;
			try {
				logger.fine("Forwarding " + message + " to " + neighbor.getID());
				router.getOverlay().send(Router.PUBLISH, message, neighbor);
				numNeighbor++;
			} catch (NotConnectedException e) {
				logger.warning("Error while forwarding message: neighbor " + neighbor.getID() + " is now disconnected.");
			}
		}
		return new FutureInt(numNeighbor);
	}

	public void setRouter(Router router) {
		if (this.router == router)
			return;
		this.router = router;
		router.getOverlay().addPacketListener(this, UPDATE_LOCATION);
		router.getOverlay().addPacketListener(this, UPDATE_ZONE);
	}

	public void signalPacket(String subject, NodeDescriptor sourceID, Serializable msg) {
		Zone newZone;
		if (subject.equals(UPDATE_LOCATION)) {
			newZone = merger.toZone((Location) msg);
		} else if (subject.equals(UPDATE_ZONE)) {
			newZone = (Zone) msg;
		} else {
			logger.warning("Received an internal message I was not registered for!");
			return;
		}
		Zone oldZone = locTable.getZone(sourceID);
		if (newZone.equals(oldZone))
			return;
		locTable.setZone(sourceID, newZone);
		if (sourceID.isBroker()) {
			// Forward subscripions not forwarded before
			Iterator it = router.getSubscriptionTable().getAllFiltersExcept(false, sourceID).iterator();
			LocationFilter f;
			Object o;
			while (it.hasNext()) {
				o = it.next();
				if (!(o instanceof LocationFilter))
					continue;
				f = (LocationFilter) o;
				if (f.getZone().overlaps(newZone) && (oldZone == null || !f.getZone().overlaps(oldZone))) {
					try {
						router.getOverlay().send(Router.SUBSCRIBE, f, sourceID);
					} catch (NotConnectedException e) {
						logger.warning("Error while zone update: neighbor " + sourceID + " is now disconnected.");
					}
				}
			}
		}
		// Forward the updated location table to neighboring brokers
		forwardLocationTable(overlay.getAllNeighborsExcept(sourceID));
	}

	public void setOverlay(Overlay o) {
		overlay = o;
		// register to update_zone and update_location
		overlay.addPacketListener(this, LocationForwardingRoutingStrategy.UPDATE_LOCATION);
		overlay.addPacketListener(this, LocationForwardingRoutingStrategy.UPDATE_ZONE);
	}

	public Overlay getOverlay() {
		return overlay;
	}

	// Forward the location table to the given neighbors
	private void forwardLocationTable(Collection neighbors) {
		Collection allKnownZones = locTable.getAllKnownZones();
		for (Iterator it = neighbors.iterator(); it.hasNext();) {
			NodeDescriptor n = (NodeDescriptor) it.next();
			if (!n.isBroker())
				continue;
			allKnownZones.remove(locTable.getZone(n));
			if (!allKnownZones.isEmpty()) {
				try {
					overlay.send("update_zone", merger.merge(allKnownZones), n);
				} catch (NotConnectedException e) {
					logger.warning("Error while zone update: neighbor " + n.getID() + " is now disconnected.");
				}
			}
			allKnownZones.add(locTable.getZone(n));
		}
	}

	public Router getRouter() {
		return this.router;
	}

	public void signalNeighborRemoved(NodeDescriptor removedNeighbor) {
		logger.fine("Soft-disconnecting " + removedNeighbor.getID());
		// Check if the neighbor exists
		if (overlay.hasNeighbor(removedNeighbor)) {
			logger.warning(removedNeighbor.getID() + " is not known, ignoring call to softDisconnect.");
			return;
		}
		// Remove all the subscription of the neighbor
		router.unsubscribeAll(removedNeighbor);
		// Remove neighbor from the location table
		locTable.removeNeighbor(removedNeighbor);
		// Remove the neighbor from the neighbors-table
		overlay.removeNeighbor(removedNeighbor);
		// Forward updated location table to all (remaining) neighbors
		forwardLocationTable(overlay.getNeighbors());

	}

	public void signalNeighborAdded(NodeDescriptor newNeighbor) {
		if (newNeighbor.isBroker()) {
			Collection allKnownZones = locTable.getAllKnownZones();
			if (!allKnownZones.isEmpty()) {
				Zone z = merger.merge(allKnownZones);
				try {
					overlay.send("update_zone", z, newNeighbor);
				} catch (NotConnectedException e) {
					logger.warning("Error while zone update: neighbor " + newNeighbor.getID() + " is now disconnected.");
				}
			}
		}
	}

	public void signalNeighborDead(NodeDescriptor deadNeighbor) {
		logger.fine("Hard-disconnecting " + deadNeighbor.getID());
		this.signalNeighborRemoved(deadNeighbor);

	}

}
