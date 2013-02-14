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

package polimi.reds;

import java.io.Serializable;

/**
 * This class is the basic template to create a reply message. Each reply has
 * the ID of its repliable message and a flag that specifies whether this is the
 * last reply or not.
 * 
 * @author Alessandro Monguzzi
 */
public class Reply implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3859052894659254659L;
	private MessageID repliableMessageID;
	private Message payload;
	private boolean last;

	/**
	 * A reply to a given message.
	 * 
	 * @param repliableMessageID
	 *            the id of the replied message
	 * @param last
	 *            <code>true</code> this is the last reply for that message
	 * @param payload
	 *            the payload of the reply.
	 */
	public Reply(MessageID repliableMessageID, boolean last, Message payload) {
		this.repliableMessageID = repliableMessageID;
		this.payload = payload;
		this.last = last;
	}

	/**
	 * Gets the ID of the message it replies to.
	 * 
	 * @return repliable message ID.
	 */
	public MessageID getRepliableMessageID() {
		return repliableMessageID;
	}

	/**
	 * Sets the value of the attribute <code>last</code>.<br>
	 * TRUE iff this is the last message sent for a specific repliable message
	 * from the neighbor which sends it.
	 * 
	 * @param value
	 *            Value of the attribute.
	 */
	public void setLast(boolean value) {
		last = value;
	}

	/**
	 * Gets the value of the attribute <code>last</code>.
	 * 
	 * @return TRUE iff this is the last message sent for a specific repliable
	 *         message from the neighbor which sends it.
	 */
	public boolean isLast() {
		return last;
	}

	/**
	 * Gets the payload.
	 * 
	 * @return the payload.
	 */
	public Message getPayload() {
		return payload;
	}

	/**
	 * Return a <code>String</code> representation of a <code>Reply</code>
	 * object.
	 */
	public String toString() {
		String pay = null;
		if (payload != null)
			pay = payload.toString();
		return "MessageID: " + this.repliableMessageID.toString() + " last: " + this.last + " payload: " + pay;
	}
}