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
 * A physical area in a geode.
 */
public class PhysicalArea implements Zone {
  /**
	 * 
	 */
	private static final long serialVersionUID = -8544183770128458311L;
protected double lowestLat;
  protected double highestLat;
  protected double lowestLong;
  protected double highestLong;

  /**
   * Creates a new area comprised within the given lowest and highest latitudes
   * and longitudes.
   * 
   * @param lowestLat the lowest latitude of the area.
   * @param highestLat the highest latitude of the area.
   * @param lowestLong the lowest longitude of the area.
   * @param highestLong the highest longitude of the area.
   */
  public PhysicalArea(double lowestLat, double highestLat, double lowestLong,
      double highestLong) {
    this.lowestLat = lowestLat;
    this.highestLat = highestLat;
    this.lowestLong = lowestLong;
    this.highestLong = highestLong;
  }

  /**
   * Builds a new area which equals the one passed as a parameter.
   * 
   * @param a the area to copy.
   */
  public PhysicalArea(PhysicalArea a) {
    this(a.lowestLat, a.highestLat, a.lowestLong, a.highestLong);
  }

  public boolean includes(Location loc) {
    if(loc instanceof PhysicalLocation) {
      PhysicalLocation l = (PhysicalLocation) loc;
      return l.latitude<=highestLat&&l.latitude>=lowestLat
          &&l.longitude<=highestLong&&l.longitude>=lowestLong;
    } else return false;
  }

  public boolean overlaps(Zone zone) {
    if(zone instanceof PhysicalArea) {
      PhysicalArea a = (PhysicalArea) zone;
      return !(highestLat<a.lowestLat||a.highestLat<lowestLat
          ||highestLong<a.lowestLong||a.highestLong<lowestLong);
    } else return false;
  }

  public boolean equals(Object obj) {
    if(obj instanceof PhysicalArea) {
      PhysicalArea a = (PhysicalArea) obj;
      return lowestLat==a.lowestLat&&highestLat==a.highestLat
          &&lowestLong==a.lowestLong&&highestLong==a.highestLong;
    } else return false;
  }

  public int hashCode() {
    return (int) Math.round(lowestLat+highestLat+lowestLong+highestLong);
  }
  public String toString() {
    return "<"+lowestLat+":"+highestLat+","+lowestLong+":"+highestLong+">";
  }
}
