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

import polimi.reds.broker.overlay.GenericOverlay;
import polimi.reds.broker.overlay.LSTreeTopologyManager;
import polimi.reds.broker.overlay.Overlay;
import polimi.reds.broker.overlay.TCPTransport;
import polimi.reds.broker.overlay.TopologyManager;
import polimi.reds.broker.overlay.Transport;
import polimi.reds.broker.overlay.UDPTransport;
import polimi.reds.broker.routing.GenericTable;
import polimi.reds.broker.routing.RoutingStrategy;
import polimi.reds.broker.routing.GenericRouter;
import polimi.reds.broker.routing.SubscriptionTable;
import polimi.reds.location.*;
import polimi.util.Locator;

import java.net.InetAddress;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.*;

/*******************************************************************************
 * A REDS location-aware broker (main class).
 ******************************************************************************/
public class LocationBroker {
	/**
	 * String to use a <code>TCPTransport</code>.
	 */
	public static String TCP = "reds-tcp";
	/**
	 * String to use a <code>UDPTransport</code>.
	 */
	public static String UDP = "reds-udp";
	/**
	 * Enable logging facility
	 */
	public static String LOGGER_ON = "--logger";
  public static void main(String[] args) {
    if(args.length ==0) {
      System.err
          .println("USAGE: java polimi.reds.examples.Broker <protocol> <localPort> [--logger]");
      return;
    }
    // configuring logging facility
    Logger logger = Logger.getLogger("polimi.reds");
    ConsoleHandler ch = new ConsoleHandler();
    logger.addHandler(ch);
    if(args.length == 3 && args[2].equals(LOGGER_ON)){
    	logger.setLevel(Level.ALL);
        ch.setLevel(Level.ALL);
    }else{
    	logger.setLevel(Level.OFF);
        ch.setLevel(Level.OFF);
    }
    // building the broker
    Transport transport = null;
    if(args[0].equals(TCP))
    	transport = new TCPTransport(Integer.parseInt(args[1]));
    else if(args[0].equals(UDP))
    	transport = new UDPTransport(Integer.parseInt(args[1]));
    else{
    	System.err.println("Unknown protocol");
    	System.exit(-1);
    }
    Set transports = new LinkedHashSet();
    transports.add(transport);
    TopologyManager  topologyManager = new LSTreeTopologyManager();
    Overlay overlay = new GenericOverlay(topologyManager, transports);
    RoutingStrategy routingStrategy = new LocationForwardingRoutingStrategy(new PhysicalZoneMerger());
    SubscriptionTable subscriptionTable = new GenericTable();
    
    GenericRouter router = new GenericRouter(overlay);
    
    routingStrategy.setOverlay(overlay);
    
    router.setOverlay(overlay);
    router.setRoutingStrategy(routingStrategy);
    router.setSubscriptionTable(subscriptionTable);
    overlay.start();
    String url = null;
    
    try {
		url = args[0]+":"+InetAddress.getLocalHost().getHostAddress()+":"+args[1];
      Locator locator = null;
      try {
        locator = new Locator(url);
        locator.startServer();
      } catch(java.io.IOException ex) {
        System.out.println("Unable to start location service");
      }
      System.out.println("Press enter to connect to other brokers");
      System.in.read();
      // search for other brokers
      System.out.println("Searching for other brokers...");
      String[] urls = null;
      try {
        urls = locator.locate(1000);
      } catch(Exception ex) {
        System.out.println("Unable to use the locator");
      }
      if(urls!=null) {
        System.out.println("Connecting to "+urls[0]);
        overlay.addNeighbor(urls[0]);
      } else {
        System.out.println("No other brokers available");
      }
    } catch(Exception ex) {
      ex.printStackTrace();
    }
  }
}
