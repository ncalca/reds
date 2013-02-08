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

package polimi.reds.test;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import polimi.reds.DispatchingService;
import polimi.reds.Message;
import polimi.reds.TCPDispatchingService;
import polimi.reds.TextFilter;
import polimi.reds.TextMessage;
import polimi.reds.UDPDispatchingService;

public class ClientTester {
	public static int PUBLISH = 0;
	public static int SUBSCRIBE = 1;

	public static void main(String[] args) {

		int numOfMessages = 10000;
		int numOfTurns = 1; // 7.1s to publish 7.9s to receive
		int action = SUBSCRIBE;
		long time;
		Message m = new TextMessage("aaa123");

		if (args.length == 0) {
			System.err
					.println("USAGE: java polimi.reds.examples.ClientTester [reds-tcp | reds-udp]:<brokerAddress>:<brokerPort>"
							+ " <localPort> [-publish | -subscribe]");
			System.exit(0);
		}

		int localPort = Integer.parseInt(args[1]);
		// configuring logging facility
		Logger logger = Logger.getLogger("polimi.reds");
		ConsoleHandler ch = new ConsoleHandler();
		logger.addHandler(ch);
		logger.setLevel(Level.ALL);
		ch.setLevel(Level.CONFIG);
		DispatchingService ds = null;
		String[] transportProtocol = args[0].split(":");
		if (transportProtocol[0].equals("reds-tcp"))
			ds = new TCPDispatchingService(transportProtocol[1],
					Integer.parseInt(transportProtocol[2]));
		else if (transportProtocol[0].equals("reds-udp"))
			ds = new UDPDispatchingService(transportProtocol[1],
					Integer.parseInt(transportProtocol[2]), localPort);
		else
			throw new IllegalArgumentException();

		if (args[2].equals("-publish"))
			action = PUBLISH;
		else
			action = SUBSCRIBE;
		try {
			ds.open();
			System.out.println("dispatching service aperto");
			Thread.sleep(5000);
			time = System.currentTimeMillis();
			if (action == PUBLISH) {
				int i = 0;
				int j = 0;
				for (i = 0; i < numOfTurns; i++) {
					for (j = 0; j < numOfMessages; j++) {
//						if (j % 100 == 0)
//							System.out.println(j);
						ds.publish(m);
					}
					System.out.println("Published " + (j) + " messages");
				}
			} else {
				ds.subscribe(new TextFilter("aaa123", TextFilter.EXACT));
				// m = ds.getNextMessage();
				time = System.currentTimeMillis();
				int i = 0;
				int j = 0;
				for (i = 0; i < numOfTurns; i++) {
					for (j = 0; j < numOfMessages; j++) {
						m = ds.getNextMessage();
						// n++;
					}
					System.out.println("Got " + (j) + " messages");
				}
			}
			System.out.println("Publish/receive time: "
					+ (System.currentTimeMillis() - time));
			ds.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}
}
