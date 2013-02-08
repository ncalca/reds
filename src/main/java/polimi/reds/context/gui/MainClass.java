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

package polimi.reds.context.gui;

import polimi.reds.context.Condition;
import polimi.reds.context.ContextFilter;
import polimi.reds.context.Property;
import polimi.reds.context.routing.PropertyRange;

public class MainClass {

	private class BrokerThread extends Thread {

		private int brokerListenPort;

		private BrokerGui broker;

		public BrokerThread( int brokerListenPort ) {
			this.brokerListenPort = brokerListenPort;
		}

		public void run() {
			broker = new BrokerGui( this.brokerListenPort );
			Dormi( 500 );
			broker.startListen();
			broker.show();
		}

		public void connect( int port ) {
			broker.connect( port );
		}
	}

	private class ClientThread extends Thread {
		private int port;

		private ClientGui c;

		public ClientThread( int port ) {
			this.port = port;
		}

		public void run() {
			c = new ClientGui( port );
			c.show();
			Dormi( 500 );
			c.connect();
			Dormi( 500 );
			// c.setContext();
			// Dormi( 500 );

		}

		public void setContext( int ram, int hd ) {
			c.setContext( ram, hd );
		}

		public void subscribe( ContextFilter f ) {
			c.subscribe( f );
		}

		public void subscribe() {
			c.subscribe();
		}

	}

	public ContextFilter createContextFilter( int ram, int hd ) {
		ContextFilter cf = new ContextFilter();
		cf.addCondition( new Condition( "RAM", Property.INTEGER, Condition.GREATER, new Integer( ram ) ) );
		cf.addCondition( new Condition( "HD", Property.INTEGER, Condition.GREATER, new Integer( hd ) ) );
		return cf;
	}

	public MainClass( int basePort ) {
		int portB1 = basePort + 1;
		int portB2 = basePort + 2;
		int portB3 = basePort + 3;

		BrokerThread b1 = new BrokerThread( portB1 );
		b1.start();

		Dormi( 2000 );

		BrokerThread b2 = new BrokerThread( portB2 );
		b2.start();

		Dormi( 2000 );

		BrokerThread b3 = new BrokerThread( portB3 );
		b3.start();

		Dormi( 5000 );

		b1.connect( portB2 );

		Dormi( 5000 );

		b3.connect( portB2 );

		Dormi( 3000 );

		ClientThread c1a = new ClientThread( portB1 );
		c1a.start();
		Dormi( 1500 );
		c1a.setContext( 0, 0 );

		ClientThread c1b = new ClientThread( portB1 );
		c1b.start();
		Dormi( 1500 );
		c1b.setContext( (int) ( basePort / 200 ), (int) ( basePort / 200 ) );

		ClientThread c3 = new ClientThread( portB3 );
		c3.start();
		Dormi( 1500 );
		c3.setContext( (int) ( basePort / 100 ), (int) ( basePort / 100 ) );

		Dormi( 2000 );
		c3.subscribe( createContextFilter( 5, 5 ) );

		Dormi( 1000 );
		c1a.subscribe();

	}

	public static void main( String[] args ) {

		MainClass main = new MainClass( 7000 );

		// Dormi(5000);

		// MainClass main2 = new MainClass(8000);

	}

	private static void Scrivi( String s ) {
		System.out.println( s );
		System.out.flush();
	}

	// private static void CollegaBroker( ContextBroker[] brokers, int source,
	// int dest ) {
	// try {
	// brokers[source].connect( 6000 + dest );
	// Scrivi( "collegato " + source + " a " + dest );
	// }
	// catch ( Exception ex ) {
	// Scrivi( ex.toString() );
	// }
	// Dormi( 100 );
	// }

	private static void Dormi( int millisec ) {
		try {
			Thread.sleep( millisec );
		}
		catch ( InterruptedException e ) {
			e.printStackTrace();
		}
	}

	// private static Context aPrinterContext( String printerType, String shape,
	// int speed, int sheet ) {
	// Context aPrinter = new Context();
	// aPrinter.addProperty( new NonSortableProperty( "Printer type",
	// printerType ) );
	// aPrinter.addProperty( new NonSortableProperty( "Shape", shape ) );
	// aPrinter.addProperty( new IntegerProperty( "Speed", speed ) );
	// aPrinter.addProperty( new IntegerProperty( "Sheet", sheet ) );
	//
	// return aPrinter;
	// }

}
