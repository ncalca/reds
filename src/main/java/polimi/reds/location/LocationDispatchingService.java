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
import polimi.reds.DispatchingService;

/**
 * The interface to a dispatching service providing location-based
 * publish-subscribe services.
 */
public interface LocationDispatchingService extends DispatchingService {
	/**
	 * Subscribes to messages matching the given filter published by clients
	 * located within the given zone.
	 * 
	 * @param filter
	 *            the <code>Filter</code> used to determine the messages the
	 *            client is interested in.
	 * @param zone
	 *            the zone the publishing client must be in for its messages to
	 *            match.
	 */
	public void subscribe(Filter filter, Zone zone);

	/**
	 * Unsubscribes from messages matching the given filter and published by
	 * clients located within the give zone.
	 * 
	 * @param filter
	 *            the <code>Filter</code> used to determine the messages the
	 *            client is no more interested in.
	 * @param zone
	 *            the zone where the publishing client resides.
	 */
	public void unsubscribe(Filter filter, Zone zone);

	/**
	 * Publish a new message to subscribers located within the given zone.
	 * 
	 * @param msg
	 *            the <code>Message</code> to publish.
	 * @param zone
	 *            the zone the subscribed client must be in for the message to
	 *            be routed toward it.
	 */
	public void publish(Message msg, Zone zone);

	/**
	 * Sets the current client location, informing the dispatching service.
	 * 
	 * @param loc
	 *            the current location of the client.
	 */
	public void setLocation(Location loc);

	/**
	 * @see DispatchingService#getNextMessage()
	 */
	public Message getNextMessage();

	/**
	 * @see DispatchingService#getNextMessage(Filter)
	 */
	public Message getNextMessage(Filter f);

	/**
	 * @see DispatchingService#getNextMessage(long)
	 */
	public Message getNextMessage(long timeout);
}
