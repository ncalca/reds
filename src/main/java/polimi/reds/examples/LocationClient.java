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

package polimi.reds.examples;

import polimi.reds.*;
import polimi.reds.location.*;
import polimi.util.Locator;
import java.io.IOException;
import java.util.*;
import java.util.logging.*;

/*******************************************************************************
 * A REDS location aware client (main class).
 ******************************************************************************/
public class LocationClient {
	public static int PUBLISH = 0;
	public static int SUBSCRIBE = 1;

	public static void printUsageAndExit() {
		System.err
				.println("USAGE: java polimi.reds.examples.Client [-locator | -dsurl <url>] [-publish <minLat> <maxLat> <minLong> <maxLong>| -subscribe <minLat> <maxLat> <minLong> <maxLong>] [-location <lat> <long>]");
		System.exit(0);
	}

	public static void main(String[] args) {
		String dsURL = null;
		String dsAddr = null;
		int dsPort = 0;
		int action = SUBSCRIBE;
		double minLat = 0, maxLat = 0, minLong = 0, maxLong = 0;
		double myLat = 0, myLong = 0;
		// configuring logging facility
		Logger logger = Logger.getLogger("polimi.reds");
		ConsoleHandler ch = new ConsoleHandler();
		logger.addHandler(ch);
		logger.setLevel(Level.ALL);
		ch.setLevel(Level.CONFIG);
		// parse the command line
		if (args.length == 0)
			printUsageAndExit();
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-locator")) {
				System.out.println("Search for available brokers...");
				String[] urls = null;
				try {
					Locator locator = new Locator();
					urls = locator.locate(1000);
				} catch (IOException e) {
					e.printStackTrace();
				}
				if (urls == null) {
					System.out.println("No other brokers available");
					System.exit(0);
				}
				dsURL = urls[0];
			} else if (args[i].equals("-dsurl")) {
				i++;
				if (i == args.length)
					printUsageAndExit();
				dsURL = args[i];
			} else if (args[i].equals("-publish")) {
				i++;
				if (args.length - i < 4)
					printUsageAndExit();
				action = PUBLISH;
				minLat = Double.parseDouble(args[i++]);
				maxLat = Double.parseDouble(args[i++]);
				minLong = Double.parseDouble(args[i++]);
				maxLong = Double.parseDouble(args[i]);
			} else if (args[i].equals("-subscribe")) {
				i++;
				if (args.length - i < 4)
					printUsageAndExit();
				action = SUBSCRIBE;
				minLat = Double.parseDouble(args[i++]);
				maxLat = Double.parseDouble(args[i++]);
				minLong = Double.parseDouble(args[i++]);
				maxLong = Double.parseDouble(args[i]);
			} else if (args[i].equals("-location")) {
				i++;
				if (args.length - i < 2)
					printUsageAndExit();
				myLat = Double.parseDouble(args[i++]);
				myLong = Double.parseDouble(args[i]);
			} else
				printUsageAndExit();
		}
		// search for other brokers
		System.out.println("Connecting to " + dsURL);
		StringTokenizer st = new StringTokenizer(dsURL, ":");
		if (st.nextToken().equals("reds-tcp")) {
			dsAddr = st.nextToken();
			dsPort = Integer.parseInt(st.nextToken());
		}
		LocationDispatchingService ds = new TCPLocationDispatchingService(dsAddr, dsPort);
		Message m;
		Zone z = new PhysicalArea(minLat, maxLat, minLong, maxLong);
		try {
			ds.open();
			ds.setLocation(new PhysicalLocation(myLat, myLong));
			if (action == PUBLISH) {
				System.out.println("Waiting for subscriptions to propagate before publishing");
				Thread.sleep(1000);
				m = new TextMessage("test 123");
				ds.publish(m, z);
				System.out.println("Waiting for message to propagate before disconnecting");
				Thread.sleep(1000);
			} else {
				ds.subscribe(new TextFilter("test", TextFilter.CONTAINS), z);
				m = ds.getNextMessage();
				System.out.println("Got message: " + m);
			}
			ds.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
