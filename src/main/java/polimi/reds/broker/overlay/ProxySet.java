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

import java.util.*;

import polimi.reds.NodeDescriptor;
import polimi.reds.broker.overlay.Proxy;

/**********************************************************************
 * As its name suggests a <code>ProxySet</code> contains a set of 
 * Proxy representing local node's neighbors, which can be accessed through their identifier.
 **********************************************************************/
class ProxySet {

  /** The <code>Map</code>, which holds the Proxies that compose this set,
   * associated to their own identifier. */
  protected Map proxies;

  /**
   * Builds a new, empty, <tt>ProxySet</tt>.
   */
  public ProxySet() {
    proxies = new HashMap();
  }

  /**
   * Adds the give neighbor to the set.
   * 
   * @param n The neighbor to add.
   */
  public synchronized void add(Proxy n) {
    proxies.put(n.getID(), n);
  }

  /**
   * Returns a neighbor given its identifier.
   * 
   * @param nodeID The identifier of the neighbor that must be get.
   * @return The neighbor having the given identifier that is included 
   *         into this set or <code>null</code> if this set does not include
   *         such a neighbor.
   */
  public synchronized Proxy get(NodeDescriptor nodeID) {
    return (Proxy)proxies.get(nodeID);
  }

  /**
   * Returns all the <code>Proxy</code> included into this set as a single collection.
   * 
   * @return A collection of <code>Proxy</code> included into this set.
   */
  public synchronized Collection getAllProxies() {
    // Returns all the neighbors
    return new ArrayList(proxies.values());
  }

  /**
   * Returns all the <code>Proxy</code> included into this set except for a single one, 
   * that one having the given identifier.
   * 
   * @param nodeID The identifier of the <code>Proxy</code> that has to be excluded.
   * @return A collection of the <code>Proxy</code> included into this set excluding 
   *         that one having the given identifier.
   */
  public synchronized Collection getAllProxiesExcept(NodeDescriptor nodeID) {
    Collection c = getAllProxies();
    c.remove(proxies.get(nodeID));
    return c;
  }

  /**
   * Returns <code>true</code> if this collection contains the specified node.
   * @param nodeID The identifier of the node whose presence in this set
   *                   is to be tested.
   * @return <code>true</code> if this set contains the specified element.
   */
  public synchronized boolean contains(NodeDescriptor nodeID) {
    return proxies.containsKey(nodeID);
  }

  /**
   * Removes the specified node from this set if it is present.
   * 
   * @param nodeID The identifier of the node that has to be removed.
   */
  public synchronized Proxy remove(NodeDescriptor nodeID) {
    return (Proxy)proxies.remove(nodeID);
  }

  /**
   * Removes all the nodes from this set.
   */
  public synchronized void clear() {
    proxies.clear();
  }

  /**
   * Returns the number of nodes in this set.
   * 
   * @return The number of nodes in this set.
   */
  public synchronized int size() {
    return proxies.size();
  }

  /**
   * Returns the number of brokers in this set (nodes can be brokers 
   * or clients).
   *  
   * @return The number of brokers in this set.
   */
  public synchronized int numberOfBrokers() {
    int numberOfDS = 0;
    Set keys = proxies.keySet();
    Iterator i = keys.iterator();
    while (i.hasNext()) {
      String idFromMap = (String)i.next();
      Proxy neighbor = (Proxy)proxies.get(idFromMap);
      if (neighbor.isBroker())
        numberOfDS++;
    }
    return numberOfDS;
  }

  /**
   * Returns the number of clients in this set (nodes can be brokers 
   * or clients).
   *  
   * @return The number of clients in this set.
   */
  public synchronized int numberOfClients() {
    return size() - numberOfBrokers();
  }

  // Added for debug purposes only.
  public synchronized String toString() {
    String s;
    s = super.toString()+":\r\n";
    Iterator i = getAllProxies().iterator();
    while(i.hasNext()) {
      Proxy c = (Proxy)i.next();
      s += "o) ID = "
        + c.getID()
        + "\r\n"
        + c;
    }
    s += "Number of nodes: " + proxies.size();
    return s;
  }
}
