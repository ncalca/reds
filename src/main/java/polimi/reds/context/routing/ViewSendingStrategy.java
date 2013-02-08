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
import polimi.reds.context.ComparisonResult;

/**
 * Defines the basic rules to send the views to other nodes
 * 
 */
public abstract class ViewSendingStrategy {

	protected CARouter caRouter;
	
	protected Logger logger;

	public ViewSendingStrategy( CARouter caRouter ) {
		logger = Logger.getLogger( "polimi.reds.context.ViewSendingStrategy" );
		this.caRouter = caRouter;
	}

	/**
	 * Define how to send the views when a node changes it context
	 * 
	 * @param senderNode
	 *            the sender node
	 */
	public abstract void sendViewDueTo( NodeDescriptor senderNode );

	/**
	 * Create a view and updates the context table
	 * 
	 * @param destinationNode
	 *            the node to compute the view
	 */
	protected void createViewAndPutInTable( NodeDescriptor destinationNode ) {
		caRouter.getContextTable().createViewAndUpdateTable( destinationNode );
	}

	/**
	 * Simplifies the computed view and updates the context table
	 * 
	 * @param destinationNode
	 *            the node to simplify the view
	 */
	protected void simplifyViewAndPutInTable( NodeDescriptor destinationNode ) {
		caRouter.getContextTable().simplifyViewAndUpdateTable( destinationNode );
	}

	/**
	 * Simplifies the computed view
	 * 
	 * @param destinationNode
	 *            the node to simplify the view
	 */
	protected ContextSet simplifyView( NodeDescriptor destinationNode ) {
		return caRouter.getContextTable().simplifyView( destinationNode );
	}

	/**
	 * Sends the simplified view and updates the context table
	 * 
	 * @param destinationNode
	 *            the node to sendthe view
	 */
	protected void sendViewAndPutInTable( NodeDescriptor destinationNode ) {
		ContextTable contextTable = caRouter.getContextTable();
		ContextSet view = contextTable.getContextToSend( destinationNode );
		ContextSet lastSent = contextTable.getContextSent( destinationNode );

		if ( !( view.compareTo( lastSent ).equals( ComparisonResult.EQUALS ) ) ) {

			try {
				caRouter.getOverlay().send( CARouter.UPDATE_CONTEXT, view, destinationNode );
				logger.finer( "ViewSenderStrategy.sendView: invio UPDATE CONTEXT" );
			}
			catch ( NotConnectedException e ) {
			}

			contextTable.putContextSent( destinationNode, view );
		}
		else {
			logger.finer(  "La vista non e' cambiata-- non invio niente" );
		}
	}

}
