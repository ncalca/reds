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
import java.util.List;
import java.util.Set;

import polimi.reds.NodeDescriptor;
import polimi.reds.NotConnectedException;

/**
 * This class represents the network abstraction for the upper levels. It offers
 * functionalities to dialogue with the neighbors of the local node. It stores
 * informations about the set of neighbors of the local node and it can send
 * messages to them.<br>
 */
public interface Overlay {
	/**
	 * Get the <code>NodeDescriptor</code> of the local node.
	 * 
	 * @return the <code>NodeDescriptor</code>
	 */
	public NodeDescriptor getID();

	/**
	 * Send a message with a specified subject to a destination.
	 * <p>
	 * The message traffic class is determined by the message subject:
	 * <ul>
	 * <li><code>Router.PUBLISH</code> -> <code>Transport.MESSAGE_CLASS</code></li>
	 * <li><code>Router.SUBSCRIBE</code> -> <code>Transport.FILTER_CLASS</code></li>
	 * <li><code>Router.UNSUBSCRIBE</code> ->
	 * <code>Transport.FILTER_CLASS</code></li>
	 * <li><code>Router.UNSUBSCRIBEALL</code> ->
	 * <code>Transport.FILTER_CLASS</code></li>
	 * <li><code>Router.REPLY</code> -> <code>Transport.REPLY_CLASS</code></li>
	 * <li>Other -> <code>Transport.MISCELLANEOUS_CLASS</code></li>
	 * <ul>
	 * </p>
	 * <p>
	 * If the destination is not connected to the local node it throws a
	 * <code>NotConnectedException</code>.
	 * </p>
	 * 
	 * @param subject
	 *            the subject of the message
	 * @param payload
	 *            the message
	 * @param receiver
	 *            the receiper of the message
	 * @throws NotConnectedException
	 *             the local node is not connected to the specified destination
	 */
	public void send(String subject, Serializable payload, NodeDescriptor receiver) throws NotConnectedException;

	/**
	 * Send a message with a specified subject and traffic class to a
	 * destination.<br>
	 * If the destination is not connected to the local node it throws an
	 * exception.
	 * 
	 * @param subject
	 *            the subject of the message
	 * @param payload
	 *            the message
	 * @param receiver
	 *            the receiper of the message
	 * @param trafficClass
	 *            the message traffic class
	 * @throws NotConnectedException
	 *             the local node is not connected to the specified destination
	 */
	public void send(String subject, Serializable payload, NodeDescriptor receiver, String trafficClass)
			throws NotConnectedException;

	/**
	 * Register a new listener for the finding of a new neighbor.
	 * 
	 * @param listener
	 *            the new listener
	 */
	public void addNeighborAddedListener(NeighborAddedListener listener);

	/**
	 * Remove the given listener.
	 * <p>
	 * Note: the removal is based upon the <code>equals</code> method.
	 * </p>
	 * 
	 * @param listener
	 *            the listener
	 */
	public void removeNeighborAddedListener(NeighborAddedListener listener);

	/**
	 * Register a new listener for messages with a specified subject.
	 * <p>
	 * The <code>Overlay</code> stores the new <code>PacketListener</code> in
	 * order to register it to all the <code>Transports</code> that could be
	 * successively added.
	 * </p>
	 * 
	 * @param listener
	 *            the new listener
	 * @param subject
	 *            the subject the listener is interested to
	 */
	public void addPacketListener(PacketListener listener, String subject);

	/**
	 * Remove the given listener from the given subject.
	 * <p>
	 * Note: the removal is based upon the <code>equals</code> method.
	 * </p>
	 * 
	 * @param listener
	 *            the listener
	 * @param subject
	 *            the subject
	 */
	public void removePacketListener(PacketListener listener, String subject);

	/**
	 * Remove the given listener from all the subjects.
	 * <p>
	 * Note: the removal is based upon the <code>equals</code> method.
	 * </p>
	 * 
	 * @param listener
	 *            the listener
	 */
	public void removePacketListener(PacketListener listener);

	/**
	 * Register a new listener for the disconnection of a neighbor.
	 * 
	 * @param listener
	 *            the new listener
	 */
	public void addNeighborRemovedListener(NeighborRemovedListener listener);

	/**
	 * Remove the given listener.
	 * <p>
	 * Note: the removal is based upon the <code>equals</code> method.
	 * </p>
	 * 
	 * @param listener
	 *            the listener
	 */
	public void removeNeighborRemovedListener(NeighborRemovedListener listener);

	/**
	 * Register a new listener for the death of a neighbor.
	 * 
	 * @param listener
	 *            the new neighbor
	 */
	public void addNeighborDeadListener(NeighborDeadListener listener);

	/**
	 * Remove the given listener.
	 * <p>
	 * Note: the removal is based upon the <code>equals</code> method.
	 * </p>
	 * 
	 * @param listener
	 *            the listener
	 */
	public void removeNeighborDeadListener(NeighborDeadListener listener);

	/**
	 * Add a new neighbor at the specified URL. If the operation goes well it
	 * returns the identifier of the new neighbor, else it throws an Exception.
	 * 
	 * @param url
	 *            the URL of the new neighbor
	 * @throws AlreadyAddedNeighborException
	 *             the node is already a neighbor of the local node
	 * @throws MalformedURLException
	 *             the given url does not respect the correct syntax
	 * @throws ConnectException
	 *             the connection has failed
	 * @return the identifier of the new neighbor
	 */
	public NodeDescriptor addNeighbor(String url) throws AlreadyAddedNeighborException, ConnectException,
			MalformedURLException;

	/**
	 * Remove a neighbor and close the connection to it.
	 * 
	 * @param removedNeighbor
	 *            the neighbor to be removed
	 */
	public void removeNeighbor(NodeDescriptor removedNeighbor);

	/**
	 * Get all the neighbors of the local node.
	 * 
	 * @return the set of the neighbors of the local node.
	 */
	public Set getNeighbors();

	/**
	 * Get all the neighboring brokers of the local node.
	 * 
	 * @return the set of the neighbors of the local node.
	 */

	public boolean hasNeighbor(NodeDescriptor node);

	/**
	 * Get all the neighbors of the local node except the one specified
	 * 
	 * @param excludedNeighbor
	 *            the neighbor that must not be returned
	 * @return a <code>List</code> of <code>NodeDescriptor</code>
	 */
	public List getAllNeighborsExcept(NodeDescriptor excludedNeighbor);

	/**
	 * Get the number of the neighbors of the local node.
	 * 
	 * @return the number of neighbors
	 */
	public int size();

	/**
	 * Get the number of brokers that are neighbors of the local node.
	 * 
	 * @return the number of brokers
	 */
	public int numberOfBrokers();

	/**
	 * Get the number of clients that are neighbors of the local node.
	 * 
	 * @return the number of clients
	 */
	public int numberOfClients();

	/**
	 * Start the <code>Overlay</code>.<br>
	 * This means starting all the componets that form the <code>Overlay</code>
	 * itself (i.e.: all the <code>Transport</code>s and the
	 * <code>TopologyManager</code>).
	 */
	public void start();

	/**
	 * Stop the <code>Overlay</code>.<br>
	 * This means stopping all the componets that form the <code>Overlay</code>
	 * itself (i.e.: all the <code>Transport</code>s and the
	 * <code>TopologyManager</code>).
	 */
	public void stop();

	/**
	 * The URLs where the broker listens for incoming connections.<br>
	 * These urls identify the <code>Transport</code>s active into this
	 * <code>Overlay</code>. If one <code>Transport</code> is removed, its url
	 * is removed too.
	 * 
	 * @return an array of urls.
	 */
	public String[] getURLs();

	/**
	 * Add a new <code>Transport</code> to the <code>Overlay</code>.<br>
	 * Note: this method register to the given <code>Transport</code> all the
	 * listeners previously registered to the other <code>Transport</code>s,
	 * included the <code>TopologyManager</code>'s <code>LinkListener</code>s.
	 * 
	 * @param t
	 *            the new <code>Transport</code>
	 */
	public void addTransport(Transport t);

	/**
	 * Stop and remove the given <code>Transport</code>.<br>
	 * If this <code>Transport</code> has some open connections, these
	 * connections are closed before removing the <code>Transport</code>.
	 * 
	 * @param t
	 *            the <code>Transport</code> to be removed
	 */
	public void removeTransport(Transport t);
}