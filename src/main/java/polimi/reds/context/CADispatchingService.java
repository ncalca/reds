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

package polimi.reds.context;

import polimi.reds.DispatchingService;
import polimi.reds.Filter;
import polimi.reds.Message;

public interface CADispatchingService extends DispatchingService {

	/**
	 * Publish a message that be reiceved only by client whose context is
	 * matched by the filter
	 * 
	 * @param msg
	 *            the message to publish
	 * @param destinationContext
	 *            the required receiver context
	 */
	public void publish(Message msg, ContextFilter destinationContext);

	/**
	 * Subscribe the client to messages matched by <tt>filter</tt> whose sender
	 * context is matched by <tt>senderContext</tt>
	 * 
	 * @param filter
	 *            the message filter
	 * @param senderContext
	 *            the required sender context
	 */
	public void subscribe(Filter filter, ContextFilter senderContext);

	/**
	 * Unsubscribe for messages matched by the pair <tt>filter</tt>-
	 * <tt>senderFilter</tt>.
	 * 
	 * @param filter
	 *            the message filter
	 * @param senderContext
	 *            the required sender context
	 */
	public void unsubscribe(Filter filter, ContextFilter senderContext);

	/**
	 * Set the client context
	 * 
	 * @param context
	 *            the context
	 */
	public void setContext(Context context);

}
