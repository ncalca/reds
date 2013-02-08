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
import java.util.logging.Logger;

import polimi.reds.Filter;
import polimi.reds.Message;
import polimi.reds.NodeDescriptor;
import polimi.reds.Repliable;
import polimi.reds.Reply;
import polimi.reds.broker.overlay.Overlay;
import polimi.reds.broker.routing.FutureInt;
import polimi.reds.broker.routing.ReplyManager;
import polimi.reds.broker.routing.ReplyTable;
import polimi.reds.broker.routing.Router;
import polimi.reds.broker.routing.RoutingStrategy;
import polimi.reds.broker.routing.SubscriptionTable;

/**
 * This class holds the most common components and method to build a router
 * 
 */
public abstract class AbstractRouter implements Router {

	protected Logger logger;

	protected int debugLevel;

	protected NodeDescriptor nodeDescriptor;

	protected Overlay overlay = null;

	protected RoutingStrategy routingStrategy;

	protected SubscriptionTable subscriptionTable;

	protected ReplyManager replyManager;

	protected ReplyTable replyTable;

	public AbstractRouter() {
		super();
	}

	public AbstractRouter( Overlay overlay, RoutingStrategy routingStrategy, SubscriptionTable subscriptionTable ) {
		super();

		this.overlay = overlay;
		this.routingStrategy = routingStrategy;
		this.subscriptionTable = subscriptionTable;
		this.routingStrategy.setOverlay( overlay );

		this.nodeDescriptor = overlay.getID();
		this.nodeDescriptor.setBroker();

		logger = Logger.getLogger( "polimi.reds.Router" );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see polimi.reds.broker.routing.Router#subscribe(polimi.reds.NodeDescriptor,
	 *      polimi.reds.Filter)
	 */
	public synchronized void subscribe( NodeDescriptor neighbor, Filter filter ) {
		this.routingStrategy.subscribe( neighbor, filter );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see polimi.reds.broker.routing.Router#unsubscribe(polimi.reds.NodeDescriptor,
	 *      polimi.reds.Filter)
	 */
	public synchronized void unsubscribe( NodeDescriptor neighbor, Filter filter ) {
		this.routingStrategy.unsubscribe( neighbor, filter );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see polimi.reds.broker.routing.Router#unsubscribeAll(polimi.reds.NodeDescriptor)
	 */
	public synchronized void unsubscribeAll( NodeDescriptor neighbor ) {
		this.routingStrategy.unsubscribeAll( neighbor );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see polimi.reds.broker.routing.Router#publish(polimi.reds.NodeDescriptor,
	 *      polimi.reds.Message)
	 */
	public synchronized void publish( NodeDescriptor neighbor, Message message ) {
		FutureInt totalSentCount = this.routingStrategy.publish( neighbor, message );
		if ( ( message instanceof Repliable ) && ( replyManager != null ) ) {
			logger.fine( "Record ID for Repliable Message= " + message.getID() );
			replyManager.recordRepliableMessage( message.getID(), neighbor, totalSentCount );
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see polimi.reds.broker.routing.Router#getID()
	 */
	public NodeDescriptor getID() {
		return nodeDescriptor;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see polimi.reds.broker.routing.Router#getSubscriptionTable()
	 */
	public SubscriptionTable getSubscriptionTable() {
		return this.subscriptionTable;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see polimi.reds.broker.routing.Router#setRoutingStrategy(polimi.reds.broker.routing.RoutingStrategy)
	 */
	public void setRoutingStrategy( RoutingStrategy routingStrategy ) {
		this.routingStrategy = routingStrategy;

		overlay.addPacketListener( this, Router.PUBLISH );
		overlay.addPacketListener( this, Router.SUBSCRIBE );
		overlay.addPacketListener( this, Router.UNSUBSCRIBE );
		overlay.addPacketListener( this, Router.UNSUBSCRIBEALL );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see polimi.reds.broker.routing.Router#setSubscriptionTable(polimi.reds.broker.routing.SubscriptionTable)
	 */
	public void setSubscriptionTable( SubscriptionTable subscriptionTable ) {
		this.subscriptionTable = subscriptionTable;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see polimi.reds.broker.routing.Router#setDebugLevel(int)
	 */
	public void setDebugLevel( int debugLevel ) {
		this.debugLevel = debugLevel;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see polimi.reds.broker.routing.Router#getDebugLevel()
	 */
	public int getDebugLevel() {
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see polimi.reds.broker.routing.Router#setOverlay(polimi.reds.broker.overlay.Overlay)
	 */
	public void setOverlay( Overlay overlay ) {
		this.overlay = overlay;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see polimi.reds.broker.routing.Router#getOverlay()
	 */
	public Overlay getOverlay() {
		return this.overlay;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see polimi.reds.broker.routing.Router#setReplyManager(polimi.reds.broker.routing.ReplyManager)
	 */
	public void setReplyManager( ReplyManager replyManager ) {
		if ( !( this.replyManager == replyManager ) ) {
			this.replyManager = replyManager;
			this.replyManager.setRouter( this );
			overlay.addPacketListener( this, Router.REPLY );
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see polimi.reds.broker.routing.Router#getReplyManager()
	 */
	public ReplyManager getReplyManager() {
		return replyManager;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see polimi.reds.broker.routing.Router#setReplyTable(polimi.reds.broker.routing.ReplyTable)
	 */
	public void setReplyTable( ReplyTable replyTable ) {
		this.replyTable = replyTable;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see polimi.reds.broker.routing.Router#getReplyTable()
	 */
	public ReplyTable getReplyTable() {
		return replyTable;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see polimi.reds.broker.routing.Router#forwardReply(polimi.reds.Reply)
	 */
	public synchronized void forwardReply( Reply reply ) {
		replyManager.forwardReply( reply );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see polimi.reds.broker.overlay.PacketListener#signalPacket(java.lang.String,
	 *      polimi.reds.NodeDescriptor, java.io.Serializable)
	 */
	public final void signalPacket( String subject, NodeDescriptor senderID, Serializable payload ) {
		logger.fine( "Packet received" );
		if ( subject.equals( Router.PUBLISH ) ) {
			publish( senderID, (Message) payload );
		}
		else if ( subject.equals( Router.REPLY ) ) {
			forwardReply( (Reply) payload );
		}
		else if ( subject.equals( Router.SUBSCRIBE ) ) {
			subscribe( senderID, (Filter) payload );
		}
		else if ( subject.equals( Router.UNSUBSCRIBE ) ) {
			unsubscribe( senderID, (Filter) payload );
		}
		else if ( subject.equals( Router.UNSUBSCRIBEALL ) ) {
			unsubscribeAll( senderID );
		}
		else {
			customProcessPacket( subject, senderID, payload );
		}
	}

	/**
	 * This method is called whenever a new packet arrives from a neighbor of
	 * the local node after having processed PUBLISH, REPLY, UNSUBSCRIBE and
	 * UNSUBCRIBEALL
	 * 
	 * @param subject
	 *            the subject of the packet
	 * @param senderID
	 *            the NodeDescriptor of the sender
	 * @param payload
	 *            the message
	 */
	protected abstract void customProcessPacket( String subject, NodeDescriptor senderID, Serializable payload );

}
