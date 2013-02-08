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

package polimi.reds.broker.overlay;

import java.io.Serializable;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import polimi.reds.NodeDescriptor;
import polimi.reds.NotConnectedException;
import polimi.reds.broker.routing.Router;

/**
 * Basic overlay. This class delegates to <code>Transport</code> and
 * <code>TopologyManager</code> components the behavior of the overlay.<br>
 * Subclasses can specify the specific behavior of some or all methods on the
 * basis of their needs.
 * 
 * @author Alessandro Monguzzi
 * 
 */
public class GenericOverlay implements Overlay {
	/**
	 * The <code>Set</code> of <code>Transport</code> used by this
	 * <code>Overlay</code>.
	 */
	protected Set transport;

	/**
	 * The topology manager used by this overlay.
	 */
	protected TopologyManager topologyManager;

	/**
	 * The local node identificator.
	 */
	protected NodeDescriptor id;

	/**
	 * The listeners registered to this <code>Overlay</code> organized as
	 * subject-listeners' <code>Collection</code>.
	 */
	protected Map listeners;

	/**
	 * The <code>Transport</code> used to connect the
	 * <code>LocalDispatchingService</code>s.
	 */
	protected LocalTransport localTransport = null;

	/**
	 * Create a new <code>Overlay</code> which uses the given
	 * <code>TopologyManager</code> and set of <code>Transport</code>.<br>
	 * It creates the <code>NodeDescriptor</code> for the local node and
	 * assign it to the <code>TopologyManager</code> and to the<code>Transport</code>.<br>
	 * Set to itself the <code>TopologyManager</code>'s <code>Overlay</code>
	 * attribute.
	 * 
	 * @param topologyManager
	 *            the <code>TopologyManager</code>
	 * @param transport
	 *            the set of <code>Transport</code>
	 */
	public GenericOverlay(TopologyManager topologyManager, Set transport) {
		this.topologyManager = topologyManager;
		this.transport = transport;
		listeners = Collections.synchronizedMap(new LinkedHashMap());
		this.id = new NodeDescriptor();
		Iterator it = transport.iterator();
		while (it.hasNext()) {
			Transport next = (Transport) it.next();
			next.setNodeDescriptor(id);
			this.id.addUrl(next.getURL());
		}
		this.topologyManager.setOverlay(this);
	}

	/**
	 * Get an instance of <code>LocalTransport</code>. If the
	 * <code>localTransport</code> does not exist, it creates it, add it to
	 * the <code>Transport</code>'s <code>Set</code> and returns it; else
	 * it simply returns it.
	 * 
	 * @return the <code>LocalTransport</code>
	 */
	public LocalTransport getLocalTransport(){
		if(localTransport == null){
			localTransport = new LocalTransport();
			addTransport(localTransport);
			localTransport.start();
		}
		return localTransport;
	}
	/**
	 * @see Overlay#send(String, Serializable, NodeDescriptor)
	 */
	public void send(String subject, Serializable payload,
			NodeDescriptor receiver) throws NotConnectedException {
		send(subject, payload, receiver, getTrafficClass(subject));
	}
	/**
	 * @see Overlay#send(String, Serializable, NodeDescriptor, String)
	 */
	public void send(String subject, Serializable payload,
			NodeDescriptor receiver, String trafficClass)throws NotConnectedException {
		Transport t = topologyManager.getTransport(receiver);
		if (t != null)
			t.send(subject, payload, receiver, trafficClass);
		else
			throw new NotConnectedException();	
	}
	/**
	 * Get the traffic class associated with the given <code>subject</code>.<br>
	 * If there is no standard association, the <code>MISCELLANEOUS_CLASS</code> is selected.
	 * @param subject the message subject
	 * @return the traffic class associated with the given subject
	 */
	private String getTrafficClass(String subject){
		if(subject.equals(Router.PUBLISH))
			return Transport.MESSAGE_CLASS;
		else if (subject.equals(Router.REPLY))
			return Transport.REPLY_CLASS;
		else if (subject.equals(Router.SUBSCRIBE) || subject.equals(Router.UNSUBSCRIBE) || subject.equals(Router.UNSUBSCRIBEALL))
			return Transport.FILTER_CLASS;
		else 
			return Transport.MISCELLANEOUS_CLASS;
	}
	/**
	 * @see Overlay#addPacketListener(PacketListener, String)
	 */
	public void addPacketListener(PacketListener listener, String subject) {
		Collection list = (Collection) listeners.get(subject);
		if (list == null) {
			list = new Vector();
			listeners.put(subject, list);
		}
		list.add(listener);
		synchronized (transport) {
			Iterator it = transport.iterator();
			while (it.hasNext())
				((Transport) it.next()).addPacketListener(listener, subject);
		}
	}
	/**
	 * @see Overlay#removePacketListener(PacketListener, String)
	 */
	public void removePacketListener(PacketListener listener, String subject){
		Collection list = (Collection) listeners.get(subject);
		if(list != null){
			list.remove(listener);
			synchronized (transport) {
				Iterator it = transport.iterator();
				while(it.hasNext())
					((Transport) it.next()).removePacketListener(listener, subject);
			}
		}
	}
	/**
	 * @see Overlay#removePacketListener(PacketListener)
	 */
	public void removePacketListener(PacketListener listener){
		Set subjects = listeners.keySet();
		Iterator it = subjects.iterator();
		while(it.hasNext()){
			String next = (String) it.next();
			removePacketListener(listener, next);
		}
	}
	/**
	 * @see Overlay#addNeighborAddedListener(NeighborAddedListener)
	 */
	public void addNeighborAddedListener(NeighborAddedListener listener) {
		topologyManager.addNeighborAddedListener(listener);
	}
	/**
	 * @see Overlay#removeNeighborAddedListener(NeighborAddedListener)
	 * @param listener the listener
	 */
	public void removeNeighborAddedListener(NeighborAddedListener listener){
		topologyManager.removeNeighborAddedListener(listener);
	}
	/**
	 * @see Overlay#addNeighborRemovedListener(NeighborRemovedListener)
	 */
	public void addNeighborRemovedListener(NeighborRemovedListener listener) {
		topologyManager.addNeighborRemovedListener(listener);
	}
	/**
	 * @see Overlay#removeNeighborRemovedListener(NeighborRemovedListener)
	 */
	public void removeNeighborRemovedListener(NeighborRemovedListener listener){
		topologyManager.removeNeighborRemovedListener(listener);
	}
	/**
	 * @see Overlay#removeNeighbor(NodeDescriptor)
	 */
	public void removeNeighbor(NodeDescriptor removedNeighbor) {
		topologyManager.removeNeighbor(removedNeighbor);
	}

	/**
	 * @see Overlay#getNeighbors()
	 */
	public Set getNeighbors() {
		return topologyManager.getNeighbors();
	}

	/**
	 * @see Overlay#hasNeighbor(NodeDescriptor)
	 */
	public boolean hasNeighbor(NodeDescriptor neighbor) {
		return topologyManager.contains(neighbor);
	}

	/**
	 * @see Overlay#getAllNeighborsExcept(NodeDescriptor)
	 */
	public List getAllNeighborsExcept(NodeDescriptor excludedNeighbor) {
		return topologyManager.getAllNeighborsExcept(excludedNeighbor);
	}

	/**
	 * @see Overlay#size()
	 */
	public int size() {
		return topologyManager.size();
	}

	/**
	 * @see Overlay#numberOfBrokers()
	 */
	public int numberOfBrokers() {
		return topologyManager.numberOfBrokers();
	}

	/**
	 * @see Overlay#numberOfClients()
	 */
	public int numberOfClients() {
		return topologyManager.numberOfClient();
	}

	/**
	 * @see Overlay#start()
	 */
	public void start() {
		synchronized (transport) {
			Iterator it = transport.iterator();
			while (it.hasNext())
				((Transport) it.next()).start();
		}
		topologyManager.start();
	}

	/**
	 * @see Overlay#stop()
	 */
	public void stop() {
		synchronized (transport) {
			Iterator it = transport.iterator();
			((Transport) it.next()).stop();
		}
		topologyManager.stop();
	}

	/**
	 * @see Overlay#addNeighborDeadListener(NeighborDeadListener)
	 */
	public void addNeighborDeadListener(NeighborDeadListener listener) {
		topologyManager.addNeighborDeadListener(listener);
	}
	public void removeNeighborDeadListener(NeighborDeadListener listener){
		topologyManager.removeNeighborDeadListener(listener);
	}
	/**
	 * @see Overlay#addNeighbor(String)
	 */
	public NodeDescriptor addNeighbor(String url)
			throws AlreadyAddedNeighborException, ConnectException,
			MalformedURLException {
		return topologyManager.addNeighbor(url);
	}

	/**
	 * @see Overlay#getID()
	 */
	public NodeDescriptor getID() {
		return id;
	}

	/**
	 * @see Overlay#getURLs()
	 */
	public String[] getURLs() {
		String[] urls = null;
		synchronized (transport) {
			urls = new String[transport.size()];
			Iterator it = transport.iterator();
			int i = 0;
			while (it.hasNext()) {
				urls[i] = ((Transport) it.next()).getURL();
				i++;
			}
		}
		return urls;
	}

	/**
	 * @see Overlay#addTransport(Transport)
	 */
	public void addTransport(Transport t) {
		t.setNodeDescriptor(id);
		// add to the transport all the packet listeners
		Set list = listeners.entrySet();
		synchronized (listeners) {
			Iterator it = list.iterator();
			while (it.hasNext()) {
				Map.Entry next = (Map.Entry) it.next();
				String subject = (String) next.getKey();
				Collection listColl = (Collection) next.getValue();
				Iterator collIt = listColl.iterator();
				while (collIt.hasNext()) {
					t.addPacketListener((PacketListener) collIt.next(),	subject);
				}
			}
		}
		// register the topology manager listeners to the transport
		t.addLinkClosedListener(this.topologyManager);
		t.addLinkDeadListener(this.topologyManager);
		t.addLinkOpenedListener(this.topologyManager);
		transport.add(t);
		this.id.addUrl(t.getURL());
	}

	/**
	 * @see Overlay#removeTransport(Transport)
	 */
	public void removeTransport(Transport t) {
		Collection nodes = topologyManager.getNeighbors(t);/*DAVIDE: removed , true);*/
		Iterator it = nodes.iterator();
		while (it.hasNext()) {
			removeNeighbor((NodeDescriptor) it.next());
		}
		t.stop();
		transport.remove(t);
		this.id.removeUrl(t.getURL());
	}

	/**
	 * Get in a non deterministic way a <code>Transport</code> that use the
	 * protocol specified in the given URL.<br>
	 * If no <code>Transport</code> supports the given protocol, it returns
	 * <code>null</code>.
	 * 
	 * @param url the url
	 * @return the <code>Transport</code> or <code>null</code>
	 */
	public Transport resolveUrl(String url) {
		String[] urlFragment = url.split(Transport.URL_SEPARATOR);
		synchronized (transport) {
			Iterator it = transport.iterator();
			while(it.hasNext()){
				Transport next = (Transport) it.next();
				String[] transportUrl = ((Transport) next).getURL().split(
						Transport.URL_SEPARATOR);
				if (transportUrl[0].equals(urlFragment[0]))
					return next;
			}
		}
		return null;
	}

	/**
	 * Get all the <code>Transport</code>s used by this <code>Overlay</code>.
	 * 
	 * @return a <code>Collection</code> of <code>Transport</code>
	 */
	public Collection getAllTransport() {
		return this.transport;
	}

	public TopologyManager getTopologyManager() {
		return topologyManager;
	}
}