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

package polimi.reds;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This class is the unique descriptor of a node. It stores the unique identificator of a node and an information about its type:
 * client or broker.
 * @author Alessandro Monguzzi
 *
 */
public class NodeDescriptor implements Serializable, Comparable {	
	private static final long serialVersionUID = 1753439889121544843L;
	
	private static final String ID_SEPARATOR = "_";
	
	private static int serial = Integer.MIN_VALUE;
	/**
	 * The <code>String</code> representing the node's unique id. 
	 */
	protected String id =  null;
	/**
	 * <code>true</code> iff the node is a broker.
	 */
	protected boolean broker = false;
	/**
	 * The node urls.
	 */
	protected List urls = null;
	/**
	 * Creates a unique <code>NodeDescriptor</code> using informations about the local network address and the actual time.
	 *
	 */
	public NodeDescriptor(){
		urls = new ArrayList();
		if(serial < Integer.MAX_VALUE-1){
			serial++;
		}else{
			serial = Integer.MIN_VALUE;
		}
		try {
			id = InetAddress.getLocalHost().getHostAddress() + ID_SEPARATOR +Long.toString(System.currentTimeMillis())+ ID_SEPARATOR + serial;
			
	    } catch(UnknownHostException e) {
	    	id = Long.toString(System.currentTimeMillis())+ ID_SEPARATOR + serial;;
	    }
	}
	/**
	 * Add a new node url.<br>
	 * Each url should be present only once.
	 * @param url the new url
	 */
	public void addUrl(String url){
		urls.add(url);
	}
	/**
	 * Remove a given url.
	 * @param url the removed url.
	 */
	public void removeUrl(String url){
		urls.remove(url);
	}
	/**
	 * Get the node urls.<br>
	 * <p>
	 * Note: these urls represents the <code>Transport</code>s present on the node <code>Overlay</code> in a specific instant.
	 * If this object is serialized and sent to another node, no attempt is made to mantain it consistent. (i.e: if the transport's set
	 * change, this information may become partially incomplete or false). 
	 * </p>
	 * @return an array of <code>String</code> containing the urls.
	 */
	public String[] getUrls(){
		String[] array = new String[urls.size()];
		synchronized (urls) {
			Iterator it = urls.iterator();
			int i = 0;
			while(it.hasNext()){
				array[i] = (String)it.next();
				i++;
			}
		}
		return array;
	}
	/**
	 * Get the unique id.
	 * @return a <code>String</code> representing the unique identificator of the node.
	 */
	public String getID(){
		return id;
	}
	/**
	 * Get the hashCode of the local node. It is based on the hashCode of the unique id.
	 */
	public int hashCode(){
		return id.hashCode();
	}
	
	/**
	 * Two <code>NodeDescriptor</code>s are equal if their id are equal.
	 */
	public boolean equals(Object o){
		if(o instanceof NodeDescriptor){
			return ((NodeDescriptor)o).getID().equals(id);
		}
		return false;
		
	}

    public int compareTo(Object o) {
        return id.compareTo(((NodeDescriptor)o).id);
    }
	/**
	 * Check whether the node is a broker.
	 * @return <code>true</code> iff the local node is a broker
	 */
	public boolean isBroker() {
		return broker;
	}
	/**
	 * Mark this node as broker.
	 *
	 */
	public void setBroker(){
		broker = true;
	}
	/**
	 * Get the string representation of the node id.
	 */
	public String toString(){
	    return "ID: "
                + id.toString()
                + (broker ? " (broker) " + " urls: " + urls.toString()
                        : " (client)");
	}
}