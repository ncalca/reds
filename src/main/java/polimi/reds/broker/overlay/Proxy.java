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

import polimi.reds.NodeDescriptor;
import polimi.reds.NotConnectedException;

/**
 * This interface represents a neighbor of the local node.<br>
 * The concrete implementations of this interface are recommended to be visible
 * only to <code>Transport</code> component, i.e. using private classes.
 * 
 * @author Alessandro Monguzzi
 * 
 */
public interface Proxy {

	/**
	 * Get informations about the identity of the neighbor.
	 * 
	 * @return <code>true</code> iff the neighbor is a broker
	 */
	public boolean isBroker();

	/**
	 * Get informations about the identity of the neighbor.
	 * 
	 * @return <code>true</code> iff the neighbor is a client
	 */
	public boolean isClient();

	/**
	 * Get the <code>NodeDescriptor</code> of the neighbor.
	 * 
	 * @return
	 */
	public NodeDescriptor getID();

	/**
	 * Check whether the neighbor is connected to the local node.
	 * 
	 * @return
	 */
	public boolean isConnected();

	/**
	 * Send a message with a specific subject to the neighbor.
	 * 
	 * @param subject
	 *            the subject of the message
	 * @param payload
	 *            the message
	 * @param trafficClass
	 *            the given traffic class
	 * @throws NotConnectedException
	 */
	public void sendMessage(String subject, Serializable payload, String trafficClass) throws NotConnectedException;

	/**
	 * Disconnect the local proxy closing the connection with the remote node.
	 */
	public void disconnect();
}