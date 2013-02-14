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
import polimi.reds.broker.overlay.Overlay;

/**
 * In a REDS broker the <code>RoutingStrategy</code> is the module in charge of
 * managing the routing of messages on top of the overlay network (which is
 * realized by the <code>Overlay</code> and maintained, in presence of
 * reconfiguration, by the <code>ConnectionManager</code>. Its methods are
 * called by the overlay (through the router) to perform the basic operations of
 * any event-based dispatching system: process subscriptions and unsubscriptions
 * and forward messages.
 */
public interface RoutingStrategy {
	/**
	 * Subscribes the specified neighbor to the messages matching the given
	 * <code>Filter</code>.
	 * 
	 * @param neighbor
	 *            the identifier of the neighbor which subscribed.
	 * @param filter
	 *            the <code>Filter</code> used to determine the messages the
	 *            neighbor is interested in.
	 */
	public void subscribe(NodeDescriptor neighbor, Filter filter);

	/**
	 * Unsubscribes the specified neighbor from the messages matching the given
	 * <code>Filter</code>. In general, it undoes a corresponding call to the
	 * <code>subscribe</code> method.
	 * 
	 * @param neighbor
	 *            the identifier of the neighbor which wants to be unsubscribed.
	 * @param filter
	 *            the <code>Filter</code> used to determine the messages the
	 *            neighbor is no more interested in.
	 */
	public void unsubscribe(NodeDescriptor neighbor, Filter filter);

	/**
	 * Removes all the subscriptions previously issued by the give neighbor.
	 * 
	 * @param neighbor
	 *            the identifier of the neighbor which requested to be
	 *            unsubscribed.
	 */
	public void unsubscribeAll(NodeDescriptor neighbor);

	/**
	 * Publish the given message coming from the specified neighbor. Depending
	 * on the routing policy adopted, this requires to forward the given message
	 * to some or any of the neighbors of the broker this router is part of.<br>
	 * It returns a <code>FutureInt</code>; it contains the number of neighbors
	 * which received the message. This object allows an asynchronous way to set
	 * this value.
	 * 
	 * @param neighbor
	 *            the identifier of the neighbor from which the message was
	 *            received.
	 * @param message
	 *            the <code>Message</code> to be published.
	 * @return number of neighbors to which the <code>Message</code> is sent.
	 * 
	 */
	public FutureInt publish(NodeDescriptor neighbor, Message message);

	/**
	 * Associates this <code>RoutingStrategy</code> with its <code>Router</code>
	 * . This method has to be invoked inside the corresponding method in
	 * Router, that is Router.setRouter(RoutingStrategy).
	 * 
	 * @param router
	 *            the <code>Router</code> this <code>RoutingStrategy</code> is
	 *            associated to.
	 */
	public void setRouter(Router router);

	/**
	 * Set the overlay.
	 * 
	 * @param o
	 *            the overlay
	 */
	public void setOverlay(Overlay o);

	/**
	 * Get the overlay.
	 * 
	 * @return the overlay
	 */
	public Overlay getOverlay();
} // end RoutingStrategy
