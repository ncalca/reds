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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.Preferences;
import polimi.util.DatagramObjectIO;

/**
 * This class implements an application level tree overlay manager for MANET
 * environments. The necessary procedures for maintaining the interconnection
 * topology are derived from the MAODV protocol with some adaptations and
 * modifications. For further information about MAODV see: Elizabeth M. Royer
 * and Charles E. Perkins. "Multicast Operation of the Ad hoc On-Demand Distance
 * Vector Routing Protocol." Proceedings of MobiCom '99, Seattle, WA, August
 * 1999, pp. 207-218. and Elizabeth M. Royer and Charles E. Perkins. "Multicast
 * Ad hoc On Demand Distance Vector (MAODV) Routing". IETF Internet Draft,
 * draft-ietf-manet-maodv-00.txt, July 2000 (Work in Progress). Since all
 * transmissions are inherently broadcast at the physical layer, all the
 * messages are always sent using UDP datagrams to the IPv4 local broadcast
 * address while suitable fields in the messages are used to keep track of the
 * intended receivers at the application level. This can be useful in managing
 * the Group Hello messages that are multicast from a given upstream node to all
 * the downstream nodes of it. These lasts constitute the majority of message
 * traffic used for maintaining the acyclic topology.
 */
public class ManetTreeOverlayMgr implements Runnable{
  /**
   * Indicates the size in byte of a single UDP datagram.
   */
  private final int BYTE_BUFFER_SIZE;
  /**
   * Indicates the time (in ms) that the local host will wait before declaring
   * that no other connections to the overlay have been found.
   */
  private final int DISCOVER_TIMEOUT;
  /**
   * Indicates the time (in ms) that the system will wait for control messages.
   */
  private final int WAIT_INTERVAL;
  /**
   * Indicates the time (in ms) that will elapse between two consecutive check
   * for the last Group Hello message received. If this host is also the group
   * leader, then this value indicates the time between two consecutive sends of
   * the group hello message.
   */
  private final int HELLO_INTERVAL;
  /**
   * If this node is the group leader, every RECONNECTION_TRIGGER group hello
   * messages a RREQ message with the reconnection flag set will be flooded
   * looking for other (disconnected) portions of the tree.
   */
  private final int RECONNECTION_TRIGGER;
  /**
   * Indicates the number of lost Hello messages that the overlay manager
   * tolerates before starting a new Group Leader election.
   */
  private final int ALLOWED_HELLO_LOSS;
  /**
   * Describes the tolerance in selecting the best reply among the ones
   * received.
   */
  private final int GROUP_SEQUENCE_TOLERANCE;
  /**
   * Indicates the value of retries looking for a new connection to the overlay
   * network.
   */
  private final int REQUEST_RETRIES;
  /**
   * Indicates the increment of the TTL value in each RREQ message.
   */
  private final int TTL_INCREMENT;
  /**
   * Indicates the value for limiting the forwarding of RREQ messages in
   * throughout the network.
   */
  private final int TTL_THRESHOLD;
  /**
   * Default value for <code> BYTE_BUFFER_SIZE </code>
   */
  private static final int DEF_BYTE_BUFFER_SIZE = 16384;
  /**
   * Default value for <code> DISCOVER_TIMEOUT </code>
   */
  private static final int DEF_DISCOVER_TIMEOUT = 1500;
  /**
   * Default value for <code> WAIT_INTERVAL </code>
   */
  private static final int DEF_WAIT_INTERVAL = 200;
  /**
   * Default value for <code> HELLO_INTERVAL </code>
   */
  private static final int DEF_HELLO_INTERVAL = 4000;
  /**
   * Default value for <code> RECONNECTION_TRIGGER </code>
   */
  private static final int DEF_RECONNECTION_TRIGGER = 1;
  /**
   * Default value for <code> ALLOWED_HELLO_LOSS </code>
   */
  private static final int DEF_ALLOWED_HELLO_LOSS = 3;
  /**
   * Default value for <code> GROUP_SEQUENCE_TOLERANCE </code>
   */
  private static final int DEF_GROUP_SEQUENCE_TOLERANCE = 2;
  /**
   * Default value for <code> REQUEST_RETRIES </code>
   */
  private static final int DEF_REQUEST_RETRIES = 3;
  /**
   * Default value for <code> TTL_INCREMENT </code>
   */
  private static final int DEF_TTL_INCREMENT = 3;
  /**
   * Default value for <code> TTL_THRESHOLD </code>
   */
  private static final int DEF_TTL_THRESHOLD = 8;
  private final static String XML_CONFIG_PATH = "polimi/manetOverlayMgr/OverlayManagerParam.xml";
  // String descriptors for system parameters.
  private static final String BYTE_BUFFER_SIZE_XMLFIELD = "BYTE_BUFFER_SIZE";
  private static final String DISCOVER_TIMEOUT_XMLFIELD = "DISCOVER_TIMEOUT";
  private static final String WAIT_INTERVAL_XMLFIELD = "WAIT_INTERVAL";
  private static final String HELLO_INTERVAL_XMLFIELD = "HELLO_INTERVAL";
  private static final String RECONNECTION_TRIGGER_XMLFIELD = "RECONNECTION_TRIGGER";
  private static final String ALLOWED_HELLO_LOSS_XMLFIELD = "ALLOWED_HELLO_LOSS";
  private static final String GROUP_SEQUENCE_TOLERANCE_XMLFIELD = "GROUP_SEQUENCE_TOLERANCE";
  private static final String REQUEST_RETRIES_XMLFIELD = "REQUEST_RETRIES_TOLERANCE";
  private static final String TTL_INCREMENT_XMLFIELD = "TTL_INCREMENT";
  private static final String TTL_THRESHOLD_XMLFIELD = "TTL_THRESHOLD";
  // A reference to the application.
  private WirelessTopologyManager topMgr = null;
  // The current broadcast id for this host.
  private long broadcastId;
  // The number of lost hello messages so far.
  private int lostHelloMessages;
  // The system time at which the last hello message has been received.
  private long lastHelloMessage;
  // The last known group sequence number.
  private long groupSequenceNumber;
  // Is true if the local host is serving as the group leader.
  private boolean groupLeader;
  // The current distance in hops to the current group leader.
  private int hopToGroupLeader;
  // The id of the current group leader.
  private String groupLeaderId;
  // A Flag used for reconnecting network partitions: only one reconnection
  // procedure at a time can be carried out.
  private boolean reconnecting;
  // The id of the current upstream node, this is derived from
  // the id of the host that forwards the hello message.
  private String upstreamNodeId;
  // A buffer of data (messages and info on forward and reverse link setup).
  private dataBuffer dataBuff;
  // Is true if the overlay manager is running.
  private boolean running;
  // A reference to the Thread in charge of parsnig the received messages.
  private parsingThread parsing;
  // A reference to the Thread in charge of receiving messages and buffering
  // them.
  private readingThread reading;
  // A reference to an object in charge of marshalling and unmarshalling
  // objects for transmission using UDP datagrams.
  private DatagramObjectIO messageIO;
  // The ID of the local host.
  private String hostId;
  // Logger facility.
  private Logger logger;
  // The UDP port used by OverlayManager.
  private int port;

  /**
   * Builds a new overlay manager for MANETs using the specified port for
   * receiving messages. The choice of the port has to be a system-wide
   * parameter.
   * 
   * @param port The port for receiving messages.
   */
  public ManetTreeOverlayMgr(int port, WirelessTopologyManager topolMag) {
    this.broadcastId = 0;
    this.running = true;
    this.groupSequenceNumber = 0;
    this.hopToGroupLeader = 0;
    this.lostHelloMessages = 0;
    this.lastHelloMessage = System.currentTimeMillis();
    this.topMgr = topolMag;
    this.port = port;
    // Configuring logging facility.
    logger = Logger.getLogger("polimi.manetOverlayMgr");
    logger.setLevel(Level.ALL);
    logger.config("Created MANET reactive overlay manager on UDP port "+port);
    // Retrieving parameters.
    try {
      Preferences.importPreferences(new FileInputStream(XML_CONFIG_PATH));
    } catch(FileNotFoundException e) {} catch(IOException e) {} catch(InvalidPreferencesFormatException e) {}
    Preferences params = Preferences
        .userNodeForPackage(ManetTreeOverlayMgr.class);
    BYTE_BUFFER_SIZE = params.getInt(BYTE_BUFFER_SIZE_XMLFIELD,
        DEF_BYTE_BUFFER_SIZE);
    DISCOVER_TIMEOUT = params.getInt(DISCOVER_TIMEOUT_XMLFIELD,
        DEF_DISCOVER_TIMEOUT);
    WAIT_INTERVAL = params.getInt(WAIT_INTERVAL_XMLFIELD, DEF_WAIT_INTERVAL);
    HELLO_INTERVAL = params.getInt(HELLO_INTERVAL_XMLFIELD, DEF_HELLO_INTERVAL);
    RECONNECTION_TRIGGER = params.getInt(RECONNECTION_TRIGGER_XMLFIELD,
        DEF_RECONNECTION_TRIGGER);
    ALLOWED_HELLO_LOSS = params.getInt(ALLOWED_HELLO_LOSS_XMLFIELD,
        DEF_ALLOWED_HELLO_LOSS);
    GROUP_SEQUENCE_TOLERANCE = params.getInt(GROUP_SEQUENCE_TOLERANCE_XMLFIELD,
        DEF_GROUP_SEQUENCE_TOLERANCE);
    REQUEST_RETRIES = params.getInt(REQUEST_RETRIES_XMLFIELD,
        DEF_REQUEST_RETRIES);
    TTL_INCREMENT = params.getInt(TTL_INCREMENT_XMLFIELD, DEF_TTL_INCREMENT);
    TTL_THRESHOLD = params.getInt(TTL_THRESHOLD_XMLFIELD, DEF_TTL_THRESHOLD);
    messageIO = new DatagramObjectIO(port, BYTE_BUFFER_SIZE, WAIT_INTERVAL);
    dataBuff = new dataBuffer();
    reconnecting = false;
  }

  /**
   * Starts the the overlay manager for MANET environments.
   */
  public void start() {
    if(topMgr!=null) {
      logger.config("Starting MANET overlay manager...");
      parsing = new parsingThread();
      reading = new readingThread();
      Thread r = new Thread(reading);
      r.setName("ManetTreeOverlayMgr.readingThread");
      Thread p = new Thread(parsing);
      p.setName("ManetTreeOverlayMgr.parsingThread");
      Thread g = new Thread(this);
      g.setName("ManetTreeOverlayMgr");
      r.setDaemon(true);
      p.setDaemon(true);
      g.setDaemon(true);
      hostId = topMgr.localID.getID();
      r.start();
      p.start();
      startGroupLeader(0);
      g.start();
      logger.config("MANET overlay manager started");
    }
  }

  /**
   * Stops the local connection manager (and the overlay manager).
   */
  public void stop() {
    running = false;
    reading.stop();
    parsing.stop();
    dataBuff.clear();
    // Saving parameters.
    Preferences params = Preferences
        .userNodeForPackage(ManetTreeOverlayMgr.class);
    params.putInt(BYTE_BUFFER_SIZE_XMLFIELD, BYTE_BUFFER_SIZE);
    params.putInt(DISCOVER_TIMEOUT_XMLFIELD, DISCOVER_TIMEOUT);
    params.putInt(WAIT_INTERVAL_XMLFIELD, WAIT_INTERVAL);
    params.putInt(HELLO_INTERVAL_XMLFIELD, HELLO_INTERVAL);
    params.putInt(RECONNECTION_TRIGGER_XMLFIELD, RECONNECTION_TRIGGER);
    params.putInt(ALLOWED_HELLO_LOSS_XMLFIELD, ALLOWED_HELLO_LOSS);
    params.putInt(GROUP_SEQUENCE_TOLERANCE_XMLFIELD, GROUP_SEQUENCE_TOLERANCE);
    params.putInt(REQUEST_RETRIES_XMLFIELD, REQUEST_RETRIES);
    params.putInt(TTL_INCREMENT_XMLFIELD, TTL_INCREMENT);
    params.putInt(TTL_THRESHOLD_XMLFIELD, TTL_THRESHOLD);
    try {
      params.exportNode(new FileOutputStream(XML_CONFIG_PATH));
    } catch(FileNotFoundException e1) {
      logger.fine("Problems while saving preferences");
    } catch(IOException e1) {
      logger.fine("Problems while saving preferences");
    } catch(BackingStoreException e1) {
      logger.fine("Problems while saving preferences");
    }
    logger.config("MANET overlay manager stopped");
  }

  /**
   * The method is used when the local host becomes the group leader for a given
   * portion of the overlay network.
   */
  private void startGroupLeader(long groupSequence) {
    groupLeader = true;
    reconnecting = false;
    upstreamNodeId = hostId;
    groupLeaderId = hostId;
    groupSequenceNumber = groupSequence;
    hopToGroupLeader = 0;
    lostHelloMessages = 0;
    dataBuff.clearReplies();
    logger.finest("Started local group leader...");
  }

  /**
   * The method is used when the local host gives the role of group leader to
   * another node.
   */
  private void stopGroupLeader() {
    groupLeader = false;
    upstreamNodeId = null;
    logger.finest("Stopped local group leader...");
  }

  /**
   * Updates the information on the current group leader.
   * 
   * @param groupLeaderId the id of the current group leader.
   * @param groupSequenceNumber the current group sequence number.
   * @param hopToGroupLeader the distance in hops to the current group leader.
   */
  private void setGroupInfo(String groupLeaderId, long groupSequenceNumber,
      int hopToGroupLeader) {
    this.groupLeaderId = groupLeaderId;
    this.hopToGroupLeader = hopToGroupLeader;
    this.groupSequenceNumber = groupSequenceNumber;
  }

  /**
   * The method is used to trigger the reconnection process in case a link
   * breaks.
   * 
   * @param neighborID the Id of the lost neighbor.
   */
  public synchronized void signalLostNeighbor(String neighborID) {
    logger.fine("Lost neighbor: "+neighborID);
    // The reconnection process starts only if the node is at the
    // downstream side of the broken link.
    if(neighborID.equals(upstreamNodeId)) {
      // The upstream node is lost.
      upstreamNodeId = null;
      logger.finer("Starting reconnection...");
      // Updating the local broadcast id.
      broadcastId++;
      dataBuff.updateLastBroadcastId(hostId, broadcastId);
      // Broadcast the request.
      ManetOverlayMgrRREQ rreq = new ManetOverlayMgrRREQ(hostId, broadcastId,
          groupSequenceNumber);
      rreq.setBroadcast(true);
      rreq.setHopCount(hopToGroupLeader);
      messageIO.sendBroadcastObject(rreq, port);
      // Waiting for replies...
      try {
        wait(DISCOVER_TIMEOUT);
      } catch(InterruptedException e) {
        logger.severe(e.toString());
      }
      // No further replies will be accepted.
      broadcastId++;
      dataBuff.updateLastBroadcastId(hostId, broadcastId);
      // Check if new links have been found.
      ManetOverlayMgrRREP newLink = dataBuff.getBestReplyAndClear();
      if(newLink!=null) {
        // At least a new link has been found, that link is activated.
        logger.fine("New link via: "+newLink.getLogicalSenderId()+" found");
        if(newLink.getHopCount()==0) {
          try {
			topMgr.addNeighbor(newLink.getActivationURL());
		} catch (AlreadyAddedNeighborException e) {
		} catch (ConnectException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        } else {
          // Unicast the activation message.
          ManetOverlayMgrMACT mact = new ManetOverlayMgrMACT(hostId, newLink
              .getActivationURL());
          mact.setLogicalReceiverId(newLink.getLogicalSenderId());
          mact.setReceiverId(newLink.getSenderId());
          mact.setHopCount(newLink.getHopCount());
          messageIO.sendBroadcastObject(mact, port);
        }
      } else {
        // No new link found, this host becomes the group leader of
        // the disconnected portion of the tree.
        logger.warning("No new link found... starting group leader");
        startGroupLeader(groupSequenceNumber);
      }
    }
  }

  /**
   * @see java.lang.Runnable#run()
   */
  public synchronized void run() {
    // This thread monitors the arriving of group hello messages
    // or sends them in case the local host is the group leader.
    long precHelloMessage = 0;
    while(running) {
      // If the local host is the group leader, it floods the overlay topology
      // with periodic group hello messages. Otherwise, the process
      // periodically checks for the receiving of group hello messages
      // from upstream nodes.
      if(groupLeader) {
        if((!activateReconnections())
            &&((groupSequenceNumber%(RECONNECTION_TRIGGER+1))==0)) {
          // Send a reconnect message for merging network partitions.
          sendReconnect();
        } else {
          // Send a group hello message.
          sendGroupHello(groupSequenceNumber);
        }
        // Increases the group sequence number.
        groupSequenceNumber++;
      } else {
        if(precHelloMessage!=lastHelloMessage) {
          // At least one Group Hello message has been received within the last
          // HELLO_INTERVAL.
          precHelloMessage = lastHelloMessage;
          lostHelloMessages = 0;
        } else {
          // The host lost at least one group hello message.
          lostHelloMessages++;
          logger.warning("Lost hello message #"+lostHelloMessages);
        }
        if(lostHelloMessages>ALLOWED_HELLO_LOSS) {
          // Starting group leader election. In case the upstreamNodeId is null
          // and the local host is not the group leader, then the local host
          // has never received any group hello message since the
          // last reconnection procedure ended (signalLostNeighbor).
          // This means that we found a new link whose activation failed.
          // In this case the local broker becomes the group leader of
          // the disconnected portion of the tree.
          if(upstreamNodeId==null) startGroupLeader(groupSequenceNumber);
          lostHelloMessages = 0;
        }
      }
      try {
        wait(HELLO_INTERVAL);
      } catch(InterruptedException e) {
        logger.severe(e.toString());
      }
    }
  }

  /**
   * The method is used for sending reconnection messages used for merging
   * network partitions or aquiring new neighbors.
   */
  private void sendReconnect() {
    logger.finest("Sending reconnection message as group leader...");
    // Increase the local broadcast id.
    broadcastId++;
    dataBuff.updateLastBroadcastId(hostId, broadcastId);
    // Broadcast the request.
    ManetOverlayMgrRREQ reconn = new ManetOverlayMgrRREQ(hostId, broadcastId,
        groupSequenceNumber);
    reconn.setReconnectFlag(true);
    reconn.setBroadcast(true);
    messageIO.sendBroadcastObject(reconn, port);
  }

  /**
   * The method implements the necessary operations for merging two network
   * partitions or aquiring new neighbors.
   * 
   * @return true if at least another host has been aquired, false otherwise.
   */
  private boolean activateReconnections() {
    // Look for other disconnected portions of the tree (or new neighbors).
    ManetOverlayMgrRREP newLink = dataBuff.getBestReplyAndClear();
    if(newLink!=null) {
      // No further replies will be accepted.
      broadcastId++;
      dataBuff.updateLastBroadcastId(hostId, broadcastId);
      // At least another portion of the tree has been reached or a new neighbor
      // has been acquired.
      logger.fine("Reconnecting disconnected portion via "
          +newLink.getLogicalSenderId());
      // Taking the greater group sequence number available.
      groupSequenceNumber = Math.max(groupSequenceNumber, newLink
          .getGroupSequenceNumber())+1;
      if(newLink.getHopCount()==0) {
        try {
			topMgr.addNeighbor(newLink.getActivationURL());
		} catch (AlreadyAddedNeighborException e) {
		} catch (ConnectException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
      } else {
        // Send the activation message.
        ManetOverlayMgrMACT mact = new ManetOverlayMgrMACT(hostId, newLink
            .getActivationURL());
        mact.setLogicalReceiverId(newLink.getLogicalSenderId());
        mact.setReceiverId(newLink.getSenderId());
        mact.setHopCount(newLink.getHopCount());
        messageIO.sendBroadcastObject(mact, port);
        logger.fine("Sending MACT message to "+mact.getLogicalReceiverId()
            +" via "+mact.getReceiverId());
      }
      return true;
    }
    return false;
  }

  /**
   * Send periodic group hello messages in case the local host is also the group
   * leader.
   */
  private void sendGroupHello(long groupSequence) {
    // Creates the message, the intended receivers are all the neighbors of the
    // group leader.
    ManetOverlayMgrGRPH groupHello = new ManetOverlayMgrGRPH(hostId,
        groupSequence);
    LinkedList neighbors = new LinkedList(topMgr.getNeighborIDs());
    if(neighbors.size()!=0) {
      logger.finest("Sending Group Hello message as group leader...");
      // Set the intended receivers of the message.
      groupHello.setReceiversIds(neighbors);
      // Send the group hello message.
      messageIO.sendBroadcastObject(groupHello, port);
    }
  }

  private boolean isNeighbor(String neighborId) {
    return new LinkedList(topMgr.getNeighborIDs()).contains(neighborId);
  }

  /**
   * This class provides a temporary data repository for messages just received,
   * information on reverse and forward paths setup and on the last known
   * broadcast ids for other nodes in the system.
   */
  class dataBuffer {
    // A list of messages waiting for the parsing thread.
    private LinkedList msgBuffer;
    // A list of replies collected for a given reconnection process.
    private LinkedList repliesBuffer;
    // Associates a node with its last known boradcast id.
    private HashMap broadcastIdMap;
    // Associates a requiring node with another node for the reverse path setup.
    private HashMap reverseLinkMap;
    // Associates a requiring node with another node for the forward path setup.
    private HashMap forwardLinkMap;
    // Associates a requiring node with a certain hop count on the forward link,
    // this information is used to set up the best forward link path for each
    // requiring node.
    private HashMap forwardLinkBestHopCount;

    /**
     * Build a new dataBuffer object.
     */
    public dataBuffer() {
      msgBuffer = new LinkedList();
      repliesBuffer = new LinkedList();
      broadcastIdMap = new HashMap();
      reverseLinkMap = new HashMap();
      forwardLinkMap = new HashMap();
      forwardLinkBestHopCount = new HashMap();
    }

    /**
     * Enqueue a message just received.
     * 
     * @param msg the message to enqueue.
     */
    private synchronized void enqueueMsgForParsing(ManetOverlayMgrMessage msg) {
      msgBuffer.addLast(msg);
      notifyAll();
    }

    /**
     * Return the first message waiting for parsing, if the buffer is empty
     * returns null.
     * 
     * @return the first message in the buffer.
     */
    private synchronized ManetOverlayMgrMessage dequeueMsgForParsing()
        throws NoSuchElementException {
      try {
        while(msgBuffer.isEmpty()) wait();
      } catch(InterruptedException e) {
        e.printStackTrace();
      }
      if(!msgBuffer.isEmpty()) return (ManetOverlayMgrMessage) msgBuffer
          .removeFirst();
      else return null;
    }

    /**
     * Clear all in the information in this data buffer.
     */
    private synchronized void clear() {
      msgBuffer.clear();
      repliesBuffer.clear();
      broadcastIdMap.clear();
      reverseLinkMap.clear();
      forwardLinkMap.clear();
      notifyAll();
    }

    /**
     * Update the last known broadcast id for a given host.
     * 
     * @param host a node in the system.
     * @param broadcastId the last known broadcast id for that host.
     */
    private void updateLastBroadcastId(String host, long broadcastId) {
      if(getLastBroadcastId(host)!=broadcastId) {
        forwardLinkBestHopCount.remove(host);
        forwardLinkMap.remove(host);
        if(broadcastIdMap.containsKey(host)) broadcastIdMap.remove(host);
        broadcastIdMap.put(host, new Long(broadcastId));
      }
    }

    /**
     * Return the last known broadcast id for a given host.
     * 
     * @param host a node in the system.
     * @return the last known broadcast id for host.
     */
    private long getLastBroadcastId(String host) {
      Long lastId = (Long) broadcastIdMap.get(host);
      if(lastId!=null) return lastId.longValue();
      else return 0;
    }

    /**
     * Set up the information for a reverse path. These information are indexed
     * depending on the node that issues the first RREQ message for a given
     * reconnection process.
     * 
     * @param requiringHost the host that issues the first RREQ message.
     * @param nextHop the next hop for this reverse path.
     */
    private void setReverseLink(String requiringHost, String nextHop) {
      if(reverseLinkMap.containsKey(requiringHost))
          reverseLinkMap.remove(requiringHost);
      reverseLinkMap.put(requiringHost, nextHop);
    }

    /**
     * Return the next hop for a certain reverse path. If the path does not
     * exist, then it returns null.
     * 
     * @param requiringHost the node that issues the first RREQ for this
     *          reconnection process.
     * @return the next hop for a certain reverse path.
     */
    private String getReverseLink(String requiringHost) {
      return (String) reverseLinkMap.get(requiringHost);
    }

    /**
     * Set up the information for a forward path. These information are indexed
     * depending on the node that issues the first RREQ message for a given
     * reconnection process.
     * 
     * @param requiringHost the host that issues the first RREQ message.
     * @param nextHop the next hop for this forward path.
     */
    private void setForwardLink(String requiringHost, String nextHop,
        int hopCount) {
      int bestHop;
      Integer b = (Integer) forwardLinkBestHopCount.get(requiringHost);
      if(b!=null) bestHop = b.intValue();
      else bestHop = Integer.MAX_VALUE;
      if(hopCount<bestHop) {
        if(forwardLinkBestHopCount.containsValue(requiringHost))
            forwardLinkBestHopCount.remove(requiringHost);
        forwardLinkBestHopCount.put(requiringHost, new Integer(hopCount));
        if(forwardLinkMap.containsKey(requiringHost))
            forwardLinkMap.remove(requiringHost);
        forwardLinkMap.put(requiringHost, nextHop);
      }
    }

    /**
     * Return the next hop for a certain forward path. If the path does not
     * exist, then it returns null.
     * 
     * @param requiringHost the node that issues the first RREQ for this
     *          reconnection process.
     * @return the next hop for a certain forward path.
     */
    private String getForwardLink(String requiringHost) {
      return (String) forwardLinkMap.get(requiringHost);
    }

    /**
     * Collects a new reply for a given reconnection process.
     * 
     * @param msg the RREP message received.
     */
    private void collectReply(ManetOverlayMgrRREP msg) {
      repliesBuffer.add(msg);
    }

    /**
     * Cleares the replies buffer.
     */
    private void clearReplies() {
      repliesBuffer.clear();
    }

    /**
     * Returns the number of replies received from the last
     * <code>clearReplies()</code> invocation.
     * 
     * @return the number of replies in the buffer.
     */
    private int pendingReplies() {
      return repliesBuffer.size();
    }

    /**
     * Return the best reply among the ones received for a given reconnection
     * process. The best reply is the one with the smallest number of hops
     * towards the overlay.
     * 
     * @return a RREP message representing the best reply received so far, null
     *         otherwise.
     */
    private ManetOverlayMgrRREP getBestReplyAndClear() {
      int bestHop = Integer.MAX_VALUE;
      long bestSequence = 0;
      ManetOverlayMgrRREP bestRREP = null;
      ManetOverlayMgrRREP msg;
      Iterator it = repliesBuffer.listIterator();
      while(it.hasNext()) {
        msg = (ManetOverlayMgrRREP) it.next();
        if(msg.getGroupSequenceNumber()>bestSequence)
            bestSequence = msg.getGroupSequenceNumber();
      }
      while(!repliesBuffer.isEmpty()) {
        msg = (ManetOverlayMgrRREP) repliesBuffer.removeFirst();
        if((msg.getHopCount()<bestHop)
            &&(msg.getGroupSequenceNumber()>=bestSequence
                -GROUP_SEQUENCE_TOLERANCE)) {
          bestRREP = msg;
          bestHop = msg.getHopCount();
        }
      }
      return bestRREP;
    }
  }

  /**
   * This class implements a thread in charge of receiving messages from the
   * network and buffering them.
   */
  class readingThread implements Runnable {
    // Is TRUE if the reading thread is active.
    private boolean running;

    /**
     * Create a new reading thread with a given buffer.
     * 
     * @param messageIO a reference to an object for receiving messages.
     * @param msgBuff the data buffer.
     */
    public readingThread() {
      this.running = true;
    }

    /**
     * Stop the reading thread.
     */
    private void stop() {
      running = false;
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {
      while(running) {
        // Tries to receive an object.
        Object o;
        try {
          o = messageIO.receiveObject();
          // The object received has to be an instance of
          // ManetOverlayMgrMessage.
          if(!(o instanceof ManetOverlayMgrMessage)) {
            logger
                .warning("MessageCastError - please check if other applications on this subnet are sending UDP datagrams on port "
                    +messageIO.getPort());
          } else {
            // Buffering the message.
            ManetOverlayMgrMessage msg = (ManetOverlayMgrMessage) o;
            dataBuff.enqueueMsgForParsing(msg);
          }
        } catch(InterruptedIOException e) {
          continue;
        } catch(IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  /**
   * This class implements a thread in charge of parsing the messages received.
   */
  class parsingThread implements Runnable {
    private boolean running;

    /**
     * Create a new parsing thread.
     */
    public parsingThread() {
      this.running = true;
    }

    /**
     * Stop the reading thread.
     */
    private void stop() {
      running = false;
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public synchronized void run() {
      while(running) {
        // Dequeues a message.
        ManetOverlayMgrMessage msg;
        try {
          msg = dataBuff.dequeueMsgForParsing();
        } catch(NoSuchElementException e) {
          continue;
        }
        if(msg==null) {
          break;
        }
        // The message is broadcast, but it was sent by the local host.
        if((msg.isBroadcast())&&(msg.getSenderId().equals(hostId))) continue;
        // The message has multiple receivers, but this host is not among them.
        if((msg.isMulticast())&&!(msg.getReceiversIds().contains(hostId)))
            continue;
        // The message is neither multicast nor broadcast and the local host
        // is not the intended receiver.
        if(!(msg.isBroadcast())&&!(msg.isMulticast())
            &&!(msg.getReceiverId().equals(hostId))) continue;
        switch(msg.getMessageType()) {
        case ManetOverlayMgrMessage.MANET_RREQ:
          ManetOverlayMgrRREQ rreq = (ManetOverlayMgrRREQ) msg;
          if(rreq.getReconnectFlag())
          // The message the host is parsing regards a reconnection procedure.
          parseRREQReconnectMessage(rreq);
          else
          // The message is a simple RREQ for a disconnected host.
          parseRREQmessage(rreq);
          break;
        case ManetOverlayMgrMessage.MANET_RREP:
          // The message is a route reply message.
          ManetOverlayMgrRREP rrep = (ManetOverlayMgrRREP) msg;
          parseRREPmessage(rrep);
          break;
        case ManetOverlayMgrMessage.MANET_MACT:
          // The message is a link activation message.
          ManetOverlayMgrMACT mact = (ManetOverlayMgrMACT) msg;
          parseMACTmessage(mact);
          break;
        case ManetOverlayMgrMessage.MANET_GPRH:
          // The message is a Group Hello message.
          ManetOverlayMgrGRPH grph = (ManetOverlayMgrGRPH) msg;
          parseGRPHmessage(grph);
          break;
        default:
          logger.warning("Wrong message format coming from "
              +msg.getLogicalSenderId()+" via "+msg.getSenderId());
          break;
        }
      }
    }

    private void parseRREQmessage(ManetOverlayMgrRREQ msg) {
      logger.fine("Received RREQ message from "+msg.getSenderId());
      // If the RREQ message carries an old broadcastId for that
      // particular requiring node, then the message will be ignored.
      if((msg.getBroadcastId()>dataBuff.getLastBroadcastId(msg
          .getLogicalSenderId()))
          &&(msg.getGroupSequenceNumber()<=groupSequenceNumber)
          &&(hopToGroupLeader<msg.getHopCount())) {
        // Update the broadcast id for the sender.
        dataBuff.updateLastBroadcastId(msg.getLogicalSenderId(), msg
            .getBroadcastId());
        // The node can satisfy the request, a RREP message gets logically
        // unicast towards the requiring node.
        ManetOverlayMgrRREP rrep = new ManetOverlayMgrRREP(hostId, selectUrl(topMgr.overlay.getURLs()), groupSequenceNumber);
        rrep.setReceiverId(msg.getSenderId());
        rrep.setLogicalReceiverId(msg.getLogicalSenderId());
        rrep.setBroadcastId(msg.getBroadcastId());
        rrep.setHopCount(0);
        messageIO.sendBroadcastObject(rrep, port);
        logger.fine("Responding to "+msg.getLogicalSenderId()+"...");
      } else if((msg.getBroadcastId()>dataBuff.getLastBroadcastId(msg
          .getLogicalSenderId()))
          ||((msg.getBroadcastId()==dataBuff.getLastBroadcastId(msg
              .getLogicalSenderId()))
              &&(upstreamNodeId!=null)&&(!upstreamNodeId.equals(dataBuff
              .getReverseLink(msg.getLogicalSenderId()))))) {
        logger.fine("Forwarding RREQ message for "+msg.getLogicalSenderId());
        // Update the broadcast id for the sender.
        dataBuff.updateLastBroadcastId(msg.getLogicalSenderId(), msg
            .getBroadcastId());
        // The RREQ gets forwarded and the reverse path is set up.
        dataBuff.setReverseLink(msg.getLogicalSenderId(), msg.getSenderId());
        forwardRREQ(msg);
      }
    }
    /**
	 * Select the first url to be inserted into the <code>LSTreeMsg</code>s.
	 * @param urls the urls
	 * @return an url selected into <code>urls</code>
	 */
	private String selectUrl(String[] urls){
		return urls[0];
	}
    
    private void parseRREQReconnectMessage(ManetOverlayMgrRREQ msg) {
      if((groupLeader)
          &&(msg.getLogicalSenderId().hashCode()>hostId.hashCode())
          &&(!reconnecting)
          &&(dataBuff.pendingReplies()==0)
          &&(msg.getBroadcastId()>dataBuff.getLastBroadcastId(msg
              .getLogicalSenderId()))) {
        // The local group leader is responding to another group leader
        // and stops the local group leader process.
        stopGroupLeader();
        // Only one reconnection procedure can be carried out at a time.
        reconnecting = true;
        // Starting reconnection procedure
        ManetOverlayMgrRREP rrep = new ManetOverlayMgrRREP(hostId, selectUrl(topMgr.overlay.getURLs()), groupSequenceNumber);
        rrep.setReceiverId(msg.getSenderId());
        rrep.setLogicalReceiverId(msg.getLogicalSenderId());
        rrep.setBroadcastId(msg.getBroadcastId());
        rrep.setHopCount(0);
        messageIO.sendBroadcastObject(rrep, port);
        logger.fine("Responding to "+msg.getLogicalSenderId()
            +" for reconnection procedure.");
        // Updates the broadcast id for the sender.
        dataBuff.updateLastBroadcastId(msg.getLogicalSenderId(), msg
            .getBroadcastId());
      } else if((!groupLeader)
          &&((msg.getBroadcastId()>dataBuff.getLastBroadcastId(msg
              .getLogicalSenderId()))||((msg.getBroadcastId()==dataBuff
              .getLastBroadcastId(msg.getLogicalSenderId()))
              &&(upstreamNodeId!=null)&&(!upstreamNodeId.equals(dataBuff
              .getReverseLink(msg.getLogicalSenderId())))))) {
        // Update the broadcast id for the sender.
        dataBuff.updateLastBroadcastId(msg.getLogicalSenderId(), msg
            .getBroadcastId());
        // The RREQ gets forwarded and the reverse path is set up.
        dataBuff.setReverseLink(msg.getLogicalSenderId(), msg.getSenderId());
        forwardRREQ(msg);
        logger.fine("Current reverse link for "+msg.getLogicalSenderId()+" is "
            +dataBuff.getReverseLink(msg.getLogicalSenderId()));
      }
      if(!groupLeader&&msg.getLogicalSenderId().equals(groupLeaderId)) {
        // If the reconnection message comes from the current group leader,
        // then it can be interpreted as a group hello message.
        logger.fine("Parsing a reconnect message as a GRPH.");
        lastHelloMessage = System.currentTimeMillis();
      }
    }

    private void forwardRREQ(ManetOverlayMgrRREQ msg) {
      if((msg.isOverlayLimited())&&(upstreamNodeId!=null)
          &&(upstreamNodeId!=hostId)) {
        // The message already traversed one hop outside the overlay network,
        // it will be forwarded only along the edges of the tree
        // towards the multicast group leader.
        msg.setReceiverId(upstreamNodeId);
        msg.setSenderId(hostId);
        messageIO.sendBroadcastObject(msg, port);
        logger.fine("Forwarding overlayLimited RREQ to:"+upstreamNodeId);
      } else if((!msg.isOverlayLimited())&&(!isNeighbor(msg.getSenderId()))
          &&(upstreamNodeId!=null)&&(upstreamNodeId!=hostId)) {
        // The message just traversed one hop outside the tree,
        // from now on, the message will be forwarded only along the edges
        // of the overlay topology in upstream direction.
        msg.setOverlayLimited(true);
        msg.setBroadcast(false);
        msg.setReceiverId(upstreamNodeId);
        msg.setSenderId(hostId);
        messageIO.sendBroadcastObject(msg, port);
        logger.fine("Setting and forwarding overlayLimited RREQ to:"
            +upstreamNodeId);
      } else if(isNeighbor(msg.getSenderId())) {
        logger.fine("Rebroadcasting RREQ coming from "+msg.getLogicalSenderId()
            +" received from "+msg.getSenderId());
        // The message comes from one of the neighboring nodes in the tree,
        // we simply rebroadcast the message.
        msg.setSenderId(hostId);
        messageIO.sendBroadcastObject(msg, port);
      }
    }

    private void parseRREPmessage(ManetOverlayMgrRREP msg) {
      logger.finer("Received RREP message from "+msg.getSenderId());
      if((msg.getLogicalReceiverId().equals(hostId))
          &&(msg.getBroadcastId()==broadcastId)) {
        // This node is the requiring node, collecting...
        dataBuff.collectReply(msg);
        logger.fine("Collecting reply from "+msg.getLogicalSenderId()+" via "
            +msg.getSenderId());
      } else if(msg.getBroadcastId()==dataBuff.getLastBroadcastId(msg
          .getLogicalReceiverId())) {
        // The node simply propagates the RREP message, the forward path is
        // setup.
        dataBuff.setForwardLink(msg.getLogicalReceiverId(), msg.getSenderId(),
            msg.getHopCount());
        msg.setSenderId(hostId);
        msg.setReceiverId(dataBuff.getReverseLink(msg.getLogicalReceiverId()));
        if(!isNeighbor(msg.getReceiverId())) {
          msg.setActivationURL(selectUrl(topMgr.overlay.getURLs()));
          msg.setHopCount(0);
          logger.finer("Rewriting RREP header for "+msg.getLogicalReceiverId());
        } else {
          msg.increaseHopCount();
        }
        messageIO.sendBroadcastObject(msg, port);
        logger.fine("Propagating RREP for "+msg.getLogicalReceiverId()+" via "
            +msg.getReceiverId());
      }
    }

    private void parseMACTmessage(ManetOverlayMgrMACT msg) {
      // The message concerns the activation of a new link.
      String nextHop = dataBuff.getForwardLink(msg.getLogicalSenderId());
      logger.fine("Parsing MACT from "+msg.getLogicalSenderId()+" via "
          +msg.getSenderId());
      if(!isNeighbor(nextHop)) {
        try {
			topMgr.addNeighbor(msg.getActivationURL());
		} catch (AlreadyAddedNeighborException e) {
		} catch (ConnectException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
      } else {
        // Propagates the MACT message.
        msg.setSenderId(hostId);
        msg.setReceiverId(nextHop);
        messageIO.sendBroadcastObject(msg, port);
      }
    }

    private void parseGRPHmessage(ManetOverlayMgrGRPH msg) {
      logger.finest("Received group hello update #"
          +msg.getGroupSequenceNumber()+" from "+msg.getLogicalSenderId()
          +" via "+msg.getSenderId());
      // Update data.
      msg.increaseHopCount();
      setGroupInfo(msg.getLogicalSenderId(), msg.getGroupSequenceNumber(), msg
          .getHopCount());
      lastHelloMessage = System.currentTimeMillis();
      upstreamNodeId = msg.getSenderId();
      // The group hello message is propagated to all the neighbors,
      // but the physical sender of the message.
      LinkedList receivers = new LinkedList(topMgr.getNeighborIDsExcept(upstreamNodeId));
      if(receivers.size()!=0) {
        // Propagate the group hello message to all intended receivers.
        msg.setReceiversIds(receivers);
        msg.setSenderId(hostId);
        messageIO.sendBroadcastObject(msg, port);
      }
    }
  }
}
