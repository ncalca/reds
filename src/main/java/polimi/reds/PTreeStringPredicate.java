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

/**
 * This class implements a sample PTreePrediacte comparing of string values.
 */
public class PTreeStringPredicate implements PTreePredicate {
  /**
	 * 
	 */
	private static final long serialVersionUID = 6082806528869743077L;
// The list of possible comparators
  public final static int CONTAINS = 0;
  public final static int EQUALS = 1;
  // The variable referred in the predicate
  private String variable;
  // The chosen comparator
  private int comparator;
  // The value to be compared
  private String value;

  public PTreeStringPredicate(String variable, int comparator, String value) {
    this.variable = variable;
    this.comparator = comparator;
    this.value = value;
  }

  /**
   * @see polimi.reds.PTreePredicate#getTestVariable()
   */
  public String getTestVariable() {
    return variable;
  }

  /**
   * @see PTreePredicate#isMatchedBy(PTreeMessage)
   */
  public boolean isMatchedBy(PTreeMessage gMsg) {
    Object o = gMsg.getValue(variable);
    if(!(o instanceof String)) return false;
    String str = (String) o;
    switch(comparator) {
    case CONTAINS:
      if(str.indexOf(value)!=-1) return true;
    case EQUALS:
      if(str.equals(value)) return true;
    }
    return false;
  }

  /**
   * @see polimi.reds.PTreePredicate#getResult()
   */
  public String getResult() {
    return String.valueOf(comparator)+" "+value;
  }

  /**
   * @return Returns the comparator.
   */
  public int getComparator() {
    return comparator;
  }

  /**
   * @return Returns the value.
   */
  public String getValue() {
    return value;
  }

  /**
   * @return Returns the variable.
   */
  public String getVariable() {
    return variable;
  }

  /**
   * @see PTreePredicate#isImpliedBy(PTreePredicate)
   */
  public boolean isImpliedBy(PTreePredicate p) {
    return false;
  }

  /**
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object o) {
    if(!(o instanceof PTreeStringPredicate)) return false;
    PTreeStringPredicate p = (PTreeStringPredicate) o;
    if(p.getTestVariable().equals(this.getTestVariable())
        &&p.getResult().equals(this.getResult())) return true;
    return false;
  }
}
