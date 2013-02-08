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

import polimi.reds.Filter;
import polimi.reds.Message;
import polimi.reds.Repliable;
import polimi.reds.TCPDispatchingService;
import polimi.reds.context.routing.CAFilter;
import polimi.reds.context.routing.CAMessage;
import polimi.reds.context.routing.CARouter;
import polimi.reds.context.routing.ContextRange;
import polimi.reds.context.routing.ContextSet;
import polimi.reds.context.routing.RepliableCAMessage;

/**
 * Context aware version of TCPDispatchingService, needed to connect a client to
 * a context aware broker network
 * 
 */
public class CATCPDispatchingService extends TCPDispatchingService implements CADispatchingService {

	private Context context;

	public CATCPDispatchingService( String host, int port, Context context ) {
		super( host, port );
	}

	/**
	 * Publish a message suitable for all destination
	 */
	@Override
	public synchronized void publish( Message msg ) {
		publish( msg, ContextFilter.ANY );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see context.routing.ContextDispatchingServer#publish(polimi.reds.Message,
	 *      context.ContextFilter)
	 */
	public void publish( Message msg, ContextFilter destinationContext ) {
		if ( msg instanceof Repliable ) {
			super.publish( new RepliableCAMessage( msg, this.context, destinationContext ) );
		}
		else
			super.publish( new CAMessage( msg, this.context, destinationContext ) );
	}

	/**
	 * Subscribe a client message matched by <tt>filter</tt> coming from all
	 * sender
	 */
	public void subscribe( Filter filter ) {
		subscribe( filter, ContextFilter.ANY );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see context.routing.ContextDispatchingServer#subscribe(polimi.reds.Filter,
	 *      context.ContextFilter)
	 */
	public void subscribe( Filter filter, ContextFilter senderContext ) {
		super.subscribe( new CAFilter( filter, senderContext ) );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see context.routing.ContextDispatchingServer#unsubscribe(polimi.reds.Filter,
	 *      context.ContextFilter)
	 */
	public void unsubscribe( Filter filter, ContextFilter senderContext ) {
		unsubscribe( new CAFilter( filter, senderContext ) );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see context.routing.ContextDispatchingServer#setContext(context.Context)
	 */
	public void setContext( Context context ) {
		this.context = context;

		ContextSet contextSet = new ContextSet();
		contextSet.addContextRange( new ContextRange( this.context ) );

		forward( CARouter.UPDATE_CONTEXT, contextSet );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see polimi.reds.TCPDispatchingService#getNextMessage()
	 */
	public Message getNextMessage() {
		Message received = super.getNextMessage();
		return received;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see polimi.reds.TCPDispatchingService#getNextMessage(polimi.reds.Filter)
	 */
	public Message getNextMessage( Filter f ) {
		Message m = super.getNextMessage( f );
		return m;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see polimi.reds.TCPDispatchingService#getNextMessage(long)
	 */
	public Message getNextMessage( long timeout ) {
		Message m = super.getNextMessage( timeout );
		return m;
	}


}
