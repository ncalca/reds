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

import polimi.reds.NodeDescriptor;
import polimi.reds.NotConnectedException;

/**
 * This interface represents the dataLink layer of a REDS broker. Its purpose is to control connections to the neighbors of the
 * local node and to send them messages.<br>
 * Classes that implement this interface should differ one another only for the type of protocol they offer.
 *
 */
public interface Transport {
	/**
	 * <code>TCPTransport</code>
	 */
	public static final String TCP = "reds-tcp";
	/**
	 * <code>UDPTransport</code>
	 */
	public static final String UDP = "reds-udp";
	/**
	 * The character that divides the url.
	 */
	public static final String URL_SEPARATOR = ":";
	/**
	 * Traffic class for <code>Message</code>s.
	 */
	public static final String MESSAGE_CLASS = "MessageClass";
	/**
	 * Traffic class for <code>Filter</code>s
	 */
	public static final String FILTER_CLASS = "FilterClass";
	/**
	 * Traffic class for replies.
	 */
	public static final String REPLY_CLASS = "ReplyClass";
	/**
	 * Generic traffic class.
	 */
	public static final String MISCELLANEOUS_CLASS = "MiscellaneousClass";
	/**
	 * Open a connection to a node located at a specified URL. If the connection is successfully opened it returns the 
	 * remote node's <code>NodeDescriptor</code>, else it throws an Exception.
	 * @param url the url of the node
	 * @return the <code>NodeDescriptor</code> of the remote node
	 * @throws MalformedURLException when <code>url</code> is not a REDS valid URL
	 * @throws ConnectException when the connection fails and is closed
	 */
	public NodeDescriptor openLink(String url) throws MalformedURLException, ConnectException;
	/**
	 * Close the connection to the specified neighbor.
	 * @param closedNeighbor the remote node of the closed connection
	 */
	public void closeLink(NodeDescriptor closedNeighbor);	
	/**
	 * Register a new listener for the opening of a new connection.
	 * 
	 * @param listener a component interested into the opening of new links
	 */
	public void addLinkOpenedListener(LinkOpenedListener listener);
	/**
	 * Register a new listener for the closing of a connection.
	 * 
	 * @param listener a component interested into the closing of links
	 */
	public void addLinkClosedListener(LinkClosedListener listener);	
	/**
	 * Register a new listener for the brutal closing of a connection.
	 * 
	 * @param listener a component interested into the brutal closing of links
	 */
	public void addLinkDeadListener(LinkDeadListener listener);
	/**
	 * Remove the given listener.
	 * <p>
	 * Note: this operation is based on the <code>equals</code> method.
	 * </p>
	 * @param listener the listener to be removed
	 */
	public void removeLinkOpenedListener(LinkOpenedListener listener);
	/**
	 * Remove the given listener.
	 * <p>
	 * Note: this operation is based on the <code>equals</code> method.
	 * </p>
	 * @param listener the listener to be removed
	 */
	public void removeLinkClosedListener(LinkClosedListener listener);
	/**
	 * Remove the given listener.
	 * <p>
	 * Note: this operation is based on the <code>equals</code> method.
	 * </p>
	 * @param listener the listener to be removed
	 */
	public void removeLinkDeadListener(LinkDeadListener listener);
	/**
	 * Send a message with a specific subject to a node neighbor of the local node.
	 * @param subject the subject of the message
	 * @param payload the message
	 * @param receiver the receiver of the <code>payload</code>
	 * @param trafficClass the traffic class of the message to be sent
	 * @throws NotConnectedException the <code>destination</code> is unknown or is not connected to the local node
	 */
	public void send(String subject, Serializable payload, NodeDescriptor receiver, String trafficClass) throws NotConnectedException;
	/**
	 * Register a new listener for the arrival of a message.
	 * 
	 * @param listener the listener for all the messages of the given subject
	 */
	public void addPacketListener(PacketListener listener, String subject);
	/**
	 * Remove the given listenerf from all the subjects it is registered to.
	 * <p>
	 * Note: this operation is based on the <code>equals</code> method.
	 * </p>
	 * @param listener the listener to be removed
	 */
	public void removePacketListener(PacketListener listener);
	/**
	 * Remove the given listener from the given subject
	 * @param listener the listener to be removed
	 * @param subject the subject
	 */
	public void removePacketListener(PacketListener listener, String subject);
	/**
	 * Start the <code>Transport</code>
	 */
	public void start();
	/**
	 * Stop the <code>Transport</code>
	 */
	public void stop();
	
	/**
	 * Get the url of the local node.
	 * @return the url of the local node
	 */
	public String getURL();
	/**
	 * Set the node descriptor of the local node
	 * @param nodeDescr the <code>NodeDescriptor</code>
	 */
	public void setNodeDescriptor(NodeDescriptor nodeDescr);
	/**
	 * Get the <code>NodeDescriptor</code> of the local node.
	 * @return the <code>NodeDescriptor</code>
	 */
	public NodeDescriptor getNodeDescriptor();
	/**
	 * Add a new traffic class creating and starting the corresponding parsing thread.<br>
	 * If the given class already exists, it does nothing.
	 * @param name the traffic class.
	 */
	public void addTrafficClass(String name);
	/**
	 * Remove the given traffic class and stop the corresponding parsing thread.<br>
	 * If there are some messages into this traffic class, they are dropped.
	 * @param name the traffic class
	 */
	public void removeTrafficClass(String name);
}