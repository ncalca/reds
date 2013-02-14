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

package polimi.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import polimi.reds.broker.overlay.REDSMarshaller;
import polimi.reds.broker.overlay.REDSUnmarshaller;

/**
 * This class provides methods for sending and receiving objects using UDP
 * datagrams. Objects serialization is carried out using suitable
 * ByteInputStream and ByteOutputStream objects.
 */
public class DatagramObjectIO {
	// The local port we use to receive UDP datagrams.
	private int receivePort;
	// The size of the receiver buffer.
	private int bufferSize;
	// A reference to a datagram socket used for receiving datagrams.
	private DatagramSocket UDPsck;
	// The IPv4 broadcast address.
	private static final String INET_BROADCAST_ADDR = "255.255.255.255";

	/**
	 * Build a new object for sending and receiving objects via UDP datagrams.
	 * 
	 * @param receivePort
	 *            the receivePort at which we are going to receive UPD
	 *            datagrams.
	 * @param bufferSize
	 *            the size of the receiver buffer.
	 * @param timeout
	 *            a timeout for receiving UDP datagrams.
	 */
	public DatagramObjectIO(int port, int bufferSize, int timeout) {
		this.receivePort = port;
		this.bufferSize = bufferSize;
		try {
			UDPsck = new DatagramSocket(port);
			UDPsck.setSoTimeout(timeout);
		} catch (SocketException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/**
	 * Build a new object for sending and receiving objects via UDP datagrams.
	 * 
	 * @param receivePort
	 *            the receivePort at which we are going to receive UPD
	 *            datagrams.
	 * @param bufferSize
	 *            the size of the receiver buffer.
	 * @param timeout
	 *            a timeout for receiving UDP datagrams.
	 * @param reuseAddress
	 *            set the reuseAddress of the <code>DatagramSocket</code>
	 * 
	 * @see DatagramSocket#setReuseAddress(boolean)
	 */
	public DatagramObjectIO(int port, int bufferSize, int timeout, boolean reuseAddress) {
		this(port, bufferSize, timeout);
		try {
			UDPsck.setReuseAddress(reuseAddress);
		} catch (SocketException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/**
	 * Sends an object to the Ipv4 local broadcast address. The receiver host
	 * has to wait for incoming objects at the same receivePort as the local
	 * DatagramObjectIO object.
	 * 
	 * @param obj
	 *            the object that gets sent.
	 */
	public void sendBroadcastObject(Serializable obj, int port) {
		sendObject(obj, INET_BROADCAST_ADDR, port);
	}

	/**
	 * Send the object o to the the specified Ipv4 address. The receiver host
	 * has to wait for incoming objects at the same receivePort as the local
	 * DatagramObjectIO object.
	 * 
	 * @param obj
	 *            the objects that gets sent.
	 * @param hostAddress
	 *            the IPv4 address of the intended receiver.
	 */
	public void sendObject(Serializable obj, String hostAddress, int port) {
		try {
			ByteArrayOutputStream byteStreamO = new ByteArrayOutputStream(bufferSize);
			REDSMarshaller marshaller = new REDSMarshaller(byteStreamO);
			marshaller.writeObject(obj);
			marshaller.flush();
			marshaller.reset();
			// Retrieves byte array.
			byte[] sendBuf = byteStreamO.toByteArray();
			// if(!hostAddress.equals(INET_BROADCAST_ADDR)) {
			// Sending the serialized object.
			DatagramPacket packet = new DatagramPacket(sendBuf, sendBuf.length, InetAddress.getByName(hostAddress),
					port);
			new DatagramSocket().send(packet);
			/*
			 * } else { // Sending the (serialized) object (for testing w/
			 * UserModeLinux) for(int i = 0; i<neighborsIp.length; i++) { ///
			 * ------ ////// DatagramPacket packet = new DatagramPacket(sendBuf,
			 * sendBuf.length, InetAddress.getByName(neighborsIp[i]), port); new
			 * DatagramSocket().send(packet); } }
			 */
			marshaller.close();
			byteStreamO.close();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Receive an object at the local receivePort specified at the creation of
	 * the object.
	 * 
	 * @return the object received.
	 * @throws IOException
	 */
	public Object receiveObject() throws IOException {
		try {
			byte[] recvBuf = new byte[bufferSize];
			DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
			// Receives a UDP datagram.
			UDPsck.receive(packet);
			// Unmarshal the object.
			ByteArrayInputStream byteStreamI = new ByteArrayInputStream(recvBuf);
			REDSUnmarshaller unmarshaller = new REDSUnmarshaller(byteStreamI);
			Object obj = unmarshaller.readObject();
			unmarshaller.close();
			byteStreamI.close();
			return obj;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return (null);
	}

	/**
	 * Return the receivePort currently in use for receiving UDP datagrams.
	 * 
	 * @return the receivePort used for receiving UDP datagrams.
	 */
	public int getPort() {
		return receivePort;
	}

	/**
	 * Close the socket.
	 * 
	 * @see DatagramSocket#close()
	 */
	public void close() {
		UDPsck.close();
	}
}