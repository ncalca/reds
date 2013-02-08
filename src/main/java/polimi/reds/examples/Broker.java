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

import polimi.reds.broker.overlay.*;
import polimi.reds.broker.routing.DeferredUnsubscriptionReconfigurator;
import polimi.reds.broker.routing.GenericTable;
import polimi.reds.broker.routing.GenericRouter;
import polimi.reds.broker.routing.SubscriptionForwardingRoutingStrategy;
import polimi.reds.broker.routing.SubscriptionTable;
import polimi.util.Locator;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.*;

/*******************************************************************************
 * A REDS broker (main class).
 ******************************************************************************/
public class Broker {
  /**
   * String to use a <code>TCPTransport</code>.
   */
  public static String TCP = "reds-tcp";
  /**
   * String to use a <code>UDPTransport</code>.
   */
  public static String UDP = "reds-udp";

  public static void main(String[] args) {
    if(args.length<2) {
      System.err
          .println("USAGE: java polimi.reds.examples.ReplyCapableBroker <protocol> <localPort> [-logger]");
      return;
    }
    // configuring logging facility
    Logger logger = Logger.getLogger("polimi.reds");
    ConsoleHandler ch = new ConsoleHandler();
    logger.addHandler(ch);
    if(args.length==3) {
      if(args[2].equals("-logger")) {
        logger.setLevel(Level.ALL);
        ch.setLevel(Level.ALL);
      } else {
        logger.setLevel(Level.OFF);
        ch.setLevel(Level.OFF);
      }
    }
    Transport transport = null;
    if(args[0].equals(TCP)) transport = new TCPTransport(Integer
        .parseInt(args[1]));
    else if(args[0].equals(UDP)) transport = new UDPTransport(Integer
        .parseInt(args[1]));
    else {
      System.err.println("Unknown protocol");
      System.exit(-1);
    }
    Set transports = new LinkedHashSet();
    transports.add(transport);
    // TopologyManager topolMgr = new LSTreeTopologyManager();
    TopologyManager topolMgr = new SimpleTopologyManager();
    Overlay overlay = new GenericOverlay(topolMgr, transports);
    SubscriptionForwardingRoutingStrategy routingStrategy = new SubscriptionForwardingRoutingStrategy();
    DeferredUnsubscriptionReconfigurator reconf = new DeferredUnsubscriptionReconfigurator();
    SubscriptionTable subscriptionTable = new GenericTable();
    GenericRouter router = new GenericRouter(overlay);
    routingStrategy.setOverlay(overlay);
    reconf.setOverlay(overlay);
    reconf.setRouter(router);
    router.setOverlay(overlay);
    router.setRoutingStrategy(routingStrategy);
    router.setSubscriptionTable(subscriptionTable);
    overlay.start();
    String url = null;
    // build my url and start the locator
    try {
      url = args[0]+":"+InetAddress.getLocalHost().getHostAddress()+":"+args[1];
    } catch(UnknownHostException e) {
      e.printStackTrace();
    }
    try {
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
