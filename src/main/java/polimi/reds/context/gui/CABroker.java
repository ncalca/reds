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

package polimi.reds.context.gui;

import java.net.ConnectException;
import java.net.MalformedURLException;
import java.util.LinkedHashSet;
import java.util.Set;

import polimi.reds.broker.overlay.AlreadyAddedNeighborException;
import polimi.reds.broker.overlay.GenericOverlay;
import polimi.reds.broker.overlay.Overlay;
import polimi.reds.broker.overlay.SimpleTopologyManager;
import polimi.reds.broker.overlay.TCPTransport;
import polimi.reds.broker.overlay.TopologyManager;
import polimi.reds.broker.overlay.Transport;
import polimi.reds.broker.routing.GenericTable;
import polimi.reds.broker.routing.HashReplyTable;
import polimi.reds.broker.routing.ImmediateForwardReplyManager;
import polimi.reds.broker.routing.Reconfigurator;
import polimi.reds.broker.routing.ReplyManager;
import polimi.reds.broker.routing.ReplyTable;
import polimi.reds.broker.routing.SubscriptionTable;
import polimi.reds.context.routing.CAReconfigurator;
import polimi.reds.context.routing.CARouter;
import polimi.reds.context.routing.CASubscriptionForwardingRoutingStratgy;
import polimi.reds.context.routing.ImmediateViewSendingStrategy;
import polimi.reds.context.routing.PropertyRangeSimplifier;
import polimi.reds.context.routing.ContextSetSimplifier;
import polimi.reds.context.routing.ContextTable;
import polimi.reds.context.routing.GenericContextTable;
import polimi.reds.context.routing.LazyViewSendingStrategy;
import polimi.reds.context.routing.SimplePropertyRangeSimplifier;
import polimi.reds.context.routing.StructuralContextSetSimplifier;
import polimi.reds.context.routing.ViewSendingStrategy;

public class CABroker {

	private Overlay overlay;

	private int myPort;

	private int portToConnect;

	public CABroker( int myPort ) {

		init( myPort );
	}

	private void init( int port ) {
		this.myPort = port;
		Transport transport = new TCPTransport( port );

		Set transports = new LinkedHashSet();
		transports.add( transport );

		TopologyManager topologyManager = new SimpleTopologyManager();

		Reconfigurator caReconfigurator = new CAReconfigurator();

		this.overlay = new GenericOverlay( topologyManager, transports );

		CASubscriptionForwardingRoutingStratgy routingStrategy = new CASubscriptionForwardingRoutingStratgy();

		routingStrategy.setOverlay( overlay );

		SubscriptionTable subscriptionTable = new GenericTable();
		ContextTable contextTable = new GenericContextTable();

		PropertyRangeSimplifier conditionSimplifier = new SimplePropertyRangeSimplifier();
		ContextSetSimplifier contextSetSimplifier = new StructuralContextSetSimplifier( conditionSimplifier );
		contextTable.setContextSetSimplifier( contextSetSimplifier );

		CARouter router = new CARouter( overlay, routingStrategy, subscriptionTable, contextTable);

		ViewSendingStrategy viewSendingStrategy = new ImmediateViewSendingStrategy( router );
		routingStrategy.setViewSendingStrategy( viewSendingStrategy );

		try {
			Thread.sleep( 1000 );
		}
		catch ( InterruptedException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		caReconfigurator.setRouter( router );
		caReconfigurator.setOverlay( overlay );

		router.setOverlay( overlay );
		router.setRoutingStrategy( routingStrategy );
		router.setSubscriptionTable( subscriptionTable );

		routingStrategy.setRouter( router );

		ReplyManager replyManager = new ImmediateForwardReplyManager();
		ReplyTable replyTable = new HashReplyTable();
		router.setReplyManager( replyManager );
		router.setReplyTable( replyTable );
		replyManager.setReplyTable( replyTable );
		replyManager.setOverlay( overlay );

		overlay.start();

	}

	public void jolly() {

	}

	public void connect( String host, int portToConnect ) throws ConnectException, MalformedURLException, AlreadyAddedNeighborException {
		this.portToConnect = portToConnect;
		overlay.addNeighbor( "reds-tcp:" + host + ":" + portToConnect );
	}

	private static void Dormi( int millisec ) {
		try {
			Thread.sleep( millisec );
		}
		catch ( InterruptedException e ) {
			e.printStackTrace();
		}
	}

}
