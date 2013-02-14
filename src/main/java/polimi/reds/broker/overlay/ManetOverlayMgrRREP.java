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

/**
 * This class represents a RREP message.
 **/
class ManetOverlayMgrRREP extends ManetOverlayMgrMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1484963504067508462L;
	// The broadcast Id of the receiver of the message.
	private long broadcastId;
	// The URL possibly used for activating a path.
	private String activationURL;
	// The most recently known group sequence number at the sender of the
	// message.
	private long groupSequenceNumber;

	/**
	 * Build a RREP message with a given sender Id and a given activation URL.
	 * The activationURL is returned by Transport and will represents the
	 * parameter for Transport.openLink if this path will be activated.
	 * 
	 * @param senderId
	 *            the Id of the sender of the message.
	 * @param activationURL
	 *            the URL for activating this link.
	 */
	public ManetOverlayMgrRREP(String senderId, String activationURL, long groupSequenceNumber) {

		super(senderId, ManetOverlayMgrMessage.MANET_RREP);
		this.groupSequenceNumber = groupSequenceNumber;
		this.activationURL = activationURL;
		this.broadcastId = 0;
	}

	/**
	 * Set the broadcast Id for this message.
	 * 
	 * @param broadcastId
	 *            the broadcast Id for this message.
	 */
	public void setBroadcastId(long broadcastId) {

		this.broadcastId = broadcastId;
	}

	/**
	 * Return the broadcast Id for this message.
	 * 
	 * @return the broadcast Id for this message.
	 */
	public long getBroadcastId() {

		return broadcastId;
	}

	/**
	 * @param string
	 */
	public void setActivationURL(String string) {

		activationURL = string;
	}

	/**
	 * Return the activation URL for this path.
	 * 
	 * @return the activation URL for this path.
	 */
	public String getActivationURL() {

		return activationURL;
	}

	/**
	 * Return the group sequence number carried in this message.
	 * 
	 * @return the group sequence number in this message.
	 */
	public long getGroupSequenceNumber() {

		return groupSequenceNumber;
	}
}
