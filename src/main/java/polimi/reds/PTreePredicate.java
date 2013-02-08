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

import java.io.Serializable;

/**
 * This interface defines the methods to be implemented for custom predicates to
 * be used as building blocks for filters of type <code>PTreeFilter</code>.
 * Different custom predicates can be mixed into the same
 * <code>PTreeFilter</code> filter. A filter of that type is always composed
 * of a conjunction of <code>PTreePredicate</code> basic predicates.
 */
public interface PTreePredicate extends Serializable{
  /**
   * Must return a representation of the test contained in this predicate, e.g.
   * the variable this predicate refers to.
   * 
   * @return the test variable contained in this predicate.
   */
  public String getTestVariable();

  /**
   * Must return a string representation for the result of the test contained in
   * this predicate. This last will basically be a combination of the chosen
   * comparator and the referred value.
   * 
   * @return a string representation for the result contained in this predicate.
   */
  public String getResult();

  /**
   * Must return TRUE if the message given as parameter can match this
   * predicate, FALSE otherwise.
   * 
   * @param gMsg the message to be examined.
   * @return TRUE if <code>gMsg</code> matches this predicate, FALSE
   *         otherwise.
   */
  public boolean isMatchedBy(PTreeMessage gMsg);

  /**
   * Must return TRUE if a message matching the predicate given as parameter
   * necessarily matches also this predicate and the two predicates have
   * different tests, i.e. <code>this.getTest().equals(p.getTest())</code>
   * return FALSE. An implementation of this method is not strctly needed for
   * the function of the PTreeSubscriptiontable. However, a correct
   * implementation of this method can greatly improve the overall efficiency of
   * the filtering algorithm.
   * 
   * @param p the predicate to be examined for logical implication.
   * @return TRUE if a message matching p necessarily mathcis also this
   *         predicate, FALSE otherwise.
   */
  public boolean isImpliedBy(PTreePredicate p);

  /**
   * The <code>equals</code> methdo has to be redefined for a correct
   * functioning of the <code>PTreeTable</code>. In particular,
   * timust return true iff o is an object of type PTreePredicate and
   * <code>this.getTestVariable().equals(p.getTestVariable())</code> and
   * <code>this.getResult().equals(p.getResult())</code> both return TRUE.
   * 
   * @param o the object to be examined for equality.
   * @return TRUE if <code>o instanceof PTreePredicate</code>,
   *         <code>this.getTestVariable().equals(p.getTestVariable())</code>
   *         and <code>this.getResult().equals(p.getResult())</code> all
   *         return TRUE, FALSE otherwise.
   */
  public boolean equals(Object o);
}
