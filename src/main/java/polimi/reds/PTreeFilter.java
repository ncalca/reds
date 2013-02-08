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

package polimi.reds;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * This class implements a <code>Filter</code> object to be sued with the
 * <code>PTreeTable</code>. Each filter of this type is composed
 * of a conjunction of basic <code>PTreePredicate</code> predicates.
 */
public class PTreeFilter implements Filter {
  /**
	 * 
	 */
	private static final long serialVersionUID = -8793668117472760272L;
// The set of basic PTreePredicate predicates contained in this filter.
  private LinkedList predicates;

  public PTreeFilter() {
    predicates = new LinkedList();
  }

  /**
   * @see polimi.reds.Filter#matches(polimi.reds.Message)
   */
  public boolean matches(Message msg) {
    if(!(msg instanceof PTreeMessage)) return false;
    PTreeMessage gMsg = (PTreeMessage) msg;
    Iterator it = predicates.iterator();
    while(it.hasNext()) {
      if(!((PTreePredicate) it.next()).isMatchedBy(gMsg)) return false;
    }
    return true;
  }

  public void addPredicate(PTreePredicate p) {
    predicates.add(p);
  }

  public PTreePredicate getPredicate(int i) {
    return (PTreePredicate) predicates.get(i);
  }

  public int getLength() {
    return predicates.size();
  }

  /**
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object o) {
    if(!(o instanceof PTreeFilter)) return false;
    int j;
    PTreeFilter s = (PTreeFilter) o;
    if(s.getLength()!=predicates.size()) return false;
    int size = predicates.size();
    for(int i = 0; i<size; i++) {
      for(j = 0; j<size; j++) {
        if(this.getPredicate(i).equals(s.getPredicate(j))) {
          break;
        }
      }
      if(j==size) return false;
    }
    return true;
  }

  // For testing purposes...
  public boolean isConsistent() {
    Iterator it1 = predicates.iterator();
    while(it1.hasNext()) {
      PTreePredicate p1 = (PTreePredicate) it1.next();
      Iterator it2 = predicates.iterator();
      while(it2.hasNext()) {
        PTreePredicate p2 = (PTreePredicate) it2.next();
        if(p2.getTestVariable().equals(p1.getTestVariable())&&p1!=p2)
            return false;
      }
    }
    return true;
  }

  // For testing purposes...
  public String toString() {
    String n = new String();
    Iterator it = predicates.iterator();
    while(it.hasNext()) {
      PTreePredicate p = (PTreePredicate) it.next();
      n = n.concat(p.getTestVariable()+" "+p.getResult()+" - ");
    }
    return n;
  }
}
