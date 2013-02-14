package polimi.reds.test;

import java.util.LinkedHashSet;
import java.util.Set;

import polimi.reds.broker.overlay.GenericOverlay;
import polimi.reds.broker.overlay.Overlay;
import polimi.reds.broker.overlay.SimpleTopologyManager;
import polimi.reds.broker.overlay.TCPTransport;
import polimi.reds.broker.overlay.TopologyManager;
import polimi.reds.broker.overlay.Transport;
import polimi.reds.broker.routing.DeferredUnsubscriptionReconfigurator;
import polimi.reds.broker.routing.GenericRouter;
import polimi.reds.broker.routing.GenericTable;
import polimi.reds.broker.routing.HashReplyTable;
import polimi.reds.broker.routing.ImmediateForwardReplyManager;
import polimi.reds.broker.routing.Reconfigurator;
import polimi.reds.broker.routing.ReplyManager;
import polimi.reds.broker.routing.ReplyTable;
import polimi.reds.broker.routing.Router;
import polimi.reds.broker.routing.RoutingStrategy;
import polimi.reds.broker.routing.SubscriptionForwardingRoutingStrategy;
import polimi.reds.broker.routing.SubscriptionTable;

/**
 * Simple example of a broker with two <code>TCPTransport</code>.
 * 
 * @author Alessandro Monguzzi
 * 
 */
public class MultipleTransportBroker {

	protected Overlay overlay = null;
	protected TopologyManager topologyManager = null;
	protected Router router = null;
	protected RoutingStrategy routingStrategy = null;
	protected SubscriptionTable subscriptionTable = null;
	protected Reconfigurator reconfigurator = null;
	protected ReplyManager replyManager = null;
	protected ReplyTable replyTable = null;

	public MultipleTransportBroker(Set transports) {
		topologyManager = new SimpleTopologyManager();
		overlay = new GenericOverlay(topologyManager, transports);
		topologyManager.setOverlay((GenericOverlay) overlay);

		router = new GenericRouter(overlay);
		routingStrategy = new SubscriptionForwardingRoutingStrategy();
		subscriptionTable = new GenericTable();
		reconfigurator = new DeferredUnsubscriptionReconfigurator();
		replyManager = new ImmediateForwardReplyManager();
		replyTable = new HashReplyTable();

		router.setRoutingStrategy(routingStrategy);
		router.setSubscriptionTable(subscriptionTable);
		router.setReplyManager(replyManager);
		router.setReplyTable(replyTable);
		replyManager.setOverlay(overlay);
		routingStrategy.setOverlay(overlay);
		reconfigurator.setOverlay(overlay);
		replyManager.setReplyTable(replyTable);
		reconfigurator.setRouter(router);
	}

	public Overlay getOverlay() {
		return overlay;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length == 0) {
			System.err.println("USAGE: java polimi.reds.test.MultipleTransportBroker <trasport1port> <transport2port>");
			System.exit(0);
		}
		Transport tcp1 = new TCPTransport(Integer.parseInt(args[0]));
		Transport tcp2 = new TCPTransport(Integer.parseInt(args[1]));
		Set transports = new LinkedHashSet();
		transports.add(tcp1);
		// transports.add(tcp2);
		MultipleTransportBroker broker = new MultipleTransportBroker(transports);
		broker.getOverlay().start();
		broker.overlay.addTransport(tcp2);
	}
}
