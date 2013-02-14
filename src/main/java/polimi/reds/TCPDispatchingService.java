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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import polimi.reds.broker.overlay.Envelope;
import polimi.reds.broker.overlay.REDSMarshaller;
import polimi.reds.broker.overlay.REDSUnmarshaller;
import polimi.reds.broker.overlay.TCPEnvelope;
import polimi.reds.broker.overlay.Transport;

/*******************************************************************************
 * The client interface to access the REDS dispatching service through TCP
 * sockets.
 ******************************************************************************/
public class TCPDispatchingService implements DispatchingService, Runnable {
	// FIXME: use the java logging api and remove all System.out/err
	/**
	 * The IP address of the host which runs the broker this client is joined.
	 */
	protected String host;
	/**
	 * The TCP port used to communicate with the broker this client is joined.
	 */
	protected int port;
	/**
	 * The TCP socket used to communicate with the broker this client is joined.
	 */
	protected Socket sock;
	/**
	 * <code>true</code> if the connection with the broker this client is
	 * joined.
	 */
	protected boolean opened;
	/**
	 * The output stream used to communicate with the broker this client is
	 * joined.
	 */
	protected REDSMarshaller marshaller;
	/**
	 * The input stream used to communicate with the broker this client is
	 * joined.
	 */
	protected REDSUnmarshaller unmarshaller;
	/**
	 * The identifier of this client (more specifically the identifier of this
	 * specific connection with the REDS dispatching network).
	 */
	protected NodeDescriptor id;
	/** The list of messages received but not yet processed by this client. */
	protected LinkedList messages;
	/**
	 * The thread which manage this connection with the REDS dispatching
	 * network.
	 */
	protected Thread clientThread;
	/**
	 * The queue that mantains all the replies received until the client reads
	 * them.
	 */
	protected Map replyQueue;
	/**
	 * It stores the expiring timeouts of the repliable messages sent but not
	 * already replied.
	 */
	protected Map timeouts;
	private final long DEFAULT_TIMEOUT = 10000;
	/**
	 * It manages the <code>queue</code> removing all the entries with the
	 * <code>timeout</code> expired.
	 */
	private GarbageCollector garbageCollector;
	/**
	 * This is the daemon that runs the garbage collection.
	 */
	private Thread garbageCollectorThread = null;

	/**
	 * Builds a new <code>TCPDispatchingService</code> to join a broker running
	 * on the specified host and waiting TCP connections at the specified port.
	 * 
	 * @param host
	 *            The IP address of the host which runs the broker to join.
	 * @param port
	 *            The TCP port to join.
	 */
	public TCPDispatchingService(String host, int port) {
		replyQueue = Collections.synchronizedMap(new Hashtable());
		timeouts = Collections.synchronizedMap(new Hashtable());
		this.id = new NodeDescriptor();
		garbageCollector = new GarbageCollector(replyQueue, timeouts);
		messages = new LinkedList();
		opened = false;
		this.host = host;
		this.port = port;
		// setJoinAddress(host, port);
	}

	/**
	 * Sets the TCP address (host and port) of the broker to join.
	 * 
	 * @param host
	 *            The IP address of the host that runs the broker to join.
	 * @param port
	 *            The TCP port to join.
	 */
	public void setJoinAddress(String host, int port) {
		this.host = host;
		this.port = port;
	}

	/**
	 * The main loop to read messages coming from the broker and store them in
	 * the local buffer. #see #messages
	 */
	public void run() {
		TCPEnvelope msg;
		try {
			sock.setSoTimeout(5000);
		} catch (SocketException ex) {
			ex.printStackTrace();
		}
		long count = 0;
		while (opened) {
			// wait for a new message
			msg = null;
			try {
				msg = (TCPEnvelope) unmarshaller.readObject();
			} catch (SocketTimeoutException ex) {
				// System.out.println("Timeout expired!");
				continue;
			} catch (java.io.EOFException ex) {
				if (opened) {
					System.out.println("The broker brutally closed this connection!");
					ex.printStackTrace();
				}
				break;
			} catch (Exception ex) {
				if (opened) {
					System.out.println("Unable to read message!");
					ex.printStackTrace();
				}
				break;
			}
			if (msg.getTypeOfMessage().equals(TCPEnvelope.CLOSE_ACK)) {
				opened = false;
			} else if (msg.getTypeOfMessage().equals(TCPEnvelope.CLOSE)) {
				TCPEnvelope response = new TCPEnvelope(TCPEnvelope.CLOSE_ACK);
				try {
					marshaller.writeObject(response);
					marshaller.flush();
					marshaller.reset();
				} catch (Exception e) {
					System.err.println("Error closing the connection");
					e.printStackTrace();
				}
				// The broker has been disconnected
				System.out.println("The broker gently closed this connection.");
				opened = false;
			} else if (msg.getTypeOfMessage().equals(TCPEnvelope.REPLY)) {
				try {
					synchronized (replyQueue) {
						LinkedList e = (LinkedList) replyQueue.get(((Reply) msg.getPayload()).getRepliableMessageID()
								.toString());
						// synchronized(e) {
						// insert the reply into the queue.
						e.addLast(((Reply) msg.getPayload()));
						// e.notifyAll();//notifies the threads waiting on the
						// corresponding
						// getNextMessage(MessageID)
						// }
						replyQueue.notifyAll();// notifies the threads waiting
												// on the
												// getNextMessage() method
					}
				} catch (NullPointerException e) {
					// if this exception is thrown, it means that the timeout
					// has expired
					// and so the reply must be discarded.
				}
			} else
				// Store the received message in the local buffer.
				synchronized (messages) {
					messages.addLast(msg.getMessage());
					messages.notifyAll();
				}
		} // end while
			// Close the streams and the socket
		try {
			marshaller.close();
			unmarshaller.close();
			sock.close();
			System.out.println("Connection closed.");
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error in closing socket and stream.");
		}
	}

	/**
	 * Opens the connection to the REDS dispatching network. If the connection
	 * is already opened this method has no effect.
	 * 
	 * @throws java.net.ConnectException
	 *             when a connection could not be opened
	 */
	public void open() throws ConnectException {
		if (opened)
			return;
		// Try to open the socket and the input and output streams to the broker
		// at
		// host:port
		try {
			sock = new Socket(host, port);
			marshaller = new REDSMarshaller(new BufferedOutputStream(sock.getOutputStream()));
			unmarshaller = new REDSUnmarshaller(new BufferedInputStream(sock.getInputStream()));
		} catch (IOException e) {
			ConnectException ex = new ConnectException("Error opening the connection with " + host + ":" + port);
			ex.initCause(e);
			throw ex;
		}
		// Send the CLIENT_OPEN message, including the local id
		TCPEnvelope openMessage = new TCPEnvelope(TCPEnvelope.CLIENT_OPEN);
		openMessage.setSenderID(id);
		try {
			marshaller.writeObject(openMessage);
			marshaller.flush();
			marshaller.reset();
		} catch (Exception e) {
			ConnectException ex = new ConnectException("Error sending the client_open message to " + host + ":" + port);
			ex.initCause(e);
			throw ex;
		}
		// Wait for the confirmation from the BROKER
		Envelope confirmMessage = null;
		try {
			confirmMessage = (Envelope) unmarshaller.readObject();
		} catch (Exception e) {
			ConnectException ex = new ConnectException("Error receiving confirm_open from " + host + ":" + port);
			ex.initCause(e);
			throw ex;
		}
		// Check the answer from the BROKER
		if (!confirmMessage.getTypeOfMessage().equals(TCPEnvelope.CONFIRM_OPEN)) {
			throw new ConnectException("Was expecting a packet starting with " + TCPEnvelope.CONFIRM_OPEN
					+ ", received a packet starting with " + confirmMessage.getTypeOfMessage());
		}
		// The connection is open
		opened = true;
		// Starts the client's thread
		clientThread = new Thread(this);
		clientThread.setDaemon(true);
		clientThread.setName("TCPDispatchingService.clientThread");
		clientThread.start();
		garbageCollectorThread = new Thread(garbageCollector);
		garbageCollectorThread.setDaemon(true);
		garbageCollectorThread.setName("TCPDispatchingService.GarbageCollector");
		garbageCollectorThread.start();
	}

	/**
	 * Closes the connection to the REDS dispatching network.
	 */
	public void close() {
		// If the connection is not open, this function ends
		if (!opened)
			return;
		// Send a close-notification to the broker
		TCPEnvelope close = new TCPEnvelope(TCPEnvelope.CLOSE);
		try {
			marshaller.writeObject(close);
			marshaller.flush();
			marshaller.reset();
		} catch (Exception e) {
			System.err.println("Error closing the connection");
			e.printStackTrace();
		}
		// opened = false;
		// wait for the clientThread to exit (after closing the streams)
		try {
			garbageCollector.exit();
			garbageCollectorThread.join();
			clientThread.join();
		} catch (InterruptedException ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Returns the identifier assigned to this client.
	 * 
	 * @return The identifier assigned to this client.
	 */
	public NodeDescriptor getID() {
		return id;
	}

	/**
	 * Forwards the specified message to dispatching service. If the connection
	 * to the dispatching service is not opened the operation has no effect.
	 * 
	 * @param msg
	 *            The message to be sent.
	 * @param subject
	 *            the subject of the message
	 */
	protected synchronized void forward(String subject, Serializable msg) {
		if (!opened)
			return;
		TCPEnvelope fw = new TCPEnvelope(subject, msg, Transport.MISCELLANEOUS_CLASS);
		try {
			marshaller.writeObject(fw);
			marshaller.flush();
			marshaller.reset();
		} catch (Exception e) {
			System.err.println("Error while forwarding the internal message!");
			e.printStackTrace();
		}
	}

	/**
	 * Publish a new message. If the connection with the broker is not opened
	 * this method has no effect. If the message is a
	 * <code>RepliableMessage</code> it creates a new entry in the
	 * <code>queue</code>.<br>
	 * Before publication each message is given a new unique
	 * <code>MessageID</code>.
	 * 
	 * @param msg
	 *            the <code>Message</code> to publish.
	 */
	public synchronized void publish(Message msg) {
		// If opened is FALSE, the connection to BROKER does not exist and this
		// function ends
		if (!opened)
			return;
		// create a new ID for the message
		msg.createID();
		TCPEnvelope publishMsg;
		// if Repliable set a new entry in the reply queue.
		if (msg instanceof Repliable) {
			synchronized (replyQueue) {
				try {
					// create a new entry in the queue of the replies.
					replyQueue.put(msg.getID().toString(), new LinkedList());
					// create a new entry in the list of the timeouts.
					synchronized (timeouts) {
						timeouts.put(msg.getID().toString(), new Long(System.currentTimeMillis() + DEFAULT_TIMEOUT));
					}
				} catch (NullPointerException e) {
					e.printStackTrace();
				}
			}
		}
		// Create a "publish-type message": it contains the message 'msg'
		publishMsg = new TCPEnvelope(TCPEnvelope.PUBLISH, msg, Transport.MESSAGE_CLASS);
		// Send the message to the BROKER
		try {
			marshaller.writeObject(publishMsg);
			marshaller.flush();
			marshaller.reset();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Subscribes to messages matching the given filter. If the connection with
	 * the broker is not opened this method has no effect.
	 * 
	 * @param filter
	 *            The <code>Filter</code> used to determine the messages this
	 *            client is interested in.
	 */
	public synchronized void subscribe(Filter filter) {
		// If opened is FALSE, the connection to BROKER does not exist and this
		// function ends
		if (!opened)
			return;
		// Create a "system-message": it is a SUBSCRIBE-type message and
		// contains a
		// filter;
		// the sender is the client (id)
		TCPEnvelope subscribeMsg = new TCPEnvelope(TCPEnvelope.SUBSCRIBE, filter, Transport.FILTER_CLASS);
		// Send the message to the BROKER
		try {
			marshaller.writeObject(subscribeMsg);
			marshaller.flush();
			marshaller.reset();
		} catch (Exception e) {
			System.err.println("Error while subscribing");
			e.printStackTrace();
		}
	}

	/**
	 * Unsubscribes for messages matching the given filter. If the connection
	 * with the broker is not opened this method has no effect.
	 * 
	 * @param filter
	 *            the <code>Filter</code> used to determine the messages this
	 *            client is no more interested in.
	 */
	public synchronized void unsubscribe(Filter filter) {
		// If opened is FALSE, the connection to BROKER does not exist and this
		// function ends
		if (!opened)
			return;
		// Create a "system-message": it is a UNSUBSCRIBE-type message and
		// contains
		// a filter; the
		// sender is the client (id)
		TCPEnvelope unsubscribeMsg = new TCPEnvelope(TCPEnvelope.UNSUBSCRIBE, filter, Transport.FILTER_CLASS);
		// Send the message to the BROKER
		try {
			marshaller.writeObject(unsubscribeMsg);
			marshaller.flush();
			marshaller.reset();
		} catch (Exception e) {
			System.err.println("Error while unsubscribing");
			e.printStackTrace();
		}
	}

	/**
	 * Removes all subscriptions issued so far. If the connection with the
	 * broker is not opened this method has no effect.
	 */
	public synchronized void unsubscribeAll() {
		// If opened is FALSE, the connection to BROKER does not exist and this
		// function ends
		if (!opened)
			return;
		// Create a "system-message": it is an UNSUBSCRIBEALL-type message and
		// contains a filter;
		// the sender is the client (id)
		TCPEnvelope unsubscribeAllMsg = new TCPEnvelope(TCPEnvelope.UNSUBSCRIBEALL, null, Transport.FILTER_CLASS);
		// Send the message to the BROKER
		try {
			marshaller.writeObject(unsubscribeAllMsg);
			marshaller.flush();
			marshaller.reset();
		} catch (Exception e) {
			System.err.println("Error while executing the unsubscribeAll");
			e.printStackTrace();
		}
	}

	/**
	 * Get first message available and removes it from the queue of received
	 * messages. If there are no messages available suspends the caller until a
	 * new message arrives.
	 * 
	 * @return first message available.
	 */
	public Message getNextMessage() {
		Message m = null;
		synchronized (messages) {
			try {
				while (messages.isEmpty())
					messages.wait();
				m = (Message) messages.removeFirst();
			} catch (Exception e) {
				System.err.println("Error while getting first available message");
				e.printStackTrace();
			}
		}
		return m;
	}

	/**
	 * Get first message available and removes it from the queue of received
	 * messages. If there are no messages available suspends the caller until a
	 * new message arrives or <code>timeout</code> expires. If this event
	 * happens, it returns <code>null</code>, else, it returns the first message
	 * available.
	 * 
	 * @param timeout
	 *            how long it will wait for a message to arrive.
	 * @return first message available.
	 */
	public Message getNextMessage(long timeout) {
		Message m = null;
		synchronized (messages) {
			try {
				if (messages.isEmpty())
					messages.wait(timeout);
				if (!messages.isEmpty())
					m = (Message) messages.removeFirst();
			} catch (Exception e) {
				System.err.println("Error while getting first available message");
				e.printStackTrace();
			}
		}
		return m;
	}

	/**
	 * @see DispatchingService#getNextMessage(Filter)
	 */
	public Message getNextMessage(Filter f) {
		Message m = null;
		synchronized (messages) {
			try {
				boolean match = false;
				while (!match) {
					Iterator it = messages.iterator();
					while (it.hasNext() && !match) {
						m = (Message) it.next();
						match = f.matches(m);
					}
					if (match) {
						messages.remove(m);
						// messages.notifyAll();
						return m;
					}
					messages.wait();
				}
			} catch (Exception e) {
				System.err.println("Error while getting first available message matching the specified filter");
				e.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * Check if there are messages available.
	 * 
	 * @return <code>true</code> if there is at least one message available,
	 *         <code>false</code> otherwise.
	 */
	public boolean hasMoreMessages() {
		return !messages.isEmpty();
	}

	/**
	 * @see DispatchingService#hasMoreMessages(Filter)
	 */
	public boolean hasMoreMessages(Filter f) {
		boolean match = false;
		synchronized (messages) {
			Iterator it = messages.iterator();
			while (it.hasNext() && !match) {
				match = f.matches((Message) it.next());
			}
		}
		return match;
	}

	/**
	 * @see polimi.reds.DispatchingService#isOpened()
	 */
	public boolean isOpened() {
		return opened;
	}

	/**
	 * @see DispatchingService#reply(Message, MessageID)
	 */
	public synchronized void reply(Message reply, MessageID repliableMessageID) {
		// If opened is FALSE, the connection to BROKER does not exist and this
		// function ends
		if (!opened)
			return;
		// create a new ID for the reply message
		reply.createID();
		// Create a "reply-type message": it contains the message 'reply'
		TCPEnvelope replyMsg = new TCPEnvelope(TCPEnvelope.REPLY, new Reply(repliableMessageID, true, reply),
				Transport.REPLY_CLASS);
		// Send the message to the BROKER
		try {
			marshaller.writeObject(replyMsg);
			marshaller.flush();
			marshaller.reset();
		} catch (Exception e) {
			// System.err.println("Error while replying");
			e.printStackTrace();
		}
	}

	/**
	 * @see DispatchingService#getNextReply()
	 */
	public Message getNextReply() {
		Message r = null;
		synchronized (replyQueue) {
			try {
				while (!hasMoreReplies())
					replyQueue.wait();
				/*
				 * If it comes here it means that at least one reply is arrived
				 * => r cannot remain null at the end of the while.
				 */
				Set rep = replyQueue.entrySet();
				Iterator repIterator = rep.iterator();
				boolean found = false;
				while (repIterator.hasNext() && !found) {
					LinkedList l = (LinkedList) ((Map.Entry) repIterator.next()).getValue();
					if (l.size() > 0) {
						Reply reply = (Reply) l.removeFirst();
						if (reply.isLast()) {// all replies arrived, => remove
												// the entry
							replyQueue.remove(reply.getRepliableMessageID());
							synchronized (timeouts) {
								timeouts.remove(reply.getRepliableMessageID());
							}
						}
						found = true;
						r = reply.getPayload();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return r;
	}

	/**
	 * @see DispatchingService#getNextReply(long)
	 */
	public Message getNextReply(long timeout) {
		Message r = null;
		synchronized (replyQueue) {
			try {
				/*
				 * Here we use ad if instead of a while because we have to wait
				 * only for one timeout; if after the given timeout the queue is
				 * empty again, it returns null. The synchronized statement
				 * guarantees that only one thread can test the second
				 * hasMoreReplies.
				 */
				if (!hasMoreReplies())
					replyQueue.wait(timeout);
				/*
				 * The timeout means that the replyQueue could be empty when the
				 * thread wakes up. => It is necessary to recheck the queue
				 * before trying to extract a reply. If two or more threads are
				 * waiting, only one wakes up and can get a positive value from
				 * the hasMoreReplies, the other ones are blocked by the
				 * synchronized statement.
				 */
				if (hasMoreReplies()) {
					Set rep = replyQueue.entrySet();
					Iterator repIterator = rep.iterator();
					boolean found = false;
					while (repIterator.hasNext() && !found) {
						LinkedList l = (LinkedList) ((Map.Entry) repIterator.next()).getValue();
						if (l.size() > 0) {
							Reply reply = (Reply) l.removeFirst();
							if (reply.isLast()) {// all replies arrived, =>
													// remove the entry
								replyQueue.remove(reply.getRepliableMessageID());
								synchronized (timeouts) {
									timeouts.remove(reply.getRepliableMessageID());
								}
							}
							found = true;
							r = reply.getPayload();
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return r;
	}

	/**
	 * @see DispatchingService#getNextReply(MessageID)
	 */
	public Message getNextReply(MessageID repliableMessageID) throws NullPointerException, TimeoutException {
		Reply r = null;
		synchronized (replyQueue) {
			LinkedList rep = (LinkedList) replyQueue.get(repliableMessageID.toString());
			if (rep == null)
				return null;
			// synchronized(rep) {
			while (rep.isEmpty()) {
				try {
					replyQueue.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				// }
			}
			try {
				r = (Reply) rep.removeFirst();
			} catch (NoSuchElementException e) {
				/*
				 * If this exception is thrown, the timeout is expired and no
				 * replies arrived. It is possible to remove the entry.
				 */
				replyQueue.remove(repliableMessageID.toString());
				throw new TimeoutException();
			}
			if (r.isLast()) {// all replies arrived, => remove the entry
				replyQueue.remove(r.getRepliableMessageID());
				synchronized (timeouts) {
					timeouts.remove(r.getRepliableMessageID());
				}
			}
			return r.getPayload();
		}
	}

	/**
	 * @see DispatchingService#hasMoreReplies()
	 */
	public boolean hasMoreReplies() {
		Set rep = replyQueue.keySet();
		boolean found = false;
		synchronized (replyQueue) {
			Iterator repIt = rep.iterator();
			while (repIt.hasNext() && !found) {
				LinkedList l = (LinkedList) replyQueue.get(repIt.next());
				if (l.size() > 0)
					return true;
			}
		}
		return false;
	}

	/**
	 * @see DispatchingService#hasMoreReplies(MessageID)
	 */
	public boolean hasMoreReplies(MessageID repliableMessageID) throws NullPointerException {
		if (repliableMessageID != null) {
			LinkedList rep = (LinkedList) replyQueue.get(repliableMessageID.toString());
			if (rep != null)
				return !rep.isEmpty();
			return false;
		}
		throw new NullPointerException();
	}

	/**
	 * @see DispatchingService#getAllReplies(MessageID)
	 */
	public Replies getAllReplies(MessageID repliableMessageID) throws NullPointerException {
		if (repliableMessageID != null) {
			LinkedList rep = (LinkedList) replyQueue.get(repliableMessageID.toString());
			if (rep == null)
				return null;
			Message[] result = null;
			boolean lastArrived = false;
			Iterator it = null;
			long t = 0;
			synchronized (timeouts) {
				t = ((Long) timeouts.get(repliableMessageID.toString())).longValue();
			}
			synchronized (rep) {
				/*
				 * The first wait is used when no reply is arrived. It is
				 * separated from the other cases because if it is empty it is
				 * not possible to check whether isLast == true. The second
				 * wait, the one in the while, is used to wait for all the
				 * replies to arrive or the timeout to expire. It is used only
				 * when isEmpty == false.
				 */
				if (rep.isEmpty() && t > System.currentTimeMillis()) // initial
																		// wait.
					try {
						rep.wait();
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				if (t > System.currentTimeMillis() && !rep.isEmpty())
					// it enters here only if isEmpty == false AND timeout is
					// not expired
					while ((t > System.currentTimeMillis()) && !(((Reply) rep.getLast()).isLast())) {
						// if timeout is not expired and the last reply is not
						// arrived,
						// wait.
						try {
							rep.wait();// wait until the timeout expires
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				// clear the queue of the replies and the timeout
				/*
				 * the LinkedList contains Reply. It must return Message => get
				 * an iterator on the list and extract the payload of each Reply
				 * and put it into the collection. Then return the collection.
				 */
				LinkedList l = (LinkedList) replyQueue.remove(repliableMessageID.toString());
				it = l.iterator();
				int i = 0;
				result = new Message[l.size()];
				while (it.hasNext()) {
					Reply r = (Reply) it.next();
					if (r.isLast())
						lastArrived = true;
					result[i] = r.getPayload();
					i++;
				}
			}
			/*
			 * synchronized(timeouts.get(repliableMessageID.toString())){
			 * timeouts.remove(repliableMessageID.toString()); }
			 */
			return new Replies(result, lastArrived);
		}
		throw new NullPointerException();
	}

	/**
	 * It manages the aging of a table. Every <code>SLEEP_INTERVAL</code> it
	 * checks all the entries in the table and removes those whose timeout has
	 * expired.
	 * 
	 * @author Alessandro Monguzzi
	 */
	private class GarbageCollector implements Runnable {
		private Map replyQueue;
		private Map timeouts;
		private static final long SLEEP_INTERVAL = 5000;
		private boolean exit = false;

		public GarbageCollector(Map replyQueue, Map timeouts) {
			this.replyQueue = replyQueue;
			this.timeouts = timeouts;
		}

		public void run() {
			while (!exit) {
				try {
					synchronized (replyQueue) {
						Iterator repl = replyQueue.entrySet().iterator();
						synchronized (timeouts) {
							Iterator tim = timeouts.entrySet().iterator();
							while (repl.hasNext() && tim.hasNext()) {
								Map.Entry elemTimeouts = (Map.Entry) tim.next();
								Map.Entry replElem = (Map.Entry) repl.next();
								// if timeout is expired, remove it and notify
								if (((Long) elemTimeouts.getValue()).longValue() < System.currentTimeMillis()) {
									synchronized (replElem.getValue()) {
										replElem.getValue().notifyAll(); // this
																			// notifyAll
																			// acts
																			// on
																			// the
																			// linked
																			// list
																			// that
																			// should
																			// contain
																			// the
																			// replies
																			// for
																			// a
																			// given
																			// message
									}
									timeouts.remove(elemTimeouts.getValue());
								}
							}
						}
					}
					Thread.sleep(SLEEP_INTERVAL);
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
	}
}// end class TCPDispatchingService
