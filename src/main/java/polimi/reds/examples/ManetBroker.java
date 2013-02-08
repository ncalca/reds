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


import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import polimi.reds.broker.overlay.GenericOverlay;
import polimi.reds.broker.overlay.TCPTransport;
import polimi.reds.broker.overlay.TopologyManager;
import polimi.reds.broker.overlay.Transport;
import polimi.reds.broker.overlay.UDPTransport;
import polimi.reds.broker.overlay.WirelessTopologyManager;
import polimi.reds.broker.routing.DeferredUnsubscriptionReconfigurator;
import polimi.reds.broker.routing.GenericTable;
import polimi.reds.broker.routing.GenericRouter;
import polimi.reds.broker.routing.SubscriptionForwardingRoutingStrategy;
import polimi.reds.broker.routing.SubscriptionTable;

/**
 * This example class describes how to start a REDS broker in MANETs using the
 * ManetOverlayMgr provided. Other brokers are automatically discovered by the
 * overlay manager without using the Locator service.
 */
public class ManetBroker {
  public ManetBroker(String protocol, int localPort, int overlayManagerPort, int simulationTime) {
    // Configuring logging facility.
    ConsoleHandler ch = new ConsoleHandler();
    ch.setLevel(Level.ALL);
    Logger logger = Logger.getLogger("polimi");
    logger.addHandler(ch);
    
    	logger.setLevel(Level.ALL);
    // Setting up the broker.
    
    Transport transport = null;
    TopologyManager topolMgr = null;
    if(protocol.equals(Transport.TCP)){
    	transport = new TCPTransport(localPort);
    	((TCPTransport)transport).setBeaconing(true);
    }
    else if(protocol.equals(Transport.UDP)){
    	transport = new UDPTransport(localPort);
    	((UDPTransport)transport).setBeaconing(true);
    }
    else{
    	System.err.println("Unknown protocol");
    	System.exit(-1);
    }
    Set transports = new LinkedHashSet();
    transports.add(transport);
    topolMgr = new WirelessTopologyManager(overlayManagerPort);
    GenericOverlay overlay = new GenericOverlay(topolMgr, transports);
    GenericRouter router = new GenericRouter(overlay);
    
    DeferredUnsubscriptionReconfigurator reconf = new DeferredUnsubscriptionReconfigurator();
    reconf.setOverlay(overlay);
    SubscriptionForwardingRoutingStrategy routingStrat = new SubscriptionForwardingRoutingStrategy();
    routingStrat.setOverlay(overlay);
    SubscriptionTable subscriptionTable = new GenericTable();
    reconf.setRouter(router);
    router.setRoutingStrategy(routingStrat);
    router.setSubscriptionTable(subscriptionTable);
    overlay.start();
         	
    waitSimulationTime(simulationTime);    
    logger.log(Level.CONFIG, "Manet Broker stopping...");
    
    overlay.stop();   
  }
  
  public synchronized void waitSimulationTime(int secs){
    
    try {
      wait(secs*1000);
    } catch(InterruptedException e) {
      e.printStackTrace();
    }    	
  }

  public static void main(String[] args) {
    if(args.length==0) {
      System.err
          .println("USAGE: java polimi.reds.examples.ManetBroker <protocol> <localPort> <topologyManagerPort> <simulationTime>");
      return;
    }
    new ManetBroker(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]));
  }
}
