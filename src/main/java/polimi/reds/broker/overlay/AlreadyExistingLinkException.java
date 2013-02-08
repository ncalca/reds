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

import polimi.reds.NodeDescriptor;

/**
 * This exception is thrown when the <code>Transport</code> tries to open a connection to an already connected node.<br>
 * It carries the <code>NodeDescriptor</code> of the remote node.
 * 
 * @author Alessandro Monguzzi
 *
 */
public class AlreadyExistingLinkException extends Exception {

	private NodeDescriptor remoteNodeDescriptor;
	/**
	 * 
	 */
	private static final long serialVersionUID = -301860628355804005L;
	/**
	 * Create a new exception.
	 * @param n the <code>NodeDescriptor</code> of the remote node.
	 */
	public AlreadyExistingLinkException(NodeDescriptor n){
		this.remoteNodeDescriptor = n;
	}
	/**
	 * Get the <code>NodeDescripton</code> of the remote node.
	 * @return <code>NodeDescripton</code>
	 */
	public NodeDescriptor getRemoteNodeDescriptor(){
		return remoteNodeDescriptor;
	}
}
