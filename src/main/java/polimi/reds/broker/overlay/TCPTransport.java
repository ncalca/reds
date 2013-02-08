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

import java.net.*;
import java.util.*;
import java.util.logging.Logger;
import java.io.*;
import polimi.reds.NodeDescriptor;
import polimi.reds.NotConnectedException;
import polimi.reds.broker.overlay.Proxy;

/**
 * Implements the <code>Transport</code> interface using TCP connections.
 */
public class TCPTransport extends AbstractTransport {
  /**
   * the port used to accept new connections
   */
  private int port;

  /**
   * Create a new <code>Transport</code>.
   * 
   * @param port
   */
  public TCPTransport(int port) {
    this.port = port;
    logger = Logger.getLogger("polimi.reds.transport");
  }

  /**
   * Get the local reds URL.
   */
  public String getURL() {
    String localurl = null;
    try {
      localurl = "reds-tcp:"+InetAddress.getLocalHost().getHostAddress()+":"
          +port;
    } catch(UnknownHostException ex) {
      logger.warning("The localhost is unknown");
      ex.printStackTrace();
    }
    return localurl;
  }

  /**
   * @see Transport#openLink(String) The protocol to open a connection is:<br>
   *      send a <code>DS_OPEN</code> message <br>
   *      if the received message is <code>CONFIRM_OPEN</code> then the
   *      connection is completed;<br>
   *      if the received message is <code>ALREADY_OPENED</code> then the
   *      connection alreay exists between the two nodes and an
   *      <code>AlreadyExistingLinkException</code> is thrown; else if the
   *      received message is <code>SLAVE</code> then the local node must
   *      check whether the remote node is already a neighbor. In this case it
   *      throws an <code>AlreadyExistingLinkException</code>. Else it starts
   *      a new <code>Proxy</code> to accept messages from the remote
   *      neighbor.
   */
  public NodeDescriptor openLinkHelper(String url) throws MalformedURLException,
      ConnectException, AlreadyExistingLinkException {
    if(!running) {
    	logger.finer("returning null because we are not running");
    	return null;
    }
    logger.fine("Opening connection to "+url);
    String remoteHost = null;
    int remotePort = 0;
    Socket sock;
    REDSMarshaller marshallerToDS;
    REDSUnmarshaller unmarshallerToDS;
    /*
     * Parse the URL. Accepted urls are in the form "reds-tcp:host:port"
     */
    String[] parts = url.split(Transport.URL_SEPARATOR);
    if(parts[0].equals("reds-tcp")) {
      remoteHost = parts[1];
      remotePort = Integer.parseInt(parts[2]);
    } else throw new MalformedURLException();
    // Try to open the socket and the input and output streams at
    // remoteHost:remotePort
    try {
    	logger.finer("creating new socket to "+remoteHost);
         
      //sock = new Socket(remoteHost, remotePort);
      sock = new Socket();
      sock.connect(new  InetSocketAddress(remoteHost, remotePort), 2000);
      
//      logger.finer("Socket timeout was "+sock.getSoTimeout());
      sock.setSoTimeout(TCPProxy.SOCKET_TIMEOUT*2);
      marshallerToDS = new REDSMarshaller(new BufferedOutputStream(sock
          .getOutputStream()));
      unmarshallerToDS = new REDSUnmarshaller(new BufferedInputStream(sock
          .getInputStream()));
    } catch(Exception e) {
    	e.printStackTrace();
    	logger.finer("exception when creating socket: "+e);
      ConnectException ex = new ConnectException("Error connecting to "
          +remoteHost+":"+remotePort);
      ex.initCause(e);
      throw ex;
    }
    // Send the DS_OPEN message, including the local id
    TCPEnvelope openMessage = new TCPEnvelope(TCPEnvelope.DS_OPEN);
    openMessage.setSenderID(localID);
    sendHandShakingMsg(openMessage, marshallerToDS, url);
    // Wait for the confirmation from the other broker
    Envelope confirmMessage = null;
    try {
      confirmMessage = (Envelope) unmarshallerToDS.readObject();
    } catch(Exception e) {
      ConnectException ex = new ConnectException(
          "Error receiving confirm_open from "+remoteHost+":"+remotePort);
      ex.initCause(e);
      logger.fine(e.getMessage());
      
      throw ex;
    }
    if(confirmMessage.equals(Envelope.SAME_NODE)) { throw new ConnectException(
        "You are connecting to yourself!"); }
    Envelope lastMsg = null;
    TCPProxy newNeighbor = null;
    if(confirmMessage.getTypeOfMessage().equals(Envelope.SLAVE)) {
      synchronized(proxySet) {
        if(proxySet.contains(confirmMessage.getSenderID())) {
          lastMsg = new Envelope(Envelope.ALREADY_OPENED);
          lastMsg.setSenderID(localID);
          sendHandShakingMsg(lastMsg, marshallerToDS, url);
          logger.finer("already opened");
          throw new AlreadyExistingLinkException(confirmMessage.getSenderID());
        } else {
          newNeighbor = createNeighbor(TCPProxy.BROKER, confirmMessage
              .getSenderID(), sock, marshallerToDS, unmarshallerToDS);
          proxySet.add(newNeighbor);
//        DAVIDE ADDING signallinkOpened
  //        signalLinkOpenedListeners(confirmMessage.getSenderID());
          
          newNeighbor.startProcessing();
          lastMsg = new Envelope(Envelope.CONFIRM_OPEN);
          lastMsg.setSenderID(localID);
          logger.fine("sending cofirmopen");
          sendHandShakingMsg(lastMsg, marshallerToDS, url);
          
        }
      }
    } else if(confirmMessage.getTypeOfMessage().equals(Envelope.ALREADY_OPENED)) {
        logger.finer("already opened");
      throw new AlreadyExistingLinkException(confirmMessage.getSenderID());
    } else {
      synchronized(proxySet) {
        newNeighbor = createNeighbor(TCPProxy.BROKER, confirmMessage
            .getSenderID(), sock, marshallerToDS, unmarshallerToDS);
        proxySet.add(newNeighbor);
        logger.finer("new neighbor");

//      DAVIDE ADDING signallinkOpened 
 //       signalLinkOpenedListeners(confirmMessage.getSenderID());
        newNeighbor.startProcessing();
        
      }
    }
    logger.fine("Connection to "+url);
    return newNeighbor.getID();
  }

  private TCPProxy createNeighbor(String neighborType,
      NodeDescriptor nodeDescriptor, Socket sock, REDSMarshaller marshaller,
      REDSUnmarshaller unmarshaller) {
    TCPProxy newNeighbor = new TCPProxy(neighborType, nodeDescriptor, sock,
        marshaller, unmarshaller);
    return newNeighbor;
  }

  private void sendHandShakingMsg(Envelope env, REDSMarshaller marshaller,
      String url) throws ConnectException {
    try {
      marshaller.writeObject(env);
      marshaller.flush();
      marshaller.reset();
    } catch(Exception e) {
      ConnectException ex = new ConnectException("Error sending the "
          +env.getTypeOfMessage()+" to "+url);
      ex.initCause(e);
      throw ex;
    }
  }

  private Envelope receiveHandshakingMsg(REDSUnmarshaller unmarshaller)
      throws ConnectException {
    try {
      return (Envelope) unmarshaller.readObject();
    } catch(Exception e) {
      ConnectException ex = new ConnectException("Error receiving envelope");
      ex.initCause(e);
      throw ex;
    }
  }

  /**
   * Start the accepting connection service at the local port.
   * 
   * @see AbstractTransport#start()
   */
  public synchronized void start() {
    logger.config("Starting TCPTransport");
    try {
      super.start();
      // Start the accepting thread
      Thread acceptingThread = new Thread() {
        public void run() {
          accept();
        }
      };
      acceptingThread.setDaemon(false);
      acceptingThread.setName("TCPTransport.acceptingThread");
      acceptingThread.start();
    } catch(Exception e) {
      // FIXME: Manage this exception in a better way.
      e.printStackTrace();
      System.err.println("Error: impossible to start the transport.");
      System.exit(-1);
    }
    logger.config("TCPTransport started at port "+port);
  }

  /**
   * Close the broker. Before closing notify all connected TCPProxies.
   */
  public synchronized void stop() {
    logger.fine("Stopping TCPTransport");
    running = false;
    Iterator it = proxySet.getAllProxies().iterator();
    Object o;
    TCPProxy neighbor;
    while(it.hasNext()) {
      o = it.next();
      if(o instanceof TCPProxy) {
        neighbor = (TCPProxy) o;
        neighbor.disconnect();
      }
    }
    super.stop();
    logger.config("TCPTransport stopped");
    // clear the two queues
    proxySet.clear();
  }

  // Main loop to accept new connections
  private void accept() {
    logger.config("Accepting thread for TCPTransport at port "+port+" started");
    ServerSocket ss = null;
    Socket sock = null;
    REDSMarshaller marshaller = null;
    REDSUnmarshaller unmarshaller;
    try {
      ss = new ServerSocket(port);
      ss.setSoTimeout(500);
    } catch(Exception e) {
      // FIXME: Manage this exception in a better way.
      System.err.println("Error creating the server socket.");
      e.printStackTrace();
      return;
    }
    while(running) {
      unmarshaller = null;
      try {
        sock = ss.accept();
      } catch(InterruptedIOException e) { // timeout expires
        continue;
      } catch(Exception e) { // I/O or socket error
    	logger.finer("Socket error "+e);
        running = false;
        e.printStackTrace();
        continue;
      }
      logger.fine("Connection request received from "
          +sock.getInetAddress().getHostAddress()+":"+sock.getPort());
      try {
        marshaller = new REDSMarshaller(new BufferedOutputStream(sock
            .getOutputStream()));
        unmarshaller = new REDSUnmarshaller(new BufferedInputStream(sock
            .getInputStream()));
      } catch(Exception e) {
        // FIXME: Manage this exception in a better way.
        System.err.println("Error creating the socket streams!");
        e.printStackTrace();
        continue;
      }
      // Wait for the OPEN message from the neighbor
      TCPEnvelope openMessage = null;
      String neighborType = null;
      try {
        openMessage = (TCPEnvelope) unmarshaller.readObject();
      } catch(Exception e) {
        logger.warning("Error reading the open message!");
        e.printStackTrace();
        continue;
      }
      if(openMessage.getSenderID().equals(localID)) {
        TCPEnvelope responseMessage = new TCPEnvelope(TCPEnvelope.SAME_NODE);
        responseMessage.setSenderID(localID);
        try {
          sendHandShakingMsg(responseMessage, marshaller, "reds-tcp:"
              +sock.getInetAddress().getHostAddress()+":"+sock.getPort());
        } catch(Exception e) {
          logger.warning("Error sending the same_node message!");
          continue;
        }
        continue;
      }
      if(openMessage.getTypeOfMessage().equals(TCPEnvelope.DS_OPEN)) {
        neighborType = TCPProxy.BROKER;
      } else if(openMessage.getTypeOfMessage().equals(TCPEnvelope.CLIENT_OPEN)) {
        neighborType = TCPProxy.CLIENT;
      } else {
        logger.warning("Unknown TCPEnvelope");
        continue;
      }
      TCPProxy newNeighbor = null;
      Envelope response = null;
      if(neighborType == TCPProxy.BROKER && (localID.compareTo(openMessage.getSenderID())<0)) {
        response = new Envelope(Envelope.SLAVE);
        response.setSenderID(localID);
        try {
          sendHandShakingMsg(response, marshaller, "reds-tcp:"
              +sock.getInetAddress().getHostAddress()+":"+sock.getPort());
        } catch(ConnectException e) {
          logger.warning("Connect exception replying to openLink coming from "
              +openMessage.getSenderID().getID());
        }
        try {
          response = receiveHandshakingMsg(unmarshaller);
        } catch(Exception e) {
          logger.warning("Error receiving the second message from "+"reds-tcp:"
              +sock.getInetAddress().getHostAddress()+":"+sock.getPort());
          continue;
        }
        if(response.getTypeOfMessage().equals(Envelope.ALREADY_OPENED)) {
          continue;
        } else if(response.getTypeOfMessage().equals(Envelope.CONFIRM_OPEN)) {
          synchronized(proxySet) {
            newNeighbor = createNeighbor(neighborType, response.getSenderID(),
                sock, marshaller, unmarshaller);
            proxySet.add(newNeighbor);
            NodeDescriptor neighborId=openMessage.getSenderID();         
            signalLinkOpenedListeners(neighborId);
            newNeighbor.startProcessing();
          }
        }
      } else {
        synchronized(proxySet) {
          if(proxySet.contains(openMessage.getSenderID())) {
            response = new Envelope(Envelope.ALREADY_OPENED);
            response.setSenderID(localID);
            try {
              sendHandShakingMsg(response, marshaller, "reds-tcp:"
                  +sock.getInetAddress().getHostAddress()+":"+sock.getPort());
            } catch(ConnectException e) {
              e.printStackTrace();
              continue;
            }
          } else {
            response = new Envelope(Envelope.CONFIRM_OPEN);
            response.setSenderID(localID);
            newNeighbor = createNeighbor(neighborType, openMessage
                .getSenderID(), sock, marshaller, unmarshaller);
            proxySet.add(newNeighbor);
            try {
              sendHandShakingMsg(response, marshaller, "reds-tcp:"
                  +sock.getInetAddress().getHostAddress()+":"+sock.getPort());
            } catch(ConnectException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            }
            NodeDescriptor neighborId=openMessage.getSenderID();
            signalLinkOpenedListeners(neighborId);
            newNeighbor.startProcessing();
          }
        }
      }
    } // end while(connected)
    try {
      ss.close();
    } catch(IOException ex) {
      // FIXME: Manage this exception in a better way.
      ex.printStackTrace();
    }
    ss = null;
  } // end run()

private void signalLinkClosedListeners(NodeDescriptor neighborId) {
	List toIterate;
	synchronized(linkClosedListeners){
		toIterate=new ArrayList(linkClosedListeners);
	}
	Iterator it = toIterate.iterator();
	while (it.hasNext()) {
		LinkClosedListener l = (LinkClosedListener) it.next();
		l.signalLinkClosed(neighborId);
	}
}
private void signalLinkOpenedListeners(NodeDescriptor neighborId) {
	List toIterate;
	synchronized(linkOpenedListeners){
		toIterate=new ArrayList(linkOpenedListeners);
	}
	Iterator it = toIterate.iterator();
	while(it.hasNext()) {
	  LinkOpenedListener l = (LinkOpenedListener) it.next();
	  l.signalLinkOpened(neighborId, this);
	}
}

  /**
   * @see AbstractTransport#closeLinkAck(NodeDescriptor)
   */
  protected void closeLinkAck(NodeDescriptor closer) {
	Proxy closing;
	synchronized(proxySet){
       closing = (Proxy) proxySet.get(closer);
	}
    try {
      closing.sendMessage(Envelope.CLOSE_ACK, null,
          Transport.MISCELLANEOUS_CLASS);
      ((TCPProxy) closing).closeStreams();
    } catch(NotConnectedException e) {
      logger.warning("The "+closer.toString()+" is not connected");
    }
  }

	protected void closeLinkHelper(NodeDescriptor neighborID){
		logger.fine("Closing link to neighbor "+neighborID);
		TCPProxy neighbor = (TCPProxy) proxySet.get(neighborID);
		neighbor.sendClose();
	}
  
  private void linkDead(NodeDescriptor linkDead) {
    logger.finer("DEAD Link to "+linkDead);
    List toIterate;
    synchronized (linkDeadListeners){
    	toIterate=new ArrayList(linkDeadListeners);
    }
    Iterator it = toIterate.iterator();
    while(it.hasNext()) {
      LinkDeadListener l = (LinkDeadListener) it.next();
      l.signalLinkDead(linkDead);
    }
  }

  /**
   * This class implements a <code>Proxy</code> reachable through a TCP link.
   * Together with the <code>TCPTransport</code> class it realizes the
   * TCP-based transport layer of a REDS broker. In case a beaconing mechanism
   * is needed, the blocking calls for receiving data from the socket will
   * return after SOCKET_TIMEOUT ms. If the socket timeouts for more than
   * TIMEOUT_THRESHOLD times, then a beacon is sent and this hosts waits for
   * ACK_WAIT*SOCKET_TIMEOUT ms an ACK back. If no ACK is received within that
   * period, then this neighbor is considered no longer connected and an
   * exception is raised.
   */
  private class TCPProxy implements Proxy, Runnable {
    /**
     * The neighbor is a client.
     */
    public static final String CLIENT = "client";
    /**
     * The neighbor is a broker.
     */
    public static final String BROKER = "broker";
    // Indicates the timeout in ms for receiving data
    private static final int SOCKET_TIMEOUT = 6000;
    private static final int BEACON_INTERVAL = 2000;
    
    // Indicates the number of times the socket can timeout before
    // sending a beacon to the neighbor for checking its reachability
    private static final int TIMEOUT_THRESHOLD = 3;
    // Indicates how may times the local broker will retry to check
    // the reachability of the neighbor.
    private static final int BEACON_RETRIES = 3;
    protected Socket sock;
    protected REDSMarshaller marshaller; // Output-stream of the neighbor
    protected REDSUnmarshaller unmarshaller; // Input-stream of the neighbor
    protected Thread readingThread; // the thread in charge of reading incoming messages
    protected String typeOfNeighbor; // Type of the neighbor (BROKER or
    // client)
    protected NodeDescriptor id; // ID of this neighbor (assigned by parent)
    protected boolean connected;
    private Logger logger;

    
    protected Date lastSent;
    protected Thread beaconingThread;
    
    public TCPProxy(String typeOfNeighbor, NodeDescriptor id, Socket sock,
        REDSMarshaller marshaller, REDSUnmarshaller unmarshaller) {
      this.lastSent=new Date();
      this.typeOfNeighbor = typeOfNeighbor;
      this.id = id;
      this.sock = sock;
      this.marshaller = marshaller;
      this.unmarshaller = unmarshaller;
      connected = true;
      try {
        if(typeOfNeighbor.equals(BROKER)&&TCPTransport.this.beaconing)
          sock.setSoTimeout(SOCKET_TIMEOUT);
      } catch(SocketException e) {
        logger.severe("Error while setting timeout on socket to "+id);
      }
      logger = Logger.getLogger("polimi.reds.transport");
      // Create the reading thread associated with this neighbor
      readingThread = new Thread(this);
      readingThread.setDaemon(true);
      readingThread.setName("TCPProxy."+id.getID());
      
    
      beaconingThread=new Thread(){
    	  public void run(){
    		  while (connected && TCPTransport.this.running){
    			  synchronized(lastSent){
    				  Date now=new Date();
    				  if (now.getTime()-lastSent.getTime()>=BEACON_INTERVAL)
    					  checkNeighbor();
    				  try {
    					  sleep(BEACON_INTERVAL);
    				  } catch (InterruptedException e) {
    					  // TODO Auto-generated catch block
    					  e.printStackTrace();
    				  }
    			  }
    		  }
    	  }
      };
    }

    synchronized void startProcessing() {
      readingThread.start();
      if(beaconing) 
    	  beaconingThread.start();
    }
    
    public synchronized void sendClose() {
      // Inform broker that the link is being closed
      TCPEnvelope disconnect = new TCPEnvelope(TCPEnvelope.CLOSE);
      try {
        marshaller.writeObject(disconnect);
        marshaller.flush();
        marshaller.reset();
      } catch(Exception e) {
        System.err.println("Error sending the close message to "+id);
        e.printStackTrace();
      }
      
    }
    public synchronized void disconnect() {
        // Inform broker that the link is being closed
        connected = false;
        System.out.println("disconnected link to "+id);
      }
    void closeStreams() {
      try {
        marshaller.close();
        unmarshaller.close();
        sock.close();
      } catch(Exception e) {
        System.err.println("Impossible to close the socket's streams");
        e.printStackTrace();
      }
      // marshaller = null;
      // unmarshaller = null;
      // sock = null;
    }

    public boolean isBroker() {
      return typeOfNeighbor.equals(TCPProxy.BROKER);
    }

    public boolean isClient() {
      return typeOfNeighbor.equals(TCPProxy.CLIENT);
    }

    public NodeDescriptor getID() {
      return id;
    }

    public boolean isConnected() {
      return connected;
    }

//    public void run() {
//      if(TCPTransport.this.beaconing) beaconRun();
//      else normalRun();
//    }

    /**
     * Main reading loop without beaconing.
     */
//    private void normalRun() {
//      logger.config("Reading thread for neighbor "+id+" started");
//      TCPEnvelope received;
//      TCPEnvelope lastReceived = null;
//      while(connected&&TCPTransport.this.running) {
//        try {
//          received = (TCPEnvelope) unmarshaller.readObject();
//          received.setSenderID(id);
//          if(received.getTypeOfMessage().equals(TCPEnvelope.CLOSE_ACK)) {
//            closeStreams();
//            break;
//          }
//          if(received.getTypeOfMessage().equals(TCPEnvelope.CLOSE)) {
//        	  synchronized(proxySet){//TODO: make sure this is right
//        	  if (mayCloseLink(received.getSenderID())){
//				closeLinkAck(received.getSenderID());
//				Iterator it = linkClosedListeners.iterator();
//				while (it.hasNext()) {
//					LinkClosedListener l = (LinkClosedListener) it.next();
//					l.signalLinkClosed(received.getSenderID());
//				}
//				proxySet.remove(received.getSenderID());
//        	  }
//        	  break;
//        	  }
//          }
//          enqueue(received);
//          lastReceived = received;
//          logger.finer("At " +(new Date())+ " Proxy received message "+received.getTypeOfMessage()+": "+received.getPayload()+" from "+ id.getUrls()[0]);
//          
////          if(received.getTypeOfMessage().equals(TCPEnvelope.CLOSE)) {
////            break;
////          }
//        } catch(SocketTimeoutException e) {
//        	System.err.println("timeout for link to "+ id.getUrls()[0] +" connected "+connected);
//            e.printStackTrace();
//        } catch(Exception e) {
//        	System.err.println("At "+(new Date())+" exception for link to "+id.getUrls()[0] +" connected "+connected);
//          e.printStackTrace();
//          brutalDisconnect(lastReceived);
//          break;
//        }
//      }// end while(true)
//      logger.config("Reading thread for neighbor "+id+" ended");
//      
//    }

    /**
     * Main reading loop with beaconing.
     */
    public void run() {
    	TCPEnvelope received = null;
    	TCPEnvelope lastReceived = null;
    	int timeoutCounter = 0;
    	logger.config("Reading thread for neighbor "+id
    			+" started with socket timeout "+SOCKET_TIMEOUT
    			+" and beacon timeout "+SOCKET_TIMEOUT*TIMEOUT_THRESHOLD);
    	while(connected&&TCPTransport.this.running) {
    		try {
    			logger.finer("waiting for message from "+id);
    			
    			received = (TCPEnvelope) unmarshaller.readObject();
    			timeoutCounter = 0;
    			received.setSenderID(id);
    			logger.finer("got message from "+id+": "+received.getTypeOfMessage());
    			if(received.getTypeOfMessage()==TCPEnvelope.BEACON) {
    				// We are just happy 
//    				replyCheckNeighbor();//LUCA
    			} else if(received.getTypeOfMessage()==TCPEnvelope.DEAD) {
    				logger.finer("DEAD ENVELOPE: brutal disconnect to: "+id);
    				brutalDisconnect(received);
    			} else if(received.getTypeOfMessage()==TCPEnvelope.BEACON_ACK) {
    				//WHY????  
    			} else if(received
    					.getTypeOfMessage().equals(TCPEnvelope.CLOSE_ACK)) {
    				disconnect();
    				closeStreams();
    				//DAVIDE Added signalLinkClosedListeners but not sure it should go here
					//signalLinkClosedListeners(received.getSenderID());
    				proxySet.remove(received.getSenderID());
					
    				break;
    			} else if(received.getTypeOfMessage().equals(TCPEnvelope.CLOSE)) {
    				logger.finer("got CLOSE");

					
    				if (mayCloseLink(received.getSenderID())){
    					logger.finer("sending closeAck");
    					closeLinkAck(received.getSenderID());
    					logger.finer("closeAck sent");
    					//Should we call disconnect and closeStreams here????
    					//ASK Gianpaolo, Alessandro.
    					signalLinkClosedListeners(received.getSenderID());
    					proxySet.remove(received.getSenderID());
        				break;
    				}   					

					
    			} else
    				  enqueue(received);
    			lastReceived = received;
    		} catch(SocketTimeoutException e) {
    		//	logger.warning("Timeout exception for "+id.getUrls()[0]);
    		//	System.err.println("Timeout Exception possibly causing brutalDisconnect:");
    		//	e.printStackTrace();
//       			if(beaconing){
//    				timeoutCounter++;
//    				if(timeoutCounter>TIMEOUT_THRESHOLD
//    						&&timeoutCounter<TIMEOUT_THRESHOLD+BEACON_RETRIES) {
//    					// Checking the reachability of the neighbor
//    					checkNeighbor();
//    				} else if(timeoutCounter>TIMEOUT_THRESHOLD+BEACON_RETRIES) {
//    					// The neighbor is no longer connected.
//    					brutalDisconnect(lastReceived);
//    					break;
//    				}
//    			}
//    			
    			if(beaconing){
//    		        try {
//						sendMessage(Envelope.DEAD, null,
//						        Transport.MISCELLANEOUS_CLASS);
//					} catch (NotConnectedException e1) {
//						// TODO Auto-generated catch block
//						e1.printStackTrace();
//					}

    				logger.finer("socket timed out: brutal discconnect to: "+id);
    				brutalDisconnect(lastReceived);
    				break; 				
    			}
    			else {
    		//		e.printStackTrace();
    			}
    		} catch(Exception e) {
    			System.err.println("Exception causing brutalDisconnect:");
    			e.printStackTrace();
    			logger.finer("exception: "+e+" causing brutalDisconnect to "+id);
    			brutalDisconnect(lastReceived);
    			break;
    		}
    	} // end while(true)
    }

    private void brutalDisconnect(TCPEnvelope lastReceivedEnvelope) {
      if(connected==true
          &&TCPTransport.this.running==true
          &&unmarshaller!=null){
//          &&(lastReceivedEnvelope==null||!lastReceivedEnvelope
//              .getTypeOfMessage().equals(TCPEnvelope.CLOSE))) {
        connected = false;
        logger.warning("Proxy to "+id+" brutally disconnected.");
        //CHECK: the order of the following calls.
        closeStreams();//Ask/check if this is necessary
        linkDead(id);
        proxySet.remove(id);
        // REPLACED BY THE ABOVE:
//        if(lastReceivedEnvelope!=null) linkDead(lastReceivedEnvelope
//            .getSenderID());
//        else linkDead(null);
      } else
    	  logger.warning("Ignoring brutal disconnect to "+id+". unmarshaller="+unmarshaller+" running:"+TCPTransport.this.running+" connected:"+connected);
      
    }

    private synchronized void checkNeighbor() {
      logger.warning("Checking the reachability of "+id);
      TCPEnvelope bcn = new TCPEnvelope(TCPEnvelope.BEACON);
      try {
        marshaller.writeObject(bcn);
        marshaller.flush();
        marshaller.reset();
      } catch(IOException e) {
        e.printStackTrace();
      }
    }

    private void replyCheckNeighbor() {
      logger.finer("Reachability acknowledged for "+id);
      TCPEnvelope ack = new TCPEnvelope(TCPEnvelope.BEACON_ACK);
      try {
        marshaller.writeObject(ack);
        marshaller.flush();
        marshaller.reset();
      } catch(IOException e) {
        e.printStackTrace();
      }
    }

    public synchronized void sendMessage(String subject, Serializable payload,
        String trafficClass) throws NotConnectedException {
      if(connected) {
    	  lastSent=new Date();
        TCPEnvelope fw = new TCPEnvelope(subject, payload, trafficClass);
        fw.setSenderID(localID);
        try {
          marshaller.writeObject(fw);
          marshaller.flush();
          marshaller.reset();
          if (payload!=null)
        	  logger.finer("Proxy sending message "+subject+" "
                  +payload.toString()+" to "+this.id.toString());
          else
        	  logger.finer("Proxy sending message "+subject+" to "+this.id.toString());
              
        } catch(Exception e) {
          if(payload!=null)
            logger.severe("Error in forwarding message "+subject+" "
                +payload.toString()+" to "+this.id.toString());

          	logger.severe("Exception is "+e.getMessage());
          	e.printStackTrace();
        }
      } else {
    	  logger.finer("proxy not connected when sending message "+payload+ " to  "+id.getUrls()[0]);
    	  throw new NotConnectedException();
      }
    }

    public String toString() {
      return this.id.getID();
    }
  }// end class Proxy
} // end TCPTransport class
