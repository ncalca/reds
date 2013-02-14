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

import java.io.Serializable;

import polimi.reds.NodeDescriptor;
import polimi.reds.broker.overlay.Overlay;
import polimi.reds.broker.routing.RoutingStrategy;
import polimi.reds.broker.routing.SubscriptionTable;

/**
 * A context aware router. It contains a context table and manages only context
 * aware components
 */
public class CARouter extends AbstractRouter {

	public static final String UPDATE_CONTEXT = "update_context";

	private ContextTable contextTable;

	public CARouter(Overlay overlay, RoutingStrategy routingStrategy, SubscriptionTable subscriptionTable,
			ContextTable contextTable) {
		super(overlay, routingStrategy, subscriptionTable);
		this.contextTable = contextTable;

		overlay.addPacketListener(this, CARouter.UPDATE_CONTEXT);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * polimi.reds.context.routing.AbstractRouter#customProcessPacket(java.lang
	 * .String, polimi.reds.NodeDescriptor, java.io.Serializable)
	 */
	@Override
	protected void customProcessPacket(String subject, NodeDescriptor sender, Serializable payload) {
		if (subject.equals(CARouter.UPDATE_CONTEXT)) {
			ContextSet newContextSet = (ContextSet) payload;
			this.logger.fine("New Context received:\n" + newContextSet.toString());
			((CASubscriptionForwardingRoutingStratgy) routingStrategy).signalNewContextReceived(sender, newContextSet);
		}

	}

	/**
	 * Gets the context table
	 * 
	 * @return the context table
	 */
	public ContextTable getContextTable() {
		return this.contextTable;
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
	public void signalNewContextReceived(NodeDescriptor node, ContextSet newContextSet) {
		((CASubscriptionForwardingRoutingStratgy) this.routingStrategy).signalNewContextReceived(node, newContextSet);
	}

}
