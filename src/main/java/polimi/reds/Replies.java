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
 * This class represents a set of replies to a published message. If all replies
 * arrived in time, <code>areAllReplies()</code> is <code>true</code>, else it
 * is <code>false</code>.
 */
public class Replies {
  private Message[] replies;
  private boolean all = false;

  /**
   * Base constructor.
   * @param replies the replies
   * @param areAll <code>true</code> iff it contains all the replies.
   */
  public Replies(Message[] replies, boolean areAll) {
    this.replies = replies;
    this.all = areAll;
  }

  /**
   * Returns all the replies.
   * 
   * @return all the replies.
   */
  public Message[] getReplies() {
    return replies;
  }

  /**
   * Check whether all the replies arrived.
   * 
   * @return <code>true</code> if all the replies arrived in time,
   *         <code>false</code> if noty.
   */
  public boolean areAllReplies() {
    return all;
  }
}
