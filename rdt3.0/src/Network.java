
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/* Dae Sung & Mukesh Rathore 
 * Network.java
 * Systems and Networks II
 * Project 2
 *
 * This file implements the Network class for RDT3.0 protocol
 * This class simply forwards messages between receiver and sender,
 * while simulating issues with networks such as time delay, data corruption,
 * and lost packets.
 */

public class Network {

	private static final int      BUFFER_SIZE = 52;
	private DatagramSocket        _socket; // The socket for communication with clients
	private int                   port; // The port number for communication with this server
	private boolean               _continueService;
	private static final String   LOCALHOST = "127.000.000.001";

	private int                   lostPercent;
	private int                   delayedPercent;
	private int                   errorPercent;
	private Random                rand = new Random();

	/**
	 * Creates a datagram socket and binds it to a free port.
	 *
	 * @return - 0 or a negative number describing an error code if the connection
	 *         could not be established
	 */
	public int createSocket(int port) {
		try {
			this._socket = new DatagramSocket(null);
			InetAddress inetAddress = InetAddress.getByName("localhost");
			SocketAddress sockAdd = new InetSocketAddress(inetAddress, port);
			_socket.bind(sockAdd);
		} catch (SocketException ex) {
			System.err.println(ex.getMessage());
			System.err.println("unable to create and bind socket");
			return -1;
		} catch (UnknownHostException ex) {
			System.err.println("host unknown, socket not bound");
			return -1;
		}
		return 0;
	}

	public void run() {
		// Run server until gracefully shut down
		_continueService = true;

		while (_continueService) {

			DatagramPacket sendingDatagramPacket = receiveRequest();

			// Parse out addresses and segment from packet
			String request = new String(sendingDatagramPacket.getData()); // .trim();
			request = request.substring(0, 52);
			String addresses = request.substring(0, 42);
			int srcPort = Integer.parseInt(addresses.substring(15, 21));
			int destPort = Integer.parseInt(addresses.substring(36, 42));
			String segment = request.substring(42, 52);

			if (segment.contains("shutdown")) {
				sendResponse(request, LOCALHOST, destPort);
				_continueService = false;
				break;
			}

			// Forward packet to sender
			boolean delay = false;
			boolean corrupt = false;
			boolean lost = false;
			if (delayedPercent > rand.nextInt(100)) {
				delay = true;
			}
			if (errorPercent > rand.nextInt(100)) {
				corrupt = true;
			}
			if (lostPercent > rand.nextInt(100)) {
				lost = true;
			}
			System.out.println("Delayed: " + delay + " Corrupted: " + corrupt + " Lost: " + lost);
			if (delay) {
				try {
					TimeUnit.MILLISECONDS.sleep(500);
				} catch (InterruptedException e) {
					System.err.println("Sleep interrupted.");
				}
			}
			if (corrupt) {
				request = request.substring(0, 43) + "1" + request.substring(45, 52);
			}
			if (!lost) {
				System.out.println("Forwarding packet from port: " + srcPort + " to " + destPort);
				sendResponse(request, LOCALHOST, destPort);
			}
		}
	}

	/**
	 * Sends a request for service to the server. Do not wait for a reply in this
	 * function. This will be an asynchronous call to the server.
	 *
	 * @param response - the response to be sent
	 * @param hostAddr - the ip or hostname of the server
	 * @param port     - the port number of the server
	 *
	 * @return - 0, if no error; otherwise, a negative number indicating the error
	 */
	public int sendResponse(String response, String hostAddr, int port) {
		DatagramPacket newDatagramPacket = createDatagramPacket(response, hostAddr, port);
		if (newDatagramPacket != null) {
			try {
				_socket.send(newDatagramPacket);
			} catch (IOException ex) {
				System.err.println("Server unable to send message to server");
				return -1;
			}

			return 0;
		}

		System.err.println("unable to create message");
		return -1;
	}

	/**
	 * Receives a client's request.
	 *
	 * @return - the datagram containing the client's request or NULL if an error Occurred
	 *         
	 */
	public DatagramPacket receiveRequest() {
		System.out.println("Network waiting to receive packet.");
		byte[] buffer = new byte[BUFFER_SIZE];
		DatagramPacket newDatagramPacket = new DatagramPacket(buffer, BUFFER_SIZE);
		try {
			_socket.receive(newDatagramPacket);
		} catch (IOException ex) {
			System.err.println("unable to receive message from server");
			return null;
		}

		return newDatagramPacket;
	}

	/*
	 * Prints the response to the screen in a formatted way.
	 *
	 * Response - the server's response as an XML formatted string
	 *
	 */
	public static void printResponse(String response) {
		System.out.println("FROM SERVER: " + response);
	}

	/*
	 * Closes an open socket.
	 *
	 * @return - 0, if no error; otherwise, a negative number indicating the error
	 */
	public int closeSocket() {
		_socket.close();

		return 0;
	}

	/**
	 * The main function. Use this function for testing your code. We will provide a
	 * new main function on the day of the lab demo.
	 */
	public static void main(String[] args) {
		Network server = new Network();

		if (args.length != 4) {
			System.err.println("Usage: Server <port number> <lost percent> <delayed percent> <error percent>\n");
			return;
		}

		try {
			server.port = Integer.parseInt(args[0]);
			server.lostPercent = Integer.parseInt(args[1]);
			server.delayedPercent = Integer.parseInt(args[2]);
			server.errorPercent = Integer.parseInt(args[3]);
		} catch (NumberFormatException xcp) {
			System.err.println("Usage: Server <port number> <lost percent> <delayed percent> <error percent>\n");
			return;
		}

		if (server.createSocket(server.port) < 0) {
			return;
		}

		System.out.println("Network running at port # " + server.port);
		server.run();
		server.closeSocket();
	}

	/**
	 * Creates a datagram from the specified request and destination host and port
	 * information.
	 *
	 * @param request  - the request to be submitted to the server
	 * @param hostname - the hostname of the host receiving this datagram
	 * @param port     - the port number of the host receiving this datagram
	 *
	 * @return a complete datagram or null if an error occurred creating the datagram
	 *         
	 */
	private DatagramPacket createDatagramPacket(String request, String hostname, int port) {
		byte buffer[] = new byte[BUFFER_SIZE];

//		Empty message into buffer
		for (int i = 0; i < BUFFER_SIZE; i++) {
			buffer[i] = '\0';
		}

//		Copy message into buffer
		byte data[] = request.getBytes();
		System.arraycopy(data, 0, buffer, 0, Math.min(data.length, buffer.length));

		InetAddress hostAddr;
		try {
			hostAddr = InetAddress.getByName(hostname);
		} catch (UnknownHostException ex) {
			System.err.println("invalid host address");
			return null;
		}

		return new DatagramPacket(buffer, BUFFER_SIZE, hostAddr, port);
	}
}
