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

import java.util.HashMap;

import polimi.reds.NodeDescriptor;
import polimi.reds.context.ComparisonResult;

/**
 * When a new is received, this strategy checks if it is smaller or equal to the
 * last received for that node. Is that is true the computation, the
 * semplification and the send are made later, otherwise the new view is
 * computated and semplified. if it is smaller or equal to the last send the
 * send is made later; otherwise the new view is sent immediately
 * 
 */
public class LazyViewSendingStrategy extends ViewSendingStrategy {

	private HashMap<NodeDescriptor, ContextSet> table;

	private class SenderThread extends Thread {

		@Override
		public void run() {

			while ( true ) {
				try {
					Thread.sleep( 20000 );
				}
				catch ( InterruptedException e ) {
				}

				ContextTable contextTable = caRouter.getContextTable();
				for ( NodeDescriptor destinationNode : contextTable.getNodes() ) {
					if ( destinationNode.isBroker() ) {

						createViewAndPutInTable( destinationNode );
						simplifyViewAndPutInTable( destinationNode );
						sendViewAndPutInTable( destinationNode );

						saveView( destinationNode );

					}
				}
			}

		}
	}

	public LazyViewSendingStrategy( CARouter caRouter ) {
		super( caRouter );

		this.table = new HashMap<NodeDescriptor, ContextSet>();

		SenderThread senderThread = new SenderThread();
		senderThread.start();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see polimi.reds.context.routing.ViewSenderStrategy#sendViewDueTo(polimi.reds.NodeDescriptor)
	 */
	@Override
	public void sendViewDueTo( NodeDescriptor senderNode ) {

		ContextTable contextTable = caRouter.getContextTable();

		ContextSet savedContextSet = table.get( senderNode );
		if ( savedContextSet == null ) {
			savedContextSet = new ContextSet();
		}
		ComparisonResult receivedCompareResult = contextTable.getContextReceived( senderNode ).compareTo( savedContextSet );
		if ( ( receivedCompareResult == ComparisonResult.BIGGER )
				|| ( receivedCompareResult == ComparisonResult.NOT_COMPARABLE ) ) {

			logger.finer(  "Il contesto e' BIGGER o NC e faccio partire l'algoritmo" );

			for ( NodeDescriptor destinationNode : caRouter.getContextTable().getNodes() ) {
				if ( !destinationNode.equals( senderNode ) && destinationNode.isBroker() ) {

					super.createViewAndPutInTable( destinationNode );
					ContextSet simplified = super.simplifyView( destinationNode );

					ComparisonResult compareResult = simplified.compareTo( contextTable.getContextSent( destinationNode ) );
					if ( ( compareResult == ComparisonResult.BIGGER )
							|| ( compareResult == ComparisonResult.NOT_COMPARABLE ) ) {
						super.sendViewAndPutInTable( destinationNode );
						this.saveView( destinationNode );
					}

				}
			}

		}
		else {
			logger.finer(  "Il contesto ricevuto e' piu' piccolo e non faccio niente" );
		}

	}

	private void saveView( NodeDescriptor node ) {
		ContextTable contextTable = caRouter.getContextTable();
		ContextSet contextSetToSave = contextTable.getContextReceived( node );

		table.put( node, contextSetToSave );
	}

}
