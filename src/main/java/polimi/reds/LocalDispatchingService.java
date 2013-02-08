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
import java.net.ConnectException;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import polimi.util.DeepCopier;
import polimi.reds.DispatchingService;
import polimi.reds.Filter;
import polimi.reds.Message;
import polimi.reds.MessageID;
import polimi.reds.NodeDescriptor;
import polimi.reds.NotConnectedException;
import polimi.reds.Repliable;
import polimi.reds.Replies;
import polimi.reds.Reply;
import polimi.reds.TimeoutException;
import polimi.reds.broker.overlay.*;

/**
 * The client interface to access the REDS local dispatching service.
 * 
 * @author Montinari
 * @author Bruno
 */
public class LocalDispatchingService implements Proxy, DispatchingService {
  /**
   * <code>true</code> if the connection with the broker this client is
   * joined.
   */
  protected boolean opened;
  /**
   * The identifier of this client (more specifically the identifier of this
   * specific connection with the REDS dispatching network).
   */
  protected NodeDescriptor id;
  /** The list of messages received but not yet processed by this client. */
  protected LinkedList messages;
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
   * Reference to LocalTransport
   */
  private LocalTransport transport;

  /**
   * Builds a new <code>LocalDispatchingService</code> to join a broker
   * running on the same jvm.
   */
  public LocalDispatchingService(LocalTransport t) {
    replyQueue = Collections.synchronizedMap(new Hashtable());
    timeouts = Collections.synchronizedMap(new Hashtable());
    this.id = new NodeDescriptor();
    garbageCollector = new GarbageCollector(replyQueue, timeouts);
    messages = new LinkedList();
    opened = false;
    transport = t;
  }

  /**
   * Opens the connection to the REDS dispatching network (local). If the
   * connection is already opened this method has no effect.
   */
  public void open() throws ConnectException {
    if(opened) return;
    // Open the connection with transport
    transport.accept(this);
    // The connection is open
    opened = true;
    garbageCollectorThread = new Thread(garbageCollector);
    garbageCollectorThread.setDaemon(true);
    garbageCollectorThread.setName("LocalDispatchingService.GarbageCollector");
    garbageCollectorThread.start();
  }

  /**
   * Closes the connection to the REDS dispatching network
   */
  public void close() {
    // If the connection is not open, this function ends
    if(!opened) return;
    // Send a close-notification to the broker
    Envelope close = new Envelope(Envelope.CLOSE);
    close.setSenderID(id);
    transport.enqueue(close);
    disconnect();
  }

  public void disconnect() {
    opened = false;
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
   * Get first message available and removes it from the queue of received
   * messages. If there are no messages available suspends the caller until a
   * new message arrives.
   * 
   * @return first message available.
   */
  public Message getNextMessage() {
    Message m = null;
    synchronized(messages) {
      try {
        while(messages.isEmpty())
          messages.wait();
        m = (Message) messages.removeFirst();
      } catch(Exception e) {
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
   * happens, it returns <code>null</code>, else, it returns the first
   * message available.
   * 
   * @param timeout how long it will wait for a message to arrive.
   * @return first message available.
   */
  public Message getNextMessage(long timeout) {
    Message m = null;
    synchronized(messages) {
      try {
        if(messages.isEmpty()) messages.wait(timeout);
        if(!messages.isEmpty()) m = (Message) messages.removeFirst();
      } catch(Exception e) {
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
    synchronized(messages) {
      try {
        boolean match = false;
        while(!match) {
          Iterator it = messages.iterator();
          while(it.hasNext()&&!match) {
            m = (Message) it.next();
            match = f.matches(m);
          }
          if(match) {
            messages.remove(m);
            // messages.notifyAll();
            return m;
          }
          messages.wait();
        }
      } catch(Exception e) {
        System.err
            .println("Error while getting first available message matching the specified filter");
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
    synchronized(messages) {
      Iterator it = messages.iterator();
      while(it.hasNext()&&!match) {
        match = f.matches((Message) it.next());
      }
    }
    return match;
  }

  /**
   * Subscribes to messages matching the given filter. If the connection with
   * the broker is not opened this method has no effect.
   * 
   * @param filter The <code>Filter</code> used to determine the messages this
   *          client is interested in.
   */
  public synchronized void subscribe(Filter filter) {
    if(!opened) return;
    // Create a "system-message": it is a SUBSCRIBE-type message and contains a
    // deep copy of the filter. The sender is the client (id)
    Envelope subscribeMsg = null;
    try {
      subscribeMsg = new Envelope(Envelope.SUBSCRIBE, (Filter) DeepCopier.copy(filter), Transport.FILTER_CLASS);
    } catch(Exception ex) {
      ex.printStackTrace();
    }
    subscribeMsg.setSenderID(id);
    transport.enqueue(subscribeMsg);
  }

  /**
   * Unsubscribes for messages matching the given filter. If the connection with
   * the broker is not opened this method has no effect.
   * 
   * @param filter the <code>Filter</code> used to determine the messages this
   *          client is no more interested in.
   */
  public synchronized void unsubscribe(Filter filter) {
    if(!opened) return;
    // Create a "system-message": it is a UNSUBSCRIBE-type message and contains
    // a deep copy of the filter. The sender is the client (id)
    Envelope unsubscribeMsg = null;
    try {
      unsubscribeMsg = new Envelope(Envelope.UNSUBSCRIBE, (Filter) DeepCopier.copy(filter), Transport.FILTER_CLASS);
    } catch(Exception ex) {
      ex.printStackTrace();
    }
    unsubscribeMsg.setSenderID(id);
    transport.enqueue(unsubscribeMsg);
  }

  /**
   * Removes all subscriptions issued so far. If the connection with the broker
   * is not opened this method has no effect.
   */
  public synchronized void unsubscribeAll() {
    // If opened is FALSE, the connection to BROKER does not exist and this
    // function ends
    if(!opened) return;
    // Create a "system-message": it is an UNSUBSCRIBEALL-type message and
    // contains a filter;
    // the sender is the client (id)
    Envelope unsubscribeAllMsg = new Envelope(Envelope.UNSUBSCRIBEALL, null, Transport.FILTER_CLASS);
    unsubscribeAllMsg.setSenderID(id);
    transport.enqueue(unsubscribeAllMsg);
  }

  /**
   * Publish a new message. If the connection with the broker is not opened this
   * method has no effect. If the message is a <code>RepliableMessage</code>
   * it creates a new entry in the <code>queue</code>.<br>
   * Before publication each message is given a new unique
   * <code>MessageID</code>.
   * 
   * @param msg the <code>Message</code> to publish.
   */
  public synchronized void publish(Message msg) {
    // If opened is FALSE, the connection to BROKER does not exist and this
    // function ends
    if(!opened) return;
    // create a new ID for the message
    msg.createID();
    Envelope publishMsg = null;
    // if Repliable set a new entry in the reply queue.
    if(msg instanceof Repliable) {
      synchronized(replyQueue) {
        try {
          // create a new entry in the queue of the replies.
          replyQueue.put(msg.getID().toString(), new LinkedList());
          // create a new entry in the list of the timeouts.
          synchronized(timeouts) {
            timeouts.put(msg.getID().toString(), new Long(System
                .currentTimeMillis()
                +DEFAULT_TIMEOUT));
          }
        } catch(NullPointerException e) {
          e.printStackTrace();
        }
      }
    }
    // Create a "publish-type message": it contains a clone of the message 'msg'
    try {
      publishMsg = new Envelope(Envelope.PUBLISH, (Serializable)DeepCopier.copy(msg), Transport.MESSAGE_CLASS);
    } catch(Exception ex) {
      ex.printStackTrace();
    }
    publishMsg.setSenderID(id);
    transport.enqueue(publishMsg);
  }

  /**
   * @see polimi.reds.DispatchingService#isOpened()
   */
  public boolean isOpened() {
    return opened;
  }

  /**
   * Forwards the specified message to dispatching service. If the connection to
   * the dispatching service is not opened the operation has no effect.
   * 
   * @param msg The message to be sent.
   * @param subject the subject of the message
   */
  protected synchronized void forward(String subject, Serializable msg) {
    if(!opened) return;
    Envelope fw=null;
    try {
      fw = new Envelope(subject, (Serializable) DeepCopier.copy(msg), Transport.MISCELLANEOUS_CLASS);
    } catch(Exception ex) {
      ex.printStackTrace();
    }
    transport.enqueue(fw);
  }

  /**
   * @see DispatchingService#reply(Message, MessageID)
   */
  public synchronized void reply(Message reply, MessageID repliableMessageID) {
    // If opened is FALSE, the connection to BROKER does not exist and this
    // function ends
    if(!opened) return;
    // create a new ID for the reply message
    if(reply!=null) reply.createID();
    // Create a "reply-type message": it contains the deep copy of the message 'reply'
    Envelope replyMsg = null;
    try {
      replyMsg = new Envelope(Envelope.REPLY, new Reply(
          repliableMessageID, true, (Message)DeepCopier.copy(reply)), Transport.REPLY_CLASS);
    } catch(Exception ex) {
      ex.printStackTrace();
    }
    // Send the message to the BROKER
    replyMsg.setSenderID(id);
    transport.enqueue(replyMsg);
  }

  /**
   * @see DispatchingService#getNextReply(MessageID)
   */
  public Message getNextReply(MessageID repliableMessageID)
      throws NullPointerException, TimeoutException {
    if(repliableMessageID!=null) {
      Reply r = null;
      synchronized(replyQueue) {
        LinkedList rep = (LinkedList) replyQueue.get(repliableMessageID
            .toString());
        if(rep==null) return null;
        while(rep.isEmpty()) {
          try {
            replyQueue.wait();
          } catch(InterruptedException e) {
            e.printStackTrace();
          }
        }
        try {
          r = (Reply) rep.removeFirst();
        } catch(NoSuchElementException e) {
          /*
           * If this exception is thrown, the timeout is expired and no replies
           * arrived. It is possible to remove the entry.
           */
          replyQueue.remove(repliableMessageID.toString());
          throw new TimeoutException();
        }
        if(r.isLast()) {// all replies arrived, => remove the entry
          replyQueue.remove(r.getRepliableMessageID());
          synchronized(timeouts) {
            timeouts.remove(r.getRepliableMessageID());
          }
        }
        return r.getPayload();
      }
    }
    throw new NullPointerException();
  }

  /**
   * @see DispatchingService#getNextReply()
   */
  public Message getNextReply() {
    Message r = null;
    synchronized(replyQueue) {
      try {
        while(!hasMoreReplies())
          replyQueue.wait();
        /*
         * If it comes here it means that at least one reply is arrived => r
         * cannot remain null at the end of the while.
         */
        Set rep = replyQueue.entrySet();
        Iterator repIterator = rep.iterator();
        boolean found = false;
        while(repIterator.hasNext()&&!found) {
          LinkedList l = (LinkedList) ((Map.Entry) repIterator.next())
              .getValue();
          if(l.size()>0) {
            Reply reply = (Reply) l.removeFirst();
            if(reply.isLast()) {// all replies arrived, => remove the entry
              replyQueue.remove(reply.getRepliableMessageID());
              synchronized(timeouts) {
                timeouts.remove(reply.getRepliableMessageID());
              }
            }
            found = true;
            r = reply.getPayload();
          }
        }
      } catch(Exception e) {
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
    synchronized(replyQueue) {
      try {
        /*
         * Here we use ad if instead of a while because we have to wait only for
         * one timeout; if after the given timeout the queue is empty again, it
         * returns null. The synchronized statement guarantees that only one
         * thread can test the second hasMoreReplies.
         */
        if(!hasMoreReplies()) replyQueue.wait(timeout);
        /*
         * The timeout means that the replyQueue could be empty when the thread
         * wakes up. => It is necessary to recheck the queue before trying to
         * extract a reply. If two or more threads are waiting, only one wakes
         * up and can get a positive value from the hasMoreReplies, the other
         * ones are blocked by the synchronized statement.
         */
        if(hasMoreReplies()) {
          Set rep = replyQueue.entrySet();
          Iterator repIterator = rep.iterator();
          boolean found = false;
          while(repIterator.hasNext()&&!found) {
            LinkedList l = (LinkedList) ((Map.Entry) repIterator.next())
                .getValue();
            if(l.size()>0) {
              Reply reply = (Reply) l.removeFirst();
              if(reply.isLast()) {// all replies arrived, => remove the entry
                replyQueue.remove(reply.getRepliableMessageID());
                synchronized(timeouts) {
                  timeouts.remove(reply.getRepliableMessageID());
                }
              }
              found = true;
              r = reply.getPayload();
            }
          }
        }
      } catch(Exception e) {
        e.printStackTrace();
      }
    }
    return r;
  }

  /**
   * @see DispatchingService#hasMoreReplies()
   */
  public boolean hasMoreReplies() {
    Set rep = replyQueue.keySet();
    boolean found = false;
    synchronized(replyQueue) {
      Iterator repIt = rep.iterator();
      while(repIt.hasNext()&&!found) {
        LinkedList l = (LinkedList) replyQueue.get(repIt.next());
        if(l.size()>0) return true;
      }
    }
    return false;
  }

  /**
   * @see DispatchingService#hasMoreReplies(MessageID)
   */
  public boolean hasMoreReplies(MessageID repliableMessageID)
      throws NullPointerException {
    if(repliableMessageID!=null) {
      LinkedList rep = (LinkedList) replyQueue.get(repliableMessageID
          .toString());
      if(rep!=null) return !rep.isEmpty();
      return false;
    }
    throw new NullPointerException();
  }

  /**
   * @see DispatchingService#getAllReplies(MessageID)
   */
  public Replies getAllReplies(MessageID repliableMessageID)
      throws NullPointerException {
    if(repliableMessageID!=null) {
      LinkedList rep = (LinkedList) replyQueue.get(repliableMessageID
          .toString());
      if(rep==null) return null;
      Message[] result = null;
      boolean lastArrived = false;
      Iterator it = null;
      long t = 0;
      synchronized(timeouts) {
        t = ((Long) timeouts.get(repliableMessageID.toString())).longValue();
      }
      synchronized(rep) {
        /*
         * The first wait is used when no reply is arrived. It is separated from
         * the other cases because if it is empty it is not possible to check
         * whether isLast == true. The second wait, the one in the while, is
         * used to wait for all the replies to arrive or the timeout to expire.
         * It is used only when isEmpty == false.
         */
        if(rep.isEmpty()&&t>System.currentTimeMillis()) // initial wait.
          try {
            rep.wait();
          } catch(InterruptedException e1) {
            e1.printStackTrace();
          }
        if(t>System.currentTimeMillis()&&!rep.isEmpty())
          // it enters here only if isEmpty == false AND timeout is not expired
          while((t>System.currentTimeMillis())
              &&!(((Reply) rep.getLast()).isLast())) {
            // if timeout is not expired and the last reply is not arrived,
            // wait.
            try {
              rep.wait();// wait until the timeout expires
            } catch(InterruptedException e) {
              e.printStackTrace();
            }
          }
        // clear the queue of the replies and the timeout
        /*
         * the LinkedList contains Reply. It must return Message => get an
         * iterator on the list and extract the payload of each Reply and put it
         * into the collection. Then return the collection.
         */
        LinkedList l = (LinkedList) replyQueue.remove(repliableMessageID
            .toString());
        it = l.iterator();
        int i = 0;
        result = new Message[l.size()];
        while(it.hasNext()) {
          Reply r = (Reply) it.next();
          if(r.isLast()) lastArrived = true;
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
   * @see Proxy#isBroker()
   */
  public boolean isBroker() {
    return false;
  }

  /**
   * @see Proxy#isClient()
   */
  public boolean isClient() {
    return true;
  }

  /**
   * @see Proxy#isConnected()
   */
  public boolean isConnected() {
    return opened;
  }

  /**
   * @see Proxy#sendMessage(String, Serializable)
   */
  public void sendMessage(String subject, Serializable payload, String trafficClass)
      throws NotConnectedException {
    if(opened) {
      if(subject.equals(Envelope.CLOSE)) {
        disconnect();
        return;
      }
      // clone the payload
      Serializable clonedPayload = null;
      try {
        clonedPayload = (Serializable) polimi.util.DeepCopier.copy(payload);
      } catch(Exception ex) {
        ex.printStackTrace();
      }
      // dispatch the payload to the proper queue depending on the subject
      if(subject.equals(Envelope.REPLY)) {
        try {
          synchronized(replyQueue) {
            LinkedList e = (LinkedList) replyQueue.get(((Reply) payload)
                .getRepliableMessageID().toString());
            // insert the reply into the queue.
            e.addLast(clonedPayload);
            replyQueue.notifyAll(); // notifies the threads waiting on the
            // getNextMessage() method
          }
        } catch(NullPointerException e) {
          // if this exception is thrown, it means that the timeout has expired
          // and so the replyQueue does not include the searched element, i.e.,
          // "e" is null
        }
      } else {
        // Store the received message in the local buffer.
        synchronized(messages) {
          messages.addLast(clonedPayload);
          messages.notifyAll();
        }
      }
    } else throw new NotConnectedException();
  }
 
  public String toString() {
    return this.id.getID();
  }

  /**
   * It manages the aging of a table. Every <code>SLEEP_INTERVAL</code> it
   * checks all the entries in the table and removes those whose timeout has
   * expired.
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
      while(!exit) {
        try {
          synchronized(replyQueue) {
            Iterator repl = replyQueue.entrySet().iterator();
            synchronized(timeouts) {
              Iterator tim = timeouts.entrySet().iterator();
              while(repl.hasNext()&&tim.hasNext()) {
                Map.Entry elemTimeouts = (Map.Entry) tim.next();
                Map.Entry replElem = (Map.Entry) repl.next();
                // if timeout is expired, remove it and notify
                if(((Long) elemTimeouts.getValue()).longValue()<System
                    .currentTimeMillis()) {
                  synchronized(replElem.getValue()) {
                    replElem.getValue().notifyAll(); // this notifyAll acts on
                    // the linked list that
                    // should contain the
                    // replies for a given
                    // message
                  }
                  timeouts.remove(elemTimeouts.getValue());
                }
              }
            }
          }
          Thread.sleep(SLEEP_INTERVAL);
        } catch(InterruptedException e) {
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
}
