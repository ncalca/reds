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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import polimi.reds.MessageID;
import polimi.reds.NodeDescriptor;

/**
 * This is a implementation of <code>ReplyTable</code> using an
 * <code>Hashtable</code>. This hashtable uses as key the ID of the
 * corresponding repliable message, and as value an <code>EntryReply</code>
 * object.<br>
 * 
 * Users can specify how long broker waits for the replies using the appropriate
 * setter and getter methods. If no value or a negative is specified, the broker
 * uses the <code>DEFAULT_TIMEOUT</code> value.
 * 
 * @author Alessandro Monguzzi
 */
public class HashReplyTable implements ReplyTable {
	/**
	 * Default Timeout value.
	 */
	public static final long DEFAULT_TIMEOUT = 10000000;
	private Map data;
	private Logger logger;
	private long timeout;

	/**
	 * Base constructor. It creates an empty <code>Hashtable</code>, and it
	 * starts a <code>GarbageCollector</code> thread.
	 * 
	 */
	public HashReplyTable() {
		data = Collections.synchronizedMap(new HashMap());
		logger = Logger.getLogger("polimi.reds.SimpleReplyTable");
		timeout = DEFAULT_TIMEOUT;
	}

	/**
	 * @see ReplyTable#addEntry(MessageID, NodeDescriptor, FutureInt)
	 */
	public void addEntry(MessageID repliableMessageID, NodeDescriptor senderID, FutureInt numNeighbor)
			throws MalformedException, DuplicateKeyException {
		if (repliableMessageID == null || senderID == null)
			throw new MalformedException();
		EntryReply e = (EntryReply) data.get(repliableMessageID.toString());
		if (e != null)
			throw new DuplicateKeyException();
		data.put(repliableMessageID.toString(), new EntryReply(senderID, timeout, numNeighbor));
		int numRepl = -1;
		if (numNeighbor.isDone())
			numRepl = numNeighbor.getValue();
		logger.fine("New entry for " + repliableMessageID.toString() + " coming from " + senderID.getID()
				+ " set in the table;" + " waiting for " + numRepl + " replies");
	}

	/**
	 * @see ReplyTable#getSender(MessageID)
	 */
	public NodeDescriptor getSender(MessageID repliableMessageID) {
		NodeDescriptor s = null;
		EntryReply e = (EntryReply) data.get(repliableMessageID.toString());
		if (e != null)
			s = e.senderID;
		return s;
	}

	/**
	 * @see polimi.reds.broker.routing.ReplyTable#removeExpiredEntries()
	 */
	public void removeExpiredEntries() {
		Set entries = data.entrySet();
		synchronized (data) {
			Iterator it = entries.iterator();
			while (it.hasNext()) {
				Map.Entry elem = (Map.Entry) it.next();
				if (((EntryReply) elem.getValue()).timeout < System.currentTimeMillis()) {
					it.remove();
					logger.fine("Entry removed");
				}
			}
		}
	}

	/**
	 * @see polimi.reds.broker.routing.ReplyTable#getNumberOfPendingReplies(MessageID)
	 */
	public int getNumberOfPendingReplies(MessageID repliableMessageID) throws NoEntryException {
		if (repliableMessageID != null) {
			EntryReply e = (EntryReply) data.get(repliableMessageID.toString());
			if (e != null)
				return e.expectedReplies.getValue();
			else
				throw new NoEntryException();
		}
		throw new NullPointerException();
	}

	/**
	 * @see polimi.reds.broker.routing.ReplyTable#getExpiringTime(MessageID)
	 */
	public long getExpiringTime(MessageID repliableMessageID) throws NoEntryException {
		if (repliableMessageID != null) {
			EntryReply e = (EntryReply) data.get(repliableMessageID.toString());
			if (e != null)
				return e.timeout;
			else
				throw new NoEntryException();
		}
		throw new NullPointerException();
	}

	/**
	 * @see polimi.reds.broker.routing.ReplyTable#decrementNumberOfPendingReplies(MessageID)
	 */
	public void decrementNumberOfPendingReplies(MessageID repliableMessageID) throws NullPointerException,
			IllegalStateException {
		if (repliableMessageID != null) {
			EntryReply e = (EntryReply) data.get(repliableMessageID.toString());
			if (e != null) {
				if (e.expectedReplies.isDone()) {
					e.expectedReplies.decrement();
					logger.fine("Number of expected replies decremented of one");
					if (e.expectedReplies.getValue() == 0)
						removeEntry(repliableMessageID);
				} else
					throw new IllegalStateException();
			}
		} else
			throw new NullPointerException();
	}

	/**
	 * @see polimi.reds.broker.routing.ReplyTable#removeEntry(MessageID)
	 */
	public void removeEntry(MessageID repliableMessageID) throws NullPointerException {
		if (repliableMessageID != null) {
			data.remove(repliableMessageID.toString());
			logger.fine("Entry for " + repliableMessageID + " removed");
		} else
			throw new NullPointerException();

	}

	/**
	 * @see ReplyTable#setTimeout(long)
	 */
	public void setTimeout(long timeout) {
		this.timeout = timeout;

	}

	/**
	 * @see ReplyTable#getTimeout()
	 */
	public long getTimeout() {
		return timeout;
	}

	/**
	 * @see ReplyTable#removeCompletedEntries()
	 */
	public Map removeCompletedEntries() {
		Map map = new HashMap();
		Set entries = data.entrySet();
		synchronized (data) {
			Iterator it = entries.iterator();
			while (it.hasNext()) {
				Map.Entry next = (Map.Entry) it.next();
				/*
				 * Need a check of isDone to not block in case of asynchronous
				 * behavior.
				 */
				if (((EntryReply) next.getValue()).expectedReplies.isDone()
						&& ((EntryReply) next.getValue()).expectedReplies.getValue() == 0) {
					map.put(((EntryReply) next.getValue()).senderID, ((EntryReply) next.getKey()));
					it.remove();
					logger.fine("Entry removed");
				}
			}
		}
		return map;
	}

	/**
	 * This is the data structure used to keep track of the replies. It is
	 * composed by a <code>String senderID</code> that is the
	 * <code>NeighborID</code> of the repliable message's sender, an
	 * <code>int expectedReplies</code> that is the number of neighbors which
	 * received the repliable messages and must respond yet, a
	 * <code>long timeout</code> and <code>LinkedList</code> used as queue to
	 * store replies.
	 * 
	 * @author Alessandro Monguzzi
	 */
	private class EntryReply {
		/**
		 * This is the <code>NeighborID</code> of the sender of the repliable
		 * message.
		 */
		protected NodeDescriptor senderID;

		/**
		 * This is the expiration timeout of the repliable message. It
		 * represents the maximum amount of time that the sender waits for the
		 * replies.
		 */
		long timeout;
		/**
		 * This is the number of the neighbors which have to reply yet.
		 */
		FutureInt expectedReplies;

		/**
		 * Base Constructor. If a negative timeout is given, it uses the
		 * <code>DEFAULT_TIMEOUT</code> value.
		 * 
		 * @param senderID
		 *            the ID of the sender of the repliable message.
		 * @param expectedReplies
		 *            number of the neighbors which have to reply yet.
		 * @param timeout
		 *            expiration timeout.
		 * @param future
		 *            future value of the expectedReplies.
		 */
		public EntryReply(NodeDescriptor senderID, long timeout, FutureInt expectedReplies) {
			this.senderID = senderID;
			this.expectedReplies = expectedReplies;
			if (timeout < 0)
				timeout = DEFAULT_TIMEOUT;
			this.timeout = timeout + System.currentTimeMillis();
		}
	}
}// end class HashReplyTable

