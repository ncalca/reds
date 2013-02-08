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
import polimi.util.Locator;
import java.io.IOException;
import java.util.*;
import java.util.logging.*;

/*******************************************************************************
 * A REDS client (main class).
 ******************************************************************************/
public class Client {
  public static int PUBLISH = 0;
  public static int SUBSCRIBE = 1;

  public static void printUsageAndExit() {
    System.err
        .println("USAGE: java polimi.reds.examples.Client [-locator | -dsurl <url>] [-publish | -subscribe] [number of messages]");
    System.exit(0);
  }

  public static void main(String[] args) {
    String dsURL = null;
    String dsAddr = null;
    int dsPort = 0;
    long numOfMessages = 0;
    int action = SUBSCRIBE;
    Message m = new TextMessage("aaa123");
    // configuring logging facility
    Logger logger = Logger.getLogger("polimi.reds");
    ConsoleHandler ch = new ConsoleHandler();
    logger.addHandler(ch);
    logger.setLevel(Level.ALL);
    ch.setLevel(Level.CONFIG);
    // parse the command line
    if(args.length==0) printUsageAndExit();
    for(int i = 0; i<args.length; i++) {
      if(args[i].equals("-locator")) {
        System.out.println("Search for available brokers...");
        String[] urls = null;
        try {
          Locator locator = new Locator();
          urls = locator.locate(1000);
        } catch(IOException e) {
          e.printStackTrace();
        }
        if(urls==null) {
          System.out.println("No other brokers available");
          System.exit(0);
        }
        dsURL = urls[0];
      } else if(args[i].equals("-dsurl")) {
        i++;
        if(i==args.length) printUsageAndExit();
        dsURL = args[i];
      } else if(args[i].equals("-publish")) action = PUBLISH;
      else if(args[i].equals("-subscribe")) action = SUBSCRIBE;
      else {
        try {
          numOfMessages = Long.parseLong(args[i]);
        } catch(NumberFormatException e) {
          printUsageAndExit();
        }
      }
    }
    // search for other brokers
    System.out.println("Connecting to "+dsURL);
    StringTokenizer st = new StringTokenizer(dsURL, ":");
    if(st.nextToken().equals("reds-tcp")) {
      dsAddr = st.nextToken();
      dsPort = Integer.parseInt(st.nextToken());
    }
    DispatchingService es = new TCPDispatchingService(dsAddr, dsPort);
    try {
      es.open();
      if(action==PUBLISH) {
        long startingTime = System.currentTimeMillis();
        for(long i = 0; i<numOfMessages; i++) {
          es.publish(m);
          if(i%1000==999) System.out.println("Published "+(i+1)+" messages");
        }
        long endingTime = System.currentTimeMillis();
        System.out.println("Starting pub at: "+startingTime+"\tEnding pub at "
            +endingTime+"\tDelta: "+(endingTime-startingTime));
      } else {
        es.subscribe(new TextFilter("aaa", TextFilter.CONTAINS));
        es.subscribe(new TextFilter("aaa1", TextFilter.CONTAINS));
        es.subscribe(new TextFilter("bbb", TextFilter.CONTAINS));
        es.subscribe(new TextFilter("ccc", TextFilter.CONTAINS));
        es.subscribe(new TextFilter("ddd", TextFilter.CONTAINS));
        es.subscribe(new TextFilter("eee", TextFilter.CONTAINS));
        es.subscribe(new TextFilter("fff", TextFilter.CONTAINS));
        es.subscribe(new TextFilter("ggg", TextFilter.CONTAINS));
        es.subscribe(new TextFilter("hhh", TextFilter.CONTAINS));
        m = es.getNextMessage();
        long startingTime = System.currentTimeMillis();
        for(int i = 0; i<numOfMessages-1; i++) {
            m = es.getNextMessage();
            if(i%1000==999) System.out.println("Received "+(i+1)+" messages");
        }
        long endingTime = System.currentTimeMillis();
        System.out.println("Arrival of first msg: "+startingTime+"\tArrival of last msg "
            +endingTime+"\tDelta: "+(endingTime-startingTime));
      }
      es.close();
    } catch(Exception ex) {
      ex.printStackTrace();
    }
  }
}
