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

package polimi.reds.location;

import polimi.reds.Message;
import polimi.reds.Repliable;

class LocationMessage extends Message {
	/**
   * 
   */
	private static final long serialVersionUID = -4352683673194932891L;
	protected Message payload;
	protected Zone destinationZone;
	protected Location sourceLocation;

	public LocationMessage(Message payload, Location sourceLocation, Zone destinationZone) {
		this.payload = payload;
		this.sourceLocation = sourceLocation;
		this.destinationZone = destinationZone;
	}

	public Message getPayload() {
		return payload;
	}

	public Zone getDestinationZone() {
		return destinationZone;
	}

	public String toString() {
		return "Message: " + payload.toString() + " coming from " + sourceLocation + " adressed to "
				+ destinationZone.toString();
	}

	/**
	 * Create the its own <code>MessageID</code> and the payload's. The two are
	 * equal.
	 */
	public void createID() {
		payload.createID();
		super.id = payload.getID();
	}
}

class RepliableLocationMessage extends LocationMessage implements Repliable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -355931333336860508L;

	public RepliableLocationMessage(Message payload, Location sourceLocation, Zone destinationZone) {
		super(payload, sourceLocation, destinationZone);
	}
}