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

import java.io.Serializable;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import polimi.reds.NodeDescriptor;
import polimi.reds.NotConnectedException;

/**
 * This class provides a skeletal implementation of the
 * <code>TopologyManager</code> interface to minimize the effort required to
 * implement this interface. Concrete implementation of this class must specify
 * the data structures they need to store listeners and neighbors.<br>
 * 
 * @author Alessandro Monguzzi
 * 
 */
public abstract class AbstractTopologyManager implements TopologyManager/*
																		 * DAVIDE:
																		 * removed
																		 * ,
																		 * PacketListener
																		 */{
	protected GenericOverlay overlay = null;
	/**
	 * <code>Map</code> containing the association <code>NodeDescriptor</code>-
	 * <code>Transport</code>, specifying for each <code>NodeDescriptor</code>
	 * which <code>Transport</code> uses.
	 */
	protected Map neighborTransport = null;
	/**
	 * The list of the components interested to the arrival of a new neighbor.
	 */
	protected List neighborAddedListeners = null;
	/**
	 * The list of the components interested to the removal of a neighbor.
	 */
	protected List neighborRemovedListeners = null;
	/**
	 * The list of the components interested to the brutal failure of a
	 * neighbor.
	 */
	protected List neighborDeadListeners = null;
	/**
	 * The <code>Map</code> of nodes, associated with their
	 * <code>Transport</code>, that are not real local node's neighbors but are
	 * connected to the local node.<br>
	 * They could become actual neighbors if the two topology managers agree.
	 */
	// DAVIDE: removed: protected Map tempNeighbors = null;
	/**
	 * The number of neighbors of the local node that are brokers.
	 */
	protected int numberOfBrokers = 0;
	/**
	 * The descriptor of the local node.
	 */
	protected NodeDescriptor localID = null;
	/**
	 * Log the events on the thread.
	 */
	protected Logger logger = null;

	/**
	 * @see TopologyManager#getNodeDescsriptor()
	 */
	public NodeDescriptor getNodeDescsriptor() {
		return localID;
	}

	/**
	 * 
	 * @see TopologyManager#addNeighbor(String)
	 */
	public NodeDescriptor addNeighbor(String url) throws AlreadyAddedNeighborException, ConnectException,
			MalformedURLException {
		NodeDescriptor id = null;
		Transport transport = overlay.resolveUrl(url);
		// try {
		id = transport.openLink(url);
		if (id != null) {
			// confirmNeighbor(id, transport);
			confirmConnection(id, transport);
			logger.finer("Added neighbor " + url);
		}
		// } catch (AlreadyExistingLinkException e) {
		// throw new AlreadyAddedNeighborException(e.getRemoteNodeDescriptor());
		// }
		return id;
	}

	/**
	 * Send a <code>NEIGHBORHOOD</code> message to the remote node.
	 * 
	 * @param id
	 *            the receiper node
	 */
	protected void confirmConnection(NodeDescriptor id, Transport transport) {
		logger.finer("Confirm the connection with " + id.getID());
		// confirm the neighbor
		try {
			transport.send(NEIGHBORHOOD, null, id, Transport.MISCELLANEOUS_CLASS);
		} catch (NotConnectedException e) {

		} catch (NullPointerException e) {
			logger.severe("NullPointerException confirming connection from " + this.localID.toString() + " to "
					+ id.toString());
		}

		confirmNeighbor(id, transport);
	}

	/**
	 * Notify to the listeners the new neighbor
	 * 
	 * @param id
	 *            the new neighbor
	 * @param transport
	 *            the <code>Transport</code> it is connected to
	 */
	protected void confirmNeighbor(NodeDescriptor id, Transport transport) {
		// nicola: remove synchronized
		// synchronized (neighborTransport) {// FIXME:check if necessary
		neighborTransport.put(id, transport);
		if (id.isBroker()) {
			numberOfBrokers++;
			logger.finer("TopologyManager: Node " + localID.getUrls()[0] + " ADDEDLINK to  neighbor " + id.getUrls()[0]
					+ " is broker " + id.isBroker());
		} else
			logger.finer("TopologyManager: Node " + localID + " ADDEDLINK to NONBROKER  neighbor " + id.getUrls()[0]
					+ " is broker " + id.isBroker());

		// }
		// notify to listeners
		Iterator it = neighborAddedListeners.iterator();
		while (it.hasNext())
			((NeighborAddedListener) it.next()).signalNeighborAdded(id);

	}

	/**
	 * @see LinkOpenedListener#signalLinkOpened(NodeDescriptor, Transport)
	 */
	public void signalLinkOpened(NodeDescriptor senderID, Transport t) {
		// nicola: removed synchronized
		// synchronized (neighborTransport) {// FIXME: may not be necessary

		if (senderID.isBroker()) {

			System.out.println("signalLinkOpened to" + senderID.getUrls()[0]);
			logger.finer("signalLinkOpened to" + senderID.getUrls()[0]);
			// add the new neighbor to the list on temp neighbors
			// DAvIDE: removed System.out.println("adding "+
			// senderID.getUrls()[0]+", "+t+ " to tempNeighbors");
			// DAVIDE: removed tempNeighbors.put(senderID, t);
			/*
			 * Here if necessary insert a dialogue with the other neighbor.
			 */
		} else {
			// a client is automatically a neighbor.
			neighborTransport.put(senderID, t);
			// notify to listeners
			Iterator it = neighborAddedListeners.iterator();
			while (it.hasNext())
				((NeighborAddedListener) it.next()).signalNeighborAdded(senderID);
		}
		// }
	}

	/**
	 * @see TopologyManager#addNeighborAddedListener(NeighborAddedListener)
	 */
	public void addNeighborAddedListener(NeighborAddedListener listener) {
		if (neighborAddedListeners == null)
			neighborAddedListeners = new LinkedList();
		neighborAddedListeners.add(listener);
	}

	/**
	 * @see TopologyManager#removeNeighborAddedListener(NeighborAddedListener)
	 */
	public void removeNeighborAddedListener(NeighborAddedListener listener) {
		if (neighborAddedListeners != null)
			neighborAddedListeners.remove(listener);
	}

	/**
	 * @see TopologyManager#addNeighborRemovedListener(NeighborRemovedListener)
	 */
	public void addNeighborRemovedListener(NeighborRemovedListener listener) {
		if (neighborRemovedListeners == null)
			neighborRemovedListeners = new LinkedList();
		neighborRemovedListeners.add(listener);
	}

	/**
	 * @see TopologyManager#removeNeighborRemovedListener(NeighborRemovedListener)
	 */
	public void removeNeighborRemovedListener(NeighborRemovedListener listener) {
		if (neighborRemovedListeners != null)
			neighborRemovedListeners.remove(listener);
	}

	/**
	 * @see TopologyManager#addNeighborDeadListener(NeighborDeadListener)
	 */
	public void addNeighborDeadListener(NeighborDeadListener listener) {
		neighborDeadListeners.add(listener);
	}

	/**
	 * @see TopologyManager#removeNeighborDeadListener(NeighborDeadListener)
	 */
	public void removeNeighborDeadListener(NeighborDeadListener listener) {
		if (neighborDeadListeners != null)
			neighborDeadListeners.remove(listener);
	}

	/**
	 * @see TopologyManager#getNeighbors()
	 */
	public Set getNeighbors() {
		synchronized (neighborTransport) {
			return new HashSet(neighborTransport.keySet());
		}
	}

	/**
	 * @see TopologyManager#contains(NodeDescriptor)
	 */
	public boolean contains(NodeDescriptor neighbor) {
		return neighborTransport.containsKey(neighbor);

	}

	/**
	 * @see TopologyManager#getAllNeighborsExcept(NodeDescriptor)
	 */
	public List getAllNeighborsExcept(NodeDescriptor excludedNeighbor) {
		List l = new ArrayList();
		Object neighs[];
		neighs = neighborTransport.keySet().toArray();

		for (int i = 0; i < neighs.length; i++) {
			NodeDescriptor d = (NodeDescriptor) neighs[i];
			if (!d.equals(excludedNeighbor))
				l.add(d);
		}

		return l;
	}

	/**
	 * @see TopologyManager#size()
	 */
	public int size() {
		return neighborTransport.size();
	}

	/**
	 * @see TopologyManager#numberOfBrokers()
	 */
	public int numberOfBrokers() {
		synchronized (neighborTransport) {
			// Possibly useless but does not hurt
			return numberOfBrokers;
		}
	}

	/**
	 * @see TopologyManager#numberOfClient()
	 */
	public int numberOfClient() {
		return size() - numberOfBrokers();
	}

	/**
	 * @see TopologyManager#start()
	 */
	public void start() {
	}

	/**
	 * @see TopologyManager#stop()
	 */
	public void stop() {
	}

	public boolean signalLinkClosing(NodeDescriptor closingNeighbor) {
		return true;
	}

	/**
	 * @see TopologyManager#removeNeighbor(NodeDescriptor)
	 */
	public void removeNeighbor(NodeDescriptor removedNeighbor) {
		logger.finer("Removing neighbor " + removedNeighbor.getID());
		// notify to the listeners
		Iterator it = neighborRemovedListeners.iterator();
		while (it.hasNext())
			((NeighborRemovedListener) it.next()).signalNeighborRemoved(removedNeighbor);
		synchronized (neighborTransport) {
			Transport transport = (Transport) neighborTransport.get(removedNeighbor);
			transport.closeLink(removedNeighbor);
			neighborTransport.remove(removedNeighbor);
			if (removedNeighbor.isBroker()) {
				numberOfBrokers--;
				logger.finer("TopologyManager: Node " + localID.getUrls()[0] + " REMOVEDLINK to neighbor "
						+ removedNeighbor.getUrls()[0] + " as a neighbor");
			}
		}
	}

	/**
	 * @see LinkClosedListener#signalLinkClosed(NodeDescriptor)
	 */
	public void signalLinkClosed(NodeDescriptor closingNeighbor) {
		logger.finer("Link closed " + closingNeighbor.toString());
		boolean containsKey;
		synchronized (neighborTransport) {
			containsKey = neighborTransport.containsKey(closingNeighbor);
		}

		if (containsKey) {
			// notify to the listeners
			Iterator it = neighborRemovedListeners.iterator();
			while (it.hasNext()) {
				NeighborRemovedListener l = (NeighborRemovedListener) it.next();
				l.signalNeighborRemoved(closingNeighbor);
			}
		}

		// remove the neighbor from the list
		/*
		 * Be careful if you remove the dead neighbor BEFORE the notifications.
		 * In this case operations will not consider the dead neighbor.
		 */
		synchronized (neighborTransport) {
			neighborTransport.remove(closingNeighbor);
		}

		if (closingNeighbor.isBroker()) {
			numberOfBrokers--;
			logger.finer("TopologyManager: Node " + localID.getUrls()[0] + " REMOVEDLINK to neighbor "
					+ closingNeighbor.getUrls()[0] + " as a neighbor");
		}

		/*
		 * DAVIDE: removed: else if(tempNeighbors.containsKey(closingNeighbor)){
		 * System.out.println("removing "+
		 * closingNeighbor.getUrls()[0]+" from tempNeighbors (CLOSE)");
		 * 
		 * tempNeighbors.remove(closingNeighbor); }
		 */

	}

	/**
	 * @see LinkDeadListener#signalLinkDead(NodeDescriptor)
	 */
	public void signalLinkDead(NodeDescriptor deadNeighbor) {
		try {
			logger.warning("Link dead " + deadNeighbor.toString());
		} catch (NullPointerException e) {
			logger.warning("Link dead");
		}
		boolean isLinkDead;
		synchronized (neighborTransport) {
			isLinkDead = neighborTransport.containsKey(deadNeighbor);

		}
		if (isLinkDead) {
			// notify to the listeners
			Iterator it = neighborDeadListeners.iterator();
			while (it.hasNext())
				((NeighborDeadListener) it.next()).signalNeighborDead(deadNeighbor);
			// remove the dead neighbor from the list
			/*
			 * Be careful if you remove the dead neighbor BEFORE the
			 * notifications. In this case operations will not consider the dead
			 * neighbor.
			 */
			synchronized (neighborTransport) {
				neighborTransport.remove(deadNeighbor);
			}

			if (deadNeighbor != null && deadNeighbor.isBroker()) {
				this.numberOfBrokers--;
				logger.finer("TopologyManager: Node " + localID.getUrls()[0] + " LOSTLINK to neighbor "
						+ deadNeighbor.getUrls()[0] + " as a neighbor");
			}
		} /* DAVIDE: removed else *//* DAVIDE Added this else block - *//*
																	 * if(
																	 * tempNeighbors
																	 * .
																	 * containsKey
																	 * (
																	 * deadNeighbor
																	 * )){
																	 * System
																	 * .out.
																	 * println (
																	 * "removing "
																	 * +
																	 * deadNeighbor
																	 * . getUrls
																	 * ( )[0]+
																	 * " from tempNeighbors (DEAD)"
																	 * );
																	 * 
																	 * tempNeighbors
																	 * . remove
																	 * (
																	 * deadNeighbor
																	 * ); }
																	 */

	}

	/**
	 * @see PacketListener#signalPacket(String, NodeDescriptor, Serializable)
	 */
	public void signalPacket(String subject, NodeDescriptor senderID, Serializable payload, Transport t) {
		try {
			logger.finer("Packet " + subject + " from " + senderID.getID() + " " + payload.toString());
		} catch (NullPointerException e) {

		}
		if (subject.equals(NEIGHBORHOOD)) {
			// the sender is a new neighbor
			// Davide Removed TempNeighbors
			// System.out.println("removing "+
			// senderID.getUrls()[0]+" from tempNeighbors (NHOOD)");

			// Transport t = (Transport)tempNeighbors.get(senderID);
			// tempNeighbors.remove(senderID);
			confirmNeighbor(senderID, t);

		}
	}

	/**
	 * Link <code>this</code> to its <code>GenericOverlay</code>.
	 * 
	 * @param o
	 *            the <code>GenericOverlay</code>
	 */
	public void setOverlay(GenericOverlay o) {
		this.overlay = o;
		Collection transports = o.getAllTransport();
		synchronized (transports) {
			Iterator it = transports.iterator();
			while (it.hasNext()) {
				final Transport next = (Transport) it.next();
				next.addLinkClosedListener(this);
				next.addLinkDeadListener(this);
				next.addLinkOpenedListener(this);
				next.addPacketListener(new PacketListener() {

					public void signalPacket(String subject, NodeDescriptor senderID, Serializable payload) {
						AbstractTopologyManager.this.signalPacket(subject, senderID, payload, next);
					}

				}, TopologyManager.NEIGHBORHOOD);
			}
		}
		this.localID = overlay.getID();
	}

	/**
	 * It search first into the set of node that is confirmed. If the given node
	 * is not present, it search into the set of the temp nodes.
	 * 
	 * @see TopologyManager#getTransport(NodeDescriptor)
	 */
	public Transport getTransport(NodeDescriptor node) {
		Transport t = null;
		t = (Transport) neighborTransport.get(node);
		/*
		 * DAVIDE: removed: if(t == null) t =
		 * (Transport)tempNeighbors.get(node);
		 */
		return t;
	}

	/**
	 * @see TopologyManager#getNeighbors(Transport, boolean)
	 */
	public Collection getNeighbors(Transport t) {
		Collection coll = null;
		synchronized (neighborTransport) {
			coll = getNeighbors(t, neighborTransport);
		}
		/*
		 * DAVIDE: removed if(temp){ coll.addAll(getNeighbors(t,
		 * tempNeighbors)); }
		 */
		return coll;
	}

	public Collection getNeighboringBrokers() {
		ArrayList coll = new ArrayList();
		Object neighs[];
		synchronized (neighborTransport) {
			neighs = neighborTransport.keySet().toArray();// It's a synch map
		}
		for (int i = 0; i < neighs.length; i++) {
			NodeDescriptor next = (NodeDescriptor) neighs[i];
			if (next.isBroker())
				coll.add(next);
		}

		return coll;
	}

	/**
	 * Get the neighbors contained into <code>data</code> that use the given
	 * <code>Transport</code>.
	 * 
	 * @param t
	 *            the <code>Transport</code>
	 * @param data
	 *            the data structure
	 * @return a <code>Collection</code> of <code>NodeDescriptor</code>
	 */
	private Collection getNeighbors(Transport t, Map data) {
		ArrayList coll = new ArrayList();
		synchronized (data) {
			Set neighs = data.keySet();
			Iterator it = neighs.iterator();
			while (it.hasNext()) {
				NodeDescriptor next = (NodeDescriptor) it.next();
				Transport tr = (Transport) data.get(next);
				if (tr == t) {
					coll.add(next);
				}
			}
		}
		return coll;
	}
}