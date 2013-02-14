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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Random;
import polimi.reds.DispatchingService;
import polimi.reds.TextFilter;
import polimi.reds.TextMessage;
import polimi.reds.UDPDispatchingService;

/**
 * This class can be used for instantiating a client in the REDS system when
 * this last is intended to run on MANETs, i.e. when each client is equipped
 * with a local dispatcher, and carrying out an evaluation of REDS dispatching
 * functionalities. In particular, the client takes as command-line parameters
 * the IP port to be used for the transport services, the IP port to be used for
 * the OverlayManager, the average publish interval and a flag stating whether
 * the local broker will be in the core set or not. A broker in the core set
 * starts with the maximum number of subscriptions and is not allowed to later
 * unsubscribe. A subscription contains a randomly generated integer number from
 * zero to RANDOM_THRESHOLD-1. All clients in the system publish events
 * according to the constants defined as static fields plus a CONTROL event used
 * to gather statistics. Each message carries MESSAGE_LENGTH randomly generated
 * integer numbers plus a counter and the local broker's id. A message matches a
 * subscription if it contains at least once the number specified in the
 * subscription. At the end of the simulation, i.e. when SIMULATION_TIME seconds
 * have elapsed, the broker records on persistent storage from two to three
 * files having the local broker id embodied in the file name. This files will
 * be input data for ManetSimParser; this last will be in charge of parsing
 * those files ad output static and dynamic statistics.
 */
public class ManetClient {
	// Simulation time in seconds
	private static final int SIMULATION_TIME = 30;
	// The local client will wait BROKER_STARTUP_TIME seconds before connecting
	// to
	// the dispatching service
	private static final int BROKER_STARTUP_TIME = 10;
	// Each subscription is composed of a number in string form between 0 and
	// RANDOM_THRESHOLD-1
	private static final int RANDOM_THRESHOLD = 100;
	// Each message will carry a MESSAGE_LENGTH numbers between 0 and
	// RANDOM_THRESHOLD-1
	private static final int MESSAGE_LENGTH = 10;
	// Every CONTROL_EVENT events, a special control event is generated
	private static final int CONTROL_EVENT = 5;
	// Minimum number of subscriptions present in a broker's subscription table
	private static final int MIN_SUBSCRIPTIONS = 1;
	// Interval for subscriptions expressed in seconds
	private static final int AVG_SUBSCRIBE_INTERVAL = 30;
	// Interval for unsubscriptions expressed in seconds
	private static final int AVG_UNSUBSCRIBE_INTERVAL = 30;
	// The current publish rate, expresssed in seconds
	private final int avgPublishInterval;
	// Maximum number of subscriptions at a non-core node,
	// in case of a core node, MAX_SUBSCRIPTIONS are generated and kept fixed
	private final int MAX_SUBSCRIPTIONS = 10;
	// Average number of subscriptions to keep at a non-core node
	private final int avgSubscriptions;
	// The set of active subscriptions
	private LinkedList activeSubscriptions;
	// A reference to the event service
	private DispatchingService srv;

	/**
	 * Instantiates a new client for running REDS on a MANET and evaluating its
	 * performances. In this particula case, each client is equipped with a
	 * local dispatcher.
	 * 
	 * @param dsPort
	 *            the port at which the local client is waiting for incoming
	 *            connections.
	 * @param core
	 *            true if the local dispatcher is in the core set, false
	 *            otherwise
	 * @param avgPublishInterval
	 *            the average interval at which the local client publishes a
	 *            message.
	 * @param avgSubscribeInterval
	 *            the average interval at which the local client generates a
	 *            (un)subscription.
	 * @param maxSubscriptions
	 *            the maximum number of subscriptions ever expressed by the
	 *            local client.
	 */
	public ManetClient(int brokerPort, int localPort, boolean core, int avgPublishInterval) {
		srv = new UDPDispatchingService("127.0.0.1", brokerPort, localPort);
		activeSubscriptions = new LinkedList();
		this.avgPublishInterval = avgPublishInterval;
		this.avgSubscriptions = MAX_SUBSCRIPTIONS / 2;
		if (core) {
			// Starts a client corresponding to a core broker
			coreClient();
		} else {
			// Starts a client corresponding to a non-core broker
			nonCoreClient();
		}
	}

	/**
	 * Starts a client corresponding to a core broker at the specified port.
	 */
	private synchronized void coreClient() {
		LinkedList subscriptions = new LinkedList();
		// Waiting for the broker...
		try {
			wait(BROKER_STARTUP_TIME * 1000);
		} catch (InterruptedException e2) {
			e2.printStackTrace();
		}
		// Opening connection with the local broker
		try {
			srv.open();
		} catch (ConnectException e) {
			e.printStackTrace();
		}
		// Setting up subscriptions at core node
		Random rnd = new Random();
		for (int i = 0; i < MAX_SUBSCRIPTIONS; i++) {
			TextFilter flt = new TextFilter(String.valueOf(rnd.nextInt(RANDOM_THRESHOLD)), TextFilter.CONTAINS);
			srv.subscribe(flt);
			subscriptions.add(flt);
		}
		// Setting up control subscription
		srv.subscribe(new TextFilter("CONTROL", TextFilter.CONTAINS));
		// Starting publisher and notifier threads
		NotifierThread notifier = new NotifierThread();
		PublisherThread publisher = new PublisherThread();
		new Thread(notifier).start();
		new Thread(publisher).start();
		// Waiting simulation time
		try {
			wait(SIMULATION_TIME * 1000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		// The simulation ended
		publisher.stop();
		notifier.stop();
		// Recording subscriptions on stable storage
		ObjectOutputStream oos = null;
		try {
			oos = new ObjectOutputStream(new FileOutputStream("SUBSCRIPTIONS." + srv.getID()));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		ListIterator it = subscriptions.listIterator();
		while (it.hasNext()) {
			try {
				oos.writeObject((TextFilter) it.next());
				oos.flush();
				oos.reset();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			oos.flush();
			oos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		srv.close();
	}

	/**
	 * Starts a client corresponding to a non-core broker at the specified port.
	 */
	private synchronized void nonCoreClient() {
		// Waiting for the broker...
		try {
			wait(BROKER_STARTUP_TIME * 1000);
		} catch (InterruptedException e2) {
			e2.printStackTrace();
		}
		// Opening connection with the local broker
		try {
			srv.open();
		} catch (ConnectException e) {
			e.printStackTrace();
		}
		// Starting variable subscriptions and unsubscriptions at non-core nodes
		SubscriberThread subscriber = new SubscriberThread();
		UnsubscriberThread unsubscriber = new UnsubscriberThread();
		new Thread(subscriber).start();
		new Thread(unsubscriber).start();
		// Setting up CONTROL subscription.
		srv.subscribe(new TextFilter("CONTROL", TextFilter.CONTAINS));
		// Starting publisher and notifier threads
		NotifierThread notifier = new NotifierThread();
		PublisherThread publisher = new PublisherThread();
		new Thread(notifier).start();
		new Thread(publisher).start();
		// Waiting simulation time
		try {
			wait(SIMULATION_TIME * 1000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		// The simulation ended
		subscriber.stop();
		unsubscriber.stop();
		publisher.stop();
		notifier.stop();
		srv.close();
	}

	private static void printUsageAndExit() {
		System.err
				.println("USAGE: java polimi.reds.examples.ManetClient <protocol> <brokerport> <ClientPort> <topologyManagerPort> <Router|NoCore> <avgPublishInterval>");
		System.exit(-1);
	}

	public static void main(String[] args) {
		if (args.length < 6 || (!args[4].equalsIgnoreCase("Router") && !args[4].equalsIgnoreCase("NoCore"))) {
			printUsageAndExit();
		}
		final int brokerPort = Integer.parseInt(args[1]);
		final int clientPort = Integer.parseInt(args[2]);
		final int topMgrPort = Integer.parseInt(args[3]);
		final String protocol = args[0];

		new Thread() {
			public void run() {
				new ManetBroker(protocol, brokerPort, topMgrPort, SIMULATION_TIME);
			}
		}.start();

		// Starts the broker

		// Starts the client
		if (args[4].equalsIgnoreCase("Router"))
			new ManetClient(brokerPort, clientPort, true, Integer.parseInt(args[5]));
		else
			new ManetClient(brokerPort, clientPort, false, Integer.parseInt(args[5]));
	}

	/**
	 * This class can be used to instantiate a thread in charge of generating
	 * random subscriptions at a certain rate specified by
	 * AVG_SUBSCRIBE_INTERVAL. It is instantied only at non-core nodes.
	 */
	class SubscriberThread implements Runnable {
		// Used for generating random numbers
		private Random rnd;
		// Used for stopping the thread
		private boolean running;

		public SubscriberThread() {
			rnd = new Random();
			running = true;
		}

		public void stop() {
			running = false;
		}

		public synchronized void run() {
			// Starts with the average number of subscriptions
			for (int i = 0; i < avgSubscriptions; i++) {
				TextFilter flt = generateTextFilter();
				srv.subscribe(flt);
				synchronized (activeSubscriptions) {
					activeSubscriptions.add(flt);
				}
			}
			// Main loop
			while (running) {
				// Generates a random number
				try {
					wait(rnd.nextInt(2 * AVG_SUBSCRIBE_INTERVAL) * 1000 + 1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				// Expresses the subscription
				synchronized (activeSubscriptions) {
					if (activeSubscriptions.size() < MAX_SUBSCRIPTIONS) {
						TextFilter flt = generateTextFilter();
						if (srv.isOpened()) {
							srv.subscribe(flt);
							activeSubscriptions.add(flt);
						}
					}
				}
			}
		}

		/**
		 * Generates a new subscription.
		 * 
		 * @return the TextFilter associated with the generated subscription.
		 */
		private TextFilter generateTextFilter() {
			return new TextFilter(String.valueOf(rnd.nextInt(RANDOM_THRESHOLD)), TextFilter.CONTAINS);
		}
	}

	/**
	 * This class can be used to instantiate a thread in charge of generating
	 * random unsubscriptions at a certain rate specified by
	 * AVG_UNSUBSCRIBE_INTERVAL. It is instantied only at non-core nodes.
	 */
	class UnsubscriberThread implements Runnable {
		// Used for generating random numbers
		private Random rnd;
		// Used for stopping the thread
		private boolean running;

		public UnsubscriberThread() {
			rnd = new Random();
			running = true;
		}

		public void stop() {
			running = false;
		}

		public synchronized void run() {
			// Main loop
			while (running) {
				try {
					wait(rnd.nextInt(2 * AVG_UNSUBSCRIBE_INTERVAL) * 1000 + 1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				// Expresses the unsubscription
				synchronized (activeSubscriptions) {
					if (activeSubscriptions.size() > MIN_SUBSCRIPTIONS) {
						TextFilter flt = (TextFilter) activeSubscriptions
								.remove(rnd.nextInt(activeSubscriptions.size()));
						if (srv.isOpened()) {
							srv.unsubscribe(flt);
						}
					}
				}
			}
		}
	}

	/**
	 * This class can be used to instantiate a thread in charge of publishing
	 * events in the system at a rate specified by one of the command-line
	 * parameters and record on persitent storage the set of published events
	 * once the simulation ended.
	 */
	class PublisherThread implements Runnable {
		// Used to generate random numbers
		private Random rnd;
		// A list containing the published messages
		private LinkedList messages;
		// Used for stopping the thread
		private boolean running;

		public PublisherThread() {
			rnd = new Random();
			messages = new LinkedList();
			running = true;
		}

		public synchronized void run() {
			long counter = 0;
			TextMessage msg = null;
			// Main loop
			while (running) {
				try {
					wait(rnd.nextInt(2 * avgPublishInterval) * 1000 + 1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				// Generate the message, each CONTROL_EVENT messages a control
				// message
				// is generated
				if (counter % CONTROL_EVENT == 0) {
					msg = generateControlEvent(counter);
				} else {
					msg = generateEvent(counter);
				}
				// Publishes the message
				if (srv.isOpened()) {
					srv.publish(msg);
					messages.addLast(msg);
					counter++;
				}
			}
		}

		public void stop() {
			// Stops the main loop
			running = false;
			// Records the list of published messages
			ObjectOutputStream oos = null;
			try {
				oos = new ObjectOutputStream(new FileOutputStream("EVENTS." + srv.getID()));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			synchronized (this) {
				ListIterator it = messages.listIterator();
				while (it.hasNext()) {
					try {
						oos.writeObject((TextMessage) it.next());
						oos.flush();
						oos.reset();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			try {
				oos.flush();
				oos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		/**
		 * Generates an event carrying MESSAGE_LENGTH integer numbers.
		 * 
		 * @param counter
		 *            an id for this messages.
		 * @return the generated message.
		 */
		private TextMessage generateEvent(long counter) {
			String eventStr = new String();
			for (int i = 0; i < MESSAGE_LENGTH; i++) {
				eventStr = eventStr.concat(String.valueOf(rnd.nextInt(RANDOM_THRESHOLD)) + " ");
			}
			return new TextMessage(eventStr.concat(srv.getID() + "-" + String.valueOf(counter)));
		}

		/**
		 * Generates a special purpose CONTROL message, these lasts are used by
		 * ManetSimParser for gathering dynamic statistics.
		 * 
		 * @param counter
		 *            an id for this messages.
		 * @return the generated message.
		 */
		private TextMessage generateControlEvent(long counter) {
			return new TextMessage(new String("CONTROL-" + srv.getID() + "-" + String.valueOf(counter)));
		}
	}

	/**
	 * This class can be used to instantiate a thread in charge of receiving
	 * notification from the dispatching service and record on persitent storage
	 * the set of received notifications once the simulation ended.
	 */
	class NotifierThread implements Runnable {
		// A list containing the recived notifications
		private LinkedList notifications;
		// Used to styop the thread
		private boolean running;

		public NotifierThread() {
			notifications = new LinkedList();
			running = false;
		}

		public void run() {
			// Main loop
			running = true;
			while (running) {
				TextMessage msg = (TextMessage) srv.getNextMessage();
				synchronized (notifications) {
					if (msg != null) {
						notifications.addLast(msg);
						System.out.println("---> NOTIFICATION RECEIVED: " + msg);
					}
				}
			}
		}

		public synchronized void stop() {
			// Stops the main loop
			running = false;
			// Records on persistent storage the set of received notifications
			ObjectOutputStream oos = null;
			try {
				oos = new ObjectOutputStream(new FileOutputStream("NOTIFICATIONS." + srv.getID()));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			synchronized (notifications) {
				ListIterator it = notifications.listIterator();
				while (it.hasNext()) {
					try {
						oos.writeObject((TextMessage) it.next());
						oos.flush();
						oos.reset();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			try {
				oos.flush();
				oos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
