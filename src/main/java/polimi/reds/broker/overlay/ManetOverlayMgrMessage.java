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
import java.util.ArrayList;
import java.util.Collection;

/**
 * This class represents the general message used for maintaining the overlay
 * network in MANET environments.
 * 
 **/
class ManetOverlayMgrMessage implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -8642326838091247207L;

	// The number of hops the message traversed.
	private int hopCount;
	
	// True if the message is broadcast.
	private boolean isBroadcast;
	// True if the message is multicast.
	private boolean isMulticast;

	// The id of the physical sender of the message (1 hop away).
	private String senderId;
	// The id of the logical sender of the message (multiple hops away).
	private String logicalSenderId;
	// The id of the physical receiver of the message (1 hop away).
	private String receiverId;
	// The id of the logical receiver of the message (multiple hops away).
	private String logicalReceiverId;
	// A list of the intended logical receivers if the message is multicast.
	private ArrayList receivers;
	
	// The type of the message.
	private int messageType;
	
	// The possible types of the message.
	public static final int MANET_RREQ = 0;
	public static final int MANET_RREP = 1;
	public static final int MANET_MACT = 2;
	public static final int MANET_GPRH = 3;

	/**
	 * Build a new message with a given sender id. 
	 * At object creation time the physical sender and the logical sender
	 * always concide. The message created is a unicast message, i.e.
	 * isBroadcast() and isMulticast() return false.
	 * 
	 * @param senderId the Id of the sender of the message.
	 * @param messageType the type of message.
	 */
	public ManetOverlayMgrMessage(String senderId, int messageType) {

		this.senderId = senderId;
		this.logicalSenderId = senderId;
		
		this.messageType = messageType;

		this.hopCount = 0;

		this.isBroadcast = false;
		this.isMulticast = false;
	}

	/**
	 * Increase by one the number of hops the message traversed.
	 */
	public void increaseHopCount() {

		hopCount++;
	}
	
	/**
	 * Returns the number of hops the message traversed.
	 * 
	 * @return the number of hops the message traversed.
	 */
	public int getHopCount() {

		return hopCount;
	}
	
	/**
	 * Set the hop count for this message.
	 * 
	 * @param hopCount the number of hops for this message.
	 */
	public void setHopCount(int hopCount) {

		this.hopCount = hopCount;
	}

	/**
	 * Sets the message type
	 * 
	 * @param messageType the message type, it can be MANET_ROUTE_REQUEST,
	 * MANET_ROUTE_REPLY or MANET_ROUTE_MACT.
	 */
	public void setMessageType (int messageType){
		
		this.messageType = messageType;
	}
	
	/**
	 * Return the type of message.
	 * 
	 * @return the type of this message.
	 */
	public int getMessageType(){
		
		return messageType;
	}

	/**
	 * Set the physical sender id of this message.
	 * 
	 * @param senderId the Id of the physical sender.
	 */
	public void setSenderId (String senderId){
		
		this.senderId = senderId;
	}
	
	/**
	 * Return the Id of the physical sender of the message.
	 * 
	 * @return the Id of the physical sender.
	 */
	public String getSenderId(){
		
		return senderId;
	}

	/**
	 * Set the Id of the physical receiver for this message.
	 * 
	 * @param receiverId the Id of the physical receiver.
	 */
	public void setReceiverId (String receiverId){
		
		this.receiverId = receiverId;
	}
	
	/**
	 * Return the Id of the physical receiver of the message.
	 * 
	 * @return the Id of the physical receiver.
	 */
	public String getReceiverId(){
		
		return receiverId;
	}

	/**
	 * Set a flag if this message is intended to be broadcast.
	 * 
	 * @param broadcast true if this message is broadcast.
	 */
	public void setBroadcast(boolean broadcast) {

		this.isBroadcast = broadcast;
	}
	
	/**
	 * Return true if the message is broadcast.
	 * 
	 * @return true if the message is broadcast
	 */
	public boolean isBroadcast() {

		return isBroadcast;
	}

	/**
	 * Set a flag if this message is intended to be multicast.
	 * 
	 * @param multicast true if this message is multicast.
	 */
	public void setMulticast(boolean multicast) {

		this.isMulticast = multicast;
	}
	
	/**
	 * Return true if the message is multicast.
	 * 
	 * @return true if the message is multicast.
	 */
	public boolean isMulticast() {

		return isMulticast;
	}

	/**
	 * Set the intended receivers of a multicast message.
	 * 
	 * @param rec a list of all the intended receivers.
	 */
	public void setReceiversIds(Collection rec) {

		this.receivers = new ArrayList(rec);
	}

	/**
	 * Return the inteded receivers of a multicast message.
	 * 
	 * @return the inteded receivers of a multicast message.
	 */
	public Collection getReceiversIds() {

		return receivers;
	}
	
	/**
	 * Return the logical sender of this message.
	 * 
	 * @return the logical sender of this message.
	 */
	public String getLogicalSenderId(){
		
		return logicalSenderId;
	}
	
	/**
	 * Set the logical receiver of this message.
	 * 
	 * @param logicalReceiverId the logical receiver of this message.
	 */
	public void setLogicalReceiverId(String logicalReceiverId){
		
			this.logicalReceiverId = logicalReceiverId;
	}
	
	/**
	 * Return the logical receiver of this message.
	 * 
	 * @return the logical receiver of this message.
	 */
	public String getLogicalReceiverId(){
		
		return logicalReceiverId;
	}
}
