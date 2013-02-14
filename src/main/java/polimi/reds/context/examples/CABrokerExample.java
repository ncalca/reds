/*
 * Created on May 30, 2007
 *
 */
package polimi.reds.context.examples;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import polimi.reds.context.gui.CABroker;

public class CABrokerExample {
	public static void main(String[] args) {
		new CABroker(6000);
		Logger l = Logger.getLogger("polimi.reds");
		l.setLevel(Level.FINE);
		ConsoleHandler ch = new ConsoleHandler();
		ch.setLevel(Level.FINE);
		l.addHandler(ch);
	}
}
