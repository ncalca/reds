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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Logger;

import polimi.reds.NodeDescriptor;
import polimi.reds.NotConnectedException;
import polimi.reds.broker.overlay.LSTreeTopologyManager.ResponseBucket.LinkDeadException;
import polimi.reds.broker.overlay.LSTreeTopologyManager.ResponseBucket.UnexpectedResponseException;

/**
 * This class implements the LSTree algorithm.
 * 
 * @author Alessandro Monguzzi
 * 
 */
public class LSTreeTopologyManager extends AbstractTopologyManager {
	/**
	 * @author frey
	 * 
	 */
	public class BusyCache extends Cache {
		double busyTimeout;

		/**
		 * @param name
		 */
		public BusyCache(String name) {
			super(name);
			// TODO Auto-generated constructor stub
		}

		/**
		 * @param c
		 */
		public BusyCache(Cache c) {
			super(c);
			// TODO Auto-generated constructor stub
		}

		public void setBusyTimeout(double bt) {
			busyTimeout = bt;
		}

		public NodeDescriptor getNextCandidate() {
			try {
				Thread.sleep((long) (rnd.nextDouble() * busyTimeout));
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return super.getNextCandidate();
		}

	}

	/**
	 * @author frey
	 * 
	 */
	protected class ResponseBucket {
		public class FullResponseBucketException extends Exception {

			/**
			 * 
			 */
			private static final long serialVersionUID = 5193404380055167360L;

			public FullResponseBucketException(String arg0) {
				super(arg0);
			}

		}

		public class LinkDeadException extends Exception {

			/**
			 * 
			 */
			private static final long serialVersionUID = -1225820293630565908L;

			public LinkDeadException(NodeDescriptor n) {
				super("Link Dead to " + n.getUrls()[0]);
			}

		}

		public class UnexpectedResponseException extends Exception {

			/**
			 * 
			 */
			private static final long serialVersionUID = -6480957315306560390L;

			public UnexpectedResponseException(String message) {
				super(message);
			}

		}

		LSTreeMsg response;
		NodeDescriptor waiting;

		synchronized void linkDead(NodeDescriptor n) {
			if (n.equals(waiting)) {
				waiting = null;
				notify();// TODO: Check if notifyAll would be
							// better of if it would make any difference at all
			}
		}

		synchronized void setResponse(LSTreeMsg msg) throws UnexpectedResponseException {
			if (waiting == null || !waiting.equals(msg.getNode())) {
				logger.finer("unexpected response: waiting " + waiting + " got it from " + msg.getNode());
				throw new UnexpectedResponseException(msg.toString());
			}
			response = msg;
			waiting = null;
			notify();// TODO: Check if notifyAll would be
						// better of if it would make any difference at all
		}

		synchronized void waitFrom(NodeDescriptor n) {
			waiting = n;
		}

		synchronized LSTreeMsg getResponse() throws LinkDeadException {
			LSTreeMsg r;
			if (response == null && waiting != null) {
				try {
					logger.finer("responsebucket waiting ");
					wait(10000);
					logger.finer("done waiting ");
				} catch (InterruptedException e) {
					logger.finer("responsebucket interrupted " + e.getMessage());
					e.printStackTrace();
				}
			}
			if (response != null) {
				r = response;
				response = null;
				waiting = null;
				return r;
			} else {
				NodeDescriptor n = waiting;
				response = null;
				waiting = null;
				throw new LinkDeadException(n);
			}
		}
	}

	/**
	 * Downstream cache.
	 */
	public static final String DOWNSTREAM = "D";
	/**
	 * Upstream cache.
	 */
	public static final String UPSTREAM = "U";
	/**
	 * MinDegree cache.
	 */
	public static final String MINDEGREE = "m";
	/**
	 * Regional cache.
	 */
	public static final String REGIONAL = "R";
	/**
	 * Busy cache.
	 */
	public static final String BUSY = "B";
	/**
	 * Global cache.
	 */
	public static final String GLOBAL = "G";
	/**
	 * MaxDegree cache.
	 */
	public static final String MAXDEGREE = "M";
	/**
	 * Separator used to divide each token composing the <code>String</code>
	 * that order the caches.
	 */
	public static final String CACHE_SEPARATOR = "-";
	/**
	 * Fraction of the global cache sent with periodic messages.
	 */
	private static double GLOBAL_CACHE_FRACTION = 0.5;
	/**
	 * Fraction of the global cache that receives the periodic message.
	 */
	private static double OTHER_GLOBAL_CACHE_FRACTION = 0.5;
	/**
	 * Global cache size.
	 */
	private static int GLOBAL_CACHE_LENGTH = 10;
	/**
	 * Upstream chain size.
	 */
	private static int UPSTREAM_CHAIN_LENGTH = 3;
	/**
	 * Max degree value.
	 */
	private static int MAX_DEGREE = 4;
	/**
	 * Min degree value.
	 */
	private static int MIN_DEGREE = 0;

	private static long BUSYTIMEOUT = 1000;

	private static final double DEFAULT_DEPTH_INCREMENT = 10;
	private static final long WAITING_TIME = 10000;

	private NodeDescriptor father = null;
	private static double EPSILON = 0.01;
	private double depth = 0;
	private double fatherDepth = 0;
	private Color color = null;
	private GlobalCache globalCache = null;
	private RegionalCache regionalCache = null;
	private Set alreadyTried = null;
	/**
	 * Order to follow to use the caches.
	 */
	private String cacheOrder = DOWNSTREAM + CACHE_SEPARATOR + UPSTREAM + CACHE_SEPARATOR + MINDEGREE + CACHE_SEPARATOR
			+ REGIONAL + CACHE_SEPARATOR + BUSY + CACHE_SEPARATOR + GLOBAL + CACHE_SEPARATOR + MAXDEGREE;
	/**
	 * It is active when the local node is the root of its tree.
	 */
	private RootConnector rootConnector = new RootConnector();
	/**
	 * The thread that search for other trees.
	 */
	private Thread rootThread = null;
	/**
	 * This flag is true when a reconfiguration process is active.
	 */
	private boolean busy = false;
	/**
	 * Stores the requests for connections as pairs <LSTreeNodeIdentifier,
	 * LSTreeMsg>
	 */
	private ResponseBucket responseBucket = new ResponseBucket();
	/**
	 * Time between two <code>PERIODIC_UPDATE</code> messages.
	 */
	private long periodicSleep = WAITING_TIME;
	/**
	 * Send the periodic update messages.
	 */
	private PeriodicUpdater periodicUpdater = new PeriodicUpdater();
	/**
	 * Thread for periodic updater.
	 */
	private Thread periodicThread = null;
	/**
	 * Create a new <code>LSTreeTopologyManager</code>.
	 * 
	 */

	private Random rnd = null;

	public LSTreeTopologyManager() {

		super.neighborTransport = Collections.synchronizedMap(new HashMap());
		// DAVIDE: removed super.tempNeighbors = Collections.synchronizedMap(new
		// HashMap());
		super.neighborAddedListeners = Collections.synchronizedList(new LinkedList());
		super.neighborRemovedListeners = Collections.synchronizedList(new LinkedList());
		super.neighborDeadListeners = Collections.synchronizedList(new LinkedList());

		// insert data for LSTree topology management
		regionalCache = new RegionalCache("regional");
		globalCache = new GlobalCache("global");
		alreadyTried = new HashSet();
		// set initial data
		color = new Color();
		rnd = new Random();
		this.depth = DEFAULT_DEPTH_INCREMENT + rnd.nextGaussian();
		logger = Logger.getLogger("polimi.reds.overlay.LSTreeTopologyManager");
		periodicThread = new Thread(periodicUpdater);
		periodicThread.setName("LSTreeTopologyManager.PeriodicUpdater");
		periodicThread.setDaemon(true);
	}

	/**
	 * Start the periodic updater thread.
	 * 
	 * @see TopologyManager#start()
	 */
	public void start() {
		super.start();
		periodicThread.start();
	}

	/**
	 * Start the recovery process.
	 * 
	 */
	public void startTopologyManager() {
		createNewTree();
		// Create new tree must be done before initializing transports
		Collection transports = overlay.getAllTransport();
		synchronized (transports) {// TODO: ask why this is synchronized
			Iterator it = transports.iterator();
			while (it.hasNext()) {
				final Transport next = (Transport) it.next();
				PacketListener pl = new PacketListener() {

					public void signalPacket(String subject, NodeDescriptor senderID, Serializable payload) {
						LSTreeTopologyManager.this.signalPacket(subject, senderID, payload, next);
					}

				};
				next.addPacketListener(pl, LSTreeMsg.BUSY);
				next.addPacketListener(pl, LSTreeMsg.CONNECTION_ACCEPTED);
				next.addPacketListener(pl, LSTreeMsg.NOT_DEPTH_OR_COLOR);
				next.addPacketListener(pl, LSTreeMsg.NOT_MAX_DEGREE);
				next.addPacketListener(pl, LSTreeMsg.NOT_MIN_DEGREE);
				next.addPacketListener(pl, LSTreeMsg.REQUEST);
				next.addPacketListener(pl, LSTreeMsg.SIBLING_DEAD);
				next.addPacketListener(pl, LSTreeMsg.UPDATE);
				next.addPacketListener(pl, LSTreeMsg.NEW_SIBLING);

				next.addPacketListener(pl, LSTreeMsg.PERIODIC_UPDATE);
				next.addPacketListener(pl, LSTreeMsg.GOOD_CANDIDATE);

				next.addPacketListener(pl, LSTreeMsg.NOLONGERCHILD);
			}
		}
		createRootConnector();
	}

	/**
	 * Stop the periodic updater thread.
	 * 
	 * @see TopologyManager#stop()
	 */
	public void stop() {
		super.stop();
		periodicUpdater.exit();
		try {
			periodicThread.join();
		} catch (InterruptedException e) {
			logger.severe(e.getMessage());
		}
	}

	/**
	 * Get the fraction of the global cache sent with periodic nodes.
	 * 
	 * @return the fraction
	 */
	public static double getGLOBAL_CACHE_FRACTION() {
		return GLOBAL_CACHE_FRACTION;
	}

	/**
	 * Set the fraction of the global cache sent with periodic nodes.
	 * 
	 * @param global_cache_fraction
	 *            the fraction
	 */
	public static void setGLOBAL_CACHE_FRACTION(double global_cache_fraction) {
		GLOBAL_CACHE_FRACTION = global_cache_fraction;
	}

	/**
	 * Get the fraction of the global cache that receives the periodic updates.
	 * 
	 * @return the fraction
	 */
	public static double getOTHER_GLOBAL_CACHE_FRACTION() {
		return OTHER_GLOBAL_CACHE_FRACTION;
	}

	/**
	 * Get the fraction of the global cache that receives the periodic updates.
	 * 
	 * @return the fraction
	 */
	public static void setOTHER_GLOBAL_CACHE_FRACTION(double other_global_cache_fraction) {
		OTHER_GLOBAL_CACHE_FRACTION = other_global_cache_fraction;
	}

	/**
	 * Get the periodic sleep time.
	 * 
	 * @return the periodic sleep time
	 */
	public long getPeriodicSleep() {
		return periodicSleep;
	}

	/**
	 * Set the periodic sleep time.
	 * 
	 * @param periodicSleep
	 *            the periodic sleep time
	 */
	public void setPeriodicSleep(long periodicSleep) {
		this.periodicSleep = periodicSleep;
	}

	/**
	 * Get the global cache length.
	 * 
	 * @return the global cache length
	 */
	public static int getGLOBAL_CACHE_LENGTH() {
		return GLOBAL_CACHE_LENGTH;
	}

	/**
	 * Set the global cache length.
	 * 
	 * @param global_cache_length
	 *            the global cache length
	 */
	public static void setGLOBAL_CACHE_LENGTH(int global_cache_length) {
		GLOBAL_CACHE_LENGTH = global_cache_length;
	}

	/**
	 * Get the max degree value.
	 * 
	 * @return the max degree value
	 */
	public static int getMAX_DEGREE() {
		return MAX_DEGREE;
	}

	/**
	 * Set the max degree value.
	 * 
	 * @param max_degree
	 *            the max degree value
	 */
	public static void setMAX_DEGREE(int max_degree) {
		MAX_DEGREE = max_degree;
	}

	/**
	 * Get the min degree value.
	 * 
	 * @return the min degree value
	 */
	public static int getMIN_DEGREE() {
		return MIN_DEGREE;
	}

	/**
	 * Set the min degree value.
	 * 
	 * @param min_degree
	 *            the min degree value
	 */
	public static void setMIN_DEGREE(int min_degree) {
		MIN_DEGREE = min_degree;
	}

	/**
	 * Get the upstream chain length.
	 * 
	 * @return the upstream chain length
	 */
	public static int getUPSTREAM_CHAIN_LENGTH() {
		return UPSTREAM_CHAIN_LENGTH;
	}

	/**
	 * Set the upstream chain length.
	 * 
	 * @param upstream_chain_length
	 *            the upstream chain length
	 */
	public static void setUPSTREAM_CHAIN_LENGTH(int upstream_chain_length) {
		UPSTREAM_CHAIN_LENGTH = upstream_chain_length;
	}

	/**
	 * Select the first url to be inserted into the <code>LSTreeMsg</code>s.
	 * 
	 * @param urls
	 *            the urls
	 * @return an url selected into <code>urls</code>
	 */
	private String selectUrl(String[] urls) {
		return urls[0];
	}

	/**
	 * Set the <code>GenericOverlay</code>.
	 * 
	 * @param o
	 *            the <code>GenericOverlay</code>
	 */
	public void setOverlay(GenericOverlay o) {
		super.setOverlay(o);

		// Moved the initialization of packetlisteners to startTopologyManager
		// after createNewTree
	}

	/**
	 * Check whether the local node can-connect-to a remote node. It is used
	 * when the local node is a root to check whether it can connect to another
	 * root node.
	 * 
	 * @param acceptingColor
	 *            the color of the remote node
	 * @param acceptingDepth
	 *            the depth of the remote node
	 * @return <code>true</code> the local node can-connect-to the remote node
	 */
	private boolean canConnectTo(Color acceptingColor, double acceptingDepth) {
		if (color.canConnectTo(acceptingColor))
			return true;
		else if (color.equals(acceptingColor))
			return (depth > acceptingDepth);
		else
			return false;
	}

	/**
	 * Check whether the remote node can-connect-to the local node. This is not
	 * the negation of the above
	 * 
	 * @param requestingColor
	 *            the color of the remote node
	 * @param requestingDepth
	 *            the depth of the remote node
	 * @return <code>true</code> the local node can-connect-to the remote node
	 */

	private boolean canConnectToMe(Color requestingColor, double requestingDepth) {
		if (requestingColor.canConnectTo(color))
			return true;
		else if (color.equals(requestingColor))
			return (depth < requestingDepth);
		else
			return false;
	}

	/**
	 * Try to repair the tree using its cache in order. If it succeeds it
	 * returns the <code>NodeDescriptor</code> of the new father node.
	 * 
	 * @param candidate
	 *            the given candidate
	 * @param force
	 *            if <code>true</code> force the connection to the candidates
	 * @return the message sent from the candidate
	 * @throws AlreadyExistingLinkException
	 */
	private LSTreeMsg tryCandidate(String url, boolean forceMax, boolean forceMin) throws AlreadyExistingLinkException {
		LSTreeMsg response;
		Transport transport = overlay.resolveUrl(url);

		// check whether the remote node is the same node

		// check whether the response is successful
		NodeDescriptor dest = openSafeLink(transport, url);
		if (localID.equals(dest)) {
			logger.finer("localID equals dest - aborting: utl: " + url + " dest: " + dest.getUrls()[0] + " localID: "
					+ localID.getUrls()[0]);

			return null;
		}
		if (dest == null) {
			logger.finer("Can't get link to candidate");
			return null;
		}

		// send a request message
		LSTreeMsg msg = new LSTreeMsg(LSTreeMsg.REQUEST, color, depth, forceMax, forceMin, null, null, null, localID);
		logger.finer("TopologyManager: REQMSG  sent from " + LSTreeTopologyManager.this.localID.getUrls()[0] + " to "
				+ dest.getUrls()[0] + ": " + msg.toString());
		// REMOVED BECAUSE NOW LinkOpenedListener fires on both sides
		// synchronized(neighborTransport){
		// tempNeighbors.put(dest, transport);
		// }
		try {
			responseBucket.waitFrom(dest);
			transport.send(LSTreeMsg.REQUEST, msg, dest, Transport.MISCELLANEOUS_CLASS);
			response = responseBucket.getResponse();
			logger.finer("TopologyManager: RESPMSG received by " + LSTreeTopologyManager.this.localID.getUrls()[0]
					+ " from " + dest.getUrls()[0] + " response: " + response.toString());
			if (response != null && response.getType().equals(LSTreeMsg.CONNECTION_ACCEPTED)) {
				logger.finer("request was accepted");
				updateLocalData(response);
				// update propagation of dowstream chain and color
				LSTreeMsg updater = new LSTreeMsg(LSTreeMsg.UPDATE, color, depth, false, false, getUpstreamChain(),
						null, null, localID);
				updatePropagation(updater);
				/*
				 * Do not remove the neighbor from the tempNeighbor before
				 * invoking the confirmConnection (which does itself this
				 * operation) because this information is used inside the
				 * confirmConnection.
				 */
				synchronized (neighborTransport) {
					/*
					 * DAVIDE: removed System.out.println("removing "+
					 * dest.getUrls()[0]+" from tempNeighbors (ACCEPTRCV)");
					 * 
					 * tempNeighbors.remove(dest);
					 */
					// Ok we are not closing
					confirmNeighbor(dest, transport);
				}
			} else {
				closeSafeLink(transport, dest);

				if (response != null) {
					// the connection was refused -> close the link and try with
					// the next
					// closeSafeLink(transport, dest);
					/*
					 * Here remove dest from the tempNeighbors because there is
					 * no confirm to send. DAVIDE: do not remove from
					 * tempneighbors because it's done in the signlaLinkClosed
					 * listener which now fires on both sides
					 */

					logger.finer("Response was not null");
					// not needed if close safe link notifies
					// tempNeighbors.remove(dest);
				} else {
					logger.finer("Response was null");
				}
			}

		} catch (NotConnectedException e) {
			return null;
		} catch (LinkDeadException e) {
			// TODO: HANDLE
			return null;
		}
		return response;
	}

	/**
	 * @throws FatherPresentException
	 *             The local node has a father and cannot connect to another
	 *             node
	 * @see TopologyManager#addNeighbor(String)
	 */
	public NodeDescriptor addNeighbor(String url) throws AlreadyAddedNeighborException {
		logger.finer("Adding neighbor " + url);
		LSTreeMsg response = null;
		synchronized (this) {
			while (busy) {
				try {
					wait();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if (father != null)
				throw new AlreadyAddedNeighborException(father);
			busy = true;
		}
		try {
			response = tryCandidate(url, false, false);
		} catch (AlreadyExistingLinkException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		synchronized (this) {
			busy = false;
			notifyAll();
		}
		if (response != null)
			return response.getNode();
		else
			return null;

	}

	private void createRootConnector() {

		logger.finer("Create a new RootConnector");
		rootConnector.reset();
		rootThread = new Thread(rootConnector);
		rootThread.setName("LSTreeTopoloyManager.RootConnector");
		rootThread.setDaemon(true);
		rootThread.start();
	}

	/**
	 * @see TopologyManager#removeNeighbor(NodeDescriptor)
	 */
	public void removeNeighbor(NodeDescriptor removedNeighbor) {
		super.removeNeighbor(removedNeighbor);
		if (father.equals(removedNeighbor)) {
			this.father = null;
		}
	}

	/**
	 * Send a propagation message to all its children.
	 * 
	 * @param msg
	 *            the message
	 */
	private void updatePropagation(LSTreeMsg msg) {
		logger.finer("UpdateMessage " + msg.toString());
		Set neighbors = new HashSet(getNeighboringBrokers());
		Iterator it = neighbors.iterator();
		while (it.hasNext()) {
			NodeDescriptor next = (NodeDescriptor) it.next();
			if (father == null || !next.equals(father)) {
				Transport t = (Transport) neighborTransport.get(next);
				try {
					t.send(msg.getType(), msg, next, Transport.MISCELLANEOUS_CLASS);
				} catch (NotConnectedException e) {
					logger.warning("The update msg " + msg.toString() + " cannot be sent to " + next.getID()
							+ " because of a " + "NotConnectedException " + e.getMessage());
				}
			}
		}

	}

	/**
	 * Manages the brutal disconnection of a neighbor of the local node.
	 * 
	 * @see AbstractTopologyManager#signalLinkDead(NodeDescriptor)
	 */
	public void signalLinkDead(NodeDescriptor deadNeighbor) {
		boolean neighbor = false;
		logger.finer("DEAD Link to " + deadNeighbor);
		if (neighborTransport.containsKey(deadNeighbor)) {
			neighbor = true;
		}
		super.signalLinkDead(deadNeighbor);
		if (neighbor) {
			logger.finer("Taking action for DEAD Link to " + deadNeighbor.getUrls()[0]);
			neighborGone(deadNeighbor);
		} else {
			logger.warning("DEAD link was not a neighbor: no explicit action");
		}
		/*
		 * The super method must be executed AFTER the neighborGone call because
		 * it removes from the neighbors the deadNeighbor, stopping the
		 * reconfiguration process. STAMINKIA
		 */
	}

	/**
	 * Get the sequence used for the caches.
	 * 
	 * @return a <code>String</code> indicating the sequence
	 */
	public String getCacheOrder() {
		return cacheOrder;
	}

	/**
	 * Set the order in which the caches are used.
	 * <p>
	 * The order is specified using the <code>String cacheOrder</code>. A valid
	 * input parameter is each sequence of the caches constants separated by the
	 * <code>CACHE_SEPARATOR</code>.
	 * </p>
	 * 
	 * @param cacheOrder
	 *            a <code>String</code> indicating the sequence
	 */
	public void setCacheOrder(String cacheOrder) {
		this.cacheOrder = cacheOrder;
	}

	/**
	 * Add to the global cache a new <code>NodeDescriptor</code> containing the
	 * given url.
	 * 
	 * @param url
	 *            a <code>String</code> representing the url
	 */
	public void populateGlobalCache(String url) {
		NodeDescriptor node = new NodeDescriptor();
		node.addUrl(url);
		synchronized (globalCache) {
			this.globalCache.addCandidate(node, LSTreeTopologyManager.GLOBAL_CACHE_LENGTH);
		}
	}

	public NodeDescriptor getFather() {
		return father;
	}

	public boolean signalLinkClosing(NodeDescriptor closingNeighbor) {
		// synchronized(linksInUse){
		// if (linksInUse.containsKey(closingNeighbor))
		// if (((Integer)linksInUse.get(closingNeighbor)).intValue()>0)
		// return false;
		//
		return true;
		// }
	}

	private void closeSafeLink(Transport t, NodeDescriptor id) {
		// synchronized(linksInUse){
		// linksInUse.put(id, new
		// Integer(((Integer)linksInUse.get(id)).intValue()-1));
		// if (((Integer)linksInUse.get(id)).intValue()==0)
		// linksInUse.remove(id);
		t.closeLink(id);

		// }
	}

	private NodeDescriptor openSafeLink(Transport t, String url) {

		// boolean alreadyOpened = false;
		// NodeDescriptor dest = null;
		// synchronized(linksInUse){
		// try {
		// dest = t.openLink(url);
		// } catch (ConnectException e) {
		// logger.finer(e.getMessage());
		// } catch (MalformedURLException e) {
		// logger.finer(e.getMessage() + " " + url);
		// } catch (AlreadyExistingLinkException e) {
		// dest = e.getRemoteNodeDescriptor();
		// }
		// if (dest!=null){
		// if(!linksInUse.containsKey(dest))
		// linksInUse.put(dest, new Integer(0));
		// linksInUse.put(dest, new
		// Integer(((Integer)linksInUse.get(dest)).intValue()+1));
		// }
		//
		// }
		NodeDescriptor dest = null;
		try {
			System.out.println("calling openlink to " + url);
			logger.finer("calling openlink to " + url);
			dest = t.openLink(url);
		} catch (ConnectException e) {
			logger.finer(e.getMessage());
		} catch (MalformedURLException e) {
			logger.finer(e.getMessage() + " " + url);
		}
		logger.finer("link opened");
		return dest;
	}

	/**
	 * Search a new father for the local node. If it is found, the new father's
	 * <code>NodeDescriptor</code> is returned.<br>
	 * The field <code>father</code> is assigned by this method, the return
	 * value is used by the <code>RootConnector</code>.
	 * 
	 * @param deadFather
	 *            the dead father
	 * @return the new father's <code>NodeDescriptor</code>
	 * @throws AlreadyExistingLinkException
	 */
	private NodeDescriptor searchFather(NodeDescriptor deadFather, boolean rootConnector) {
		NodeDescriptor newFather = null;
		RegionalCache tmpRegionalCache = null;
		GlobalCache tmpGlobalCache = null;
		boolean found = false;
		// the dead neighbor is the father -> start reconfiguration
		synchronized (this) {
			while (busy)
				try {
					wait();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			if (father != null)
				return father;
			busy = true;
			// create the caches copies.
			tmpRegionalCache = new RegionalCache(regionalCache);
			tmpGlobalCache = new GlobalCache((GlobalCache) globalCache);
			alreadyTried.clear();
		}
		Cache downstreamCache = new Cache("downStream");
		Cache upstreamCache = new Cache("upStream");
		MinDegreeCache minDegreeCache = new MinDegreeCache("minDegree");
		BusyCache busyList = new BusyCache("busy");
		busyList.setBusyTimeout(BUSYTIMEOUT);
		MaxDegreeCache maxDegreeCache = new MaxDegreeCache("maxDegree");
		List caches = new ArrayList();
		// order the caches on the basis of the string.
		String[] tokens = cacheOrder.split(LSTreeTopologyManager.CACHE_SEPARATOR);
		for (int i = 0; i < tokens.length; i++) {
			if (tokens[i].equals(DOWNSTREAM))
				caches.add(downstreamCache);
			else if (tokens[i].equals(UPSTREAM))
				caches.add(upstreamCache);
			else if (tokens[i].equals(MINDEGREE))
				caches.add(minDegreeCache);
			else if (tokens[i].equals(REGIONAL))
				caches.add(tmpRegionalCache);
			else if (tokens[i].equals(BUSY))
				caches.add(busyList);
			else if (tokens[i].equals(GLOBAL))
				caches.add(tmpGlobalCache);
			else if (tokens[i].equals(MAXDEGREE))
				caches.add(maxDegreeCache);
			else
				logger.warning("Unknown parsed charachter: " + tokens[i]);
		}
		LSTreeMsg res = null;
		do {
			try {
				logger.finer("searchFather iteration begun");
				// reset res from the previous iteration
				res = null;
				Cache c = getFirstNonEmptyCache(caches);
				if (c != null) {
					logger.finer(c.name + " has " + c.size() + "more candidates");
					newFather = c.getNextCandidate();
					alreadyTried.add(newFather);
					logger.finer("Trying with " + newFather.getUrls()[0] + " from " + c.name + " which has " + c.size()
							+ "more candidates");
				} else {
					logger.finer("All the caches are empty");
					break;
				}
				try {
					res = c.tryCandidate(newFather);
				} catch (AlreadyExistingLinkException e) {
					logger.finer("AlreadyExistingLink adding " + newFather.getUrls()[0] + " to busyList");
					busyList.addCandidate(newFather, Integer.MAX_VALUE);
				}
				if (res != null) {
					if (res.getType().equals(LSTreeMsg.BUSY)) {
						logger.finer("BusyReply adding " + newFather.getUrls()[0] + " to busyList");
						busyList.addCandidate(newFather, Integer.MAX_VALUE);
					} else if (res.getType().equals(LSTreeMsg.NOT_MAX_DEGREE)) {
						maxDegreeCache.addCandidate(res.getNode(), Integer.MAX_VALUE);
						downstreamCache.addAllCandidates(res.getDownStreamChain(), Integer.MAX_VALUE);
					} else if (res.getType().equals(LSTreeMsg.NOT_MIN_DEGREE)) {
						minDegreeCache.addCandidate(res.getNode(), Integer.MAX_VALUE);
						upstreamCache.addAllCandidates(res.getUpstreamChain(),
								LSTreeTopologyManager.UPSTREAM_CHAIN_LENGTH);
					} else if (res.getType().equals(LSTreeMsg.NOT_DEPTH_OR_COLOR)) {
						if (res.getGoodCandidate() != null) {
							// the local node can connect to the root of the
							// remote tree
							if (!alreadyTried.contains(res.getGoodCandidate()))
								tmpGlobalCache.addCandidate(res.getGoodCandidate(), GLOBAL_CACHE_LENGTH);
							globalCache.addCandidate(res.getGoodCandidate(), GLOBAL_CACHE_LENGTH);
						}
					} else if (res.getType().equals(LSTreeMsg.CONNECTION_ACCEPTED))
						found = true;
				}
				logger.finer("searchFather iteration done");
			} catch (Exception e) {
				logger.finer("Exception in searchFather: " + e.toString());
				e.printStackTrace();
			}
		} while (!found);
		synchronized (this) {
			busy = false;
			if (res != null && res.getType().equals(LSTreeMsg.CONNECTION_ACCEPTED)) {
				notifyAll();
				return newFather;
			}
			/*
			 * if is the rootConnector itself to have made the search do not
			 * create another rootConnector else if a Proxy made the research
			 * and no father has been found, start a rootConnector.
			 */
			if (!rootConnector) {
				createNewTree();
				createRootConnector();
			}
			notifyAll();
			return null;
		}
	}

	/**
	 * Manages the disconnection of a neighbor of the local node.<br>
	 * If the <code>deadNeighbor</code> is a client it does nothing. If it is a
	 * son of the local node, it informs the other children, else it start the
	 * search of a new father using its caches. If the research fails, it
	 * becomes the root of a new tree.
	 */
	private void neighborGone(NodeDescriptor deadNeighbor) {
		// if the neighbor is a client there is no need of reconfiguration
		if (deadNeighbor != null && !deadNeighbor.isBroker())
			return;
		if (father != null && deadNeighbor != null && deadNeighbor.equals(father)) {
			father = null;
			fatherDepth = 0;
			rootConnector.exit();
			searchFather(deadNeighbor, false);
		} else {
			// the dead neighbor is a son -> inform the remaining sons.
			LSTreeMsg msg = new LSTreeMsg(LSTreeMsg.SIBLING_DEAD, this.color, this.depth, false, false, null, null,
					globalCache.cache, deadNeighbor);
			Iterator it = null;
			Map sib = new HashMap(super.neighborTransport);
			sib.remove(father);
			it = sib.keySet().iterator();
			while (it.hasNext()) {
				NodeDescriptor next = (NodeDescriptor) it.next();
				try {
					Transport transport = (Transport) sib.get(next);
					transport.send(LSTreeMsg.SIBLING_DEAD, msg, next, Transport.MISCELLANEOUS_CLASS);
				} catch (NotConnectedException e) {
					logger.warning("The sibling dead msg " + msg.toString() + " cannot be sent to " + next.getID()
							+ " because of a " + "NotConnectedException " + e.getMessage());
				}
			}
		}
	}

	/**
	 * Get the first <code>Cache</code> into the given <code>List</code> that is
	 * not empty.
	 * 
	 * @param caches
	 *            a <code>List</code> containing <code>Cache</code>s
	 * @return the first <code>Cache</code> into <code>caches</code> that
	 *         contains at least an element
	 */
	private Cache getFirstNonEmptyCache(List caches) {
		synchronized (caches) {
			Iterator it = caches.iterator();
			while (it.hasNext()) {
				Cache next = (Cache) it.next();
				if (next.size() > 0)
					return next;
			}
		}
		return null;
	}

	private List getUpstreamChain() {
		List upstr = new ArrayList(regionalCache.cache);
		// upstr.addAll(regionalCache.siblings);
		if (father != null) {
			upstr.add(father);// adds at the end
		}
		return upstr;
	}

	private void makeDepthLessThan(Color otherCol, double otherDepth) {
		if (color.equals(otherCol)) {
			if (otherDepth < depth) {
				double increment;

				increment = 2 - rnd.nextDouble() * 2;
				while (otherDepth - fatherDepth <= increment)
					increment = increment / 2;
				depth = otherDepth - increment;
			}
		}
	}

	private boolean shouldAcceptRequest(Color otherCol, double otherDepth) {
		logger.finer("TopologyManager, determining whether " + otherCol + "," + otherDepth + " can connect to me: "
				+ color + "," + depth);
		if (canConnectToMe(otherCol, otherDepth)) {
			logger.finer("color Ok");
			return true;
		} else if (color.equals(otherCol) && !color.equals(Color.COLORLESS) && otherDepth > fatherDepth) {
			logger.finer("depthOk");
			return true;
		} else {
			logger.finer("No: are colors equal? " + color + ".equals(" + otherCol + ") -> " + color.equals(otherCol)
					+ " fatherDepth is " + fatherDepth + "toStringEquals "
					+ color.toString().equals(otherCol.toString()) + " compareTo "
					+ color.toString().compareTo(otherCol.toString()));
			return false;
		}
	}

	/**
	 * Check whether the <code>senderID</code> can be a new son of the local
	 * node and sends it a response message.
	 * 
	 * @param m
	 *            the message received from the requesting node.
	 * @param senderID
	 *            the requesting node
	 */
	private synchronized void examineRequest(LSTreeMsg m, NodeDescriptor senderID, Transport senderTransport) {
		logger.finer("TopologyManager: REQMSG received by " + LSTreeTopologyManager.this.localID.getUrls()[0]
				+ " from " + senderID.getUrls()[0] + ": " + m.toString());
		LSTreeMsg response = null;
		boolean accepted = false;
		/*
		 * If this node is colorless, and the other node is colored, color this
		 * node and accept connection. Start the rootConnector.
		 */
		if (overlay.hasNeighbor(senderID)) {
			logger.finer("TopologyManager: Ignoring request from a neighbor " + senderID.getUrls()[0]);
			return;
		} else if (!shouldAcceptRequest(m.getColor(), m.getDepth())) {
			response = new LSTreeMsg(LSTreeMsg.NOT_DEPTH_OR_COLOR, color, depth, false, false, null, null, null,
					this.localID);
			if (color.canConnectTo(m.getColor())) {
				NodeDescriptor rootNd = new NodeDescriptor();
				rootNd.addUrl(color.getRootUrl());
				logger.finer("Telling own root " + rootNd.getUrls()[0] + " about " + senderID.getUrls()[0]);
				tellNodeAbout(rootNd, senderID);// tell own root about the
												// sender.
			} else {

				NodeDescriptor rootNd = new NodeDescriptor();
				rootNd.addUrl(color.getRootUrl());
				logger.finer("Telling other " + senderID.getUrls()[0] + " about my root " + rootNd.getUrls()[0]);
				response.setGoodCandidate(rootNd);
			}
		} else if (busy && !canConnectToMe(m.getColor(), m.getDepth())) {
			// Busy Refusal
			response = new LSTreeMsg(LSTreeMsg.BUSY, color, depth, false, false, null, null, null, this.localID);
		} else if (numberOfBrokers() >= MAX_DEGREE && !m.isMaxForced()) {
			List sons = (List) new ArrayList(getNeighboringBrokers());
			// the upstream chain of the remote node is formed by the local
			// node's siblings plus the local node's upstream chain
			if (father != null) {
				sons.remove(father);
			}
			response = new LSTreeMsg(LSTreeMsg.NOT_MAX_DEGREE, color, depth, false, false, null, sons, null,
					this.localID);
		} else if (numberOfBrokers() < MIN_DEGREE && !m.isMinForced()) {
			List upstr = new ArrayList();
			if (father != null)
				upstr.add(father);
			response = new LSTreeMsg(LSTreeMsg.NOT_MIN_DEGREE, color, depth, false, false, upstr, null, null,
					this.localID);

		} else {
			makeDepthLessThan(m.getColor(), m.getDepth());
			List sons = (List) new ArrayList(getNeighboringBrokers());
			// the upstream chain of the remote node is formed by the local
			// node's siblings plus the local node's upstream chain
			List upstr = getUpstreamChain();
			// upstr.addAll(regionalCache.siblings);
			if (father != null) {
				// to be sure of not risk a cycle, remove the local father from
				// the list.
				sons.remove(father);
			}
			response = new LSTreeMsg(LSTreeMsg.CONNECTION_ACCEPTED, color, depth, false, false, upstr, sons, null,
					this.localID);
			accepted = true;
			informDownstreamNeighbors(m, senderID, response);// in the simulator
																// this is done
																// after sending
																// the response,
																// but it should
																// not make any
																// difference
		}
		// send the message to the requesting node
		logger.finer("TopologyManager: RESPMSG sent by " + this.localID.getUrls()[0] + " to " + senderID.getUrls()[0]
				+ " response: " + response.toString());
		try {
			// DAVIDE: removed: Transport transport = (Transport)
			// tempNeighbors.get(senderID);
			// CHECK
			System.out.println("transport" + senderTransport);

			senderTransport.send(response.getType(), response, senderID, Transport.MISCELLANEOUS_CLASS);
		} catch (NotConnectedException e) {
			logger.warning("The msg " + response.toString() + " cannot be sent to " + senderID.getID()
					+ " because of a NotConnectedException " + e.getMessage());
			accepted = false; // if we didn't send the response, let us just
								// undo the accept... but shouldn't it be
								// notified by signal linkdead
		}
		synchronized (neighborTransport) {
			if (accepted) {
				System.out.println("removing " + senderID.getUrls()[0] + " from tempNeighbors (ACCEPTSENT)");

				// DAVIDE: removed: tempNeighbors.remove(senderID);
				confirmNeighbor(senderID, senderTransport);
			}
		}
	}

	private void tellNodeAbout(NodeDescriptor recipient, NodeDescriptor candidate) {
		if (selectUrl(recipient.getUrls()).equals(selectUrl(localID.getUrls()))) {
			logger.finer("adding " + candidate.getUrls()[0] + " to own global cache");
			globalCache.setGoodCandidate(candidate);
		} else {
			logger.finer("sending good candidate containing " + candidate.getUrls()[0] + " to "
					+ recipient.getUrls()[0]);
			LSTreeMsg msg = new LSTreeMsg(candidate);
			String url = selectUrl(recipient.getUrls());
			Transport t = overlay.resolveUrl(url);
			NodeDescriptor dest = openSafeLink(t, url);
			if (dest != null) {
				try {
					t.send(msg.getType(), msg, dest, Transport.MISCELLANEOUS_CLASS);
				} catch (NotConnectedException e) {
					if (dest != null)
						logger.finer("NotConnectedException to " + " " + dest.toString());
					else
						logger.finer("NotConnectedException to " + " null ");
				}
				closeSafeLink(t, dest);
			}
		}
	}

	/**
	 * When a connection request is accepted, insert the requester into the data
	 * structure of the active neighbor and informs the other sons of the local
	 * node of the new sibling.
	 * 
	 * @param request
	 *            the request message sent by the requester node
	 * @param requester
	 *            the requester node
	 * @param response
	 *            the response to the request
	 */
	private void informDownstreamNeighbors(LSTreeMsg request, NodeDescriptor requester, LSTreeMsg response) {

		// add the new son
		// if connection is accepted, notify the other sons.
		// send a new sibling msg to all my children except the new one.
		Map l = new HashMap(neighborTransport);
		// remove the new neighbor from the list
		l.remove(request.getNode());
		if (father != null)
			if (l.keySet().contains(father))
				l.remove(father);
		Iterator children = l.keySet().iterator();
		LSTreeMsg childMsg = new LSTreeMsg(LSTreeMsg.NEW_SIBLING, request.getColor(), request.getDepth(), false, false,
				request.getUpstreamChain(), request.getDownStreamChain(), request.getGlobalCache(), request.getNode());
		while (children.hasNext()) {
			NodeDescriptor next = (NodeDescriptor) children.next();
			try {
				Transport transport = (Transport) l.get(next);
				transport.send(LSTreeMsg.NEW_SIBLING, childMsg, next, Transport.MISCELLANEOUS_CLASS);
			} catch (NotConnectedException e) {
				logger.warning("The msg " + childMsg.toString() + " cannot be sent to " + next.getID()
						+ " because of a NotConnectedException " + e.getMessage());
			} catch (Exception e) {
				e.printStackTrace();
				logger.warning("The msg " + childMsg.toString() + " cannot be sent to " + next.getID()
						+ " because of an exception " + e.getMessage());
			}
		}
	}

	private void makeCDGreaterThan(Color otherCol, double otherDepth) {
		Color oldColor = color;
		color = otherCol;
		logger.finer("making depth which is now " + depth + " greater than " + otherDepth);
		if (depth <= otherDepth) {
			if (oldColor.equals(otherCol))
				logger.severe("TopologyManager: ERROR in makeCDGreater Inverted Depth in same tree: color " + color
						+ " mydepth=" + depth + " otherDepth=" + otherDepth);

			depth = otherDepth + 2 + rnd.nextDouble();
		}
		if (depth > otherDepth + 2 + 1)
			depth = otherDepth + 2 + rnd.nextDouble();

		logger.finer("made it " + depth);
	}

	/**
	 * Manages the arrival of a new message and if is a message for the local
	 * topology manager it performs the corresponding tasks.
	 * 
	 * @see AbstractTopologyManager#signalPacket(String, NodeDescriptor,
	 *      Serializable)
	 */
	public void signalPacket(String subject, NodeDescriptor senderID, Serializable payload, Transport t) {
		super.signalPacket(subject, senderID, payload, t);
		if (subject.equals(LSTreeMsg.SIBLING_DEAD)) {
			// a sibling is dead
			synchronized (this) {
				regionalCache.removeSibling(((LSTreeMsg) payload).getNode());
				// update father informations
				fatherDepth = ((LSTreeMsg) payload).getDepth();
				globalCache.addAllCandidates(((LSTreeMsg) payload).getGlobalCache(), GLOBAL_CACHE_LENGTH);
			}
		} else if (subject.equals(LSTreeMsg.REQUEST)) {
			// a request for connection from another topology manager
			LSTreeMsg m = (LSTreeMsg) payload;
			examineRequest(m, senderID, t);
		} else if (subject.equals(LSTreeMsg.UPDATE)) {
			LSTreeMsg m = (LSTreeMsg) payload;
			synchronized (this) {
				if (father != null && father.equals(m.getNode())) {
					boolean changedColor = false;
					if (!color.equals(m.getColor()))
						changedColor = true;
					fatherDepth = m.getDepth();
					this.regionalCache.clear();
					this.regionalCache.addAllCandidates(m.getUpstreamChain(), UPSTREAM_CHAIN_LENGTH);
					makeCDGreaterThan(m.getColor(), m.getDepth());
					// send the message to my children
					long hopToLive = m.getHopToLive() - 1;
					if (hopToLive < 0)
						hopToLive = 0;
					LSTreeMsg msg = null;
					if (hopToLive > 0 || changedColor)
						msg = new LSTreeMsg(LSTreeMsg.UPDATE, this.color, this.depth, false, false, getUpstreamChain(),
								null, null, this.localID, hopToLive);
					if (msg != null)
						updatePropagation(msg);
				} else {

					logger.severe("update message from an unidentified neighbor");
				}
			}
		} else if (subject.equals(LSTreeMsg.NEW_SIBLING)) {
			synchronized (this) {
				// a new sibling has been found
				if (((LSTreeMsg) payload).getNode().equals(localID))
					// if the new sibling is the same node, ingore the message
					return;
				regionalCache.siblings.add(((LSTreeMsg) payload).getNode());
				globalCache.addAllCandidates(((LSTreeMsg) payload).getGlobalCache(), GLOBAL_CACHE_LENGTH);
			}
			logger.finer("New sibling: " + ((LSTreeMsg) payload).toString());
		} else if (subject.equals(LSTreeMsg.BUSY) || subject.equals(LSTreeMsg.NOT_DEPTH_OR_COLOR)
				|| subject.equals(LSTreeMsg.NOT_MAX_DEGREE) || subject.equals(LSTreeMsg.NOT_MIN_DEGREE)
				|| subject.equals(LSTreeMsg.CONNECTION_ACCEPTED)) {
			/*
			 * NOTE: it is necessary to list all these cases without using the
			 * else because in this cases could be treated also messages that do
			 * not belong to the LSTreeTopologyManager.
			 */
			logger.finer("putting response " + subject + ": " + payload + " into pendingConnections");
			try {
				responseBucket.setResponse((LSTreeMsg) payload);
			} catch (UnexpectedResponseException e) {
				logger.severe("Unexpected response: " + e.getMessage());
				e.printStackTrace();
				handleUnexpectedResponse((LSTreeMsg) payload, t, senderID);
			}

		} else if (subject.equals(LSTreeMsg.PERIODIC_UPDATE)) {
			logger.finer("got periodic update");
			List nodes = ((LSTreeMsg) payload).getGlobalCache();
			nodes.removeAll(this.regionalCache.cache);
			nodes.removeAll(this.regionalCache.siblings);
			this.globalCache.addAllCandidates(nodes, GLOBAL_CACHE_LENGTH);
		} else if (subject.equals(LSTreeMsg.GOOD_CANDIDATE)) {
			logger.finer("being told aboout good candidate: " + ((LSTreeMsg) payload).getGoodCandidate().getUrls()[0]);
			globalCache.setGoodCandidate(((LSTreeMsg) payload).getGoodCandidate());
		} else if (subject.equals(LSTreeMsg.NOLONGERCHILD)) {
			logger.finer("child signalled an unxpected (late) accept: " + senderID.getUrls()[0]);
			signalLinkDead(senderID);
			// if(neighborTransport.containsKey(senderID)){
			// logger.finer("Taking action for INVALID Link to "+senderID.getUrls()[0]);
			// neighborGone(senderID);
			// } else {
			// logger.severe("child was not a neighbor: "+senderID.getUrls()[0]);
			// }
		}
	}

	private void handleUnexpectedResponse(LSTreeMsg msg, Transport t, NodeDescriptor senderID) {
		if (msg.getType().equals(LSTreeMsg.CONNECTION_ACCEPTED)) {
			LSTreeMsg m = new LSTreeMsg();
			try {
				t.send(m.getType(), m, senderID, Transport.MISCELLANEOUS_CLASS);
			} catch (NotConnectedException e) {
				logger.finer("Not Connected Exception when sending NOLONGERCHILD");
				e.printStackTrace();
			}

		}
	}

	/**
	 * Set the local data.
	 * 
	 * @param response
	 *            the message containing the local data.
	 */
	private synchronized void updateLocalData(LSTreeMsg response) {
		logger.finer("Update local data: " + response.toString() + " setting father to " + response.getNode());
		// the connection has been accepted
		// save data from the father
		father = response.getNode();
		fatherDepth = response.getDepth();
		rootConnector.exit();
		makeCDGreaterThan(response.getColor(), response.getDepth());
		regionalCache.siblings.clear();
		regionalCache.cache.clear();
		regionalCache.addAllCandidates(response.getUpstreamChain(), UPSTREAM_CHAIN_LENGTH);
		regionalCache.addAllSiblings(response.getDownStreamChain());

	}

	/**
	 * Create a new tree creating a new color. It then informs the children of
	 * the new color.
	 */
	private synchronized void createNewTree() {
		globalCache.addAllCandidates(regionalCache.cache, GLOBAL_CACHE_LENGTH);
		globalCache.addAllCandidates(regionalCache.siblings, GLOBAL_CACHE_LENGTH);
		regionalCache.clear();
		father = null;
		fatherDepth = 0;
		this.color.updateColor(selectUrl(localID.getUrls()));
		Random rnd = new Random();
		this.depth = DEFAULT_DEPTH_INCREMENT + rnd.nextGaussian();
		logger.finer("TopologyManager: Node " + this.localID.getUrls()[0] + " (" + this.color.toString() + ","
				+ this.depth + ") " + "created a new tree");
		// update propagation of dowstream chain and color
		LSTreeMsg updater = new LSTreeMsg(LSTreeMsg.UPDATE, color, depth, false, false, getUpstreamChain(), null, null,
				localID, UPSTREAM_CHAIN_LENGTH);
		updatePropagation(updater);
	}

	/**
	 * Search for other tree to connect. It must be active only when the local
	 * node is the root of its tree.
	 * 
	 * @author Alessandro Monguzzi
	 * 
	 */
	private class RootConnector implements Runnable {
		private boolean exit = false;
		private Object synch = new Object();

		/**
		 * Stop the thread and exit.
		 * 
		 */
		public void exit() {
			synchronized (synch) {
				exit = true;
			}
		}

		/**
		 * Reset the state of the <code>RootConnector</code>.
		 * 
		 */
		public void reset() {
			synchronized (synch) {
				exit = false;
			}
		}

		/**
		 * Search for other trees to connect to.
		 */
		public void run() {
			while (!exit) {
				try {
					Thread.sleep(WAITING_TIME);
				} catch (InterruptedException e) {
				}
				synchronized (synch) {
					/*
					 * Double check, do not remove it.
					 */
					if (!exit) {
						NodeDescriptor newParent = null;
						newParent = searchFather(father, true);
						if (newParent != null) {
							exit = true;
							// NOT TO BE DONE ALREADY DONE IN TRY CANDIDATE
							// confirmConnection(newParent);
						}
					}
				}
			}
		}
	}

	private class PeriodicUpdater implements Runnable {
		private boolean exit = false;
		private Object synch = new Object();

		public void exit() {
			synchronized (synch) {
				exit = true;
			}
		}

		public void run() {
			while (!exit) {
				synchronized (synch) {

					logger.finer("my color is " + color + " and depth is " + depth);
					if (father != null)
						logger.finer("my parent is " + father.getUrls()[0] + " with depth " + fatherDepth);
					else
						logger.finer("my parent is null");
					Collection allNeighbors = getNeighboringBrokers();
					Iterator ch = allNeighbors.iterator();
					while (ch.hasNext()) {
						NodeDescriptor n = (NodeDescriptor) ch.next();
						logger.finer("neighbor: " + n.getUrls()[0]);
						System.out.println("neighbor " + n.getUrls()[0] + " has transport "
								+ ((Transport) neighborTransport.get(n)));
					}
					Collection nodesToSend = ((GlobalCache) LSTreeTopologyManager.this.globalCache).getAFraction(
							LSTreeTopologyManager.GLOBAL_CACHE_FRACTION, null);
					Collection nodesThatReceive = ((GlobalCache) LSTreeTopologyManager.this.globalCache).getAFraction(
							LSTreeTopologyManager.GLOBAL_CACHE_FRACTION, nodesToSend);
					LSTreeMsg msg = new LSTreeMsg((List) nodesToSend);
					Iterator it = nodesThatReceive.iterator();
					while (it.hasNext()) {
						NodeDescriptor next = (NodeDescriptor) it.next();
						String url = selectUrl(next.getUrls());
						Transport t = overlay.resolveUrl(url);
						NodeDescriptor dest = openSafeLink(t, url);
						if (dest != null) {
							try {
								t.send(msg.getType(), msg, dest, Transport.MISCELLANEOUS_CLASS);
							} catch (NotConnectedException e) {
								if (dest != null)
									logger.finer("NotConnectedException to " + " " + dest.toString());
							}
							closeSafeLink(t, dest);
						}
					}
				}
				try {
					Thread.sleep(LSTreeTopologyManager.this.periodicSleep);
				} catch (InterruptedException e) {
					logger.severe(e.getMessage());
				}
			}
		}
	}

	/**
	 * This class contains all the candidate for reconnection known to the local
	 * node.<br>
	 * It acts like a circular queue of candidates and offers all the methods
	 * necessary to manage it.<br>
	 * It guarantees the absence of duplicates.
	 * <p>
	 * Users of this class should redefine the <code>tryCache</code> method, at
	 * least specifying the flag <code>force</code>.
	 * </p>
	 * 
	 * @author Alessandro Monguzzi
	 * 
	 */
	private class Cache {
		/**
		 * The cache.
		 */
		protected List cache = null;
		/**
		 * The next candidate returned.
		 */
		protected int position = 0;
		/**
		 * Cache type.
		 */
		protected String name = null;

		/**
		 * Create a new <code>Cache</code>.
		 * 
		 * @param name
		 *            cache type
		 */
		public Cache(String name) {
			this.name = name;
			cache = new Vector();
		}

		/**
		 * Create a new <code>Cache</code> containing a real copy of the given
		 * <code>Cache</code>.
		 * 
		 * @param c
		 *            the given <code>Cache</code>
		 */
		public Cache(Cache c) {
			cache = (List) ((Vector) c.cache).clone();
			this.name = c.name;
		}

		/**
		 * Get a new candidate.
		 * 
		 * @return the next candidate in order.
		 */
		public NodeDescriptor getNextCandidate() {
			if (cache.size() > 0) {
				return (NodeDescriptor) cache.remove(cache.size() - 1);
			} else
				return null;
		}

		/**
		 * Add a new candidate into the cache. If it is already present, it does
		 * nothing.
		 * 
		 * @param n
		 *            the new candidate.
		 */
		public void addCandidate(NodeDescriptor n, int maxNumber) {
			if (!cache.contains(n)) {
				if (cache.size() > maxNumber)
					cache.remove(0);
				cache.add(n);
			}
		}

		/**
		 * Add all the candidates of the collection to the local cache. If the
		 * maximun number allowed is exceeded, it removes the first elements of
		 * the cache.
		 * 
		 * @param nodeDescriptors
		 */
		public void addAllCandidates(Collection nodeDescriptors, int maxNumber) {
			if (nodeDescriptors != null) {
				Iterator it = nodeDescriptors.iterator();
				while (it.hasNext()) {
					NodeDescriptor node = (NodeDescriptor) it.next();
					if (!alreadyTried.contains(node))
						addCandidate(node, maxNumber);
				}
			}
		}

		/**
		 * Remove a candidate from the cache. If it is not present, it does
		 * nothing.
		 * 
		 * @param n
		 *            the candidate removed.
		 */
		public void removeCandidate(NodeDescriptor n) {
			cache.remove(n);
		}

		/**
		 * Clear the cache.
		 * 
		 */
		public void clear() {
			cache.clear();
		}

		/**
		 * Get the size of the cache.
		 */
		public int size() {
			return cache.size();
		}

		public LSTreeMsg tryCandidate(NodeDescriptor candidate, boolean forceMax, boolean forceMin)
				throws AlreadyExistingLinkException {
			logger.finer("Trying candidate with " + this.name + " cache");
			return LSTreeTopologyManager.this.tryCandidate(selectUrl(candidate.getUrls()), forceMax, forceMin);
		}

		public LSTreeMsg tryCandidate(NodeDescriptor candidate) throws AlreadyExistingLinkException {
			return tryCandidate(candidate, false, false);
		}

	}

	/**
	 * Concrete implementation of the <code>Cache</code> class. It forces the
	 * candidate to accept the connection.
	 * 
	 * @author Alessandro Monguzzi
	 * 
	 */
	private class MaxDegreeCache extends Cache {
		/**
		 * Create a new <code>MaxDegreeCache</code>.
		 * 
		 * @param name
		 *            cache name
		 */
		public MaxDegreeCache(String name) {
			super(name);
		}

		/**
		 * It acts like <code>Cache.tryCache(deadNeighbor, true)</code>.
		 * 
		 * @param deadNeighbor
		 * @return
		 * @throws AlreadyExistingLinkException
		 */
		public LSTreeMsg tryCandidate(NodeDescriptor deadNeighbor) throws AlreadyExistingLinkException {
			return super.tryCandidate(deadNeighbor, true, false);
		}
	}

	/**
	 * Concrete implmentation of the <code>Cache</code> class. It forces the
	 * candidate to accept the connection.
	 * 
	 * @author Alessandro Monguzzi
	 * 
	 */
	private class MinDegreeCache extends Cache {
		/**
		 * Create a new <code>MinDegreeCache</code>.
		 * 
		 * @param name
		 *            cache name
		 */
		public MinDegreeCache(String name) {
			super(name);
		}

		/**
		 * It acts like <code>Cache.tryCache(deadNeighbor, true)</code>.
		 * 
		 * @param deadNeighbor
		 * @return
		 * @throws AlreadyExistingLinkException
		 */
		public LSTreeMsg tryCandidate(NodeDescriptor deadNeighbor) throws AlreadyExistingLinkException {
			return super.tryCandidate(deadNeighbor, false, true);
		}
	}

	/**
	 * Concrete implmentation of the <code>Cache</code> class. It does not force
	 * the candidate to accept the connection.
	 * 
	 * @author Alessandro Monguzzi
	 * 
	 */
	private class GlobalCache extends Cache {
		/**
		 * Create a new <code>GlobalCache</code>.
		 * 
		 */

		NodeDescriptor goodCandidate;

		public synchronized NodeDescriptor getNextCandidate() {
			NodeDescriptor gc = getGoodCandidate();
			if (gc != null) {
				logger.finer("returning good candidate " + gc.getUrls()[0]);
				return gc;
			} else
				return super.getNextCandidate();
		}

		public GlobalCache(String name) {
			super(name);
			goodCandidate = null;
		}

		/**
		 * Create a new <code>GlobalCache</code> containing a real copy of the
		 * given <code>GlobalCache</code>.
		 * 
		 * @param c
		 *            the given <code>GlobalCache</code>
		 */
		public GlobalCache(GlobalCache c) {
			super(c);
			goodCandidate = c.goodCandidate;
		}

		public synchronized void setGoodCandidate(NodeDescriptor gc) {
			goodCandidate = gc;
		}

		public synchronized NodeDescriptor getGoodCandidate() {
			NodeDescriptor n = goodCandidate;
			goodCandidate = null;
			return n;
		}

		/**
		 * Get a subset of the <code>NodeDescriptor</code>s contained into the
		 * cache.
		 * 
		 * @param fraction
		 *            the dimension of the subset as percentage of the cache
		 *            size
		 * @param ignores
		 *            <code>NodeDescriptor</code>s that cannot be returned
		 * @return a <code>Collection</code> of <code>NodeDescriptor</code>
		 */
		public Collection getAFraction(double fraction, Collection ignores) {
			Random rnd = new Random();
			Collection results = new Vector();
			double nodesToGet = this.cache.size() * LSTreeTopologyManager.GLOBAL_CACHE_FRACTION;
			while (nodesToGet >= 1) {
				synchronized (this.cache) {
					NodeDescriptor n = (NodeDescriptor) this.cache.get(rnd.nextInt(this.cache.size()));
					if ((ignores == null || !ignores.contains(n)) && !results.contains(n)) {
						results.add(n);
						nodesToGet--;
					}
				}
			}
			return results;
		}
	}

	/**
	 * Implementation of the Regional Cache. It extends the <code>Cache</code>
	 * adding another queue of candidates with the same properties of unicity.<br>
	 * When it is requested for a candidate, it returns the element if the new
	 * queue with the probability specified by <code>SIBLINGCACHE</code>.
	 * <p>
	 * The add/remove method of the super class are used to add/remove
	 * candidates in the upstream chain; to add/remove candidates into the
	 * sibling set use the specific methods of this class.
	 * </p>
	 * 
	 * @author Alessandro Monguzzi
	 * 
	 */
	private class RegionalCache extends Cache {
		/**
		 * Chanche to use the sibling set instead of the upstream chain.
		 */
		public static final double SIBLINGCHANCHE = 0.75;
		protected List siblings = null;

		/**
		 * Create a new <code>RegionalCache</code>.
		 * 
		 */
		public RegionalCache(String name) {
			super(name);
			siblings = new Vector();
		}

		public RegionalCache(RegionalCache c) {
			super(c);
			siblings = (List) ((Vector) c.siblings).clone();
		}

		/**
		 * Get a new candidate from one of the two queues with a specific
		 * chanche. If some queue is empty it tries with the other one. If both
		 * are empty, it returns <code>null</code>.
		 */
		public NodeDescriptor getNextCandidate() {
			if (cache.size() > 0 && siblings.size() > 0) {
				Random rnd = new Random();
				if (rnd.nextGaussian() > SIBLINGCHANCHE) {
					return super.getNextCandidate();
				} else {
					return (NodeDescriptor) siblings.remove(siblings.size() - 1);
				}
			} else if (siblings.size() > 0) {
				return (NodeDescriptor) siblings.remove(siblings.size() - 1);
			} else if (cache.size() > 0) {
				return super.getNextCandidate();
			} else
				return null;
		}

		/**
		 * Add a new candidate into the sibling set. If it is already present,
		 * it does nothing.
		 * 
		 * @param n
		 *            the new candidate.
		 */
		public void addSibling(NodeDescriptor n) {
			if (!siblings.contains(n))
				siblings.add(n);
		}

		/**
		 * Add to the sibling set all the <code>NodeDescriptor</code>s contained
		 * into the <code>Collection</code>.
		 * 
		 * @param nodeIds
		 *            a <code>Collection</code> of <code>NodeDescriptor</code>
		 */
		public void addAllSiblings(Collection nodeIds) {
			if (nodeIds != null) {
				Iterator it = nodeIds.iterator();
				while (it.hasNext())
					addSibling((NodeDescriptor) it.next());
			}
		}

		/**
		 * Clear both the sibling set and the upstream chain.
		 */
		public void clear() {
			super.clear();
			siblings.clear();
		}

		/**
		 * Get the size of the two caches.
		 */
		public int size() {
			return super.size() + siblings.size();
		}

		/**
		 * Remove a candidate from the sibling set. If it is not present, it
		 * does nothing.
		 * 
		 * @param n
		 *            the candidate removed.
		 */
		public void removeSibling(NodeDescriptor n) {
			siblings.remove(n);
		}
	}

}

/**
 * This class represents the color of the LSTree algorithm.<br>
 * The color is a sequence of colors that can be compared one another. The
 * comparison between two color is based on the number of colors they are
 * composed to and by the lexicographical order.
 * 
 * @author Alessandro Monguzzi
 * 
 */
class Color implements Serializable {

	/**
	 * The non color lesser than any other color.
	 */
	public static String COLORLESS = "";
	private static final long serialVersionUID = -2651276035008460235L;
	/**
	 * The separator between two color forming the local color.
	 */
	public static String SEPARATOR = ";";
	private String color = null;

	/**
	 * Create a new color <code>COLORLESS</code>.
	 * 
	 */
	public Color() {
		color = COLORLESS;
	}

	/**
	 * Update the actual color adding <code>s</code>.
	 * 
	 * @param s
	 */
	public void updateColor(String s) {
		if (color == null)
			color = s;
		else
			color = color + SEPARATOR + s;
	}

	/**
	 * Get the color.
	 * 
	 * @return the actual color
	 */
	public String getColor() {
		return color;
	}

	/**
	 * Remove the color.
	 * 
	 */
	public void resetColor() {
		color = COLORLESS;
	}

	/**
	 * Compare the local color with a color passed as parameter.<br>
	 * <p>
	 * Initially it checks the lengths.<br>
	 * If the local length is greater than the parameter's it returns a positive
	 * value. If the local length is lesser than the parameter's it returns a
	 * negative value.
	 * </p>
	 * If the two lengths are equal, it checks the lexicographical order.<br>
	 * If the local node's precedes the the parameter's it returns a positive
	 * value, else a negative value. If the two colors are equal again, it
	 * returns zero.
	 * 
	 * @param arg0
	 * @return a positive value if the local color is greater than the
	 *         parameter's; a negative if it is lesser, zero if they are equal.
	 */

	public boolean canConnectTo(Color other) {
		String[] thisCol = color.split(SEPARATOR);
		String[] otherCol = other.color.split(SEPARATOR);
		System.out.print("comparing " + thisCol.length + ":" + color + " and " + otherCol.length + ":" + other.color
				+ " ");
		if (color.equals(Color.COLORLESS) && !other.color.equals(Color.COLORLESS)) {
			System.out.println("TRUE 1");
			return true;
		} else {
			if (thisCol.length == otherCol.length) {
				int i = 0;
				int order = 0;
				do {
					order = thisCol[i].compareTo(otherCol[i]);
					i++;
				} while (order == 0 && i < thisCol.length);
				if (order > 0) {
					System.out.println("TRUE SAME LENGTH");
					return true;
				} else {
					System.out.println("FALSE SAME LENGTH");
					return false;
				}
			} else {
				System.out.println("LONGER");
				return thisCol.length < otherCol.length;
			}
		}
	}

	/**
	 * @see String#equals(java.lang.Object)
	 */
	public boolean equals(Object arg0) {
		if (this == arg0)
			return true;
		else if (!(arg0 instanceof Color))
			return false;
		else
			return color.equals(((Color) arg0).color);
	}

	/**
	 * @see String#hashCode()
	 */
	public int hashCode() {
		return color.hashCode();
	}

	/**
	 * Return the String representation of the color.
	 */
	public String toString() {
		return color;
	}

	/**
	 * Return the url of the actual root.
	 * 
	 * @return a <code>String</code> representing the root url
	 */
	public String getRootUrl() {
		return this.color.substring(color.lastIndexOf(SEPARATOR) + 1);
	}
}
