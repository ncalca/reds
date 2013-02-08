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

import java.util.logging.Logger;

import polimi.reds.NodeDescriptor;
import polimi.reds.NotConnectedException;
import polimi.reds.broker.overlay.Overlay;
import polimi.reds.broker.routing.Reconfigurator;
import polimi.reds.broker.routing.Router;

/**
 * Context aware version of a simple REDS reconfigurator
 * 
 */
public class CAReconfigurator implements Reconfigurator {

	private Logger logger;
	
	private CARouter caRouter;

	private Overlay overlay;

	public CAReconfigurator() {
		logger = Logger.getLogger( "polimi.reds.CAReconfigurator" );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see polimi.reds.broker.routing.Reconfigurator#setRouter(polimi.reds.broker.routing.Router)
	 */
	public void setRouter( Router router ) {
		if ( !( router instanceof CARouter ) ) {
			throw new IllegalArgumentException( "Router must be a ContextRouter" );
		}
		this.caRouter = (CARouter) router;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see polimi.reds.broker.routing.Reconfigurator#getRouter()
	 */
	public Router getRouter() {
		return this.caRouter;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see polimi.reds.broker.routing.Reconfigurator#setOverlay(polimi.reds.broker.overlay.Overlay)
	 */
	public void setOverlay( Overlay overlay ) {
		this.overlay = overlay;
		this.overlay.addNeighborAddedListener( this );
		this.overlay.addNeighborRemovedListener( this );
		this.overlay.addNeighborDeadListener( this );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see polimi.reds.broker.routing.Reconfigurator#getOverlay()
	 */
	public Overlay getOverlay() {
		return this.overlay;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see polimi.reds.broker.overlay.NeighborRemovedListener#signalNeighborRemoved(polimi.reds.NodeDescriptor)
	 */
	public void signalNeighborRemoved( NodeDescriptor removedNeighbor ) {

//		if ( overlay.hasNeighbor( removedNeighbor ) ) {
			scrivi( "NeighborRemoved: " + removedNeighbor.toString() );
			caRouter.unsubscribeAll( removedNeighbor );

//			overlay.removeNeighbor( removedNeighbor );

			caRouter.signalNewContextReceived( removedNeighbor, new ContextSet() );

			ContextTable contextTable = caRouter.getContextTable();
			contextTable.removeNeighbor( removedNeighbor );

//		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see polimi.reds.broker.overlay.NeighborAddedListener#signalNeighborAdded(polimi.reds.NodeDescriptor)
	 */
	public void signalNeighborAdded( NodeDescriptor newNeighbor ) {
		scrivi( "NeighborAdded: " + StampaNodo( newNeighbor ) );

		// Non bisogna fare niente, in quanto l'eventuale invio di sttoscrizioni
		// ecc va fatto al momento di un cambio di Contesto

		// Devo inviare il mio contesto al nuovo vicino
		ContextTable contextTable = caRouter.getContextTable();
		contextTable.putContextReceived( newNeighbor, new ContextSet() );

		if ( newNeighbor.isBroker() ) {

			contextTable.createViewAndUpdateTable( newNeighbor );
			contextTable.simplifyViewAndUpdateTable( newNeighbor );

			ContextSet view = contextTable.getContextToSend( newNeighbor );

			try {
				caRouter.getOverlay().send( CARouter.UPDATE_CONTEXT, view, newNeighbor );
			}
			catch ( NotConnectedException e ) {
				e.printStackTrace();
			}
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see polimi.reds.broker.overlay.NeighborDeadListener#signalNeighborDead(polimi.reds.NodeDescriptor)
	 */
	public void signalNeighborDead( NodeDescriptor deadNeighbor ) {
		signalNeighborRemoved( deadNeighbor );
	}

	private void scrivi( String s ) {
		logger.fine(s);
	}

	private String StampaNodo( NodeDescriptor n ) {
		String result = "";
		if ( n.isBroker() ) {
			result += "Broker: ";
		}
		else {
			result += "Client: ";
		}

		result += n.getID();

		return result;
	}

}
