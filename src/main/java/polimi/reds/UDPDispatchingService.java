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

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.Serializable;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Logger;

import polimi.reds.broker.overlay.Transport;
import polimi.reds.broker.overlay.UDPEnvelope;
import polimi.util.DatagramObjectIO;

public class UDPDispatchingService implements DispatchingService, Runnable {
  // Indicates the size of the incoming UDP buffer
  private final static int BUFFER_SIZE = 16384;
  // Indicates the timeout in ms for receiving data via DatagramObjectIO
  private final static int RECEIVE_TIMEOUT = 1000;
  /**
   * The IP address of the brokerIP which runs the broker this client is joined.
   */
  private String brokerIP;
  /**
   * The TCP brokerPort used to communicate with the broker this client is
   * joined.
   */
  private int brokerPort;
  private int localPort;
  private LinkedList messages;
  private DatagramObjectIO messageIO;
  private Logger logger;
  private NodeDescriptor id;
  private String localIP;
  private boolean opened;
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
  private final long DEFAULT_TIMEOUT = 5000;
  /**
   * This thread manages the <code>queue</code> removing all the entries with
   * the <code>timeout</code> expired.
   */
  private GarbageCollector garbageCollector;

  public UDPDispatchingService(String hostName, int brokerPort, int localPort) {
    replyQueue = new Hashtable();
    timeouts = new Hashtable();
    garbageCollector = new GarbageCollector(replyQueue, timeouts);
    this.brokerIP = hostName;
    this.brokerPort = brokerPort;
    this.localPort = localPort;
    messages = new LinkedList();
    messageIO = new DatagramObjectIO(localPort, BUFFER_SIZE, RECEIVE_TIMEOUT);
    logger = Logger.getLogger("polimi.reds");
    // The ID of this client is a string built by merging the local IP address
    // and the current time in milliseconds.    
      try {
		localIP = InetAddress.getLocalHost().getHostAddress();
	} catch (UnknownHostException e) {
		e.printStackTrace();
	}
      id = new NodeDescriptor();
    opened = false;
  }

  /**
   * @see polimi.reds.DispatchingService#open()
   */
  public void open() throws ConnectException {
    // Sends the CLIENT_OPEN message, including the local id
    UDPEnvelope openMessage = new UDPEnvelope(UDPEnvelope.CLIENT_OPEN);
    openMessage.setSenderID(id);
    openMessage.setSenderIP(localIP);
    openMessage.setSenderPort(localPort);
    opened = true;
	Thread t = new Thread(this);
	t.setDaemon(true);
	t.setName("UDPDispatchingService.clientThread");
	t.start();
	garbageCollector.setDaemon(true);
    garbageCollector.start();
    messageIO.sendObject(openMessage, brokerIP, brokerPort);
    logger.fine("Opening dispatching service to "+brokerIP+":"+brokerPort);
  }

  /**
   * @see polimi.reds.DispatchingService#close()
   */
  public synchronized void close() {
    // If the connection is not open, this function ends
    if(!opened) return;
    opened = false;
    UDPEnvelope close = new UDPEnvelope(UDPEnvelope.CLOSE);
    close.setSenderID(id);
    close.setSenderIP(localIP);
    close.setSenderPort(localPort);
    messageIO.sendObject(close, brokerIP, brokerPort);
    synchronized(messages) {
      messages.notifyAll();
    }
    garbageCollector.exit();
  }

  /**
   * @see polimi.reds.DispatchingService#getID()
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
    synchronized(messages) {
      try {
        while(messages.isEmpty())
          messages.wait();
      } catch(InterruptedException e) {
        e.printStackTrace();
      }
      Message m = null;
      try {
        m = (Message) messages.removeFirst();
      } catch(NoSuchElementException e) {
        return null;
      }
      return m;
    }
  }
  
  /**
   * Get first message available and removes it from the queue of received messages. 
   * If there are no messages available suspends the caller until a new message arrives or 
   * <code>timeout</code> expires. If this event happens, it returns <code>null</code>, else, it
   * returns the first message available.
   * 
   * @param timeout how long it will wait for a message to arrive.
   * @return first message available.
   */
  public Message getNextMessage(long timeout) {
	  Message m = null;
	    synchronized (messages) {
	      try {
	        messages.wait(timeout);
			if (!messages.isEmpty())
				m = (Message)messages.removeFirst();
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
  public Message getNextMessage(Filter f){
	  Message m  = null;
	  synchronized (messages) {
		  try{
			  boolean match = false;
			  while(!match){
				  Iterator it = messages.iterator();
				  while(it.hasNext() && !match){
					  m = (Message)it.next();
					  match = f.matches(m);
				  }
				  if(match){
					  messages.remove(m);
					  //messages.notifyAll();
					  return m;
				  }
				  messages.wait();
			  }
			  
		  }catch(Exception e){
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
  public boolean hasMoreMessages(Filter f){
	  boolean match = false;
	  synchronized (messages) {
		  Iterator it = messages.iterator();
		  while(it.hasNext() && !match){
			  match = f.matches((Message)it.next());
		  }
	}
	  return match;
  }
  /**
   * @see polimi.reds.DispatchingService#subscribe(polimi.reds.Filter)
   */
  public synchronized void subscribe(Filter filter) {
    // If opened is FALSE, the connection to BROKER does not exist and this
    // function ends
    if(!opened) return;
    UDPEnvelope msg = new UDPEnvelope(UDPEnvelope.SUBSCRIBE, filter, Transport.FILTER_CLASS);
    msg.setSenderID(id);
    messageIO.sendObject(msg, brokerIP, brokerPort);
  }

  /**
   * @see polimi.reds.DispatchingService#unsubscribe(polimi.reds.Filter)
   */
  public synchronized void unsubscribe(Filter filter) {
    // If opened is FALSE, the connection to BROKER does not exist and this
    // function ends
    if(!opened) return;
    UDPEnvelope msg = new UDPEnvelope(UDPEnvelope.UNSUBSCRIBE, filter, Transport.FILTER_CLASS);
    msg.setSenderID(id);
    messageIO.sendObject(msg, brokerIP, brokerPort);
  }

  /**
   * @see polimi.reds.DispatchingService#unsubscribeAll()
   */
  public synchronized void unsubscribeAll() {
    // If opened is FALSE, the connection to BROKER does not exist and this
    // function ends
    if(!opened) return;
    UDPEnvelope msg = new UDPEnvelope(UDPEnvelope.UNSUBSCRIBEALL, null, Transport.FILTER_CLASS);
    msg.setSenderID(id);
    messageIO.sendObject(msg, brokerIP, brokerPort);
  }

  /**
   * Forwards the specified message to dispatching
   * service. If the connection to the dispatching service is not opened the
   * operation has no effect.
   * 
   * @param msg The message to be sent.
   * @param subject the subject of the message.
   */
  protected synchronized void forward(String subject, Serializable msg) {
    if(!opened) return;
    UDPEnvelope envelope = new UDPEnvelope(subject, msg, Transport.MISCELLANEOUS_CLASS);
    envelope.setSenderID(id);
    messageIO.sendObject(envelope, brokerIP, brokerPort);
  }

  /**
   * @see polimi.reds.DispatchingService#publish(polimi.reds.Message)
   */
  public synchronized void publish(Message msg) {
    // If opened is FALSE, the connection to BROKER does not exist and this
    // function ends
    if(!opened) return;
    //create a new ID for the message
    msg.createID();
    UDPEnvelope m;
    // if RepliableMessage set a new entry in the reply queue.
    if(msg instanceof Repliable) {
      synchronized(replyQueue) {
        try {
          replyQueue.put(msg.getID().toString(), new LinkedList());
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
    m = new UDPEnvelope(UDPEnvelope.PUBLISH, msg, Transport.MESSAGE_CLASS);
    m.setSenderID(id);
    messageIO.sendObject(m, brokerIP, brokerPort);
  }

  /**
   * Can be used for checking whether the dispatching service is still opened.
   * 
   * @return Returns TRUE if the dispatching service is still opened.
   */
  public boolean isOpened() {
    return opened;
  }

  /**
   * @see java.lang.Runnable#run()
   */
  public void run() {
    while(opened) {
      try {
        Object o = messageIO.receiveObject();
        if(!(o instanceof UDPEnvelope)) {
          logger.warning("Unrecognized format for message received");
          continue;
        }
        UDPEnvelope msg = (UDPEnvelope) o;
        if(msg.getTypeOfMessage().equals(UDPEnvelope.CONFIRM_OPEN))
        	logger.fine("Opened dispatching service to "+brokerIP);
        else if(msg.getTypeOfMessage().equals(UDPEnvelope.CLOSE)){
        	logger.fine("Broker "+brokerIP+" closed dispatching service");
            opened = false;
            synchronized(messages) {
              messages.notifyAll();
            }
        }else if(msg.getTypeOfMessage().equals(UDPEnvelope.REPLY)){
        	//Take the right entry from the table.
            try {
            	synchronized (replyQueue) {
            		LinkedList e = (LinkedList) replyQueue.get(((Reply) msg.getPayload()).getRepliableMessageID().toString());
              //synchronized(e) {
                // Add the reply to the reply queue
            		e.addLast(((Reply) msg.getPayload()));
                //e.notifyAll();
              //}
            	  //notifies all the threads waiting on a generic reply
            		replyQueue.notifyAll();
            	}
            } catch(NullPointerException e) {
              // if this exception is thrown, it means that the timeout has
              // expired and so the
              // reply must be discarded.
            }
        }else{
        	synchronized(messages) {
                messages.addLast(msg.getMessage());
                messages.notifyAll();
              }
        }
      } catch(InterruptedIOException e) {
        continue;
      } catch(IOException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * @see polimi.reds.DispatchingService#reply(Message, MessageID)
   */
  public synchronized void reply(Message reply, MessageID repliableMessageID) {
    // If opened is FALSE, the connection to BROKER does not exist and this
    // function ends
    if(!opened) return;
    //create a new ID for the reply message
    reply.createID();
    // Create a "reply-type message": it contains the message 'reply'
    UDPEnvelope replyMsg = new UDPEnvelope(UDPEnvelope.REPLY, new Reply(
        repliableMessageID, true, reply), Transport.REPLY_CLASS);
    // Send the message to the BROKER
    replyMsg.setSenderID(id);
    messageIO.sendObject(replyMsg, brokerIP, brokerPort);
  }

  /**
   * @see DispatchingService#getNextReply(MessageID)
   */
  public Message getNextReply(MessageID repliableMessageID)
      throws NullPointerException, TimeoutException {
	  Reply r = null;
	    synchronized(replyQueue) {
	      LinkedList rep = (LinkedList) replyQueue.get(repliableMessageID
	          .toString());
	      if(rep==null) return null;
	      // synchronized(rep) {
	      while(rep.isEmpty()) {
	        try {
	          replyQueue.wait();
	        } catch(InterruptedException e) {
	          e.printStackTrace();
	        }
	        // }
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
  /**
   * @see DispatchingService#getNextReply()
   */
  public Message getNextReply(){  
	Message r = null;
	synchronized (replyQueue) {
		try{
			while(!hasMoreReplies())
				replyQueue.wait();
			/* If it comes here it means that at least one reply is arrived => r cannot remain null at the end of the while.
			 */
			Set rep = replyQueue.entrySet();
	    	Iterator repIterator = rep.iterator();
	    	boolean found = false;
	    	while(repIterator.hasNext() && !found){
	    		LinkedList l = (LinkedList)((Map.Entry)repIterator.next()).getValue();
	    		if(l.size() > 0){
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
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	return r;
  }
  /**
   * @see DispatchingService#getNextReply(long)
   */
  public Message getNextReply(long timeout){
	  Message r = null;
		synchronized (replyQueue) {
			try{
				if(!hasMoreReplies())
					replyQueue.wait(timeout);
				/* The timeout means that the replyQueue could be empty when the thread wakes up.
				 * => It is necessary to recheck the queue before trying to extract a reply.
				 * If two or more threads are waiting, only one wakes up and can get a positive value from the
				 * hasMoreReplies, the other ones are blocked by the synchronized statement.
				 */
				if(hasMoreReplies()){
					Set rep = replyQueue.entrySet();
			    	Iterator repIterator = rep.iterator();
			    	boolean found = false;
			    	while(repIterator.hasNext() && !found){
			    		LinkedList l = (LinkedList)((Map.Entry)repIterator.next()).getValue();
			    		if(l.size() > 0){
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
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		return r;
  }
  /**
   * @see DispatchingService#hasMoreReplies()
   */
  public boolean hasMoreReplies(){
	  Set rep = replyQueue.keySet();
	  Iterator repIt = rep.iterator();
	  boolean found = false;
	  while(repIt.hasNext() && !found){
		  synchronized (replyQueue) {
			  LinkedList l = (LinkedList) replyQueue.get(repIt.next());
			  if(l.size() > 0)
				  return true;
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
	    LinkedList rep = (LinkedList) replyQueue.get(repliableMessageID.toString());
	    if(rep != null)
	    	return !rep.isEmpty();
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
	    LinkedList rep = (LinkedList) replyQueue.get(repliableMessageID.toString());
	    if(rep == null)
	    	return null;
	    Message[] result = null;
	    boolean lastArrived = false;
	    Iterator it = null;
	    long t = 0;
	    synchronized(timeouts) {
	      t = ((Long) timeouts.get(repliableMessageID.toString())).longValue();
	    }
	    synchronized(rep) {
	      /*
	       * The first wait is used when no reply is arrived. It is separated
	       * from the other cases because if it is empty it is not possible to
	       * check whether isLast == true. The second wait, the one in the
	       * while, is used to wait for all the replies to arrive or the timeout
	       * to expire. It is used only when isEmpty == false.
	       */
	      if(rep.isEmpty() && t>System.currentTimeMillis()) // initial wait.
	        try {
	          rep.wait();
	        } catch(InterruptedException e1) {
	          e1.printStackTrace();
	        }
	      if(t>System.currentTimeMillis() && !rep.isEmpty())
	        // it enters here only if isEmpty == false AND timeout is not expired
	        while((t>System.currentTimeMillis()) && !(((Reply) rep.getLast()).isLast())) {
	          // if timeout is not expired and the last reply is not arrived, wait.
	          try {
	            rep.wait();// wait until the timeout expires
	          } catch(InterruptedException e) {
	            e.printStackTrace();
	          }
	        }
	      // clear the queue of the replies and the timeout
	      /*
	       * the LinkedList contains Reply. It must return Message => get an
	       * iterator on the list and extract the payload of each Reply and put
	       * it into the collection. Then return the collection.
	       */
	      LinkedList l = (LinkedList) replyQueue.remove(repliableMessageID.toString());
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
   * This thread can be used to manage the aging of a table. Every
   * <code>SLEEP_INTERVAL</code> it checks all the entries in the table and
   * removes those whose timeout has expired.
   * 
   * @author Alessandro Monguzzi
   */
  private class GarbageCollector extends Thread {
    private Map replyQueue;
    private Map timeouts;
    private static final long SLEEP_INTERVAL = 5000;
    private boolean exit = false;

    public GarbageCollector(Map replyQueue, Map timeouts) {
      this.replyQueue = replyQueue;
      this.timeouts = timeouts;
	  this.setName("UDPDispatchingService.GarbageCollector");
    }

    public void run() {
      while(!exit) {
        try {
          Iterator repl = replyQueue.entrySet().iterator();
          Iterator tim = timeouts.entrySet().iterator();
          while(repl.hasNext()&&tim.hasNext()) {
            synchronized(timeouts) {
              Map.Entry elemTimeouts = (Map.Entry) tim.next();
              Map.Entry replElem = (Map.Entry) repl.next();
              // if timeout is expired, remove it and notify
              if(((Long) elemTimeouts.getValue()).longValue()<System
                  .currentTimeMillis()) {
                synchronized(replElem.getValue()) {
                  replElem.getValue().notifyAll();
                }
                timeouts.remove(elemTimeouts.getValue());
              }
            }
          }
          sleep(SLEEP_INTERVAL);
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
