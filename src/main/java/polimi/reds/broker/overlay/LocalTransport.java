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

import java.net.ConnectException;
import java.util.Iterator;
import java.util.logging.Logger;

import polimi.reds.NodeDescriptor;
import polimi.reds.NotConnectedException;

/**
 * Implements the <code>Transport</code> interface.
 * 
 * @author Montinari
 * @author Bruno
 */
public class LocalTransport extends AbstractTransport {

	/**
	 * Create a new <code>Transport</code>
	 */
	public LocalTransport() {
		logger = Logger.getLogger("polimi.reds.transport");
	}

	/**
	 * This method have no action, but if it was invoked, throws a
	 * <code>ConnectException</code>.
	 * 
	 * @throws ConnectException
	 *             the <code>LocalTransport</code> cannot open any connection
	 * @see Transport#openLink(String)
	 */
	public NodeDescriptor openLinkHelper(String url) throws ConnectException {
		ConnectException ex = new ConnectException("Error connecting to " + url);
		logger.warning("The LocalTransport is trying to open a connection to " + url);
		throw ex;
	}

	/**
	 * @see Transport#closeLink(NodeDescriptor)
	 */
	protected void closeLinkHelper(NodeDescriptor closedNeighbor) {
		Proxy p = (Proxy) proxySet.remove(closedNeighbor);
		try {
			p.sendMessage(Envelope.CLOSE, null, Transport.MISCELLANEOUS_CLASS);
		} catch (NotConnectedException ex) {
			logger.warning("The node " + closedNeighbor.toString() + " is not connected to the local node");
			ex.printStackTrace();
		}
	}

	protected void closeLinkAck(NodeDescriptor closer) {
		// TODO Auto-generated method stub
	}

	/**
	 * Confirm opening of the connection, send the local ID, connect the
	 * neighbor to the nertwork. If the connection is already opened, throw a
	 * ConnectionException.
	 * 
	 * @param neighbor
	 *            who requires the opening of a connection
	 * @throws ConnectException
	 */
	synchronized public void accept(Proxy neighbor) throws ConnectException {
		// check whether the connection is already esablished
		boolean alreadyOpened = proxySet.contains(neighbor.getID());
		if (alreadyOpened) {
			throw new ConnectException("The connection with " + neighbor.getID().toString() + " is already opened!");
		}
		// Connect the new neighbor to the network.
		proxySet.add(neighbor);
		Iterator it = linkOpenedListeners.iterator();
		while (it.hasNext()) {
			LinkOpenedListener l = (LinkOpenedListener) it.next();
			l.signalLinkOpened(neighbor.getID(), this);
		}
	}

	/**
	 * Close the broker. Before closing notify all connected LocalProxies.
	 */
	public synchronized void stop() {
		logger.fine("Stopping LocalTransport");
		running = false;
		Iterator it = proxySet.getAllProxies().iterator();
		while (it.hasNext()) {
			closeLink(((Proxy) it.next()).getID());
		}
		super.stop();
		logger.config("LocalTransport stopped");
		// clear the queue
		proxySet.clear();
	}

	/**
	 * Get the local reds URL.
	 */
	public String getURL() {
		return "reds-local";
	}
}