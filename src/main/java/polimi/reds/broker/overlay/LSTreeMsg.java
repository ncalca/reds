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
import java.util.Collection;
import java.util.List;

import polimi.reds.NodeDescriptor;
import polimi.reds.broker.overlay.Color;

/**
 * This class represents the messages exchanged between two
 * <code>LSTreeTopologyManager</code>s.
 * 
 * @author Alessandro Monguzzi
 * 
 */
public class LSTreeMsg implements Serializable {
	private static final long serialVersionUID = -8558148241211234803L;
	/**
	 * Message to inform neighbors of the dead of one of theirs sibling.
	 */
	public final static String SIBLING_DEAD = "LSTreeSiblingDead";
	/**
	 * Message to request a connection to a node.
	 */
	public final static String REQUEST = "LSTreeNeighborRequest";
	/**
	 * Message to update upstream chain or color data.
	 */
	public final static String UPDATE = "LSTreeUpstreamOrColorUpdate";
	/**
	 * Message to accept a connection from a requesting node.
	 */
	public final static String CONNECTION_ACCEPTED = "LSTreeConnectionAccepted";
	/**
	 * Message to refuse a connection cause depth or color.
	 */
	public final static String NOT_DEPTH_OR_COLOR = "LSTreeNotDepthOrColor";
	/**
	 * Message to refuse a connection cause max degree.
	 */
	public final static String NOT_MAX_DEGREE = "LSTreeNotMaxDegree";
	/**
	 * Message to refuse a connection cause min degree.
	 */
	public final static String NOT_MIN_DEGREE = "LSTreeNotMinDegree";
	/**
	 * Message to refuse a connection cause busy.
	 */
	public final static String BUSY = "LSTreeBusy";
	/**
	 * Message to notify sons of a new sibling.
	 */
	public final static String NEW_SIBLING = "LSTreeNewSibling";
	/**
	 * Message to populate the global cache.
	 */
	public final static String PERIODIC_UPDATE = "LSTreePeriodicUpdate";
	/**
	 * Message to notify to the root a good candidate to connect to.
	 */
	public final static String GOOD_CANDIDATE = "LSTreeGoodCandidate";
	public static final String NOLONGERCHILD = "NoLongerChild";

	private Color col;
	private double depth = 0;
	private String url = null;
	private List upstream = null;
	private List siblings = null;
	private List global = null;
	private NodeDescriptor node = null;
	private NodeDescriptor good = null;
	private long hopToLive = 0;
	/**
	 * Identifies the subject of the message. It should be equal to the subject
	 * used to send it.
	 */
	private String type = null;

	/**
	 * Force the connection even in the cases of refusal from maxDegree.
	 */
	private boolean forceMax = false;
	/**
	 * Force the connection even in the cases of refusal from minDegree.
	 */
	private boolean forceMin = false;

	/**
	 * Create a new <code>LSTreeMsg</code>.
	 * 
	 * @param type
	 *            the type of the message
	 * @param c
	 *            the color of the sender
	 * @param depth
	 *            the depth of the sender
	 * @param forceMax
	 *            if <code>true</code> force the connection
	 * @param forceMin
	 *            if <code>true</code> force the connection
	 * @param upstream
	 *            the upstream chain
	 * @param sons
	 *            the sibling set
	 * @param global
	 *            the global cache
	 * @param node
	 *            the node sender/receiver.
	 */
	public LSTreeMsg(String type, Color c, double depth, boolean forceMax, boolean forceMin, List upstream, List sons,
			List global, NodeDescriptor node) {
		this.col = c;
		this.depth = depth;
		this.siblings = sons;
		this.upstream = upstream;
		this.global = global;
		this.forceMax = forceMax;
		this.forceMin = forceMin;
		this.node = node;
		this.type = type;
	}

	public LSTreeMsg(String type, Color c, double depth, boolean forceMax, boolean forceMin, List upstream, List sons,
			List global, NodeDescriptor node, long htl) {
		this(type, c, depth, forceMax, forceMin, upstream, sons, global, node);
		hopToLive = htl;
	}

	/**
	 * Create a new
	 * <code>PERIODIC_UPDATE/code> message in which the <code>globalCache</code>
	 * is a <code>List</code> of node to be insterted into the global cache.
	 * 
	 * @param good
	 *            the <code>NodeDescriptor</code> of a good node
	 * @param nodes
	 *            a <code>List</code> of <code>NodeDescriptor</code>s
	 */
	public LSTreeMsg(List nodes) {
		this.type = LSTreeMsg.PERIODIC_UPDATE;
		this.global = nodes;
	}

	/**
	 * Create a new <code>GOOD_CANDIDATE</code> message.
	 * 
	 * @param good
	 *            the <code>NodeDescriptor</code> of the good candidate
	 */
	public LSTreeMsg(NodeDescriptor good) {
		this.type = LSTreeMsg.GOOD_CANDIDATE;
		this.good = good;
	}

	public LSTreeMsg() {
		this.type = LSTreeMsg.NOLONGERCHILD;
	}

	/**
	 * Get the upstream chain.
	 * 
	 * @return the cache
	 */
	public List getUpstreamChain() {
		return upstream;
	}

	/**
	 * The given node is a good candidate.
	 * 
	 * @param good
	 *            the given node's <code>NodeDescriptor</code>
	 */
	public void setGoodCandidate(NodeDescriptor good) {
		this.good = good;
	}

	/**
	 * Get the good candidate.
	 * 
	 * @return the good candidate's <code>NodeDescriptor</code>
	 */
	public NodeDescriptor getGoodCandidate() {
		return good;
	}

	/**
	 * Get the downstream chain.
	 * 
	 * @return the cache
	 */
	public List getDownStreamChain() {
		return siblings;
	}

	/**
	 * Get the message type.
	 * 
	 * @return the message type
	 */
	public String getType() {
		return type;
	}

	/**
	 * Set the type of the message.
	 * 
	 * @param type
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * Set the sender of the message.
	 * 
	 * @return the sender
	 */
	public NodeDescriptor getNode() {
		return node;
	}

	/**
	 * Get the force message.
	 * 
	 * @return <code>true</code> iff the flag force is <code>true</code>
	 */
	public boolean isMaxForced() {
		return forceMax;
	}

	/**
	 * Get the force message.
	 * 
	 * @return <code>true</code> iff the flag force is <code>true</code>
	 */
	public boolean isMinForced() {
		return forceMin;
	}

	/**
	 * Get the color.
	 * 
	 * @return the color
	 */
	public Color getColor() {
		return col;
	}

	/**
	 * Get the depth.
	 * 
	 * @return the depth
	 */
	public double getDepth() {
		return depth;
	}

	/**
	 * Get the url of the sender.
	 * 
	 * @return the sender url
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Get the global cache.
	 * 
	 * @return the cache
	 */
	public List getGlobalCache() {
		return global;
	}

	/**
	 * Add a collection of elements to the global cache.
	 * 
	 * @param c
	 *            the collection
	 */
	public void addToGlobalCache(Collection c) {
		global.addAll(c);
	}

	/**
	 * String representation of the message.
	 */
	public String toString() {
		String nodeS = "";
		try {
			nodeS = node.getUrls()[0];
		} catch (Exception e) {
		}
		String flags = new String();
		if (forceMax)
			flags = flags + "M";
		if (forceMin)
			flags = flags + "m";
		String up = "";
		String sib = "";
		String glo = "";
		if (upstream != null)
			up = " U:[" + upstream.toString() + "]";
		if (siblings != null)
			sib = " S:[" + siblings.toString() + "]";
		if (global != null)
			glo = " G:[" + global.toString() + "]";

		return "LSTreeMsg: (" + type + ") from " + nodeS + " (" + col + "," + depth + ") " + flags + up + sib + glo;

	}

	public long getHopToLive() {
		return hopToLive;
	}

	public void setHopToLive(long hopToLive) {
		this.hopToLive = hopToLive;
	}
}
