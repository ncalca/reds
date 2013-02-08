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

import polimi.reds.*;

import java.util.*;

/**********************************************************************
 * A <code>SubscriptionTable</code> is used by the REDS broker to
 * store information about the subscriptions (i.e., filters) it received
 * from its neighbors. This is the generic interface, classes implementing 
 * this interface may differ for the strategy adopted to store subscriptions
 * and, most important for the performances of the match method, which mainly
 * influences the broker's performances.
 **********************************************************************/
public interface SubscriptionTable {
  /**
   * Adds the given subscription to the table.
   *
   * @param n The neighbor who subscribed.
   * @param f The filter that describes the interests of the neighbor.
   */
  public void addSubscription(NodeDescriptor n, Filter f);

  /**
   * Removes the given subscription to the table.
   *
   * @param n The neighbor who unsubscribed.
   * @param f The filter to remove.
   */
  public void removeSubscription(NodeDescriptor n, Filter f);

  /**
   * Removes all the filters associated to the given neighbor.
   * Usually called when a neighbor leaves.
   *
   * @param n The neighbor who unsubscribed.
   */
  public void removeAllSubscriptions(NodeDescriptor n);

  /**
   * Clears the entire table.
   */
  public void clear();

  /**
   * Returns <code>true</code> if the given neighbor has at least one
   * subscription.
   * 
   * @param n The neighbor to test.
   * @return <code>true</code> if the given neighbor has at least one subscription,
   *         <code>false</code> if it never subscribed.
   */
  public boolean isSubscribed(NodeDescriptor n);

  /**
   * Returns <code>true</code> if at least one neighbor subscribed with the given 
   * filter.
   * 
   * @param filter The filter to test.
   * @return <code>true</code> if the given filter appears into the table.
   */
  public boolean isFilterInTable(Filter filter);

  /**
   * If just a single neighbor and in particular a broker subscribed with 
   * the given filter this method returns it, otherwise it returns null.
   * 
   * @param filter The filter to test.
   * @return The single broker subscribed with the given filter, or null if more
   *         than one neighbor subscribed with the given filter or if a client
   *         subscribed with the given filter.
   */
  public NodeDescriptor getSingleSubscribedBroker(Filter filter);

  /**
   * Returns the collection of filters associated to the given neighbor into the table.
   * 
   * @param n The neighbor whose subscriptions must be taken.
   * @return The collection of filters associated to the given neighbor into the table.
   */
  public Collection getAllFilters(NodeDescriptor n);

  /**
   * Returns the collection of all filters included in this table (i.e., independently 
   * from the neighbors they are associated to). Duplicates are kept or removed
   * depending on the value of the <code>duplicate</code> parameter.
   *  
   * @param duplicate If <code>true</code> accept duplicates, otherwise eliminate them.
   * @return The collection of all filters included in this table.
   */
  public Collection getAllFilters(boolean duplicate);

  /**
   * Returns the collection of all filters included in this table (i.e., independently 
   * from the neighbors they are associated to) not considering the given neighbor.
   * Duplicates are kept or removed depending on the value of the
   * <code>duplicate</code> parameter.
   *  
   * @param duplicate If <code>true</code> accept duplicates, otherwise eliminate them.
   * @param n The neighbor whose filters must be excluded.
   * @return The collection of all filters included in this table.
   */
  public Collection getAllFiltersExcept(boolean duplicate, NodeDescriptor n);

  /**
   * Returns the collection of all neighbors that have at least one filter that matches the 
   * given message.
   *  
   * @param message The message to match.
   * @return The collection of all neighbors that have at least one filter that matches the 
   *         given message.
   */
  public Collection matches(Message message);

  /**
   * Returns the collection of all neighbors that have at least one filter that matches the 
   * given message. It ignores the neighbor whose ID is passed as parameter.
   *  
   * @param message The message to match.
   * @param excludedDestination the ID of the neighbor that is excluded from matching.
   * @return The collection of all neighbors that have at least one filter that matches the 
   *         given message.
   */
  public Collection matches(Message message, NodeDescriptor excludedDestination);
  
  /**
   * Returns the collection of all neighbors that subscribed to the given filter.
   *  
   * @param f The filter to search for.
   * @return The collection of all neighbors that subscribed to the given filter.
   */
  public Collection getSubscribedNeighbors(Filter f);
}
