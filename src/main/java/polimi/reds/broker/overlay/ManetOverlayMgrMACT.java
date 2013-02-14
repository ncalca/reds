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
 * This class represent a MACT message used for activating a path to the
 * multicast tree.
 **/
class ManetOverlayMgrMACT extends ManetOverlayMgrMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6992950503359991388L;
	// The URL possibly used for activating a path.
	private String activationURL;

	/**
	 * Creates a MACT message with the given sender Id, when the message is a
	 * group hello message, the logical sender field will be used as the group
	 * leader id.
	 * 
	 * @param senderId
	 *            the Id of the physical sender.
	 */
	public ManetOverlayMgrMACT(String senderId) {

		super(senderId, ManetOverlayMgrMessage.MANET_MACT);
	}

	/**
	 * Creates a MACT message with the given sender Id and activationURL, when
	 * the message is a group hello message, the logical sender field will be
	 * used as the group leader id.
	 * 
	 * @param senderId
	 *            the Id of the physical sender.
	 * @param activationURL
	 *            the activation URL for this path being activated.
	 */
	public ManetOverlayMgrMACT(String senderId, String activationURL) {

		super(senderId, ManetOverlayMgrMessage.MANET_MACT);
		this.activationURL = activationURL;
	}

	/**
	 * Returns the activation URL for this path.
	 * 
	 * @return the activation URL for this path.
	 */
	public String getActivationURL() {

		return activationURL;
	}
}
