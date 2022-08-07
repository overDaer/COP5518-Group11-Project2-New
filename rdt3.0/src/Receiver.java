
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
 * You may also implement any auxiliary functions you deem necessary.
 */
public class Receiver {
	private boolean _continueService;
	private int rdtSendState = 0; // rdt has two possible states for receiver
	private static final String LOCALHOST = "127.000.000.001";
	private static final int BUFFER_SIZE = 54;
	private DatagramSocket _socket; // the socket for communication with a server
	private int networkPort;
	private int seq = 0;

	/**
	 * Constructs a TCPclient object.
	 */
	public Receiver() {
	}

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

	/**
	 * Sends a request for service to the server. Do not wait for a reply in this
	 * function. This will be an asynchronous call to the server.
	 *
	 * @param request  - the request to be sent
	 * @param hostAddr - the ip or hostname of the server
	 * @param port     - the port number of the server
	 *
	 * @return - 0, if no error; otherwise, a negative number indicating the error
	 */
	public int sendResponse(String request, String hostAddr, int port) {

		DatagramPacket newDatagramPacket = createDatagramPacket(request, hostAddr, port);
		if (newDatagramPacket != null) {
			try {
				_socket.send(newDatagramPacket);
			} catch (IOException ex) {
				System.err.println("Receiver unable to send message to server");
				return -1;
			}

			return 0;
		}

		System.err.println("unable to create message");
		return -1;
	}

	/**
	 * Receives the server's response following a previously sent request.
	 *
	 * @return - the server's response or NULL if an error occurred
	 */
	public DatagramPacket receiveResponse() {
		byte[] buffer = new byte[BUFFER_SIZE];
		DatagramPacket receivedDatagramPacket = new DatagramPacket(buffer, BUFFER_SIZE);

		try {
			_socket.receive(receivedDatagramPacket);
		} catch (IOException ex) {
			System.err.println("unable to receive message from server");
			return null;
		}
		networkPort = receivedDatagramPacket.getPort();
		return receivedDatagramPacket; // .trim();

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

	public void run() {
		// run server until gracefully shut down
		_continueService = true;

		while (_continueService) {

			DatagramPacket receivedDatagram = receiveResponse();

			String message = new String(receivedDatagram.getData()); // .trim();

			sendResponse(message, LOCALHOST, networkPort);

//			//parse out addresses and segment from packet
//			String request = new String (sendingDatagramPacket.getData()); //.trim();
//			String addresses = request.substring(0,42);
//			int srcPort = Integer.parseInt(addresses.substring(16,6));
//			int destPort = Integer.parseInt(addresses.substring(38,6));
//			String segment = request.substring(44,10);
//			//forward packet to sender
//			sendResponse(request, LOCALHOST, destPort);

//			switch(rdtSendState) {
//			case 0:
//				
//				break;
//			case 1:
//				break;
//				
//			}

//			DatagramPacket newDatagramPacket = receiveRequest();
//
//			String request = new String (newDatagramPacket.getData()).trim();
//
//			System.out.println ("sender IP: " + newDatagramPacket.getAddress().getHostAddress());
//			System.out.println ("sender request: " + request);
//			
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
	 * The main function. Use this function for testing your code. We will provide a
	 * new main function on the day of the lab demo.
	 */
	public static void main(String[] args) {
		Receiver client;
		// String serverName;
		String req;
		int portNum;

//        if (args.length != 1) {
//            System.err.println("Usage: UDPreceiver <port number>\n");
//            return;
//        }

		try {

			portNum = 3000;
			// portNum = Integer.parseInt(args[0]);

		} catch (NumberFormatException xcp) {
			System.err.println("Usage: receiver <port number>\n");
			return;
		}

		// construct client and client socket
		client = new Receiver();
		if (client.createSocket(portNum) < 0) {
			return;
		}

		client.run();

//        System.out.print ("Enter a request: ");
//        req = System.console().readLine();
//        
//        if (client.sendRequest(req, LOCALHOST, portNum) < 0) {
//            client.closeSocket();
//            return;
//        }
//
//        String response = client.receiveResponse();
//        if (response != null) {
//            Receiver.printResponse(response.trim());
//        }
//        else {
//            System.err.println ("incorrect response from server");
//        }

		client.closeSocket();
	}

	public String padLeftZeros(String input, int length) {
		if (input.length() >= length)
			return input;
		StringBuilder sb = new StringBuilder();
		while (sb.length() < length - input.length()) {
			sb.append('0');
		}
		sb.append(input);
		return sb.toString();
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
		String addresses = request.substring(0, 42);
		// the substrings are flipped here so srcPort and destPorts swapped
		int srcPort = Integer.parseInt(addresses.substring(36, 42));
		int destPort = Integer.parseInt(addresses.substring(15, 21));
		String segment = request.substring(43, 53);

		// to show that packet was received print out
		System.out.println(segment);

		// format is srcIP 16 bytes, srcPort 6 bytes, destIP 16 bytes, destPort 6 bytes
		String networkLayer = LOCALHOST + padLeftZeros(String.valueOf(srcPort), 6) + LOCALHOST
				+ padLeftZeros(String.valueOf(destPort), 6);
		// format is seq# 1 byte, checkSum 1 byte, message 8 bytes
		String transportLayer = String.valueOf(this.seq) + "0        ";
		String message = networkLayer + transportLayer;
		// copy message into buffer
		byte data[] = message.getBytes();

		System.arraycopy(data, 0, buffer, 0, Math.min(data.length, buffer.length));

		InetAddress hostAddr;
		try {
			hostAddr = InetAddress.getByName(hostname);
		} catch (UnknownHostException ex) {
			System.err.println("invalid host address");
			return null;
		}

		return new DatagramPacket(buffer, BUFFER_SIZE, hostAddr, this.networkPort);
	}
}
