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

package polimi.reds.context.routing;

import polimi.reds.Message;
import polimi.reds.context.Context;
import polimi.reds.context.ContextFilter;

/**
 * A context aware REDS message. It contains the payload, the sender context and
 * a filter for the required receiver context
 */
public class CAMessage extends Message {

	private static final long serialVersionUID = -6009680403193487647L;

	private Message message;

	private Context sourceContext;

	private ContextFilter destinationContext;

	/***************************************************************************
	 * Create a new instance of CAMessage
	 * 
	 * @param message
	 *            the payload of this message
	 * @param sourceContext
	 *            the sender's context
	 * @param destinationContext
	 *            a filter that matches the required client context
	 */
	public CAMessage(Message message, Context sourceContext, ContextFilter destinationContext) {
		super();
		this.message = message;
		this.sourceContext = sourceContext;
		this.destinationContext = destinationContext;
		this.id = message.getID();
	}

	/***************************************************************************
	 * This method returns the payload of this message
	 * 
	 * @return the payload
	 */
	public Message getMessage() {
		return this.message;
	}

	/***************************************************************************
	 * This method returns the destination filter, that represent the context
	 * required to a client to receive this message
	 * 
	 * @return the destination filter
	 */
	public ContextFilter getDestinationContext() {
		return this.destinationContext;
	}

	/***************************************************************************
	 * This method returns the sender's context of this message
	 * 
	 * @return the sender contexts
	 */
	public Context getSourceContext() {
		return this.sourceContext;
	}

	/***************************************************************************
	 * Test if this message is equal to another object. <br>
	 * A CAMessage is equals to an Object o iff<br>
	 * <ul>
	 * <li>o is a CAMessage</li>
	 * <li>the payloads are equals</il>
	 * <li>the destination filters are equals</il>
	 * <li>the sender are equals</il>
	 * </ul>
	 */
	@Override
	public boolean equals(Object other) {
		if (!(other.getClass().equals(this.getClass()))) {
			return false;
		}

		CAMessage otherContextMessage = (CAMessage) other;

		if (!this.message.equals(otherContextMessage.message)) {
			return false;
		}
		if (!this.sourceContext.equals(otherContextMessage.sourceContext)) {
			return false;
		}
		if (!this.destinationContext.equals(otherContextMessage.destinationContext)) {
			return false;
		}

		return true;
	}

	@Override
	public String toString() {
		String result = "";
		result += "ContextMessage: \n" + this.message.toString() + "\nproveniente da " + this.sourceContext.toString();
		return result;
	}

	@Override
	public void createID() {
		message.createID();
		this.id = message.getID();
	}

}
