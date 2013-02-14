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

import polimi.reds.DispatchingService;
import polimi.reds.Message;
import polimi.reds.TCPDispatchingService;
import polimi.reds.TextFilter;
import polimi.reds.UDPDispatchingService;

/**
 * @author Alessandro Monguzzi
 */
public class ReplyCapableClientReplier {
	private static String TCP = "reds-tcp";
	private static String UDP = "reds-udp";
	public static int NUMBER_OF_MESSAGES = 2;

	public static void main(String[] args) {

		if (args.length == 0) {
			System.err
					.println("USAGE: java polimi.reds.examples.reply.ReplyCapableSubjectClientSender <brokerUrl> [<localPort>]");
			System.exit(0);
		}
		DispatchingService ds = null;
		String urlFragments[] = args[0].split(":");
		if (urlFragments[0].equals(TCP))
			ds = new TCPDispatchingService(urlFragments[1], Integer.parseInt(urlFragments[2]));
		else if (urlFragments[0].equals(UDP))
			ds = new UDPDispatchingService(urlFragments[1], Integer.parseInt(urlFragments[2]),
					Integer.parseInt(args[1]));
		else {
			System.err.println("Malformed url");
			System.exit(-1);
		}

		try {
			ds.open();
			System.out.println("Replier: open ok");
			ds.subscribe(new TextFilter("prova", TextFilter.CONTAINS));
			System.out.println("Replier: suscribe ok");
			for (int i = 0; i < NUMBER_OF_MESSAGES; i++) {
				Message m = ds.getNextMessage();
				ds.reply(m, m.getID());
				System.out.println("Replier: reply ok :" + m.getID().toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		ds.close();
	}
}
