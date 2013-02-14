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
import polimi.reds.broker.routing.FutureInt;
import polimi.reds.broker.routing.Router;
import polimi.reds.broker.routing.RoutingStrategy;
import polimi.reds.broker.routing.SubscriptionTable;

/**
 * Context Aware version of <tt>SubscriptionForwardingRoutingStrategy</tt>
 * 
 */
public class CASubscriptionForwardingRoutingStratgy implements RoutingStrategy {

	private Logger logger;
	protected CARouter caRouter = null;

	// private Logger logger;
	private Overlay overlay = null;

	private ViewSendingStrategy viewSendingStrategy;

	public CASubscriptionForwardingRoutingStratgy() {
		super();
		logger = Logger.getLogger("polimi.reds.CAReconfigurator");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * polimi.reds.broker.routing.RoutingStrategy#setRouter(polimi.reds.broker
	 * .routing.Router)
	 */
	public void setRouter(Router router) {
		if (!(router instanceof CARouter)) {
			throw new IllegalArgumentException("Router must be a ContextRouter");
		}
		this.caRouter = (CARouter) router;
	}

	public void setViewSendingStrategy(ViewSendingStrategy viewSendingStrategy) {
		this.viewSendingStrategy = viewSendingStrategy;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * polimi.reds.broker.routing.RoutingStrategy#setOverlay(polimi.reds.broker
	 * .overlay.Overlay)
	 */
	public void setOverlay(Overlay overlay) {
		this.overlay = overlay;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see polimi.reds.broker.routing.RoutingStrategy#getOverlay()
	 */
	public Overlay getOverlay() {
		return this.overlay;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see polimi.reds.broker.routing.RoutingStrategy#subscribe(polimi.reds.
	 * NodeDescriptor, polimi.reds.Filter)
	 */
	public void subscribe(NodeDescriptor sender, Filter filter) {

		logger.fine(sender.getID() + " subscribed");
		if (!(filter instanceof CAFilter)) {
			throw new IllegalArgumentException("Filter must be a ContextFilter");
		}
		CAFilter caFilter = (CAFilter) filter;

		SubscriptionTable subscriptionTable = caRouter.getSubscriptionTable();
		if (!subscriptionTable.isFilterInTable(caFilter)) {
			forwardToAllExcept(caFilter, sender, Router.SUBSCRIBE);
		}

		else {
			// FIXME: cambiare la lingua
			// Se ho solo un interessato, devo avvisarlo di inoltrare anche a me
			// i messaggi corretti
			// Se ne ho pi di uno, sono gi un punto di passaggio, quindi i
			// mess mi arrivano gi
			ContextTable contextTable = caRouter.getContextTable();
			NodeDescriptor singleNode = (NodeDescriptor) subscriptionTable.getSingleSubscribedBroker(caFilter);

			if (singleNode != null && !singleNode.equals(sender))
				if (contextTable.getContextReceived(singleNode).isMatchedBy(caFilter.getContextFilter()))
					try {
						overlay.send(Router.SUBSCRIBE, caFilter, singleNode);
					} catch (NotConnectedException e) {
						// TODO: se necessario, mettere il logger
					}
		}

		// Update the local subscription table

		// if ( !( subscriptionTable.getSubscribedNeighbors( contextFilter
		// ).contains( sender ) ) ) {
		subscriptionTable.addSubscription(sender, caFilter);
		// }

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see polimi.reds.broker.routing.RoutingStrategy#unsubscribe(polimi.reds.
	 * NodeDescriptor, polimi.reds.Filter)
	 */
	public void unsubscribe(NodeDescriptor source, Filter filter) {
		if (!(filter instanceof CAFilter)) {
			throw new IllegalArgumentException("Filter must be a ContextFilter");
		}
		CAFilter contextFilter = (CAFilter) filter;

		SubscriptionTable subscriptionTable = caRouter.getSubscriptionTable();
		subscriptionTable.removeSubscription(source, contextFilter);

		if (!subscriptionTable.isFilterInTable(contextFilter)) {
			forwardToAllExcept(contextFilter, source, Router.UNSUBSCRIBE);
		} else {
			// FIXME: cambiare la lingua
			// Se rimasto un solo interessato, devo avvisarlo di non
			// inoltrarmi pi i suoi mess
			// Se ce n' pi di uno, perch sono un punto di passaggio, quindi
			// devo continuare a fare da ponte
			NodeDescriptor node = (NodeDescriptor) subscriptionTable.getSingleSubscribedBroker(filter);
			if (node != null)
				try {
					overlay.send(Router.UNSUBSCRIBE, filter, node);
				} catch (NotConnectedException e) {
				}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * polimi.reds.broker.routing.RoutingStrategy#unsubscribeAll(polimi.reds
	 * .NodeDescriptor)
	 */
	@SuppressWarnings("unchecked")
	public void unsubscribeAll(NodeDescriptor neighbor) {
		SubscriptionTable subscriptionTable = caRouter.getSubscriptionTable();
		Collection neighborFilters = subscriptionTable.getAllFilters(neighbor);
		if (neighborFilters != null && !neighborFilters.isEmpty()) {
			Iterator it = new ArrayList(neighborFilters).iterator();
			while (it.hasNext())
				unsubscribe(neighbor, (Filter) it.next());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * polimi.reds.broker.routing.RoutingStrategy#publish(polimi.reds.NodeDescriptor
	 * , polimi.reds.Message)
	 */
	public FutureInt publish(NodeDescriptor source, Message message) {

		if (!(message instanceof CAMessage)) {
			return null;
		}

		int messagesSent = forwardToAllInterestedNeighborExcept((CAMessage) message, source);
		return new FutureInt(messagesSent);
	}

	private void forwardToAllExcept(CAFilter filter, NodeDescriptor sender, String action) {
		List neighbors = overlay.getAllNeighborsExcept(sender);
		ContextTable contextTable = caRouter.getContextTable();

		if (neighbors != null) {
			NodeDescriptor node;
			Iterator it = neighbors.iterator();
			while (it.hasNext()) {
				node = (NodeDescriptor) it.next();
				if (node.isBroker()) {
					ContextSet cs = contextTable.getContextReceived(node);

					if (cs.isMatchedBy(filter.getContextFilter())) {
						logger.finest("Il " + cs.toString() + "isMatchedBy \n" + filter.getContextFilter().toString()
								+ " --> Invio il filtro\n");
						try {
							overlay.send(action, filter, node);
						} catch (NotConnectedException e) {
							// logger.warning( "Error while forwarding
							// subscription:
							// neighbor " + sender.getID()
							// + " is now disconnected." );
						}
					}
				}
			}
		}
	}

	private int forwardToAllInterestedNeighborExcept(CAMessage message, NodeDescriptor source) {
		SubscriptionTable subscriptionTable = caRouter.getSubscriptionTable();
		ContextTable contextTable = caRouter.getContextTable();

		Collection interestedNeighbor = subscriptionTable.matches(message, source);
		Iterator it = interestedNeighbor.iterator();

		int messagesSent = 0;
		NodeDescriptor node;
		while (it.hasNext()) {
			node = (NodeDescriptor) it.next();

			if ((!node.equals(source))
					&& (contextTable.getContextReceived(node).isMatchedBy(message.getDestinationContext()))) {
				try {
					overlay.send(Router.PUBLISH, message, node);
					messagesSent++;
				} catch (NotConnectedException e) {
					// logger.warning( "Error while forwarding message: neighbor
					// " + source.getID() + " is now disconnected." );
				}
			}
		}
		return messagesSent;

	}

	/**
	 * This method must be called whenever a node changes its context and it's
	 * necessary to "inform" the other node of this.
	 * 
	 * @param node
	 *            the node whose context changed
	 * @param newContextSet
	 *            the new node context
	 */
	public synchronized void signalNewContextReceived(NodeDescriptor node, ContextSet newContextSet) {
		logger.fine("Start update view algorithm ");
		this.updateContextTable(node, newContextSet);
		this.executePreActionsForSender(node);

		this.sendViewToOtherNodes(node);

		this.executePostActionsForOtherNodes(node);
	}

	/**
	 * Updates the contextTable putting the received context set
	 */
	private void updateContextTable(NodeDescriptor senderNode, ContextSet newContextSet) {
		ContextTable contextTable = caRouter.getContextTable();
		contextTable.putContextReceived(senderNode, newContextSet);
	}

	/**
	 * If the new context matches some subscription then sends them to the node
	 */
	private void executePreActionsForSender(NodeDescriptor senderNode) {
		if (senderNode.isBroker()) {
			sendAllSubscriptionsMatchedByContext(senderNode);
		}
	}

	/**
	 * Calls the view sender strategy method <code>sendViewDueToNode</code>
	 */
	private void sendViewToOtherNodes(NodeDescriptor senderNode) {
		this.viewSendingStrategy.sendViewDueTo(senderNode);
	}

	/**
	 * For each node, if the new sent view doesn't match an old subscription
	 * then remove it from the subscription table
	 */
	private void executePostActionsForOtherNodes(NodeDescriptor senderNode) {
		SubscriptionTable subscriptionTable = caRouter.getSubscriptionTable();
		ContextTable contextTable = caRouter.getContextTable();

		for (NodeDescriptor n : contextTable.getNodes()) {
			if (n.isBroker() && (!n.equals(senderNode)))

				if (subscriptionTable.isSubscribed(n)) {

					ContextSet lastViewSent = contextTable.getContextSent(n);

					Collection<CAFilter> allSubscriptions = new ArrayList<CAFilter>(subscriptionTable.getAllFilters(n));
					for (CAFilter subscription : allSubscriptions) {
						if (!lastViewSent.isMatchedBy(subscription.getContextFilter())) {
							subscriptionTable.removeSubscription(n, subscription);
						}
					}
				}

		}
	}

	private void sendAllSubscriptionsMatchedByContext(NodeDescriptor receiver) {
		SubscriptionTable subscriptionTable = caRouter.getSubscriptionTable();

		ContextTable contextTable = caRouter.getContextTable();
		ContextSet receiverContextSet = contextTable.getContextReceived(receiver);

		if (receiverContextSet != null) {

			Collection<CAFilter> allSubscriptions = subscriptionTable.getAllFiltersExcept(false, receiver);
			for (CAFilter subscription : allSubscriptions) {
				if (receiverContextSet.isMatchedBy(subscription.getContextFilter())) {
					logger.finer("Il " + receiverContextSet.toString() + "isMatchedBy \n" + subscription.toString()
							+ " --> Invio il filtro\n");
					try {
						caRouter.getOverlay().send(Router.SUBSCRIBE, subscription, receiver);
					} catch (NotConnectedException e) {
						e.printStackTrace();
					}
				}
			}

		}

	}

}
