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

package polimi.reds.context.routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import polimi.reds.NodeDescriptor;

/**
 * A simple context table using a map to store data
 * 
 */
public class GenericContextTable implements ContextTable {

	private static final int FIELD_RECEIVED = 0;

	private static final int FIELD_COMPUTED = 1;

	private static final int FIELD_TOSEND = 2;

	private static final int FIELD_SENT = 3;

	private HashMap<NodeDescriptor, ContextSet[]> table;

	private ContextSetSimplifier contextSetSimplifier;

	public GenericContextTable() {
		table = new HashMap<NodeDescriptor, ContextSet[]>();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see polimi.reds.context.routing.ContextTable#removeNeighbor(polimi.reds.NodeDescriptor)
	 */
	public void removeNeighbor( NodeDescriptor n ) {
		table.remove( n );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see polimi.reds.context.routing.ContextTable#getNodes()
	 */
	public Collection<NodeDescriptor> getNodes() {
		return new ArrayList<NodeDescriptor>( table.keySet() );
	}

	private void putField( NodeDescriptor n, ContextSet contextSet, int field ) {
		if ( !table.containsKey( n ) ) {
			table.put( n, new ContextSet[4] );
		}
		ContextSet[] contextSets = table.get( n );
		contextSets[field] = contextSet;
	}

	private ContextSet getField( NodeDescriptor n, int field ) {
		if ( !table.containsKey( n ) ) {
			return null;
		}
		ContextSet[] contextSets = table.get( n );
		return contextSets[field];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see polimi.reds.context.routing.ContextTable#putContextReceived(polimi.reds.NodeDescriptor,
	 *      polimi.reds.context.definition.ContextSet)
	 */
	public void putContextReceived( NodeDescriptor n, ContextSet contextSet ) {
		putField( n, contextSet, FIELD_RECEIVED );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see polimi.reds.context.routing.ContextTable#putContextToSend(polimi.reds.NodeDescriptor,
	 *      polimi.reds.context.definition.ContextSet)
	 */
	public void putContextToSend( NodeDescriptor n, ContextSet contextSet ) {
		putField( n, contextSet, FIELD_TOSEND );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see polimi.reds.context.routing.ContextTable#putContextSent(polimi.reds.NodeDescriptor,
	 *      polimi.reds.context.definition.ContextSet)
	 */
	public void putContextSent( NodeDescriptor n, ContextSet contextSet ) {
		putField( n, contextSet, FIELD_SENT );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see polimi.reds.context.routing.ContextTable#putContextComputed(polimi.reds.NodeDescriptor,
	 *      polimi.reds.context.definition.ContextSet)
	 */
	public void putContextComputed( NodeDescriptor n, ContextSet contextSet ) {
		putField( n, contextSet, FIELD_COMPUTED );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see polimi.reds.context.routing.ContextTable#getContextReceived(polimi.reds.NodeDescriptor)
	 */
	public ContextSet getContextReceived( NodeDescriptor n ) {
		return getField( n, FIELD_RECEIVED );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see polimi.reds.context.routing.ContextTable#getContextToSend(polimi.reds.NodeDescriptor)
	 */
	public ContextSet getContextToSend( NodeDescriptor n ) {
		return getField( n, FIELD_TOSEND );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see polimi.reds.context.routing.ContextTable#getContextSent(polimi.reds.NodeDescriptor)
	 */
	public ContextSet getContextSent( NodeDescriptor n ) {
		return getField( n, FIELD_SENT );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see polimi.reds.context.routing.ContextTable#getContextComputed(polimi.reds.NodeDescriptor)
	 */
	public ContextSet getContextComputed( NodeDescriptor n ) {
		return getField( n, FIELD_COMPUTED );
	}

	public void setContextSetSimplifier( ContextSetSimplifier contextSetSimplifier ) {
		this.contextSetSimplifier = contextSetSimplifier;
	}

	public void createViewAndUpdateTable( NodeDescriptor destinationNode ) {

		ContextSet viewForNode = new ContextSet();
		for ( NodeDescriptor n : this.getNodes() ) {
			if ( !n.equals( destinationNode ) ) {
				viewForNode.addAll( this.getContextReceived( n ) );
				this.putContextComputed( destinationNode, viewForNode );
			}
		}
	}

	public ContextSet simplifyView( NodeDescriptor destinationNode ) {
		return contextSetSimplifier.simplify( this.getContextComputed( destinationNode ) );
	}

	public void simplifyViewAndUpdateTable( NodeDescriptor destinationNode ) {
		ContextSet simplified = this.simplifyView( destinationNode );
		this.putContextToSend( destinationNode, simplified );
	}

}
