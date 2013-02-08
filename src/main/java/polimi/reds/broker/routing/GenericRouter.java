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

package polimi.reds.broker.routing;

import java.io.Serializable;
import java.util.logging.Logger;
import polimi.reds.Filter;
import polimi.reds.Message;
import polimi.reds.NodeDescriptor;
import polimi.reds.Repliable;
import polimi.reds.Reply;
import polimi.reds.broker.overlay.Overlay;

/**
 * As its name suggests, this class implements a simple <code>Router</code> for a REDS broker. It just delegates all its operations 
 * to the various other components that constitutes the broker.
 */
public class GenericRouter implements Router {
  private NodeDescriptor id; // The ID of the core
  protected RoutingStrategy routingStrategy; // The routingStrategy
  private SubscriptionTable subscriptionTable; // The local subscriptions table
  // processors registered
  protected Logger logger; // The logger
  protected Overlay overlay = null;

  private ReplyManager replyManager; //The reply manager
  private ReplyTable replyTable; //The reply table
  
  public GenericRouter(Overlay o) {
    routingStrategy = null;
    subscriptionTable = null;
    
    replyManager = null;
    replyTable = null;
    overlay = o;
    logger = Logger.getLogger("polimi.reds.Router");
	id = overlay.getID();
	id.setBroker();
    logger.config("GenericRouter created for broker "+id.getID());   
  }

  /**
   * @see Router#subscribe(NodeDescriptor, Filter)
   */
  public synchronized void subscribe(NodeDescriptor neighborID, Filter filter) {
    routingStrategy.subscribe(neighborID, filter);
  }
  /**
   * @see Router#unsubscribe(NodeDescriptor, Filter)
   */
  public synchronized void unsubscribe(NodeDescriptor neighborID, Filter filter) {
    routingStrategy.unsubscribe(neighborID, filter);
  }
  /**
   * @see Router#unsubscribeAll(NodeDescriptor)
   */
  public synchronized void unsubscribeAll(NodeDescriptor neighborID) {
    routingStrategy.unsubscribeAll(neighborID);
  }
  /**
   * @see Router#publish(NodeDescriptor, Message)
   */
  public synchronized void publish(NodeDescriptor neighbor, Message message) {
  	FutureInt numNeighbor = routingStrategy.publish(neighbor, message);
  	if (message instanceof Repliable){
  	  if (replyManager != null)  
    	replyManager.recordRepliableMessage(message.getID(),neighbor, numNeighbor);
  	}
  }

  /**
   * @see Router#setRoutingStrategy(RoutingStrategy)
   */
  public void setRoutingStrategy(RoutingStrategy routingStrategy) {
    if(this.routingStrategy==routingStrategy) return;
    this.routingStrategy = routingStrategy;
    this.routingStrategy.setRouter(this);
    //register listeners for RoutingStrategy
    overlay.addPacketListener(this, Router.PUBLISH);
    overlay.addPacketListener(this, Router.SUBSCRIBE);
    overlay.addPacketListener(this, Router.UNSUBSCRIBE);
    overlay.addPacketListener(this, Router.UNSUBSCRIBEALL);
  }
  
  /**
   * @see Router#setSubscriptionTable(SubscriptionTable)
   */
  public void setSubscriptionTable(SubscriptionTable subscriptionTable) {
    this.subscriptionTable = subscriptionTable;
  }
  /**
   * @see Router#getID()
   */
  public NodeDescriptor getID() {
    return id;
  }
  /**
   * @see Router#getSubscriptionTable()
   */
  public SubscriptionTable getSubscriptionTable() {
    return subscriptionTable;
  }
  /**
   * @see Router#setReplyManager(ReplyManager)
   */
  public void setReplyManager(ReplyManager replyManager){
  	if(this.replyManager == replyManager) return;
  	this.replyManager = replyManager;
  	this.replyManager.setRouter(this);
  	 overlay.addPacketListener(this, Router.REPLY);
  }
  /**
   * @see Router#getReplyManager()
   */
  public ReplyManager getReplyManager(){
  	return replyManager;
  }
  /**
   * @see Router#setReplyTable(ReplyTable)
   */
  public void setReplyTable(ReplyTable replyTable){
  	if(this.replyTable == replyTable) return;
  	this.replyTable = replyTable;
  }
  /**
   * @see Router#getReplyTable()
   */
  public ReplyTable getReplyTable(){
  	return replyTable;
  }
  
  public void setDebugLevel(int debugLevel) {
    
  }
  public int getDebugLevel() {
    return 5;
  }

  /**
   * @see Router#forwardReply(Reply)
   */
  public synchronized void forwardReply(Reply reply) {
  	replyManager.forwardReply(reply);
  }
  /**
   * @see Router#setOverlay(Overlay)
   */
	public void setOverlay(Overlay o) {
		overlay = o;
		
	}
/**
 * @see Router#getOverlay()
 */
public Overlay getOverlay() {
	return overlay;
}

/**
 * @see polimi.reds.broker.overlay.PacketListener#signalPacket(String, NodeDescriptor, Serializable)
 */
public void signalPacket(String subject, NodeDescriptor senderID, Serializable payload) {
	
	if(subject.equals(Router.PUBLISH))
		publish(senderID, (Message)payload);
	else if(subject.equals(Router.REPLY))
		forwardReply((Reply)payload);	
	else if(subject.equals(Router.SUBSCRIBE))
		subscribe(senderID, (Filter)payload);
	else if(subject.equals(Router.UNSUBSCRIBE))
		unsubscribe(senderID, (Filter)payload);
	else if(subject.equals(Router.UNSUBSCRIBEALL))
		unsubscribeAll(senderID);
	else{
		logger.severe("unrecognized message");
	}
}
}