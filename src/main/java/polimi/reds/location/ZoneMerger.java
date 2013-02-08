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

import java.util.Collection;

/**
 * <code>ZoneMerger</code>s encapsulate the merging policies used by
 * <code>LocationForwardingBroker</code>s. They define how locations have to
 * be merged in zones and zones have to be merged together.
 */
public interface ZoneMerger {
  /**
   * Enlarges a location to a zone, which includes it. More precisely:<br>
   * toZone(loc).includes(loc) must be true <br>
   * It is used by <code>LocationForwardingBroker</code>s to approximate
   * locations of clients.
   * 
   * @param loc the location to be approximated with th returned zone.
   * @return a zone that approximates the given location.
   */
  public Zone toZone(Location loc);

  /**
   * Merges a collection of zones into a single zone, which includes all of
   * them. Formally:<br>
   * foreach z in zones : merge(zones).overlaps(z)
   * 
   * @param zones the zones to be merged.
   * @return a zone that merges the given ones.
   */
  public Zone merge(Collection zones);
}
