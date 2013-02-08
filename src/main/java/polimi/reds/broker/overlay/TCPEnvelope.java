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

/*******************************************************************************
 * The "internal" message exchanged via TCP by each REDS client and the broker
 * it is attached to or among neighboring brokers.
 ******************************************************************************/
public class TCPEnvelope extends Envelope {
  private static final long serialVersionUID = -1233116801597947207L;

  /**
   * Base constructor.
   * 
   * @see Envelope#Envelope(String, Serializable, String)
   */
  public TCPEnvelope(String typeOfMessage, Serializable payload, String trafficClass) {
    super(typeOfMessage, payload, trafficClass);
  }

  /**
   * An empty envelope.
   * 
   * @see Envelope#Envelope(String)
   */
  public TCPEnvelope(String typeOfMessage) {
    this(typeOfMessage, null, Transport.MISCELLANEOUS_CLASS);
  }
}
