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

/**********************************************************************
 * The client interface to access the REDS event service. This is the most
 * general interface. Classes implementing this interface usually differ for the
 * transport protocol used to access the REDS dispatching network.
 **********************************************************************/
public interface DispatchingService {
	/**
	 * Opens the connection to the REDS dispatching network.
	 * 
	 * @throws java.net.ConnectException
	 *             when a connection could not be opened
	 */
	public void open() throws java.net.ConnectException;

	/**
	 * Closes the connection to the REDS dispatching network.
	 */
	public void close();

	/**
	 * Used to access the identifier assigned to this client.
	 * 
	 * @return the identifier assigned to this client.
	 */
	public NodeDescriptor getID();

	/**
	 * Get the first message available and removes it from the queue of received
	 * messages. If there are no messages available suspends the caller until a
	 * new message arrives.
	 * 
	 * @return first message available.
	 */
	public Message getNextMessage();

	/**
	 * Get the first message available and removes it from the queue of received
	 * messages. If there are no messages available suspends the caller until a
	 * new message arrives or <code>timeout</code> expires. If this event
	 * happens, it returns <code>null</code>, else, it returns the first message
	 * available.
	 * 
	 * @param timeout
	 *            how long it will wait for a message to arrive.
	 * @return first message available.
	 */
	public Message getNextMessage(long timeout);

	/**
	 * Get the first message available that matches the specified filter and
	 * removes it from the queue of received messages. If there are no messages
	 * available suspends the caller until a new message arrives.
	 * 
	 * @param f
	 *            the <code>Filter</code> used to select the messages
	 * @return the first message available that matches the filter
	 *         <code>f</code>
	 */
	public Message getNextMessage(Filter f);

	/**
	 * Check whether there are messages available.
	 * 
	 * @return <code>true</code> if there is at least one message available,
	 *         <code>false</code> otherwise.
	 */
	public boolean hasMoreMessages();

	/**
	 * Check whether there are messages available that match the specified
	 * <code>Filter</code>.
	 * 
	 * @param f
	 *            the <code>Filter</code> used to select the messages
	 * @return the first message available that matches the filter
	 *         <code>f</code>
	 */
	public boolean hasMoreMessages(Filter f);

	/**
	 * Subscribes to messages matching the given filter.
	 * 
	 * @param filter
	 *            the <code>Filter</code> used to determine the messages the
	 *            client is interested in.
	 */
	public void subscribe(Filter filter);

	/**
	 * Unsubscribes for messages matching the given filter.
	 * 
	 * @param filter
	 *            the <code>Filter</code> used to determine the messages the
	 *            client is no more interested in.
	 */
	public void unsubscribe(Filter filter);

	/**
	 * Removes all subscriptions issued so far.
	 */
	public void unsubscribeAll();

	/**
	 * Publish a new message. At the moment of publication, each message id
	 * given a new unique id using the <code>MessageID</code> class.<br>
	 * If <code>msg</code> is instance of <code>Repliable</code> it will wait
	 * for replies.
	 * 
	 * @param msg
	 *            the <code>Message</code> to publish.
	 */
	public void publish(Message msg);

	/**
	 * Can be used for checking whether the dispatching service is still opened.
	 * 
	 * @return Returns TRUE if the dispatching service is still opened.
	 */
	public boolean isOpened();

	/**
	 * Send a reply. The reply message is given a new unique
	 * <code>MessageID</code>.
	 * 
	 * @param reply
	 *            reply to send.
	 * @param repliableMessageID
	 *            ID of the message to which it replies.
	 */
	public void reply(Message reply, MessageID repliableMessageID);

	/**
	 * It returns the next reply available for the given message. If no reply is
	 * available, it suspends until a reply arrive. If the reply timeout expires
	 * while suspended it throws a <code>TimeoutException</code>.<br>
	 * If the <code>repliableMessageID</code> is not present it returns
	 * <code>null</code>. This may happen if a repliable message with the given
	 * identifier has never been sent or if all the replies have already been
	 * been got.
	 * <p>
	 * Example:<br>
	 * <br>
	 * <code>
	 * Message r = null;<br>
	 * try{<br>
	 * 	 do{<br>
	 *     r = getNextReply(MessageID);<br>
	 * 	 }while(r != null);
	 * }catch(TimeoutException e){<br>
	 *   System.out.println("Error: Some recipient did not sent their reply or some reply got lost");
	 * }<br>
	 * </code>
	 * </p>
	 * 
	 * @param repliableMessageID
	 *            ID of the corresponding repliable message.
	 * @throws TimeoutException
	 *             the timeout expires
	 * @throws NullPointerException
	 *             the ID is <code>null</code>.
	 * @return first reply if present else <code>null</code>.
	 * @see #hasMoreReplies(MessageID)
	 */
	public Message getNextReply(MessageID repliableMessageID) throws NullPointerException, TimeoutException;

	/**
	 * It returns the next reply available, else it suspends until a reply
	 * arrive.<br>
	 * Note that this method does not use any timeout, exactly like the
	 * <code>getNextMessage</code> method. It could remain suspended forever.
	 * <p>
	 * Example:<br>
	 * <br>
	 * <code>
	 * Message r = null;<br>
	 * do{<br>
	 *   r = getNextReply();<br>
	 * }while(r != null);
	 * </code>
	 * </p>
	 * 
	 * @return the first reply message that arrives
	 * 
	 */
	public Message getNextReply();

	/**
	 * Get the first reply present in the local queue.<br>
	 * If there are no reply available suspends the caller until a new reply
	 * arrives or <code>timeout</code> expires. If this event happens, it
	 * returns <code>null</code>, else, it returns the first reply available.
	 * 
	 * @param timeout
	 *            how long it will wait for a reply to arrive
	 * @return the first reply to arrive
	 */
	public Message getNextReply(long timeout);

	/**
	 * Can be used for checking whether there is a reply in the queue.
	 * 
	 * 
	 * @return TRUE iff there is a reply in the queue.
	 */
	public boolean hasMoreReplies();

	/**
	 * Can be used for checking whether there is a reply in the queue for the
	 * specific repliable message. If <code>repliableMessageID</code> is not
	 * present it returns <code>false</code>.
	 * 
	 * @param repliableMessageID
	 *            ID of the repliable message.
	 * @return TRUE iff there is a reply in the queue.
	 * @throws NullPointerException
	 *             iff the ID is null
	 */
	public boolean hasMoreReplies(MessageID repliableMessageID) throws NullPointerException;

	/**
	 * Return all the replies for a specific repliable message. It waits until
	 * all the replies arrived or timeout expires.
	 * 
	 * @param repliableMessageID
	 *            ID of the repliable message.
	 * @throws NullPointerException
	 *             the ID is <code>null</code>.
	 * @return all the replies in the queue.
	 */
	public Replies getAllReplies(MessageID repliableMessageID);
}
