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

class ManetOverlayMgrGRPH extends ManetOverlayMgrMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 59404839170404018L;
	// The most recently known group sequence number at the sender of the
	// message.
	private long groupSequenceNumber;

	/**
	 * Builds a new Group Hello message.
	 * 
	 * @param senderId
	 *            the logical sender of the message, i.e. the current group
	 *            leader of a given partition.
	 * @param groupSequenceNumber
	 *            the current value of the group sequence number.
	 */
	public ManetOverlayMgrGRPH(String senderId, long groupSequenceNumber) {

		super(senderId, ManetOverlayMgrMessage.MANET_GPRH);
		this.groupSequenceNumber = groupSequenceNumber;

		setMulticast(true);
	}

	/**
	 * Return the group sequence number the message carries.
	 * 
	 * @return the value of the group sequence number contained in the message.
	 */
	public long getGroupSequenceNumber() {

		return groupSequenceNumber;
	}
}
