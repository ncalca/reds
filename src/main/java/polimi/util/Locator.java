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

import java.net.*;
import java.util.*;

/*******************************************************************************
 * A <code>Locator</code> can be used to publish the url of every kind of
 * server/resource or to retrieve urls of other servers/resources. It uses
 * multicast communication to perform its job. In particular, requests are
 * addressed to a special multicast group while replies are sent back through
 * traditional UDP sockets.
 ******************************************************************************/
public class Locator implements Runnable {
	public static final String MULTICAST_ADDRESS = "228.0.0.1";
	public static final int MULTICAST_PORT = 1110;
	private String urlToPublish;
	private byte[] urlBuffer;
	private int replyPort;
	private MulticastSocket multicastSocket;
	private InetAddress group;
	private DatagramSocket socket;
	private boolean serverRunning;
	private boolean continueRunning;
	private Thread serverThread;

	/**
	 * Builds a new <code>Locator</code>.
	 */
	public Locator() throws java.io.IOException {
		this(null);
	}

	/**
	 * Builds a new <code>Locator</code>, which when used server-side publishes
	 * the specified URL.
	 * 
	 * @param urlToPublish
	 *            The URL to publish.
	 */
	public Locator(String urlToPublish) throws java.io.IOException {
		this.urlToPublish = urlToPublish;
		if (urlToPublish != null)
			buildUrlBuffer();
		serverRunning = false;
		serverThread = null;
		group = InetAddress.getByName(MULTICAST_ADDRESS);
		multicastSocket = new MulticastSocket(MULTICAST_PORT);
		multicastSocket.setSoTimeout(300);
		multicastSocket.joinGroup(group);
		socket = new DatagramSocket();
		replyPort = socket.getLocalPort();
	}

	/**
	 * Sets the URL to publish.
	 * 
	 * @param urlToPublish
	 *            The URL to publish.
	 */
	public void setUrlToPublish(String urlToPublish) {
		this.urlToPublish = urlToPublish;
		if (urlToPublish != null)
			buildUrlBuffer();
	}

	/**
	 * Returns the URL to publish.
	 * 
	 * @return The URL to publish.
	 */
	public String getUrlToPublish() {
		return urlToPublish;
	}

	/**
	 * Starts the server thread that waits for requests from other locators and
	 * publishes the <code>urlToPublish</code> URL upon requests.
	 */
	public void startServer() {
		if (serverRunning || urlToPublish == null)
			return;
		if (serverThread == null)
			serverThread = new Thread(this);
		serverThread.setName("Locator.serverThread");
		serverThread.start();
		serverRunning = true;
	}

	/**
	 * Stops the server thread.
	 * 
	 * @see #startServer
	 */
	public void stopServer() {
		continueRunning = false;
	}

	public void run() {
		byte[] reqBuffer = new byte[256];
		StringBuffer addr = new StringBuffer();
		int port;
		DatagramPacket requestPacket = new DatagramPacket(reqBuffer, 256);
		DatagramPacket replyPacket = null;
		continueRunning = true;
		while (continueRunning) {
			try {
				try {
					multicastSocket.receive(requestPacket);
				} catch (SocketTimeoutException ex) {
					continue;
				}
				// parse the address
				addr.delete(0, addr.length());
				// Parse the address keeping care of the fact that byte are
				// always
				// signed in Java
				for (int i = 1; i < reqBuffer[0] - 3; i++) {
					if (reqBuffer[i] < 0) {
						addr.append(reqBuffer[i] + 256);
					} else {
						addr.append(reqBuffer[i]);
					}
					addr.append('.');
				}
				if (reqBuffer[reqBuffer[0] - 3] < 0) {
					addr.append(reqBuffer[reqBuffer[0] - 3] + 256);
				} else {
					addr.append(reqBuffer[reqBuffer[0] - 3]);
				}
				// parse the port keeping care of the fact that byte are always
				// signed
				// in Java
				int portByteMS, portByteLS;
				if (reqBuffer[reqBuffer[0] - 1] < 0) {
					portByteLS = reqBuffer[reqBuffer[0] - 1] + 256;
				} else {
					portByteLS = reqBuffer[reqBuffer[0] - 1];
				}
				if (reqBuffer[reqBuffer[0] - 2] < 0) {
					portByteMS = reqBuffer[reqBuffer[0] - 2] + 256;
				} else {
					portByteMS = reqBuffer[reqBuffer[0] - 2];
				}
				port = portByteMS * 256 + portByteLS;
				// now avoid replying yourself
				if (InetAddress.getByName(addr.toString()).equals(InetAddress.getLocalHost()) && port == replyPort) {
					continue;
				}
				// build the reply packet and send it
				replyPacket = new DatagramPacket(urlBuffer, urlBuffer.length, InetAddress.getByName(addr.toString()),
						port);
				socket.send(replyPacket);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		serverRunning = false;
	}

	/**
	 * Publish a request and waits at least <code>timeout</code> milliseconds
	 * for replies. If <code>timeout</code> is 0 waits until the first reply
	 * come. If no replies come before timeout elapses, it returns an empty
	 * array;
	 * 
	 * @param timeout
	 *            The timeout to wait for replies.
	 * @return The set of URLs received as a reply or <code>null</code> if the
	 *         timeout has elapsed before the arrival of the first reply.
	 * @throws java.io.IOException
	 */
	public String[] locate(int timeout) throws java.io.IOException {
		byte[] localAddress = null;
		byte[] buffer = null;
		List urls = new ArrayList();
		// read the local address
		try {
			localAddress = InetAddress.getLocalHost().getAddress();
		} catch (Exception ex) {
			ex.printStackTrace();
		} // build the request packet: length+addr+port
		buffer = new byte[localAddress.length + 3];
		buffer[0] = (byte) (localAddress.length + 3);
		// packet length
		for (int i = 0; i < localAddress.length; i++) {
			buffer[i + 1] = localAddress[i];
		}
		buffer[localAddress.length + 1] = (byte) (replyPort / 256);
		buffer[localAddress.length + 2] = (byte) (replyPort % 256);
		// send the request
		DatagramPacket reqPacket = new DatagramPacket(buffer, buffer.length, group, MULTICAST_PORT);
		multicastSocket.send(reqPacket);
		// wait for replies
		buffer = new byte[256];
		DatagramPacket replyPacket = new DatagramPacket(buffer, 256);
		long startingTime = System.currentTimeMillis();
		int elapsedTime = 0;
		do {
			socket.setSoTimeout(timeout - elapsedTime);
			try {
				socket.receive(replyPacket);
				urls.add(new String(buffer, 1, buffer[0] - 1));
			} catch (SocketTimeoutException ex) {
				break;
			}
			elapsedTime = (int) (System.currentTimeMillis() - startingTime);
		} while (elapsedTime <= timeout);
		// Building the array of Strings to return
		String[] reply;
		if (urls.size() == 0)
			reply = null;
		else {
			reply = new String[urls.size()];
			for (int i = 0; i < reply.length; i++) {
				reply[i] = (String) urls.get(i);
			}
		}
		return reply;
	}

	/**
	 * Builds the array of bytes that holds the URL to publish. Such an array is
	 * used by the server thread to build the reply packet. It is called every
	 * time the url to publish changes.
	 */
	private void buildUrlBuffer() {
		urlBuffer = new byte[urlToPublish.length() + 1];
		urlBuffer[0] = (byte) (urlToPublish.length() + 1);
		for (int i = 0; i < urlToPublish.length(); i++) {
			urlBuffer[i + 1] = urlToPublish.getBytes()[i];
		}
	}

	public static void main(String args[]) {
		try {
			Locator l = new Locator(args[0]);
			l.startServer();
			System.out.println("Press a key to locate other brokers");
			System.in.read();
			String[] urls = l.locate(1000);
			for (int i = 0; i < urls.length; i++) {
				System.out.println(urls[i]);
			}
			l.stopServer();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
