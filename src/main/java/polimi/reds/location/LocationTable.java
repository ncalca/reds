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

package polimi.reds.location;

import java.util.*;

import polimi.reds.NodeDescriptor;

/**
 * A <code>LocationTable</code> is used by location aware brokers to keep
 * track of the zones emanaged by ach of its neighbors. It associates neighbors
 * with zones and is able of checking which neighbor manage a zone: (i)
 * including a given location, (ii) overlapping a given zone.
 */
class LocationTable {
  protected Map data;

  /**
   * Builds an empty location table.
   */
  public LocationTable() {
    data = new HashMap();
  }

  /**
   * Sets the zone a neighbor is responsible for.
   * 
   * @param n
   * @param z
   */
  public void setZone(NodeDescriptor n, Zone z) {
    data.put(n, z);
  }
  

  /**
   * Removes a neighbor from this location table.
   * 
   * @param n the neighbor to remove
   */
  public void removeNeighbor(NodeDescriptor n) {
    data.remove(n);
  }

  /**
   * Gets the zone the given neighbor is responsible for.
   * 
   * @param n the neighbor.
   * @return the zone the node <code>n</code> is responsible for or
   *         <code>null</code> if nothing is known about <code>n</code>.
   */
  public Zone getZone(NodeDescriptor n) {
    return (Zone) data.get(n);
  }

  /**
   * Used to obtain the set of neighbors that are known to manage a zone that
   * includes the given location.
   * 
   * @param loc the location of interest.
   * @return the collection of neighbors whose managed zone includes location
   *         <code>loc</code> or <code>null</code> if no neighbor exists,
   *         which satisfy this condition.
   */
  public Collection getManagingNeighbors(Location loc) {
    Collection result = new HashSet();
    for(Iterator it = data.entrySet().iterator(); it.hasNext();) {
      Map.Entry e = (Map.Entry) it.next();
      if(((Zone) e.getValue()).includes(loc)) result.add(e.getKey());
    }
    if(result.isEmpty()) return null;
    else return result;
  }

  /**
   * Used to obtain the set of neighbors that are known to manage a zone that
   * overlaps zone <code>z</code>.
   * 
   * @param z the zone of interest.
   * @return the collection of neighbors whose managed zone overlaps zone
   *         <code>z</code> or <code>null</code> if no neighbor exists,
   *         which satisfy this condition.
   */
  public Collection getManagingNeighbors(Zone z) {
    Collection result = new HashSet();
    for(Iterator it = data.entrySet().iterator(); it.hasNext();) {
      Map.Entry e = (Map.Entry) it.next();
      if(((Zone) e.getValue()).overlaps(z)) result.add(e.getKey());
    }
    if(result.isEmpty()) return null;
    else return result;
  }

  /**
   * Calculates the size of this table.
   * 
   * @return the size (number of known neighbors) of this table.
   */
  public int size() {
    return data.size();
  }

  /**
   * Used to obtain the collection of all known zones. If the same zone is
   * associated to different neighbors in this table, all of them are returned.
   * 
   * @return the collection of all known zones.
   */
  public Collection getAllKnownZones() {
    return new ArrayList(data.values());
  }
}
