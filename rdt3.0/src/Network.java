
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/*
 * UDPclient.java
 * Systems and Networks II
 * Project 2
 *
 * This file describes the functions to be implemented by the UDPclient class
 * You may also implement any auxillary functions you deem necessary.
 */
public class Network {

	private static final int BUFFER_SIZE = 54;
	private DatagramSocket _socket; // the socket for communication with clients
	private int port; // the port number for communication with this server
	private boolean _continueService;
	private static final String LOCALHOST = "127.000.000.001";

	private int lostPercent;
	private int delayedPercent;
	private int errorPercent;

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
		// run server until gracefully shut down
		_continueService = true;

		while (_continueService) {
			DatagramPacket sendingDatagramPacket = receiveRequest();

			// parse out addresses and segment from packet
			String request = new String(sendingDatagramPacket.getData()); // .trim();
			String addresses = request.substring(0, 42);
			int srcPort = Integer.parseInt(addresses.substring(15, 21));
			int destPort = Integer.parseInt(addresses.substring(36, 42));
			String segment = request.substring(42, 53);
			// forward packet to sender
			sendResponse(request, LOCALHOST, destPort);

//			System.out.println ("sender IP: " + newDatagramPacket.getAddress().getHostAddress());
//			System.out.println ("sender request: " + request);

//			if (request.equals("<shutdown/>")) {
//				_continueService = false;
//			}
//
//			if (request != null) {
//
//				String response = "<echo>"+request+"</echo>";
//            
//				sendResponse(
//					response, 
//					newDatagramPacket.getAddress().getHostName(), 
//					newDatagramPacket.getPort());
//			}
//			else {
//				System.err.println ("incorrect response from server");
//			}
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
	 * @return - the datagram containing the client's request or NULL if an error
	 *         occured
	 */
	public DatagramPacket receiveRequest() {
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
	 * response - the server's response as an XML formatted string
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
		String serverName;
		String req;

//        if (args.length != 4) {
//            System.err.println("Usage: Server <port number> <lost percent> <delayed percent> <error percent>\n");
//            return;
//        }

		try {
			server.port = 2999;
			server.lostPercent = 0;
			server.delayedPercent = 0;
			server.errorPercent = 0;

//            server.port = Integer.parseInt(args[0]);
//            server.lostPercent = Integer.parseInt(args[1]);
//            server.delayedPercent = Integer.parseInt(args[2]);
//            server.errorPercent = Integer.parseInt(args[3]);
		} catch (NumberFormatException xcp) {
			System.err.println("Usage: Server <port number> <lost percent> <delayed percent> <error percent>\n");
			return;
		}

		if (server.createSocket(server.port) < 0) {
			return;
		}

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
	 * @return a complete datagram or null if an error occurred creating the
	 *         datagram
	 */
	private DatagramPacket createDatagramPacket(String request, String hostname, int port) {
		byte buffer[] = new byte[BUFFER_SIZE];

		// empty message into buffer
		for (int i = 0; i < BUFFER_SIZE; i++) {
			buffer[i] = '\0';
		}

		// copy message into buffer
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
