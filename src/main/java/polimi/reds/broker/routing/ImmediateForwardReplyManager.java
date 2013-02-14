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
import polimi.reds.NotConnectedException;
import polimi.reds.Reply;
import polimi.reds.broker.overlay.Overlay;

import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

/**
 * This is the basic implementation of the <code>ReplyManager</code>.
 * 
 * @author Alessandro Monguzzi
 */
public class ImmediateForwardReplyManager implements ReplyManager {
	private Router router;
	private ReplyTable replyTable;
	private GarbageCollector garbageCollector;
	private Logger logger;
	private Overlay overlay = null;

	/**
	 * Base constructor.
	 */
	public ImmediateForwardReplyManager() {
		router = null;
		replyTable = null;
		logger = Logger.getLogger("polimi.reds.ImmediateForwardReplyManager");
	}

	/**
	 * @see ReplyManager#recordRepliableMessage(MessageID, NodeDescriptor,
	 *      FutureInt)
	 */
	public void recordRepliableMessage(MessageID repliableMessageID, NodeDescriptor senderID,
			FutureInt numExpectedReplies) {
		if (!numExpectedReplies.isDone() || numExpectedReplies.getValue() > 0) {
			// some reply must arrive => record in the reply table
			try {
				logger.fine("Recording message " + repliableMessageID.toString());
				replyTable.addEntry(repliableMessageID, senderID, numExpectedReplies);
			} catch (MalformedException e) {
				logger.warning("Error in recording " + repliableMessageID + ": Malformed parameters");
				// e.printStackTrace();
			} catch (DuplicateKeyException e) {
				logger.warning("Error in recording " + repliableMessageID + ": Duplicate Key");
				// e.printStackTrace();
			}
		} else {// no reply will ever arrive from this message => send a fake
				// last reply
			logger.fine("Sending an empty reply for " + repliableMessageID);
			sendReply(new Reply(repliableMessageID, true, null), senderID);
		}
	}

	/**
	 * @see ReplyManager#forwardReply(Reply)
	 */
	public void forwardReply(Reply reply) {
		// Take the right entry from the table.
		if (reply != null) {
			try {
				MessageID repMsgID = reply.getRepliableMessageID();
				boolean wasLast = reply.isLast();
				if (reply.isLast()) {
					if (replyTable.getNumberOfPendingReplies(repMsgID) > 1) {
						reply.setLast(false);// other replies must arrive yet.
					}
				}
				if (reply.getPayload() != null || reply.isLast()) {
					logger.fine("Reply " + reply + " sent");
					sendReply(reply);
				}
				/*
				 * The wasLast is useful because the numberOfPendingReplies must
				 * be decremented if the reply is marked as last at the time it
				 * is received, no matter what happens after. However, this
				 * operation must be done AFTER the sending of the message,
				 * because it can potentially delete the record with the
				 * information of the destination of the reply msg. The
				 * information wasLast record the state of the boolean last at
				 * the arrive of this reply msg.
				 */
				if (wasLast)
					replyTable.decrementNumberOfPendingReplies(repMsgID);// decrement
																			// the
																			// number
																			// of
																			// replies
																			// expected.
			} catch (NullPointerException e) {
				logger.severe("MessageID is null");
			} catch (NoEntryException e) {
				/*
				 * if this exception is thrown, it means that a reply is arrived
				 * after the expiration of the timeout and its entry in the
				 * reply table no longer exists. => it's not possible to know
				 * who waited for this reply and so it is discarded.
				 */
				logger.warning("Reply" + reply.toString()
						+ " arrived after the expiration of the timeout and is discarded.");
			}
		}
	}

	/**
	 * @see ReplyManager#setOverlay(Overlay)
	 */
	public void setOverlay(Overlay o) {
		overlay = o;
	}

	/**
	 * @see ReplyManager#getOverlay()
	 */
	public Overlay getOverlay() {
		return overlay;
	}

	/**
	 * Finds the NodeDescriptor neighbor and forwards to it the reply. If it
	 * does not find the senderID, it does nothing.
	 * 
	 * @param reply
	 *            the reply to be sent.
	 */
	private void sendReply(Reply reply) {
		// finds the neighbor to which it has to send the reply.
		NodeDescriptor senderID = replyTable.getSender(reply.getRepliableMessageID());
		sendReply(reply, senderID);
	}

	/**
	 * Sends the reply to its NodeDescriptor. If the sender of the repliable
	 * message is unknown, it does nothing.
	 * 
	 * @param reply
	 *            the reply to be sent.
	 * @param senderID
	 *            the ID of the sender of the repliable message.
	 */
	private void sendReply(Reply reply, NodeDescriptor senderID) {
		if (senderID != null) {
			// sends the reply
			logger.fine("Reply " + reply + " sent to " + senderID);
			try {
				overlay.send(Router.REPLY, reply, senderID);
			} catch (NotConnectedException e) {
				logger.severe("Node " + senderID.getID() + " not connected");
			}
		}
	}

	/**
	 * @see ReplyManager#setRouter(Router)
	 */
	public void setRouter(Router router) {
		if (this.router == router)
			return;
		this.router = router;
	}

	/**
	 * @see ReplyManager#setReplyTable(ReplyTable)
	 */
	public void setReplyTable(ReplyTable replyTable) {
		if (this.replyTable == replyTable)
			return;
		this.replyTable = replyTable;
		garbageCollector = new GarbageCollector(replyTable);
		garbageCollector.setName("ImmediateForwardReplyManager.garbageCollector");
		garbageCollector.start();
	}

	/**
	 * Stops the garbage collector thread.
	 */
	public void stop() {
		garbageCollector.exit();
	}

	/**
	 * This thread can be used to manage the aging of a table. Every
	 * <code>SLEEP_INTERVAL</code> it invokes the method
	 * <code>ReplyTable.removeExpiredEntries()</code>.
	 * 
	 * @author Alessandro Monguzzi
	 */
	private class GarbageCollector extends Thread {
		private ReplyTable replyTable;
		private static final long SLEEP_INTERVAL = 5000;
		private boolean exit = false;

		public GarbageCollector(ReplyTable replyTable) {
			this.replyTable = replyTable;
		}

		public void run() {
			while (!exit) {
				/*
				 * Check into the replyTable whether are present entries that
				 * are waiting for zero replies and remove them. This case can
				 * happen when the adopted RoutingPolicy is asynchronous. It is
				 * necessary to send to them a fake reply.
				 */
				Map fakeNeighbors = replyTable.removeCompletedEntries();
				Iterator it = fakeNeighbors.entrySet().iterator();
				while (it.hasNext()) {
					Map.Entry next = (Map.Entry) it.next();
					logger.fine("Sending an empty reply for " + next);
					sendReply(new Reply((MessageID) next.getValue(), true, null), (NodeDescriptor) next.getKey());
				}
				replyTable.removeExpiredEntries();
				try {
					sleep(SLEEP_INTERVAL);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		/**
		 * Causes the thread to stop and exit.
		 */
		public void exit() {
			this.exit = true;
		}
	}// end class GarbageCollector

}
