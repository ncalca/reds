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

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.Serializable;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import polimi.reds.NodeDescriptor;
import polimi.reds.NotConnectedException;
import polimi.reds.broker.overlay.Proxy;
import polimi.reds.broker.overlay.Transport;
import polimi.util.DatagramObjectIO;

/**
 * This class implements a UDP based transport service for the REDS system.
 * Object serialization is handled via the service class DatagramObjectIO.
 */
public class UDPTransport extends AbstractTransport {
  // Indicates the size of the incoming UDP buffer
  private final static int BUFFER_SIZE = 16384;
  // Indicates the timeout in ms for receiving data
  private final static int RECEIVE_TIMEOUT = 1000;
  // Indicates a timeout that, when expired, triggers the send
  // of up to BEACON_RETRIES beacons for cheking the reachability of a neighbor
  private final static int BEACON_TIMEOUT = 3000;
  // Indicates how may times the local broker will retry to check
  // the reachability of the neighbor.
  private final static int BEACON_RETRIES = 3;
  
  private final static long WAITING_TIME = 10000;
 
  private DatagramObjectIO messageIO;
  private int port;
  private Map pendingOpenings;
  private String localIP;  
  /**
   * Creates a new <code>UDPTransport</code> listening on the specified port.
   * @param port the port on which it listen to
   */
  public UDPTransport(int port) {
    this.port = port;
    messageIO = new DatagramObjectIO(port, BUFFER_SIZE, RECEIVE_TIMEOUT);
    logger = Logger.getLogger("polimi.reds.transport");
    pendingOpenings = new HashMap();
    try {
      localIP = InetAddress.getLocalHost().getHostAddress();
    } catch(UnknownHostException e) {
      e.printStackTrace();
    }
  }
  /**
   * @see Transport#start()
   */
  public void start() {
    logger.fine("Starting UDPTransport");
    if(running==true) {
      logger.warning("Start method called on an already started UDPTransport");
      return;
    }
    super.start();
    // Starts the parsing Thread
    Thread readingThread = new Thread() {
      public void run() {
        receiveMessages();
      }
    };
    readingThread.start();
    // Starts the beaconing Thread if needed
    if(beaconing) {
      Thread beaconingThread = new Thread() {
        public void run() {
          beaconHandler();
        }
      };
      beaconingThread.setName("UDPTransport.beaconingThread");
      beaconingThread.start();
    }
    logger.config("UDPTransport started at port "+port);
  }
  /**
   * @see Transport#stop()
   */
  public void stop() {
    logger.fine("Stopping UDPTransport");
    if(running==false) {
      logger.warning("Stop method called on an already stopped UDPTransport");
      return;
    }
    running = false;
    Iterator it = proxySet.getAllProxies().iterator();
    Object o;
    UDPProxy neighbor;
    while(it.hasNext()) {
      o = it.next();
      if(o instanceof UDPProxy) {
        neighbor = (UDPProxy) o;
        neighbor.disconnect();
      }
    }
    stopParserThreads();
    logger.config("UDPTransport stopped");
  }
  
  /**
   * @see Transport#getURL()
   */
  public String getURL() {
    return "reds-udp:"+localIP+":"+port;
  }
   
  /**
   * This method is in charge of handling the beaconing process towards
   * neighboring brokers in case it is needed.
   */
  private synchronized void beaconHandler() {
    while(running) {
      try {
        wait(BEACON_TIMEOUT);
      } catch(InterruptedException e) {
        e.printStackTrace();
      }
      synchronized(proxySet) {
        LinkedList toRemove = new LinkedList();
        Collection c = proxySet.getAllProxies();
        Iterator it = c.iterator();
        while(it.hasNext()) {
          UDPProxy neighbor = (UDPProxy) it.next();
          if(neighbor.getLastContact()+BEACON_TIMEOUT<System
              .currentTimeMillis()
              &&neighbor.isBroker()&&neighbor.getLostBeacons()>BEACON_RETRIES) {
            // Timeout expired for at least BEACON_RETRIES times, disconnecting
            neighbor.brutalDisconnect();
            toRemove.add(neighbor);
          } else if(neighbor.getLastContact()+BEACON_TIMEOUT<System
              .currentTimeMillis()
              &&neighbor.isBroker()) {
            // Timeout expired, sending a beacon
            logger.warning("Sending beacon to "+neighbor.getID());
            try{
            	neighbor.sendMessage(UDPEnvelope.BEACON, null, Transport.MISCELLANEOUS_CLASS);
            }catch (NotConnectedException e){
            	
            }
          }
        }
        // Updating local list of UDP neighbors
        it = toRemove.iterator();
        while(it.hasNext()) {
          proxySet.remove(((UDPProxy)it.next()).getID());
        }
      }
    }
  }

  private void receiveMessages() {
    while(running) {
      try {
        Object o = messageIO.receiveObject();
        if(!(o instanceof UDPEnvelope)) {
          logger.warning("Unrecognized format for message received.");
          continue;
        }
        UDPEnvelope msg = (UDPEnvelope) o;
        UDPEnvelope responseMessage = null;
        if(msg.getSenderID().equals(localID)){
        	responseMessage = new UDPEnvelope(UDPEnvelope.SAME_NODE);
        	responseMessage.setSenderID(localID);
        	messageIO.sendObject(responseMessage, msg.getSenderIP(), msg.getSenderPort());
        	continue;
        }
        if(msg.getTypeOfMessage().equals(UDPEnvelope.CONFIRM_OPEN) || msg.getTypeOfMessage().equals(UDPEnvelope.ALREADY_OPENED)){
        	if(pendingOpenings.containsKey(msg.getURL()))
	        	synchronized (pendingOpenings) {
	        		pendingOpenings.put(msg.getURL(), msg);
	        		pendingOpenings.notifyAll();
				}
        }else if(msg.getTypeOfMessage().equals(UDPEnvelope.DS_OPEN) || msg.getTypeOfMessage().equals(UDPEnvelope.CLIENT_OPEN)){
        	//check whether exists a neighbor for the local node
        	boolean alreadyOpened = proxySet.contains(msg.getSenderID());
            if(alreadyOpened){
          	  responseMessage = new UDPEnvelope(UDPEnvelope.ALREADY_OPENED);
          	  responseMessage.setSenderID(this.localID);
          	  messageIO.sendObject(responseMessage, msg.getSenderIP(), msg.getSenderPort());
          	  continue;
            }
    	  UDPEnvelope confirm = new UDPEnvelope(UDPEnvelope.CONFIRM_OPEN);
          confirm.setSenderID(localID);
          confirm.setSenderIP(localIP);
          confirm.setSenderPort(port);
          confirm.setURL(msg.getURL());
          messageIO.sendObject(confirm, msg.getSenderIP(), msg.getSenderPort());
          // Creates the new neighbor
          int neighborType = msg.getTypeOfMessage().equals(UDPEnvelope.DS_OPEN) ? UDPProxy.BROKER : UDPProxy.CLIENT;
          UDPProxy newNeighbor = new UDPProxy(messageIO, msg.getSenderID(), msg.getSenderIP(), msg.getSenderPort(),
              neighborType);
          proxySet.add(newNeighbor);
          //notify the new connection to the topology manager
          Iterator it = linkOpenedListeners.iterator();
          while(it.hasNext()){
        	  LinkOpenedListener l = (LinkOpenedListener) it.next();
        	  l.signalLinkOpened(msg.getSenderID(), this);
          }
          logger.fine("Connection accepted from "+msg.getSenderID().getID());
        }else if (msg.getTypeOfMessage().equals(UDPEnvelope.BEACON)){
    	  //Just received a beacon, responding
          logger.finer("Responding beacon from "+msg.getSenderID());
          UDPEnvelope beaconAck = new UDPEnvelope(UDPEnvelope.BEACON_ACK);
          beaconAck.setSenderID(localID);
          beaconAck.setSenderIP(localIP);
          beaconAck.setSenderPort(port);
          messageIO.sendObject(beaconAck, msg.getSenderIP(), msg.getSenderPort());  
        }else{
          // Updating last contact times for sending neighbor
          synchronized(proxySet) {
            Collection c = proxySet.getAllProxies();
            Iterator it =  c.iterator();
            while(it.hasNext()) {
              UDPProxy neighbor = (UDPProxy) it.next();
              if(neighbor.getID().equals(msg.getSenderID())
                  &&neighbor.isBroker()) {
                logger.finer("Received a message from: "+neighbor.getID()+" updating lastContact info");
                neighbor.setLastContact(System.currentTimeMillis());
                neighbor.resetLostBeacons();
              }
            }
          }
          if(!msg.getTypeOfMessage().equals(UDPEnvelope.BEACON_ACK)) enqueue(msg);
        }
      } catch(InterruptedIOException e) {
        continue;
      } catch(IOException e) {
        e.printStackTrace();
      }
    }
  }
  public String getIP() {
    return localIP;
  }

  public int getPort() {
    return port;
  }
  /**
   * @see Transport#openLink(String)
   */
  public NodeDescriptor openLinkHelper(String url) throws MalformedURLException, ConnectException, AlreadyExistingLinkException {
	if(!running) return null;
    logger.fine("Opening link to "+url);
    // Parses the URL; accepted urls are in the form "reds-udp:host:port"
    int remotePort;
    String remoteHost;
    StringTokenizer st = new StringTokenizer(url, ":");
    if(st.nextToken().equals("reds-udp")) {
      remoteHost = st.nextToken();
      remotePort = Integer.parseInt(st.nextToken());
    } else throw new MalformedURLException();
    // Send the DS_OPEN message, including the local id
    UDPEnvelope openMessage = new UDPEnvelope(UDPEnvelope.DS_OPEN);
    openMessage.setSenderIP(localIP);
    openMessage.setSenderID(localID);
    openMessage.setSenderPort(port);
    openMessage.setURL(url);
    messageIO.sendObject(openMessage, remoteHost, remotePort);    
	//wait for the confirmation message
    while(pendingOpenings.get(url) == null)
	    synchronized(pendingOpenings) {
	      pendingOpenings.put(url, null);
	      try {
			pendingOpenings.wait(WAITING_TIME);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
    openMessage = (UDPEnvelope) pendingOpenings.get(url);
    
    if(openMessage.getTypeOfMessage().equals(UDPEnvelope.ALREADY_OPENED)){
    	//the proxy already exist, return its id
    	throw new AlreadyExistingLinkException(openMessage.getSenderID());
    }else if(openMessage.getTypeOfMessage().equals(UDPEnvelope.CONFIRM_OPEN)){
    	// An open confirmation, create the new neighbor
        UDPProxy newNeighbor = new UDPProxy(messageIO, openMessage.getSenderID(), openMessage.getSenderIP(), 
        		openMessage.getSenderPort(), UDPProxy.BROKER);
        pendingOpenings.remove(url);
        logger.fine("Link opened with "+openMessage.getSenderID());
        proxySet.add(newNeighbor);
        return newNeighbor.getID();
    }else if(openMessage.getTypeOfMessage().equals(UDPEnvelope.SAME_NODE)){
    	logger.severe("You were trying to connect to the local node");
        throw new ConnectException();
    }else{
    	logger.warning("Unrecognized message");
    	throw new ConnectException();
    }
	  
  }
  protected void closeLinkAck(NodeDescriptor closer) {
	  Proxy closing = (Proxy) proxySet.get(closer);
	  try {
	    closing.sendMessage(Envelope.CLOSE_ACK, null, Transport.MISCELLANEOUS_CLASS);	      
	  } catch(NotConnectedException e) {
	    logger.warning("The " + closer.toString() + " is not connected");
	  }		
  }
  private void linkDead(NodeDescriptor linkDead){
	Iterator it = linkDeadListeners.iterator();
	while(it.hasNext()){
		LinkDeadListener l = (LinkDeadListener)it.next();
		l.signalLinkDead(linkDead);
	}
  }
  
  /**
   * This class implements a <code>Proxy</code> reachable using UDP datagrams.
   * Together with the <code>UDPTransport</code> class it realizes the UDP-based
   * transport layer of a REDS broker. 
   * A beaconing mechanisms to recognize disconnections is also implemented.
   */
  private class UDPProxy implements Proxy {
    // The neighbor is a client
    public static final int CLIENT = 0;
    // The neighbor is a broker
    public static final int BROKER = 1;
    // A refernce to the object in charge of transmitting objects via UDP datagrams
    private DatagramObjectIO messageIO;
    // The IP address of the neighbor
    private String neighborIP;
    // The identifier of the neighbor
    private NodeDescriptor neighborId;
    // The port at which the neighbor waits for incoming messages
    private int neighborPort;
    // The type of neighbor
    private int typeOfNeighbor;
    // A flag indicating whether this neighbor is connected or not 
    private boolean connected;
    // A timestamp indicating when this neighbor has sent the last message towards this host
    private long lastContact;
    // The number of lost beacons so far
    private int lostBeacons;
    // A reference to the logger
    private Logger logger;
    /**
     * Creates a new <code>UDPProxy</code>.
     * @param messageIO the UDP channel it uses
     * @param neighborId the id of the remote node
     * @param neighborIP the IP of the remote node
     * @param neighborPort the port of the remote node
     * @param typeOfNeighbor the remote node is a client or a broker
     */
    public UDPProxy(DatagramObjectIO messageIO, NodeDescriptor neighborId, String neighborIP, int neighborPort, int typeOfNeighbor) {
      this.messageIO = messageIO;
      this.neighborIP = neighborIP;
      this.neighborId = neighborId;
      this.neighborPort = neighborPort;
      this.lastContact = System.currentTimeMillis();
      this.lostBeacons = 0;
      this.typeOfNeighbor = typeOfNeighbor;
      connected = true;
      logger = Logger.getLogger("polimi.reds.transport");
    }

    public void disconnect() {
      connected = false;
      UDPEnvelope envelope = new UDPEnvelope(UDPEnvelope.CLOSE);
      envelope.setSenderID(localID);
      messageIO.sendObject(envelope, neighborIP, neighborPort);
    }
    /**
     * @see Proxy#isBroker()
     */
    public boolean isBroker() {
      return (typeOfNeighbor==BROKER);
    }
    /**
     * @see Proxy#isClient()
     */
    public boolean isClient() {
      return (typeOfNeighbor==CLIENT);
    }
    /**
     * @see Proxy#getID()
     */
    public NodeDescriptor getID() {
      return neighborId;
    }
    /**
     * @see Proxy#isConnected()
     */
    public boolean isConnected() {
      return connected;
    }

    protected long getLastContact() {
      return lastContact;
    }

    protected void setLastContact(long lastContact) {
      this.lastContact = lastContact;
    }

    protected int getLostBeacons() {
      return lostBeacons;
    }

    protected void resetLostBeacons() {
      lostBeacons = 0;
    }

    protected void brutalDisconnect() {
      if(connected) {
        connected = false;
        logger.warning("Proxy "+neighborId+" brutally disconnected.");
        linkDead(neighborId);
      }
    }
    /**
     * @see Proxy#sendMessage(String, Serializable)
     */
	public void sendMessage(String subject, Serializable payload, String trafficClass) throws NotConnectedException {
		if(connected) {
	        UDPEnvelope envelope = new UDPEnvelope(subject, payload, trafficClass);
	        envelope.setSenderID(localID);
	        envelope.setSenderIP(UDPTransport.this.getIP());
	        envelope.setSenderPort(UDPTransport.this.getPort());
	        messageIO.sendObject(envelope, neighborIP, neighborPort);
	        if(subject == UDPEnvelope.BEACON)
	        	lostBeacons++;
	      }
		
	}
	public String toString(){
		return this.neighborId.getID();
	}
  }//end class UDPProxy
}//end class UDPTransport
