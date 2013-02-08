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

import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import polimi.reds.broker.overlay.AlreadyAddedNeighborException;
import polimi.reds.broker.overlay.GenericOverlay;
import polimi.reds.broker.overlay.Overlay;
import polimi.reds.broker.overlay.SimpleTopologyManager;
import polimi.reds.broker.overlay.TCPTransport;
import polimi.reds.broker.overlay.TopologyManager;
import polimi.reds.broker.overlay.Transport;
import polimi.reds.broker.overlay.UDPTransport;
import polimi.reds.broker.routing.DeferredUnsubscriptionReconfigurator;
import polimi.reds.broker.routing.GenericRouter;
import polimi.reds.broker.routing.GenericTable;
import polimi.reds.broker.routing.HashReplyTable;
import polimi.reds.broker.routing.ImmediateForwardReplyManager;
import polimi.reds.broker.routing.Reconfigurator;
import polimi.reds.broker.routing.ReplyManager;
import polimi.reds.broker.routing.ReplyTable;
import polimi.reds.broker.routing.RoutingStrategy;
import polimi.reds.broker.routing.SubscriptionForwardingRoutingStrategy;
import polimi.reds.broker.routing.SubscriptionTable;

/**
 * @author Alessandro Monguzzi
 *
 */
public class ReplyCapableBroker {
	/**
	 * String to use a <code>TCPTransport</code>.
	 */
	public static String TCP = "reds-tcp";
	/**
	 * String to use a <code>UDPTransport</code>.
	 */
	public static String UDP = "reds-udp";
	/**
	 * Enable logging facility.
	 */
	public static String LOGGER_ON = "--logger";
	/**
	 * Neighbor to connect to.
	 */
	public static String NEIGHBOR_ON = "--neighbor";
		
	public static void main(String[] args) {
		if(args.length ==0) {
	      System.err
	          .println("USAGE: java polimi.reds.examples.ReplyCapableBroker <protocol> <localPort> [--neighbor <broker url>] " +
	          		"[--logger <level>]");
	      return;
	    }//configuring logging facility
		int indexLogger = indexOf(LOGGER_ON, args);
		Logger logger = null;
		FileHandler fh = null;
	    if(indexLogger != NOT_FOUND){
	    	logger = Logger.getLogger("polimi.reds");
	    	try {
	    	fh = new FileHandler(args[indexLogger +1]);
    		logger.setLevel(getLevel(args[indexLogger + 1]));
	    	fh.setLevel(logger.getLevel());	
	    	logger.addHandler(fh);
	    	} catch (SecurityException e) {
	    		// TODO Auto-generated catch block
	    		e.printStackTrace();
	    	} catch (IOException e) {
	    		// TODO Auto-generated catch block
	    		e.printStackTrace();
	    	}
		}
	    //check neighbor
	    String url = null;
	    int indexUrl = indexOf(NEIGHBOR_ON, args);
	    if(indexUrl != NOT_FOUND)
	    	url = args[indexUrl+1];
	    
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
	    TopologyManager topolMgr = new SimpleTopologyManager();
	    Overlay overlay = new GenericOverlay(topolMgr,transports);
	    RoutingStrategy routingStrategy = new SubscriptionForwardingRoutingStrategy();
	    Reconfigurator reconf = new DeferredUnsubscriptionReconfigurator();
	    GenericRouter router = new GenericRouter(overlay);
	    
	    SubscriptionTable subscriptionTable = new GenericTable();
	    routingStrategy.setOverlay(overlay);
	    reconf.setOverlay(overlay);
	    ReplyManager replyMgr = new ImmediateForwardReplyManager();
	    ReplyTable replyTbl = new HashReplyTable();
	    replyMgr.setOverlay(overlay);
	    router.setOverlay(overlay);
	    router.setSubscriptionTable(subscriptionTable);
	    router.setRoutingStrategy(routingStrategy);
	    router.setReplyManager(replyMgr);
	    router.setReplyTable(replyTbl);
	    reconf.setRouter(router);
	    replyMgr.setReplyTable(replyTbl);
	    overlay.start();
	    if (url != null){
	    	try {
				overlay.addNeighbor(url);
				System.out.println("Connected to "+ url);
			} catch (AlreadyAddedNeighborException e) {
				e.printStackTrace();
				System.err.println("already connected neighbor");
			} catch (ConnectException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
	    System.out.println("broker started");
	}//end main
	
	public static int NOT_FOUND = -1;
	/**
	 * Search into <code>args</code> the index of <code>searchedArg</code>
	 * @param searchedArg the searched argument
	 * @param args the arguments' array
	 * @return the index of <code>searchedArg</code> if present else <code>NOT_FOUND</code>
	 */
	public static int indexOf(String searchedArg, String[] args){
		int i=0;
		while(i<args.length){
			if(args[i].equals(searchedArg))
				return i;
			i++;
		}
		return NOT_FOUND;
	}
	
	public static String SEVERE = "severe";
	public static String WARNING = "warning";
	public static String INFO = "info";
	public static String CONFIG = "config";
	public static String FINE = "fine";
	public static String FINER = "finer";
	public static String FINEST = "finest";
	public static String OFF = "off";
	public static Level getLevel(String level){
		if(level.equalsIgnoreCase(OFF))
			return Level.OFF;
		else if(level.equalsIgnoreCase(FINEST))
			return Level.FINEST;
		else if(level.equalsIgnoreCase(FINER))
			return Level.FINER;
		else if(level.equalsIgnoreCase(FINE))
			return Level.FINE;
		else if(level.equalsIgnoreCase(CONFIG))
			return Level.CONFIG;
		else if(level.equalsIgnoreCase(INFO))
			return Level.INFO;
		else if(level.equalsIgnoreCase(WARNING))
			return Level.WARNING;
		else if(level.equalsIgnoreCase(SEVERE))
			return Level.SEVERE;
		else
			return Level.ALL;
	}
}// end class ReplyCapableSubjectBroker
