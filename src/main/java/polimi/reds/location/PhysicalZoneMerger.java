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

/**
 * A <code>ZoneMerger</code> for <code>PhysicalArea</code>
 */
public class PhysicalZoneMerger implements ZoneMerger {
	private static final double MARGIN = 0.01;

	public PhysicalZoneMerger() {
		super();
	}

	public Zone toZone(Location loc) {
		PhysicalLocation l = (PhysicalLocation) loc;
		return new PhysicalArea(l.latitude - MARGIN, l.latitude + MARGIN, l.longitude - MARGIN, l.longitude + MARGIN);
	}

	public Zone merge(Collection zones) {
		Iterator it = zones.iterator();
		PhysicalArea a = new PhysicalArea((PhysicalArea) it.next());
		while (it.hasNext()) {
			PhysicalArea a1 = (PhysicalArea) it.next();
			a.lowestLat = Math.min(a.lowestLat, a1.lowestLat);
			a.lowestLong = Math.min(a.lowestLong, a1.lowestLong);
			a.highestLat = Math.max(a.highestLat, a1.highestLat);
			a.highestLong = Math.max(a.highestLong, a1.highestLong);
		}
		return a;
	}
}
