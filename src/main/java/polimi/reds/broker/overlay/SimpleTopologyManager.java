package polimi.reds.broker.overlay;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.logging.Logger;

/**
 * This is a simple implementation of the <code>TopologyManager</code> that does
 * not make any attempt of reconfiguration. Its behaviour is the same as the one
 * of the <code>AbstractTopologyManager</code>.
 * 
 * @author Alessandro Monguzzi
 * 
 */
public class SimpleTopologyManager extends AbstractTopologyManager {
	/**
	 * Create a new <code>SimpleTopologyManager</code>.
	 */
	public SimpleTopologyManager() {
		super.neighborTransport = Collections.synchronizedMap(new HashMap());
		// super.tempNeighbors = Collections.synchronizedMap(new HashMap());
		super.neighborAddedListeners = Collections.synchronizedList(new LinkedList());
		super.neighborRemovedListeners = Collections.synchronizedList(new LinkedList());
		super.neighborDeadListeners = Collections.synchronizedList(new LinkedList());
		logger = Logger.getLogger("polimi.reds.overlay.SimpleTopologyManager");
	}
}
