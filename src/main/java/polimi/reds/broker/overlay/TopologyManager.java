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
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import polimi.reds.NodeDescriptor;

/**
 * It represents the component responsible of the management of the topology of
 * the brokers network.<br>
 * It stores the neighbors of the local node into a set and offers all the
 * operations needed to manage it.
 * 
 * @author Alessandro Monguzzi
 * 
 */
public interface TopologyManager extends LinkOpenedListener, LinkClosedListener, LinkDeadListener {
	/**
	 * Subject to specify a neighborhood status between two nodes.
	 */
	public static final String NEIGHBORHOOD = "neighborhood";

	/**
	 * Add a new neighbor located at <code>url</code> to the neighbor set of the
	 * local node. If the new node is already a neighbor it throws a
	 * <code>AlreadyAddedNeighborException</code>.
	 * 
	 * @param url
	 *            the url of the neighbor
	 * @throws AlreadyAddedNeighborException
	 *             when a node is already a neighbor of the local node
	 * @throws MalformedURLException
	 *             the given url does not respect the correct syntax
	 * @throws ConnectException
	 *             the connection has failed
	 * @return the <code>NodeDescriptor</code> of the node added
	 */
	public NodeDescriptor addNeighbor(String url) throws AlreadyAddedNeighborException, ConnectException,
			MalformedURLException;

	/**
	 * Remove a neighbor from the set. If the <code>removedNeighbor</code> is
	 * not present into the set, it does nothing.
	 * 
	 * @param removedNeighbor
	 *            the removed neighbor
	 */
	public void removeNeighbor(NodeDescriptor removedNeighbor);

	/**
	 * Register a new listener for the addition of new neighbors.
	 * 
	 * @param listener
	 *            the listener
	 */
	public void addNeighborAddedListener(NeighborAddedListener listener);

	/**
	 * Remove the given listener.
	 * <p>
	 * Note: the removal is based upon the <code>equals()</code> method.
	 * </p>
	 * 
	 * @param listener
	 *            the listener
	 */
	public void removeNeighborAddedListener(NeighborAddedListener listener);

	/**
	 * Register a new listener for the removal of a neighbor.
	 * 
	 * @param listener
	 *            the listener
	 */
	public void addNeighborRemovedListener(NeighborRemovedListener listener);

	/**
	 * Remove the given listener.
	 * <p>
	 * Note: the removal is based upon the <code>equals()</code> method.
	 * </p>
	 * 
	 * @param listener
	 *            the listener
	 */
	public void removeNeighborRemovedListener(NeighborRemovedListener listener);

	/**
	 * Register a new listener for the brutal failure of a neighbor.
	 * 
	 * @param listener
	 *            the listener
	 */
	public void addNeighborDeadListener(NeighborDeadListener listener);

	/**
	 * Remove the given listener.
	 * <p>
	 * Note: the removal is based upon the <code>equals()</code> method.
	 * </p>
	 * 
	 * @param listener
	 *            the listener
	 */
	public void removeNeighborDeadListener(NeighborDeadListener listener);

	/**
	 * Get the set of neighbors of the local node.
	 * 
	 * @return the set of neighbors
	 */
	public Set getNeighbors();

	/**
	 * Check whether a node is a neighbor of the local node.
	 * 
	 * @return <code>true</code> iff the node is a neighbor of the local node
	 */
	public boolean contains(NodeDescriptor neighbor);

	/**
	 * Get all the neighbors of the local node except the one specified.l
	 * 
	 * @param excludedNeighbor
	 *            the excluded neighbor
	 * @return a list of neighbors
	 */
	public List getAllNeighborsExcept(NodeDescriptor excludedNeighbor);

	/**
	 * Get the number of neighbors of the local node.
	 * 
	 * @return the size
	 */
	public int size();

	/**
	 * Get the number of brokers neighbor of the local node.
	 * 
	 * @return the number of brokers
	 */
	public int numberOfBrokers();

	/**
	 * Get the number of clients neighbor of the local node.
	 * 
	 * @return the number of clients
	 */
	public int numberOfClient();

	/**
	 * Start the topology manager.
	 * 
	 */
	public void start();

	/**
	 * Stop the topology manager.
	 * 
	 */
	public void stop();

	/**
	 * Get the <code>NodeDescriptor</code> of the local node.
	 * 
	 * @return the <code>NodeDescriptor</code>
	 */
	public NodeDescriptor getNodeDescsriptor();

	/**
	 * Return the <code>Tranport</code> associated with the given neighbor.<br>
	 * If there is no <code>Transport</code> associated with it, it returns
	 * <code>null</code>.
	 * 
	 * @param node
	 *            the given neighbor's <code>NodeDescriptor</code>
	 * @return a <code>Transport</code> or <code>null</code>
	 */
	public Transport getTransport(NodeDescriptor node);

	/**
	 * Get all the neighbors that are connected to this node using the specified
	 * <code>Transport</code>.<br>
	 * The boolean parameter select whether the neighbor not confirmed are
	 * included or not into the returned collection. DAVIDE: check this
	 * 
	 * @param t
	 *            the given <code>Transport</code>
	 * @param temp
	 *            if <code>true</code> are also returned the temp neighbors
	 * @return a <code>Collection</code> of <code>NodeDescriptor</code>
	 */
	public Collection getNeighbors(Transport t);

	/**
	 * Set the <code>GenericOverlay</code> and register itself as LinkListener
	 * for all the <code>Overlay</code>'s <code>Transport</code>.
	 * 
	 * @param o
	 *            the <code>GenericOverlay</code>
	 */
	public void setOverlay(GenericOverlay o);
}