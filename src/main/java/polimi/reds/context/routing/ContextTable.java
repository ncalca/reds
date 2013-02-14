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

import java.util.Collection;

import polimi.reds.NodeDescriptor;

/*******************************************************************************
 * A <code>ContextTable</code> is used by the REDS broker to store information
 * about the node context it received from its neighbors.
 ******************************************************************************/
public interface ContextTable {

	/**
	 * Returns all node contained in the table
	 * 
	 * @return all node contained in the table
	 */
	public Collection<NodeDescriptor> getNodes();

	/**
	 * Updates in the table the received context for node
	 * 
	 * @param n
	 *            the node
	 * @param contextSet
	 *            the received context
	 */
	public void putContextReceived(NodeDescriptor n, ContextSet contextSet);

	/**
	 * Updates in the table the computed context for node
	 * 
	 * @param n
	 *            the node
	 * @param contextSet
	 *            the computed context
	 */
	public void putContextComputed(NodeDescriptor n, ContextSet contextSet);

	/**
	 * Updates in the table the context to send for node
	 * 
	 * @param n
	 *            the node
	 * @param contextSet
	 *            the context to send
	 */
	public void putContextToSend(NodeDescriptor n, ContextSet contextSet);

	/**
	 * Updates in the table the last sent context for node
	 * 
	 * @param n
	 *            the node
	 * @param contextSet
	 *            the last sent context
	 */
	public void putContextSent(NodeDescriptor n, ContextSet contextSet);

	/**
	 * Returns from the table the received context for node
	 * 
	 * @param n
	 *            the node
	 */
	public ContextSet getContextReceived(NodeDescriptor n);

	/**
	 * Returns from the table the computed context for node
	 * 
	 * @param n
	 *            the node
	 */
	public ContextSet getContextToSend(NodeDescriptor n);

	/**
	 * Returns from the table the context to send for node
	 * 
	 * @param n
	 *            the node
	 */
	public ContextSet getContextSent(NodeDescriptor n);

	/**
	 * Returns from the table the last sent context for node
	 * 
	 * @param n
	 *            the node
	 */
	public ContextSet getContextComputed(NodeDescriptor n);

	/**
	 * Remove a neighbor from the table
	 * 
	 * @param n
	 *            the neigbor to remove
	 */
	public void removeNeighbor(NodeDescriptor n);

	public void setContextSetSimplifier(ContextSetSimplifier contextSetSimplifier);

	public void createViewAndUpdateTable(NodeDescriptor destinationNode);

	public void simplifyViewAndUpdateTable(NodeDescriptor destinationNode);

	public ContextSet simplifyView(NodeDescriptor destinationNode);

}
