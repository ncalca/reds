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

import java.net.ConnectException;
import java.util.LinkedList;
import java.util.Random;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import polimi.reds.DispatchingService;
import polimi.reds.Filter;

import polimi.reds.TCPDispatchingService;
import polimi.reds.UDPDispatchingService;

public class ClientPerturbator {
	public static int PUBLISH = 0;
	public static int SUBSCRIBE = 1;

	// distanza massima dalla media
	// private static int MAX_DEVIATION = 10;
	private static int ITERATIONS = 10000;

	public static void main(String[] args) {

		LinkedList filterList = new LinkedList();
		IntegerFilter f = null;
		int actualFilters = 0;

		if (args.length == 0) {
			System.err
					.println("USAGE: java polimi.reds.examples.ClientTester [reds-tcp | reds-udp]:<brokerAddress>:<brokerPort>"
							+ " <localPort> <publishProbability> <mean> <stdDev> <minSubscriptions> <maxSubscriptions>");
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
			ds = new TCPDispatchingService(transportProtocol[1], Integer.parseInt(transportProtocol[2]));
		else if (transportProtocol[0].equals("reds-udp"))
			ds = new UDPDispatchingService(transportProtocol[1], Integer.parseInt(transportProtocol[2]), localPort);
		else
			throw new IllegalArgumentException();
		try {
			ds.open();
		} catch (ConnectException e) {
			e.printStackTrace();
		}
		long startTime = System.currentTimeMillis();
		for (int i = 0; i < ITERATIONS; i++) {
			// publish probability
			Random rnd = new Random();
			if (rnd.nextGaussian() <= Double.parseDouble(args[2])) {
				ds.publish(new IntegerMessage(rnd.nextInt()));
			} else {// subscribe
				if (actualFilters < Integer.parseInt(args[5])) {// subs less
																// than minimum
					double meanInterv = Double.parseDouble(args[3]) + Double.parseDouble(args[4]) * rnd.nextGaussian();
					f = getFilter(meanInterv, Double.parseDouble(args[4]) / 4);
					filterList.addLast(f);
					actualFilters++;
					ds.subscribe(f);
				} else if (actualFilters > Integer.parseInt(args[6])) {// subs
																		// more
																		// than
																		// max
					ds.unsubscribe((Filter) filterList.getFirst());
					actualFilters--;
				} else {// random
					if (rnd.nextGaussian() <= Double.parseDouble(args[2])) {
						// subscribe
						double meanInterv = Double.parseDouble(args[3]) + Double.parseDouble(args[4])
								* rnd.nextGaussian();
						f = getFilter(meanInterv, Double.parseDouble(args[4]) / 4);
						filterList.addLast(f);
						actualFilters++;
						ds.subscribe(f);

					} else {// unsubscribe
						ds.unsubscribe((Filter) filterList.getFirst());
						actualFilters--;
					}
				}
			}
		}
		ds.close();
		System.out.println("Exit after " + (System.currentTimeMillis() - startTime) + " milliseconds");
	}

	private static IntegerFilter getFilter(double mean, double std) {
		Random rnd = new Random();
		int maxDist = (int) std * 2;
		int v = (int) (mean + std * rnd.nextGaussian());
		return new IntegerFilter(v - maxDist, v + maxDist);
	}

}
