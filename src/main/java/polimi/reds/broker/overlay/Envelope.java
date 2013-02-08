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

import polimi.reds.Filter;
import polimi.reds.Message;
import polimi.reds.NodeDescriptor;

/*******************************************************************************
 * The "internal" message exchanged by each REDS client and the broker it is
 * attached to or among neighboring brokers.
 ******************************************************************************/
public class Envelope implements Serializable {
	private static final long serialVersionUID = -1233116801597947207L;

	/**
	 * The sender is slave in the open connection process.
	 */
	public static final String SLAVE = "slave";
	/**
	 * The connection already exists.
	 */
	public static final String ALREADY_OPENED = "alreadyOpened";

	/**
	 * From BROKER to client or BROKER to BROKER: message conferms the open
	 * request and includes the ID
	 * 
	 */
	public static final String CONFIRM_OPEN = "confirmOpen";

	/**
	 * From client to BROKER: message contains an open-connection request
	 * 
	 */
	public static final String CLIENT_OPEN = "clientOpen";

	/**
	 * From BROKER to BROKER for a open-connection request
	 * 
	 */
	public static final String DS_OPEN = "ds_open";

	/**
	 * Message contains a close-connection notification
	 * 
	 */
	public static final String CLOSE = "close";

	/**
	 * Message contains a message to publish
	 * 
	 */
	public static final String PUBLISH = "publish";

	/**
	 * Message contains a filter to subscribe
	 * 
	 */
	public static final String SUBSCRIBE = "subscribe";

	/**
	 * Message contains a filter to unsubscribe
	 * 
	 */
	public static final String UNSUBSCRIBE = "unsubscribe";

	/**
	 * Message contains a notification to unsubscribe from all subscribtions
	 * 
	 */
	public static final String UNSUBSCRIBEALL = "unsubscribeAll";

	/**
	 * Message contains a beacon
	 * 
	 */
	public static final String BEACON = "beacon";

	/**
	 * Message contains an ACK to a previously sent beacon
	 * 
	 */
	public static final String BEACON_ACK = "beaconAck";

	/**
	 * Message contains a reply to forward.
	 * 
	 */
	public static final String REPLY = "reply";

	/**
	 * Confirm a CLOSE message.
	 */
	public static final String CLOSE_ACK = "closeAck";

	/**
	 * The accepting node and the requesting node are the same node.
	 */
	public static final String SAME_NODE = "sameNode";
	

	/**
	 * Link is dead due to an excedeed timeout on one side only
	 */
	public static final String DEAD = "dead";

	// Local private variables
	private String typeOfMessage;

	private NodeDescriptor senderID;

	private Serializable payload;

	private String trafficClass = null;

	/**
	 * Base constructor.
	 * 
	 * @param typeOfMessage
	 *            the message's type
	 * @param payload
	 *            the message transported by the envelope
	 * @param the
	 *            traffic class
	 */
	public Envelope(String typeOfMessage, Serializable payload,
			String trafficClass) {
		this.typeOfMessage = typeOfMessage;
		this.payload = payload;
		this.senderID = null;
		this.trafficClass = trafficClass;
	}
	/**
	 * An empty envelope of <code>Transport.MISCELLANEOUS_CLASS</code>.
	 * 
	 * @param typeOfMessage
	 *            the message's type
	 */
	public Envelope(String typeOfMessage) {
		this(typeOfMessage, null, Transport.MISCELLANEOUS_CLASS);
	}
	/**
	 * Get the message's type.
	 * 
	 * @return the message type
	 */
	public String getTypeOfMessage() {
		return typeOfMessage;
	}
	/**
	 * The sender id.
	 * 
	 * @return a <code>NodeDescriptor</code> representing of the message id
	 */
	public NodeDescriptor getSenderID() {
		return senderID;
	}
	/**
	 * Set the sender id
	 * 
	 * @param senderID
	 *            the sender id
	 */
	public void setSenderID(NodeDescriptor senderID) {
		this.senderID = senderID;
	}
	/**
	 * Get the payload of the message.
	 * 
	 * @return the payload
	 */
	public Serializable getPayload() {
		return payload;
	}
	/**
	 * Get the payload iff the type is <code>PUBLISH</code> else
	 * <code>null</code>.
	 * 
	 * @return the payload or null.
	 */
	public Message getMessage() {
		if (typeOfMessage.equals(PUBLISH))
			return (Message) payload;
		else
			return null;
	}
	/**
	 * Get the payload iff the type is <code>SUBSCRIBE</code> or
	 * <code>UNSUBSCRIBE</code>, else <code>null</code>.
	 * 
	 * @return the payload or null
	 */
	public Filter getFilter() {
		if (typeOfMessage.equals(SUBSCRIBE)
				|| typeOfMessage.equals(UNSUBSCRIBE))
			return (Filter) payload;
		else
			return null;
	}
	/**
	 * Get the traffic class of this <code>Envelope</code>.
	 * 
	 * @return the traffic class
	 */
	public String getTrafficClass() {
		return this.trafficClass;
	}
	/**
	 * Get the <code>String</code> representation.
	 * @return a <code>String</code> representing this object
	 */
	public String toString(){
		String p = null;
		String s = null;
		if(payload != null)
			p = payload.toString();
		if (senderID != null)
			s = senderID.toString();
		return "Type: " + typeOfMessage + " payload: " + p + " sender: " + s + " traffic class: " + trafficClass;
	}
}