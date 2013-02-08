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

import polimi.reds.Filter;
import polimi.reds.Message;
import polimi.reds.Repliable;
import polimi.reds.TCPDispatchingService;

/**
 * This class implements the <code>LocationDispatchingService</code> by using a TCP
 * connection to connect to the REDS dispatching service.
 */
public class TCPLocationDispatchingService extends TCPDispatchingService
    implements LocationDispatchingService {
  protected Location myLocation;
  /*public TCPLocationDispatchingService() {
    super();
  }*/

  public TCPLocationDispatchingService(String host, int port) {
    super(host, port);
  }

  public void publish(Message msg, Zone zone) {
	if(msg instanceof Repliable){
	  publish(new RepliableLocationMessage(msg, myLocation, zone));
	}else
    publish(new LocationMessage(msg, myLocation, zone));
  }

  public void subscribe(Filter filter, Zone zone) {
    subscribe(new LocationFilter(filter, zone));
  }

  public void unsubscribe(Filter filter, Zone zone) {
    unsubscribe(new LocationFilter(filter, zone));
  }

  public void setLocation(Location loc) {
    myLocation = loc;
    forward("update_location", loc);
  }
  
  public Message getNextMessage(){
	  Message m = super.getNextMessage();
	  return checkLocationMessage(m);
  }
  /**
   * Check whether a message is instance of <code>LocationMessage</code>. If <code>true</code> returns the payload, else
   * it returns the message itself.
   * @param m the message
   * @return the payload or the message itself
   */
  private Message checkLocationMessage(Message m){
	  if(m instanceof LocationMessage)
		  return ((LocationMessage)m).getPayload();
	  return m;
  }
  
  public Message getNextMessage(Filter f){
	  Message m = super.getNextMessage(f);
	  return checkLocationMessage(m);
  }
  
  public Message getNextMessage(long timeout){
	  Message m = super.getNextMessage(timeout);
	  return checkLocationMessage(m);
  }
}
