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

package polimi.reds.broker.routing;

import polimi.reds.broker.overlay.NeighborDeadListener;
import polimi.reds.broker.overlay.Overlay;
import polimi.reds.broker.overlay.NeighborRemovedListener;
import polimi.reds.broker.overlay.NeighborAddedListener;

/**
 * In a REDS broker the <code>Reconfigurator</code> is the module in charge of
 * managing the dispatching network in presence of reconfigurations. In
 * particular, it forwards subscriptions/unsubscriptions as required. <br>
 * It is registered to the <code>Overlay</code> to receive notifications about
 * the neighbors of the local node.
 */
public interface Reconfigurator extends NeighborRemovedListener, NeighborAddedListener, NeighborDeadListener {

	/**
	 * Set the router.
	 * 
	 * @param r
	 */
	public void setRouter(Router r);

	/**
	 * Get the router.
	 * 
	 * @return the router
	 */
	public Router getRouter();

	/**
	 * Set the overlay.
	 * 
	 * @param o
	 */
	public void setOverlay(Overlay o);

	/**
	 * Get the overlay.
	 * 
	 * @return the overlay
	 */
	public Overlay getOverlay();

}
