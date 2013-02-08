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

package polimi.reds.broker.overlay;

import java.io.Serializable;

/**
 * This class is used as an envelope to transport messages using UDP.
 */
public class UDPEnvelope extends Envelope {
  private static final long serialVersionUID = -1925905765953589547L;
  // The IP address of the sender
  private String senderIP;
  // The originating port
  private int port;
  private String url;

  /**
   * Base constructor.
   * @see Envelope#Envelope(String, Serializable, String)
   */
  public UDPEnvelope(String typeOfMessage, Serializable payload, String trafficClass) {
    super(typeOfMessage, payload, trafficClass);
  }

  /**
   * An empty envelope.
   * 
   * @see Envelope#Envelope(String)
   */
  public UDPEnvelope(String typeOfMessage) {
    this(typeOfMessage, null, Transport.MISCELLANEOUS_CLASS);
  }

  /**
   * Get the sender ip.
   * 
   * @return the ip of the sender of the message
   */
  public String getSenderIP() {
    return senderIP;
  }

  /**
   * Set the ip of the sender message.
   * 
   * @param senderIP the ip
   */
  public void setSenderIP(String senderIP) {
    this.senderIP = senderIP;
  }

  /**
   * Get the port of the sender of the message.
   * 
   * @return the port
   */
  public int getSenderPort() {
    return port;
  }

  /**
   * Set the port of the sender message.
   * 
   * @param port the port
   */
  public void setSenderPort(int port) {
    this.port = port;
  }
  /**
   * Get the url. 
   * @return the sender url
   */
    public String getURL(){
  	  return url;
    }
    /**
     * Set the url.
     * @param url the url
     */
    public void setURL(String url){
    	this.url = url;
    }
}
