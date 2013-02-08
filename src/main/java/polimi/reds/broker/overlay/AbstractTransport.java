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
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Logger;

import polimi.reds.NodeDescriptor;
import polimi.reds.NotConnectedException;
/**
 * Multithreaded abstract implementation of the <code>Transport</code> interface.<br>
 * This class supports multiple traffic classes each served by a specific <code>Thread</code>. Besides the four basic traffic classes
 * <code>MESSAGE_CLASS, FILTER_CLASS, REPLY_CLASS</code> and <code>MISCELLANEOUS_CLASS</code>, it allows the creation of applicaton
 * specific traffic classes.<br>
 * No priority is given to the <code>Thread</code>s, so no guarantee is given about the order in which two messages belonging to 
 * different class are processed.
 * 
 * @author Alessandro Monguzzi
 */
public abstract class AbstractTransport implements Transport {
	protected boolean running = false; 

	protected boolean beaconing = false;

	protected Logger logger;

	protected ProxySet proxySet = new ProxySet();

	protected NodeDescriptor localID = null;

	private Map linksInUse=new HashMap();
	/**
	 * This map contains all the packetListeners for messages directed to the
	 * routing level. The key is the subject, the value is the listener.
	 */
	protected Map packetListeners = new HashMap();

	/**
	 * This list contains all the LinkOpenedListeners.
	 */
	protected List linkOpenedListeners = new LinkedList();

	/**
	 * This list contains all the LinkClosedListeners.
	 */
	protected List linkClosedListeners = new LinkedList();

	/**
	 * This list contains all the LinkClosingListeners.
	 */
	protected List linkClosingListeners = new LinkedList();
	
	/**
	 * This list contains all the LinkDeadListeners.
	 */
	protected List linkDeadListeners = new LinkedList();

	/**
	 * This <code>Map</code> pairs each traffic class with its serving
	 * <code>Thread</code>.
	 */
	protected Map trafficThread = new HashMap();

	/**
	 * This <code>Map</code> pairs each traffic class with its message queue.
	 */
	protected Map trafficQueues = new HashMap();

	
	protected abstract NodeDescriptor openLinkHelper(String url) throws MalformedURLException,
    ConnectException, AlreadyExistingLinkException;
	
	/**
	 * Stop all the threads that are waiting for messages and remove the corresponding traffic classes.
	 *
	 */
	protected void stopParserThreads() {
		String[] classes = new String[trafficQueues.keySet().size()];
		synchronized (trafficQueues) {
			Iterator iter = trafficQueues.keySet().iterator();
			int i = 0;
			while (iter.hasNext()) {
				classes[i] = (String) iter.next();
				i++;
			}
		}
		for (int i = 0; i < classes.length; i++)
			removeTrafficClass(classes[i]);
	}

	/**
	 * @see Transport#addTrafficClass(String)
	 */
	public void addTrafficClass(String name) {
		Object queue = trafficQueues.get(name);
		if (queue == null) {
			LinkedList messageList = new LinkedList();
			trafficQueues.put(name, messageList);
			ParserThread parserThread = new ParserThread(
					(LinkedList) messageList);
			parserThread.setName("Transport." + name + "ParserThread");
			parserThread.setDaemon(false);
			trafficThread.put(name, parserThread);
			parserThread.start();
		}
	}

	/**
	 * @see Transport#removeTrafficClass(String)
	 */
	public void removeTrafficClass(String name) {
		ParserThread parser = (ParserThread) trafficThread.remove(name);
		if (parser != null) {
			parser.exit = false;
			List list = (List) trafficQueues.remove(name);
			synchronized (list) {
				list.notifyAll();
			}
		}
	}
	/**
	 * @see Transport#addLinkOpenedListener(LinkOpenedListener)
	 */
	public void addLinkOpenedListener(LinkOpenedListener listener) {
		linkOpenedListeners.add(listener);
	}

	/**
	 * @see Transport#addLinkClosedListener(LinkClosedListener)
	 */
	public void addLinkClosedListener(LinkClosedListener listener) {
		linkClosedListeners.add(listener);
	}

	/**
	 * @see Transport#addLinkDeadListener(LinkDeadListener)
	 */
	public void addLinkDeadListener(LinkDeadListener listener) {
		linkDeadListeners.add(listener);
	}

	/**
	 * @see Transport#removeLinkOpenedListener(LinkOpenedListener)
	 */
	public void removeLinkOpenedListener(LinkOpenedListener listener) {
		linkOpenedListeners.remove(listener);
	}

	/**
	 * @see Transport#removeLinkClosedListener(LinkClosedListener)
	 */
	public void removeLinkClosedListener(LinkClosedListener listener) {
		linkClosedListeners.remove(listener);
	}

	/**
	 * @see Transport#removeLinkDeadListener(LinkDeadListener)
	 */
	public void removeLinkDeadListener(LinkDeadListener listener) {
		linkDeadListeners.remove(listener);
	}

	boolean mayCloseLink(NodeDescriptor closingNeighbor){
		synchronized(linksInUse){
			if (linksInUse.containsKey(closingNeighbor))
				if (((Integer)linksInUse.get(closingNeighbor)).intValue()>0)
					return false;
			return true;
		}
	}
	
	/**
	 * @see Transport#send(String, Serializable, NodeDescriptor)
	 */
	public void send(String subject, Serializable payload,
			NodeDescriptor receiver, String trafficClass)
			throws NotConnectedException {
		Proxy n = proxySet.get(receiver);
		if (n != null)
			n.sendMessage(subject, payload, trafficClass);
		else
			throw new NotConnectedException();
	}

	/**
	 * @see Transport#addPacketListener(PacketListener, String)
	 */
	public void addPacketListener(PacketListener listener, String subject) {
		if (packetListeners.containsKey(subject)) {
			List l = (List) packetListeners.get(subject);
			l.add(listener);
		} else {
			List l = new LinkedList();
			l.add(listener);
			packetListeners.put(subject, l);
		}
	}

	/**
	 * @see Transport#removePacketListener(PacketListener, String)
	 */
	public void removePacketListener(PacketListener listener, String subject) {
		List listeners = (List) packetListeners.get(subject);
		if (listeners != null) {
			listeners.remove(listener);
		}
	}

	/**
	 * @see Transport#removePacketListener(PacketListener)
	 */
	public void removePacketListener(PacketListener listener) {
		Set subjects = packetListeners.keySet();
		Iterator it = subjects.iterator();
		while (it.hasNext()) {
			String next = (String) it.next();
			removePacketListener(listener, next);
		}
	}
	/**
	 * @see Transport#start()
	 */
	public void start() {
		if (running == true) {
			logger.warning("Start method called on an already started Transport");
			return;
		}
		running = true;
		// create the basic traffic classes
		addTrafficClass(MESSAGE_CLASS);
		addTrafficClass(FILTER_CLASS);
		addTrafficClass(REPLY_CLASS);
		addTrafficClass(MISCELLANEOUS_CLASS);
	}
	/**
	 * @see Transport#stop()
	 */
	public void stop() {
		stopParserThreads();
	}

	/**
	 * @see Transport#setNodeDescriptor(NodeDescriptor)
	 */
	public void setNodeDescriptor(NodeDescriptor nodeDescr) {
		localID = nodeDescr;
	}

	/**
	 * @see Transport#getNodeDescriptor()
	 */
	public NodeDescriptor getNodeDescriptor() {
		return localID;
	}

	private class ParserThread extends Thread {
		private LinkedList packets = null;

		private boolean exit = false;

		public ParserThread(LinkedList packetList) {
			this.packets = packetList;
		}

		public void run() {
			parseAndDeliver(!exit, packets);
		}
	}

	/**
	 * Enable/Disable beacon mode.
	 */
	public void setBeaconing(boolean beaconing) {
		this.beaconing = beaconing;
	}

	/**
	 * Check whether the local node is in beacon mode.
	 */
	public boolean isBeaconing() {
		return beaconing;
	}
	
	public void enqueue(Envelope e){
		try {
	    	LinkedList list = null;
	    	synchronized (trafficQueues) {
	    		list = (LinkedList) trafficQueues.get(e.getTrafficClass());
			}
	    	if(list != null){
	    		synchronized (list) {
	    			list.addLast(e);
	    			list.notifyAll();
	    			logger.finer("equeued " + e.toString());
				}
	    	}else{
	    		logger.warning("No traffic class for message " + e.toString());
	    	}
	    } catch(Exception ex) {
	      ex.printStackTrace();
	    }
	}

	// Main loop to parse the list of received messages and deliver them
	private void parseAndDeliver(boolean running, LinkedList packets) {
		Envelope received = null;
			while (running) {
				try{
				synchronized (packets) {
					try {
						while (packets.size() == 0) {
							logger.finer("Parse and deliver waiting list size "+packets.size());
							packets.wait();
							logger.finer("Parse and deliver woken up list size "+packets.size());
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					try {
						received = (Envelope) packets.removeFirst();
					} catch (NoSuchElementException e) {
						System.out.println("No Such Element in packet queue");
						System.err.println("No Such Element in packet queue");
						continue;
					}
				}
				if (received == null){
					System.out.println("received is NULL: skipping");
					System.err.println("received is NULL: skipping");
					continue;
				}
				if (received.getTypeOfMessage().equals(TCPEnvelope.CLOSE)) {
					logger.severe("This code should be unreachable with TCPTransport");
					// THE FOLLOWING CODE HAS BEEN MOVED AND SHOULD NOW NEVER RUN in TCPTransport confirm the closing and close the stream
					if (mayCloseLink(received.getSenderID())){
						closeLinkAck(received.getSenderID());
						Iterator it = linkClosedListeners.iterator();
						while (it.hasNext()) {
							LinkClosedListener l = (LinkClosedListener) it.next();
							l.signalLinkClosed(received.getSenderID());
						}
						proxySet.remove(received.getSenderID());
					}
				}
				else {
					List l = (List) packetListeners.get(received.getTypeOfMessage());
					if ((l != null)) {
						Iterator it = l.iterator();
						while (it.hasNext()) {
							PacketListener list = (PacketListener) it.next();
							list.signalPacket(received.getTypeOfMessage(),
									received.getSenderID(), received
											.getPayload());
						}
					}
				}
				} // end try
				catch (Exception e) {
					// FIXME: Manage this exception in a better way.
					System.err.println("Error parsing and delivering");
					e.printStackTrace();
					logger.severe("Error parsing and delivering");
				}
			} // end while
	
	} // end method
	
	protected void closeLinkHelper(NodeDescriptor neighborID){
		logger.fine("Closing link to neighbor "+neighborID);
		Proxy neighbor = (Proxy) proxySet.get(neighborID);
		neighbor.disconnect();
		proxySet.remove(neighborID);
	}
	
		 
	/**
	   * @see Transport#closeLink(NodeDescriptor)
	   */
	  public final void closeLink(NodeDescriptor neighborID) {
		if (mayCloseLink(neighborID)){
			closeLinkHelper(neighborID);
		} else {
			System.out.println("Not closing link to "+neighborID);
		}
	  }

	  
	  /**
	   * 
	   */
	  public final NodeDescriptor openLink(String url) throws MalformedURLException,
      ConnectException{
		 NodeDescriptor dest = null;
		 logger.finer("acquiring linksInUseLock");
		 try{
		  synchronized(linksInUse){
			  logger.finer("acquired");
			  try {
				  dest = openLinkHelper(url);
			  } catch (AlreadyExistingLinkException e) {
				  dest = e.getRemoteNodeDescriptor();
			  }
			  if (dest!=null){
				  if(!linksInUse.containsKey(dest))
					  linksInUse.put(dest, new Integer(0));
				  linksInUse.put(dest, new Integer(((Integer)linksInUse.get(dest)).intValue()+1));			
			  }
		  }
		  }
		 finally{
			  logger.finer("released linksInUseLock");		
		 }
		return dest;
	  }
	  
	/**
	 * Manage the request of closing a connection from the given neighbor.
	 * 
	 * @param closer
	 *            the neighbor that request the closing of the connection
	 */
	protected abstract void closeLinkAck(NodeDescriptor closer);
}