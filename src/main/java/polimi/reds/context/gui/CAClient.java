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

package polimi.reds.context.gui;

import java.net.ConnectException;

import polimi.reds.Filter;
import polimi.reds.Message;
import polimi.reds.TextMessage;
import polimi.reds.context.CATCPDispatchingService;
import polimi.reds.context.Context;
import polimi.reds.context.ContextFilter;
import polimi.reds.context.routing.RepliableCAMessage;

public class CAClient extends Thread {

	private CATCPDispatchingService ds;

	private int port;

	private ClientGui gui;

	private class ReplyThread extends Thread {

		public void run() {
			while (true) {
				Message reply = ds.getNextReply();
				gui.scrivi("Ho ricevuto una reply: ");
			}
		}
	}

	public CAClient(String host, int port, Context context, ClientGui gui) {
		super();
		this.port = port;
		ds = new CATCPDispatchingService(host, port, context);
		this.gui = gui;
		// ds.setContext( context );
	}

	public void setContext(Context context) {
		ds.setContext(context);
	}

	public void publish(Message m, ContextFilter contextFilter) {
		gui.scrivi("Invio messaggio: " + m.toString() + "  verso i " + contextFilter.toString());
		ds.publish(m, contextFilter);
	}

	public void subscribe(Filter filter, ContextFilter destinationContextFilter) {
		ds.subscribe(filter, destinationContextFilter);
	}

	public void run() {
		try {
			ds.open();

			ReplyThread replyThread = new ReplyThread();
			replyThread.start();

		} catch (ConnectException e) {
			e.printStackTrace();
		}

		while (true) {
			Message m = ds.getNextMessage();
			gui.scrivi("Ho ricevuto: " + m.toString());

			if (m instanceof RepliableCAMessage) {
				RepliableCAMessage repliableMessage = (RepliableCAMessage) m;
				gui.scrivi("Il messaggio e replyable.. invio un ACK");

				TextMessage reply = new TextMessage("Ho ricevuto il mess");
				ds.reply(reply, repliableMessage.getID());
			}
		}

	}

	public void unsubscribeAll() {
		ds.unsubscribeAll();

	}

}
