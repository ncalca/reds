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

import polimi.reds.MessageID;
import polimi.reds.NodeDescriptor;
import polimi.reds.Reply;
import polimi.reds.broker.overlay.Overlay;


/**
 * It is the module which manages the replies. It interacts with
 * a data structure, the <code>ReplyTable</code>, into which are 
 * registered all the interesting data of the replies such as 
 * the sender ID, the expiration timeout and the number of neighbors
 * to which the repliable message was given.
 * 
 * @author Alessandro Monguzzi
 */
public interface ReplyManager {

	/**
	 * Adds a new entry in the local reply table to manage a new repliable message.<br>
	 * If <code>numExpectedReplies</code> == 0, no reply will ever arrive for this message. It will
	 * forward a fake last reply with the same <code>repliableMessageID</code>, 
	 * <code>last</code> == <code>true</code> and <code>payload</code> == <code>null</code>.
	 * @param repliableMessageID repliable message ID
	 * @param senderID ID of the neighbor which sent the repliable message
	 * @param numExpectedReplies future value of <code>numExpectedReplies</code>
	 */
	public void recordRepliableMessage(MessageID repliableMessageID, NodeDescriptor senderID, 
	    FutureInt numExpectedReplies);
	
	/**
	 * Forwards a given reply towards the sender of the corresponding repliable message.
	 * This method can be used to implement a no queueing system, all the replies are immediately sent
	 * and never stored. If the NodeDescriptor is not present, the reply is lost.
	 * 
	 * @param reply the reply to be sent.
	 */
	public void forwardReply(Reply reply);
	
	/**
	 * Set the <code>Router</code>.
	 * 
	 * @param r the given router.
	 */
	public void setRouter(Router r);
	
	/**
	 * Set the <code>ReplyTable</code>.
	 * 
	 * @param replyTable the given reply table.
	 */
	public void setReplyTable(ReplyTable replyTable);
	/**
	 * Set the <code>Overlay</code>.
	 * @param o the overlay
	 */
	public void setOverlay(Overlay o);
	/**
	 * Get the <code>Overlay</code>.
	 * @return the overlay
	 */
	public Overlay getOverlay();
	
}
