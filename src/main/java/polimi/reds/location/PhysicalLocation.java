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

/**
 * A physical location in a geode, represented by a latitude and a longitude.
 */
public class PhysicalLocation implements Location {
	/**
	 * 
	 */
	private static final long serialVersionUID = -658916060646033079L;
	protected double latitude;
	protected double longitude;

	/**
	 * Creaes a new location which represent the point of given latitude and
	 * longitde.
	 * 
	 * @param latitude
	 *            the latitude of the new location.
	 * @param longitude
	 *            the longitude of the new location.
	 */
	public PhysicalLocation(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public boolean equals(Object obj) {
		if (obj instanceof PhysicalLocation) {
			PhysicalLocation l = (PhysicalLocation) obj;
			return latitude == l.latitude && longitude == l.longitude;
		} else
			return false;
	}

	public int hashCode() {
		return (int) Math.round(latitude + longitude);
	}

	public String toString() {
		return "<" + latitude + "," + longitude + ">";
	}
}
