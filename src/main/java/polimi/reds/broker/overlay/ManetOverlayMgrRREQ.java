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
* This class implements a RREQ message for multicast operations.
**/
class ManetOverlayMgrRREQ extends ManetOverlayMgrMessage {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -7985091217684288591L;
	// Is true if this RREQ message is used to reconnect two partitions of the tree.
	private boolean reconnectFlag;
	// The broadcast Id of the sender of the message.
	private long broadcastId;
	// The most recently known group sequence number at the sender of the message.
	private long groupSequenceNumber;
	// A flag used for forwarding the RREQ message among members of the overlay.
	private boolean overlayLimited;

	/**
	 * Build a RREQ Message with a given sender Id, broadcast Id and groupSequenceNumber.
	 * 
	 * @param senderId the Id of the sender of the message.
	 * @param broadcastId the broadcast Id of the sender.
	 * @param groupSequenceNumber the most recently known group sequence number.
	 */
	public ManetOverlayMgrRREQ(String senderId, long broadcastId, long groupSequenceNumber){
	
		super(senderId,ManetOverlayMgrMessage.MANET_RREQ);
		this.broadcastId=broadcastId;
		this.reconnectFlag=false;
		this.overlayLimited = false;
		this.groupSequenceNumber = groupSequenceNumber;
	}
	
	/**
	 * Set the reconnect flag for this message
	 * 
	 * @param reconnectFlag the reconnect flag for this message.
	 */
	public void setReconnectFlag(boolean reconnectFlag){
	
		this.reconnectFlag=reconnectFlag;
	}
	
	/**
	 * Return the reconnect flag for this message.
	 * 
	 * @return the reconnect flag for this message.
	 */
	public boolean getReconnectFlag(){
	
		return reconnectFlag;
	}
	
	/**
	 * Set the broadcast Id for this message.
	 * 
	 * @param broadcastId the broadcast Id for this message.
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
	 * Return the group sequence number carried in this message.
	 * 
	 * @return the group sequence number in this message.
	 */
	public long getGroupSequenceNumber(){
		
		return groupSequenceNumber;
	}
	
	/**
	 * Set the group sequence number in this message.
	 * 
	 * @param groupSequenceNumber the group sequence number in this message.
	 */
	public void setGroupSequenceNumber(long groupSequenceNumber){
		
		this.groupSequenceNumber = groupSequenceNumber;
	}
	/**
	 * Returns the current status of the OverlayLimited flag in this message.
	 * 
	 * @return TRUE if OverlayLimited is set, FALSE otherwise.
	 */
	public boolean isOverlayLimited() {
		return overlayLimited;
	}

	/**
	 * Set the current status of the OverlayLimited flag in this message.
	 * 
	 * @param b TRUE for setting the the OverlayLimited flag in the message.
	 */
	public void setOverlayLimited(boolean b) {
		overlayLimited = b;
	}

}
