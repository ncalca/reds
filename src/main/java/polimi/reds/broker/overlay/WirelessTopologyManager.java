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

package polimi.reds.broker.overlay;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import polimi.reds.NodeDescriptor;

/**
 * A topology manager using the <code>ManetOverlayManager</code>.
 * 
 * 
 */
public class WirelessTopologyManager extends AbstractTopologyManager {

	private ManetTreeOverlayMgr manetOvrMgr = null;

	/**
	 * Create a new topology manager
	 * 
	 * @param manetTopolMgrPort
	 *            the port used by the manet topology manager
	 * 
	 */
	public WirelessTopologyManager(int manetTopolMgrPort) {
		// register to the transport for topologymanagement messages.
		super.neighborTransport = Collections.synchronizedMap(new HashMap());
		// super.tempNeighbors = Collections.synchronizedMap(new HashMap());
		super.neighborAddedListeners = Collections.synchronizedList(new LinkedList());
		super.neighborRemovedListeners = Collections.synchronizedList(new LinkedList());
		super.neighborDeadListeners = Collections.synchronizedList(new LinkedList());

		manetOvrMgr = new ManetTreeOverlayMgr(manetTopolMgrPort, this);
	}

	/**
	 * Stop the topology manager.
	 */
	public void stop() {
		manetOvrMgr.stop();
	}

	/**
	 * Start the topology manager.
	 */
	public void start() {
		manetOvrMgr.start();
	}

	/**
	 * Signal a brutal disconnection and tries a recovery of the tree.
	 */
	public void signalLinkDead(NodeDescriptor deadNeighbor) {
		// notify to the listeners
		Iterator it = neighborDeadListeners.iterator();
		while (it.hasNext())
			((NeighborDeadListener) it.next()).signalNeighborDead(deadNeighbor);
		// remove the dead neighbor from the list
		neighborTransport.remove(deadNeighbor);
		// try reconfiguration
		manetOvrMgr.signalLostNeighbor(deadNeighbor.getID());
	}

	/**
	 * Get the <code>NodeDescriptor</code>s of the neighbors of the local node.
	 * 
	 * @return a collection of <code>NodeDescriptor</code>
	 */
	protected Collection getNeighborIDs() {
		LinkedList neighbors = new LinkedList(super.neighborTransport.entrySet());
		Iterator it = neighbors.iterator();
		LinkedList neighborsSet = new LinkedList();
		while (it.hasNext()) {
			NodeDescriptor ngh = (NodeDescriptor) it.next();
			if (ngh.isBroker())
				neighborsSet.add(ngh.getID());
		}
		return neighborsSet;
	}

	/**
	 * Get the <code>NodeDescriptor</code>s of the neighbors of the local node
	 * except the one specified.
	 * 
	 * @param neighborID
	 *            the neighbor excluded
	 * @return a collection of <code>NodeDescriptor</code>
	 */
	protected Collection getNeighborIDsExcept(String neighborID) {
		LinkedList neighborsSet = new LinkedList(getNeighborIDs());
		neighborsSet.remove(neighborID);
		return neighborsSet;
	}
}
