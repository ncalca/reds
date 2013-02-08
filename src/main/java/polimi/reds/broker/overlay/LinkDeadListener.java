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
 * This interface is the primary method for being notified about the brutal break of a connection.<br>
 * Users implement the <code>LinkDeadListener</code> interface and register their listener using the 
 * <code>AddLinkDeadListener</code> method. The users should also remove their listener after they have completed using the 
 * listener.
 * @author Alessandro Monguzzi
 *
 */
interface LinkDeadListener {
	
	/**
	 * This method is called whenever a link with a neighbor is broken.
	 * @param deadNeighbor the lost neighbor
	 */
	public void signalLinkDead(NodeDescriptor deadNeighbor);
	
}
