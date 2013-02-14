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

import java.util.Map;

import polimi.reds.MessageID;
import polimi.reds.NodeDescriptor;

/**
 * This is a data structure to manage the repliable messages and theirs replies.
 * 
 * @author Alessandro Monguzzi
 */
public interface ReplyTable {
	/**
	 * Adds a new entry in the reply table to manage a new repliable message. If
	 * some of the parameters are not acceptable, (i.e. <code>null</code>) it
	 * throws a <code>MalformedException</code>
	 * 
	 * @param repliableMessageID
	 *            repliable message ID
	 * @param senderID
	 *            ID of the neighbor which sent the repliable message
	 * @param numPendingReplies
	 *            replies that must arrive yet
	 * 
	 * @throws MalformedException
	 *             some parameter is not acceptable
	 * @throws DuplicateKeyException
	 *             the repliable message id is already present
	 */
	public void addEntry(MessageID repliableMessageID, NodeDescriptor senderID, FutureInt numPendingReplies)
			throws MalformedException, DuplicateKeyException;

	/**
	 * Gets the neighbor that sent the given repliable message.
	 * 
	 * @param repliableMessageID
	 *            ID of the message.
	 * @return the neighbor's ID which sent the given message.
	 */
	public NodeDescriptor getSender(MessageID repliableMessageID);

	/**
	 * It inspects the table and removes all the entries with an expired
	 * timeout.
	 */
	public void removeExpiredEntries();

	/**
	 * Gets the number of the replies not arrived yet.<br>
	 * If <code>repliableMessageID</code> is <code>null</code>, it throws a
	 * <code>NullPointerException</code>.<br>
	 * This method checks whether the value has been set. If not, it waits for.<br>
	 * If the given <code>MessageID</code> is not present into the table it
	 * throws a <code>NoEntryException</code>.
	 * 
	 * @param repliableMessageID
	 *            the ID of the repliable message
	 * @throws NoEntryException
	 *             there is no entry for this id
	 * @return number of the waited replies.
	 */
	public int getNumberOfPendingReplies(MessageID repliableMessageID) throws NoEntryException;

	/**
	 * Gets the exact moment in which the timeout of the repliable message
	 * expires.<br>
	 * It the given <code>MessageID</code> is not present into the table it
	 * returns <code>ReplyTable.NO_ENTRY</code>.
	 * 
	 * @param repliableMessageID
	 *            the ID of the repliable message.
	 * @return the time in which the timeout expires.
	 * @throws NoEntryException
	 *             there is no entry for this id
	 */
	public long getExpiringTime(MessageID repliableMessageID) throws NoEntryException;

	/**
	 * Decrements the number of the replies already waited. This number must be
	 * positive. If after the decrementation becomes 0, it removes the entry
	 * from the table.<br>
	 * This method can be invoked only when the value has been set, else it
	 * throws an <code>IllegalStateException</code>.
	 * 
	 * @param repliableMessageID
	 *            the ID of the repliable message.
	 * @throws NoEntryException
	 *             there is no entry for this id
	 * @throws IllegalStateException
	 *             if the method is invoked before setting the value.
	 */
	public void decrementNumberOfPendingReplies(MessageID repliableMessageID) throws NullPointerException,
			IllegalStateException;

	/**
	 * Remove the entries that are waiting for zero replies and return the
	 * corresponding nodes'id that is waiting for the message.<br>
	 * The number of pending replies for these nodes is zero.
	 * 
	 * @return a <code>Map</code> of <code>NodeDescriptor</code>-
	 *         <code>MessageID</code> <br>
	 *         . If there is no entry waiting for zero replies, it returns an
	 *         empty map.
	 * 
	 */
	public Map removeCompletedEntries();

	/**
	 * Removes the entry from the table.
	 * 
	 * @param repliableMessageID
	 *            the ID of the repliable message
	 * @throws NullPointerException
	 *             the <code>repliableMessageID</code> is <code>null</code>
	 */
	public void removeEntry(MessageID repliableMessageID) throws NullPointerException;

	/**
	 * Set how long to wait for replies
	 * 
	 * @param timeout
	 *            milliseconds
	 */
	public void setTimeout(long timeout);

	/**
	 * Get the reply timeout.
	 * 
	 * @return milliseconds the reply messages have to arrive
	 */
	public long getTimeout();
}// end of RepliyTable

class NoEntryException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6621864710313370990L;

}

/**
 * This exception is thrown when a key is already present in the hashtable.
 * 
 * @author Alessandro Monguzzi
 */
class DuplicateKeyException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7807148317404030186L;

}

/**
 * This exception is thrown when some in parameters are not acceptable.
 * 
 * @author Alessandro Monguzzi
 * 
 */
class MalformedException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7792817793618728882L;

}
