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

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import polimi.reds.Filter;
import polimi.reds.Message;
import polimi.reds.TextMessage;

/**
 * This class can be used for parsing the files given as output by ManetClient
 * at the end of a test run. The results of the global analysis are written in
 * the indicated output file, while the dynamic statistics regarding any node in
 * the system associated with a core subscriber are written out in different
 * files carrying the identifiers of those two nodes.
 */
public class ManetSimParser {
  public ManetSimParser(String dataPath, String outFile) {
    // The information related to the published events at non-core nodes are
    // kept in an object of type NodeData.
    NodeData node;
    // The information related to published events, received notifications and
    // subscriptions at core nodes are kept in an object of type CoreNodeData.
    CoreNodeData cNode;
    // A list containing information related to non-core nodes.
    LinkedList nodes = new LinkedList();
    // A list containing information related to core nodes.
    LinkedList coreNodes = new LinkedList();
    // Reads the content of the dataPath directory.
    String[] fileNames = new File(dataPath).list();
    for(int i = 0; i<fileNames.length; i++) {
      // Loading subscriptions and received events for core nodes...
      if(fileNames[i].startsWith("SUBSCRIPTIONS.")) {
        cNode = new CoreNodeData(fileNames[i].substring(fileNames[i]
            .indexOf(".")+1));
        loadSubscriptions(cNode, dataPath.concat(fileNames[i]));
        loadReceivedMessages(cNode, dataPath.concat(fileNames[i].replaceFirst(
            "SUBSCRIPTIONS.", "NOTIFICATIONS.")));
        coreNodes.add(cNode);
      }
      // Loading published events for all nodes...
      if(fileNames[i].startsWith("EVENTS.")) {
        node = new NodeData(fileNames[i].substring(fileNames[i].indexOf(".")+1));
        loadPublishedMessages(node, dataPath.concat(fileNames[i]));
        nodes.add(node);
      }
    }
    // Gathering statistics...
    int tickTotal = 0;
    int tickMissed = 0;
    NodeData publisherNode;
    LinkedList testEvents;
    LinkedList counterEvents;
    TextMessage tMsg;
    ListIterator it;
    ListIterator counterIt;
    ListIterator itCore = coreNodes.listIterator();
    // A list containing already preocessed nodes.
    LinkedList processedNodes = new LinkedList();
    // Iterating on core nodes...
    while(itCore.hasNext()) {
      cNode = (CoreNodeData) itCore.next();
      TextMessage ctrlEvent = cNode.getNextTxtCtrlMessage();
      // Parsing a CONTROL with respect to the snder node.
      while(ctrlEvent!=null) {
        String nodeId = ctrlEvent.getData().substring(
            ctrlEvent.getData().indexOf("-")+1,
            ctrlEvent.getData().lastIndexOf("-"));
        if(!processedNodes.contains(nodeId)) {
          // Computing averages values...
          System.out.print("Computing average values for "+cNode.getNodeId()
              +"...");
          processedNodes.add(nodeId);
          publisherNode = retrieveNodeData(nodeId, nodes);
          testEvents = publisherNode.getTxtEventsAfter(ctrlEvent);
          it = testEvents.listIterator();
          // Comparing published events with received notifications.
          while(it.hasNext()) {
            tMsg = (TextMessage) it.next();
            // If the event macthes a core's susbcriptions and was not notified, then it is a missed event.
            if(cNode.matches(tMsg)&&(!cNode.isTxtNotified(tMsg)))
                cNode.addMissedMessage(tMsg);
          }
          System.out.println("done!");
          // Computing dynamic values...
          System.out.print("Computing dynamic values for "+cNode.getNodeId()
              +"...");
          int timeCounter = Integer.parseInt(ctrlEvent.getData().substring(
              ctrlEvent.getData().lastIndexOf("-")+1));
          // Retrieving the number of pubslihed events starting from the given CONTROL event.
          counterEvents = publisherNode.getTxtEventsJunkFrom(timeCounter);
          do {
            tickTotal = 0;
            tickMissed = 0;
            counterIt = counterEvents.listIterator();
            while(counterIt.hasNext()) {
              tMsg = (TextMessage) counterIt.next();
              // Computing the number of published events matching a core's subscriptions in this time tick.
              if(cNode.matches(tMsg)) tickTotal++;
              // Computing the number of missed events in this time tick.
              if(cNode.matches(tMsg)&&(!cNode.isTxtNotified(tMsg)))
                  tickMissed++;
            }
            // Computing percentages.
            cNode.addPercentageNodeCounter(nodeId, timeCounter,
                ((double) (tickTotal-tickMissed)/tickTotal)*100);
            timeCounter += counterEvents.size()+1;
            counterEvents = publisherNode.getTxtEventsJunkFrom(timeCounter);
          } while(counterEvents.size()!=0);
          System.out.println("done!");
        }
        ctrlEvent = cNode.getNextTxtCtrlMessage();
      }
      processedNodes.clear();
    }
    // Computing totals and average percentages...
    int[] deliveredMsg = new int[coreNodes.size()];
    int[] missedMsg = new int[coreNodes.size()];
    int[] totalMsg = new int[coreNodes.size()];
    double[] deliveryPercentage = new double[coreNodes.size()];
    double deliveryPercentageAvg = 0;
    int j = 0;
    it = coreNodes.listIterator();
    while(it.hasNext()) {
      cNode = (CoreNodeData) it.next();
      deliveredMsg[j] = cNode.getNotifiedEvents().size();
      missedMsg[j] = cNode.getMissedEvents().size();
      totalMsg[j] = deliveredMsg[j]+missedMsg[j];
      deliveryPercentage[j] = ((double) deliveredMsg[j]/totalMsg[j])*100;
      j++;
    }
    for(int i = 0; i<deliveryPercentage.length; i++) {
      deliveryPercentageAvg += deliveryPercentage[i];
    }
    deliveryPercentageAvg = deliveryPercentageAvg/coreNodes.size();
    // Writing and displaying average results...
    PrintWriter dos = null;
    try {
      dos = new PrintWriter(new FileOutputStream(outFile.concat(".overall")));
    } catch(FileNotFoundException e) {
      e.printStackTrace();
    }
    dos.println("Node\tRecMsg\tMissMsg\tDeliv%");
    for(int i = 0; i<coreNodes.size(); i++) {
      dos.println(i+"\t"+deliveredMsg[i]+"\t"+missedMsg[i]+"\t"
          +deliveryPercentage[i]);
    }
    System.out.println("\nAverage delivery ratio: "+deliveryPercentageAvg);
    dos.println("\nAverage delivery ratio: "+deliveryPercentageAvg);
    dos.close();
    // Writing dynamic percentages.
    System.out.println("Writing output data...");
    HashMap counterPercentages;
    ListIterator itTime;
    Integer time;
    ListIterator itNodes = nodes.listIterator();
    while(itNodes.hasNext()) {
      node = (NodeData) itNodes.next();
      itCore = coreNodes.listIterator();
      while(itCore.hasNext()) {
        cNode = (CoreNodeData) itCore.next();
        if(!cNode.getNodeId().equals(node.getNodeId())) {
          try {
            dos = new PrintWriter(new FileOutputStream(outFile.concat("."
                +cNode.getNodeId()+"-"+node.getNodeId())));
          } catch(FileNotFoundException e1) {
            e1.printStackTrace();
          }
          dos.println("Time\tDeliv%");
          counterPercentages = cNode.getPercentageNode(node.getNodeId());
          if(counterPercentages!=null) {
            itTime = new LinkedList(counterPercentages.keySet()).listIterator();
            while(itTime.hasNext()) {
              time = (Integer) itTime.next();
              dos.println(time+"\t"+counterPercentages.get(time));
            }
          }
          dos.close();
        }
      }
    }
  }

  /**
   * Retrieves the information related to a given node from a list composed of NodeData objects.
   * 
   * @param nodeId the id of the node we are looking for.
   * @param nodes the list containig the information on nodes.
   * @return a NodeData object carrying the requested information.
   */
  private NodeData retrieveNodeData(String nodeId, LinkedList nodes) {
    NodeData nodeData;
    ListIterator it = nodes.listIterator();
    while(it.hasNext()) {
      nodeData = (NodeData) it.next();
      if(nodeData.getNodeId().equals(nodeId)) return nodeData;
    }
    return null;
  }

  /**
   * Loads from a given filename the list of published events and stores it in a NodeData object.
   * 
   * @param node the object where to store the read information.
   * @param filename the name of the file to read.
   */
  private void loadPublishedMessages(NodeData node, String filename) {
    try {
      ObjectInputStream ois = new ObjectInputStream(new FileInputStream(
          filename));
      Message msg;
      do {
        msg = (Message) ois.readObject();
        node.addPublishedEvent(msg);
      } while(msg!=null);
      ois.close();
    } catch(FileNotFoundException e) {
      e.printStackTrace();
    } catch(IOException e1) {
      if(e1 instanceof EOFException) return;
      e1.printStackTrace();
    } catch(ClassNotFoundException e1) {
      e1.printStackTrace();
    }
  }

  /**
   * Loads from a given filename the list of subscriptions at a core node and stores it in a CoreNodeData object.
   * 
   * @param cNode the object where to store the read information.
   * @param filename the name of the file to read.
   */
  private void loadSubscriptions(CoreNodeData cNode, String filename) {
    try {
      ObjectInputStream ois = new ObjectInputStream(new FileInputStream(
          filename));
      Filter flt;
      do {
        flt = (Filter) ois.readObject();
        cNode.addSubscription(flt);
      } while(flt!=null);
      ois.close();
    } catch(FileNotFoundException e) {
      e.printStackTrace();
    } catch(IOException e1) {
      if(e1 instanceof EOFException) return;
      e1.printStackTrace();
    } catch(ClassNotFoundException e1) {
      e1.printStackTrace();
    }
  }

  /**
   * Loads from a given filename the list of received notifications and stores it in a CoreNodeData object.
   * 
   * @param cNode the object where to store the read information.
   * @param filename the name of the file to read.
   */
  private void loadReceivedMessages(CoreNodeData cNode, String filename) {
    try {
      ObjectInputStream ois = new ObjectInputStream(new FileInputStream(
          filename));
      Message msg;
      do {
        msg = (Message) ois.readObject();
        cNode.addMessageNotify(msg);
      } while(msg!=null);
      ois.close();
    } catch(FileNotFoundException e) {
      e.printStackTrace();
    } catch(IOException e1) {
      if(e1 instanceof EOFException) return;
      e1.printStackTrace();
    } catch(ClassNotFoundException e1) {
      e1.printStackTrace();
    }
  }

  private static void printUsageAndExit() {
    System.err
        .println("USAGE: java polimi.reds.examples.ManetSymAnalyzer <dataDirPath> <outputFile>");
    System.exit(-1);
  }

  public static void main(String[] args) {
    if(args.length<2) printUsageAndExit();
    new ManetSimParser(args[0], args[1]);
  }
}

/**
 * This class serves as a repository for data related to core brokers.
 */
class CoreNodeData {
  private String nodeId;
  private LinkedList subscriptions;
  private LinkedList notifiedEvents;
  private LinkedList missedEvents;
  private int counter;
  private HashMap nodeToData;

  public CoreNodeData(String nodeId) {
    this.nodeId = nodeId;
    subscriptions = new LinkedList();
    notifiedEvents = new LinkedList();
    missedEvents = new LinkedList();
    nodeToData = new HashMap();
    counter = 0;
  }

  public void addSubscription(Filter flt) {
    subscriptions.add(flt);
  }

  public void addMessageNotify(Message msg) {
    notifiedEvents.add(msg);
  }

  public void addMissedMessage(Message msg) {
    missedEvents.add(msg);
  }

  /**
   * This method resturns the next CONTROL message notified at this core broker.
   * 
   * @return a message carrying the next CONTROL message notified.
   */
  public TextMessage getNextTxtCtrlMessage() {
    TextMessage msg;
    while(counter<notifiedEvents.size()) {
      msg = (TextMessage) notifiedEvents.get(counter++);
      if(msg.getData().indexOf("CONTROL")!=-1) { return msg; }
    }
    return null;
  }

  /**
   * This methods adds an item ragrding dynamic statistics between this core node and any other node in the system.
   * @param nodeId the id of the publisher node we are referring to.
   * @param counter the time counter.
   * @param percentage the percentage of delivered messages in the time tick starting from <code>counter</code>. 
   */
  public void addPercentageNodeCounter(String nodeId, int counter,
      double percentage) {
    HashMap data;
    if(!nodeToData.containsKey(nodeId)) {
      nodeToData.put(nodeId, new HashMap());
    }
    data = (HashMap) nodeToData.get(nodeId);
    data.put(new Integer(counter), new Double(percentage));
  }

  public HashMap getPercentageNode(String nodeId) {
    return (HashMap) nodeToData.get(nodeId);
  }

  /**
   * This methods checks whether this broker's subscription matches a given event.
   * 
   * @param msg a message published from some other node in the system.
   * @return TRUE if this broker's subscription table matches <code>msg</code>, FALSE otherwise.
   */
  public boolean matches(Message msg) {
    Filter flt;
    ListIterator it = subscriptions.listIterator();
    while(it.hasNext()) {
      flt = (Filter) it.next();
      if(flt.matches(msg)) return true;
    }
    return false;
  }

  public LinkedList getMissedEvents() {
    return missedEvents;
  }

  public LinkedList getNotifiedEvents() {
    return notifiedEvents;
  }

  /**
   * This method checks whether this broker has been notified about a given message.
   * 
   * @param msg a message published from some other node in the system.
   * @return TRUE if this broker has been notified about <code>msg</code>, FALSE otherwise.
   */
  public boolean isTxtNotified(TextMessage msg) {
    ListIterator it = notifiedEvents.listIterator();
    while(it.hasNext()) {
      if(((TextMessage) it.next()).getData().equals(msg.getData()))
          return true;
    }
    return false;
  }

  public String getNodeId() {
    return nodeId;
  }
}
/**
 * This class serves as a repository for data related to non-core brokers.
 */
class NodeData {
  private String nodeId;
  private LinkedList publishedEvents;

  public NodeData(String nodeId) {
    this.nodeId = nodeId;
    publishedEvents = new LinkedList();
  }

  public String getNodeId() {
    return nodeId;
  }

  public void addPublishedEvent(Message msg) {
    publishedEvents.add(msg);
  }

  /**
   * This method returns the messages publsihed from this broker after a given message,
   * it is actually invoked passing a CONTROL event as parameter.
   * 
   * @param ctrlMsg a given event published from this broker. 
   * @return a list containing the messages published after <code>ctrlMsg</code>.
   */
  public LinkedList getTxtEventsAfter(TextMessage ctrlMsg) {
    TextMessage msg;
    LinkedList res = new LinkedList();
    ListIterator it = publishedEvents.listIterator();
    while(it.hasNext()) {
      if(((TextMessage) it.next()).getData().equals(ctrlMsg.getData())) break;
    }
    while(it.hasNext()) {
      msg = (TextMessage) it.next();
      if(msg.getData().indexOf("CONTROL")==-1) {
        res.add(msg);
      }
    }
    return res;
  }

  /**
   * This method returns the messages published from this broker after a specified point in time.
   * This information is needed to compute dynamic statistics.
   * 
   * @param counter a specified the point in time.
   * @return a list containing the messages published after <code>counter</code>.
   */
  public LinkedList getTxtEventsJunkFrom(int counter) {
    TextMessage msg;
    LinkedList res = new LinkedList();
    ListIterator it = publishedEvents.listIterator();
    while(it.hasNext()) {
      msg = (TextMessage) it.next();
      if((Integer.parseInt(msg.getData().substring(
          msg.getData().lastIndexOf("-")+1)))==counter) break;
    }
    while(it.hasNext()) {
      msg = (TextMessage) it.next();
      if(msg.getData().indexOf("CONTROL")==-1) res.add(msg);
      else break;
    }
    return res;
  }
}
