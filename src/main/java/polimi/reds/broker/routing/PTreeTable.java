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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import polimi.reds.Filter;
import polimi.reds.Message;
import polimi.reds.NodeDescriptor;
import polimi.reds.PTreeFilter;
import polimi.reds.PTreeMessage;
import polimi.reds.PTreePredicate;

/**
 * This class provides the REDS middleware with an efficient filtering algorithm
 * derived from the one reported in M.K.Aguilera, R.E.Strom, D.C.Sturman,
 * M.Astley and T.D.Chandra: "Matching Events in a Content-Based Subscription
 * System", "Symposium on Principles of Distributed Computing", 1999. It is
 * basically build upon an internal tree representation of the various
 * predicates a susbcription is composed of. A subscription is always considered
 * as a conjunction of basic predicates. Each predicates has to be instantied
 * from a class implementing the <code>PTreePredicate</code> interface. A sample
 * predicate composed of a String value and a set of possible comparators is
 * provided in <code> PTreeStringPredicate</code>.
 */
public class PTreeTable implements SubscriptionTable {
	// A particular predicate used in place of a boolean TRUE on a given arc of
	// the internal tree
	private final PTreePredicate DO_NOT_CARE = new DontCarePredicate();
	// A map associating a subscription with the set of neighbor that issued
	// that subscription
	private HashMap subscriptionsNeighbors;
	// A map associating a neighbor with the list of subscriptions it issued
	private HashMap neighborsSubscriptions;
	// A map used for directly accesing leafs of the internal tree
	private HashMap subscriptionsLeafs;
	// The root of the internal tree
	private TreeNode root;

	public PTreeTable() {
		neighborsSubscriptions = new HashMap();
		subscriptionsNeighbors = new HashMap();
		subscriptionsLeafs = new HashMap();
	}

	/**
	 * @see polimi.reds.broker.routing.SubscriptionTable#addSubscription(NodeDescriptor,
	 *      polimi.reds.Filter)
	 */
	public void addSubscription(NodeDescriptor n, Filter f) {
		if (!(f instanceof PTreeFilter))
			return;
		PTreeFilter s = (PTreeFilter) f;
		// Updating the list of neighbors subscribed to this filter
		LinkedList neighborList = (LinkedList) subscriptionsNeighbors.get(s);
		if (neighborList == null) {
			neighborList = new LinkedList();
			subscriptionsNeighbors.put(s, neighborList);
		}
		neighborList.add(n);
		// Updating the list of subscriptions issued by this neighbor
		LinkedList subscriptionsList = (LinkedList) neighborsSubscriptions.get(n);
		if (subscriptionsList == null) {
			subscriptionsList = new LinkedList();
			neighborsSubscriptions.put(n, subscriptionsList);
		}
		subscriptionsList.add(s);
		// Inserting the new subscription in the tree
		treeInsert(s);
	}

	/**
	 * @see polimi.reds.broker.routing.SubscriptionTable#removeSubscription(NodeDescriptor,
	 *      polimi.reds.Filter)
	 */
	public void removeSubscription(NodeDescriptor n, Filter f) {
		if (!(f instanceof PTreeFilter))
			return;
		PTreeFilter s = (PTreeFilter) f;
		LinkedList subscriptionsList = (LinkedList) neighborsSubscriptions.get(n);
		subscriptionsList.remove(f);
		// This neighbor has no more subscriptions.
		if (subscriptionsList.size() == 0) {
			neighborsSubscriptions.remove(n);
		}
		LinkedList neighborList = (LinkedList) subscriptionsNeighbors.get(s);
		neighborList.remove(n);
		// There are no more neighbors subscribed to this filter.
		if (neighborList.size() == 0) {
			subscriptionsNeighbors.remove(s);
			treeRemove((TreeNode) subscriptionsLeafs.get(s));
			subscriptionsLeafs.remove(s);
		}
	}

	/**
	 * @see polimi.reds.broker.routing.SubscriptionTable#removeAllSubscriptions(NodeDescriptor)
	 */
	public void removeAllSubscriptions(NodeDescriptor n) {
		Iterator it = new LinkedList(((LinkedList) neighborsSubscriptions.get(n))).iterator();
		while (it.hasNext()) {
			removeSubscription(n, (Filter) it.next());
		}
	}

	/**
	 * @see polimi.reds.broker.routing.SubscriptionTable#clear()
	 */
	public void clear() {
		neighborsSubscriptions = new HashMap();
		subscriptionsNeighbors = new HashMap();
		subscriptionsLeafs = new HashMap();
		root = null;
	}

	/**
	 * @see polimi.reds.broker.routing.SubscriptionTable#isSubscribed(NodeDescriptor)
	 */
	public boolean isSubscribed(NodeDescriptor n) {
		if (neighborsSubscriptions.get(n) == null)
			return false;
		else
			return true;
	}

	/**
	 * @see polimi.reds.broker.routing.SubscriptionTable#isFilterInTable(polimi.reds.Filter)
	 */
	public boolean isFilterInTable(Filter filter) {
		if (subscriptionsNeighbors.get(filter) == null)
			return false;
		else
			return false;
	}

	/**
	 * @see polimi.reds.broker.routing.SubscriptionTable#getSingleSubscribedBroker(polimi.reds.Filter)
	 */
	public NodeDescriptor getSingleSubscribedBroker(Filter filter) {
		LinkedList neighborList = (LinkedList) subscriptionsNeighbors.get(filter);
		if (neighborList == null)
			return null;
		if (neighborList.size() != 1)
			return null;
		NodeDescriptor n = (NodeDescriptor) neighborList.getFirst();
		if (!n.isBroker())
			return null;
		else
			return n;
	}

	/**
	 * @see polimi.reds.broker.routing.SubscriptionTable#getAllFilters(NodeDescriptor)
	 */
	public Collection getAllFilters(NodeDescriptor n) {
		LinkedList subscriptionsList = (LinkedList) neighborsSubscriptions.get(n);
		if (subscriptionsList == null)
			return new LinkedList();
		else
			return subscriptionsList;
	}

	/**
	 * @see polimi.reds.broker.routing.SubscriptionTable#getAllFilters(boolean)
	 */
	public Collection getAllFilters(boolean duplicate) {
		return getAllFiltersExcept(duplicate, null);
	}

	/**
	 * @see polimi.reds.broker.routing.SubscriptionTable#getAllFiltersExcept(boolean,
	 *      NodeDescriptor)
	 */
	public Collection getAllFiltersExcept(boolean duplicate, NodeDescriptor n) {
		Collection result;
		if (duplicate)
			result = new ArrayList();
		else
			result = new HashSet();
		// Iterates over the subscribed neighbors...
		Iterator it = neighborsSubscriptions.keySet().iterator();
		NodeDescriptor currentNeighbor;
		while (it.hasNext()) {
			currentNeighbor = (NodeDescriptor) it.next();
			if (currentNeighbor.equals(n))
				continue; // Skip neighbor n
			result.addAll((Collection) neighborsSubscriptions.get(currentNeighbor));
		}
		return result;
	}

	/**
	 * @see polimi.reds.broker.routing.SubscriptionTable#getSubscribedNeighbors(polimi.reds.Filter)
	 */
	public Collection getSubscribedNeighbors(Filter f) {
		LinkedList neighborList = (LinkedList) subscriptionsNeighbors.get(f);
		if (neighborList == null)
			return new LinkedList();
		else
			return neighborList;
	}

	/**
	 * @see polimi.reds.broker.routing.SubscriptionTable#matches(polimi.reds.Message)
	 */
	public Collection matches(Message message) {
		LinkedList subscriptions = new LinkedList();
		if (!(message instanceof PTreeMessage))
			return subscriptions;
		// Computing the set of mathcing subscriptions
		treeVisit(root, (PTreeMessage) message, subscriptions);
		// Retrieveing subscribed neighbors
		Iterator it = subscriptions.iterator();
		HashSet neighbors = new HashSet();
		while (it.hasNext()) {
			PTreeFilter sub = (PTreeFilter) it.next();
			neighbors.addAll((Collection) subscriptionsNeighbors.get(sub));
		}
		return neighbors;
	}

	/**
	 * Visits the tree with a depth-first search following those predicates
	 * matching the message given as parameter. When the visit reaches a leaf,
	 * then that subscription is taken as a matching subscription.
	 * 
	 * @param r
	 *            the current node of the internal tree
	 * @param msg
	 *            the message the algorithm is considering
	 * @param subscriptions
	 *            the set of matching subscriptions reached so far
	 */
	private void treeVisit(TreeNode r, PTreeMessage msg, LinkedList subscriptions) {
		if (r.isLeaf()) {
			subscriptions.add(r.getSubscription());
			return;
		}
		LinkedList outgoingArcs = r.getOutgoingArcs();
		Iterator it = outgoingArcs.iterator();
		// Iterating over the outgoing arcs of the current node
		while (it.hasNext()) {
			PTreePredicate p = (PTreePredicate) it.next();
			if (p.isMatchedBy(msg)) {
				treeVisit(r.getChildNode(p), msg, subscriptions);
			}
		}
		// Following DO_NOT_CARE arcs
		TreeNode dontCare = r.getOutgoingDontCare();
		if (dontCare != null) {
			treeVisit(dontCare, msg, subscriptions);
		}
	}

	/**
	 * Inserts the subscription given as parameter into the current internal
	 * tree.
	 * 
	 * @param s
	 *            the subscription to be inserted.
	 */
	private void treeInsert(PTreeFilter s) {
		int q, i;
		boolean found;
		TreeNode v, vP;
		TreeNode w = null;
		PTreePredicate rP;
		i = 0;
		q = s.getLength();
		if (q == 0)
			return;
		if (root == null) {
			root = new TreeNode(s.getPredicate(i).getTestVariable());
			found = false;
		} else {
			found = true;
		}
		v = root;
		// Traversing the tree
		while (found && i < q) {
			if (v.isLeaf()) {
				// Inserting just before a leaf
				w = v.getParentNode();
				rP = v.getParentArc();
				vP = new TreeNode(s.getPredicate(i).getTestVariable());
				// Making vP child of w via rP
				w.removeChildNode(rP);
				w.addChildNode(rP, vP);
				vP.setParent(w, rP);
				// Making v child of vP via DO_NOT_CARE_ARC
				vP.addChildNode(DO_NOT_CARE, v);
				v.setParent(vP, DO_NOT_CARE);
				// Iterating
				v = vP;
				found = false;
			} else if (v.getTest().equals(s.getPredicate(i).getTestVariable())) {
				// Traversing the tree...
				w = v.getChildNode(s.getPredicate(i));
				if (w == null) {
					found = false;
				} else {
					v = w;
					i++;
				}
			} else {
				w = v.existsImpliedBy(s.getPredicate(i));
				if (w != null) {
					// Traversing the arc towards w, the current predicate is
					// implied by
					// that arc
					v = w;
				} else if (v.getOutgoingDontCare() != null) {
					// Traversing a DO_NOT_CARE arc
					v = v.getOutgoingDontCare();
				} else {
					// Adding a DO_NOT_CARE arc
					vP = new TreeNode(s.getPredicate(i).getTestVariable());
					v.addChildNode(DO_NOT_CARE, vP);
					vP.setParent(v, DO_NOT_CARE);
					v = vP;
					found = false;
				}
			}
		}
		if (!found) {
			i++;
			while (i <= q) {
				// Adding remaining predicates
				if (i < q) {
					// Adding a new test node
					vP = new TreeNode(s.getPredicate(i).getTestVariable());
				} else {
					// Adding a new leaf node
					vP = new TreeNode(s);
					subscriptionsLeafs.put(s, vP);
				}
				v.addChildNode(s.getPredicate(i - 1), vP);
				vP.setParent(v, s.getPredicate(i - 1));
				v = vP;
				i++;
			}
		} else {
			if (v.isLeaf()) {
				return;
			} else {
				while (v.getOutgoingDontCare() != null) {
					// Reaching a leaf...
					v = v.getOutgoingDontCare();
				}
				if (v.isLeaf()) {
					return;
				} else {
					vP = new TreeNode(s);
					subscriptionsLeafs.put(s, vP);
					v.addChildNode(DO_NOT_CARE, vP);
					vP.setParent(v, DO_NOT_CARE);
				}
			}
		}
	}

	/**
	 * Removes a node from the tree. In case the remove operation makes the
	 * parent a leaf, then this function recursively traverses the tree up to
	 * the first node with at least a child.
	 * 
	 * @param v
	 *            the node to be removed.
	 */
	private void treeRemove(TreeNode v) {
		// Removing v from the tree
		if (v != root) {
			TreeNode parent = v.getParentNode();
			parent.removeChildNode(v.getParentArc());
			// The remove operation made the parent a leaf
			if (parent.getFanOut() == 0)
				treeRemove(parent);
		} else
			root = null;
	}

	// For testing purposes...
	public void outputTreeRepresentation() {
		treeRep(root);
	}

	// For testing purposes...
	private void treeRep(TreeNode n) {
		if (n.isLeaf())
			return;
		LinkedList outArcs = n.getOutgoingArcs();
		System.out.print("[ " + n.getTest());
		Iterator it = outArcs.iterator();
		while (it.hasNext()) {
			PTreePredicate p = (PTreePredicate) it.next();
			if (n.getChildNode(p).isLeaf())
				System.out.print(":" + p.getResult() + "->SUB: " + n.getChildNode(p).getSubscription());
			else
				System.out.print(":" + p.getResult() + "->" + n.getChildNode(p).getTest() + " ");
		}
		System.out.println("] ");
		it = outArcs.iterator();
		while (it.hasNext()) {
			PTreePredicate p = (PTreePredicate) it.next();
			treeRep(n.getChildNode(p));
		}
	}

	/**
	 * This class represents a node in the internal tree representation. The
	 * data contained in this node can be a test if the node is not a leaf,
	 * otherwise a subscription if the node is a leaf.
	 */
	class TreeNode {
		// A map associating outgoing arcs with child nodes
		private HashMap outArcs;
		// A reference to the local subscription, in case this node is a leaf
		private PTreeFilter subscription;
		// A reference to the test contained in the local node, if this last is
		// not
		// a leaf
		private String test;
		// A reference to the parent node
		private TreeNode parent;
		// The arc connecting this node to its parent
		private PTreePredicate parentArc;
		// TRUE if this node is a leaf
		private boolean leaf;

		public TreeNode(String test) {
			this.test = test;
			outArcs = new HashMap();
			leaf = false;
		}

		public TreeNode(PTreeFilter s) {
			subscription = s;
			leaf = true;
		}

		public void addChildNode(PTreePredicate result, TreeNode child) {
			outArcs.put(result, child);
		}

		public void removeChildNode(PTreePredicate result) {
			outArcs.remove(result);
		}

		public TreeNode getChildNode(PTreePredicate result) {
			return (TreeNode) outArcs.get(result);
		}

		public TreeNode getParentNode() {
			return parent;
		}

		public PTreePredicate getParentArc() {
			return parentArc;
		}

		public LinkedList getOutgoingArcs() {
			return new LinkedList(outArcs.keySet());
		}

		public String getTest() {
			return test;
		}

		public void setParent(TreeNode parent, PTreePredicate parentArc) {
			this.parent = parent;
			this.parentArc = parentArc;
		}

		public boolean isLeaf() {
			return leaf;
		}

		public TreeNode existsImpliedBy(PTreePredicate p) {
			Iterator it = new LinkedList(outArcs.keySet()).iterator();
			while (it.hasNext()) {
				PTreePredicate child = (PTreePredicate) it.next();
				if (child.isImpliedBy(p))
					return (TreeNode) outArcs.get(child);
			}
			return null;
		}

		public TreeNode getOutgoingDontCare() {
			return (TreeNode) outArcs.get(DO_NOT_CARE);
		}

		public PTreeFilter getSubscription() {
			return subscription;
		}

		public int getFanOut() {
			return outArcs.size();
		}
	}

	/**
	 * This class implements a particular predicate that is always taken as
	 * satisfied, i.e. it represents the boolean value TRUE.
	 */
	class DontCarePredicate implements PTreePredicate {
		/**
	 * 
	 */
		private static final long serialVersionUID = 1791511027632091707L;

		public DontCarePredicate() {
		}

		public String getTestVariable() {
			return null;
		}

		public String getResult() {
			return "*";
		}

		public boolean isMatchedBy(PTreeMessage gMsg) {
			return true;
		}

		public boolean equals(PTreePredicate p) {
			return false;
		}

		public boolean isImpliedBy(PTreePredicate p) {
			return false;
		}
	}

	public Collection matches(Message message, NodeDescriptor senderID) {
		Collection c = matches(message);
		Iterator it = c.iterator();
		boolean found = false;
		while (it.hasNext() && !found) {
			NodeDescriptor n = (NodeDescriptor) it.next();
			if (n.equals(senderID)) {
				found = true;
				c.remove(n);
			}
		}
		return c;
	}
}
