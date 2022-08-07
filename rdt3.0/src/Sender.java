
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Scanner;

/*
 * UDPclient.java
 * Systems and Networks II
 * Project 2
 *
 * This file describes the functions to be implemented by the UDPclient class
 * You may also implement any auxiliary functions you deem necessary.
 */
public class Sender {

	private static final int BUFFER_SIZE = 54;
	private DatagramSocket _socket; // the socket for communication with clients
	private static final String LOCALHOST = "127.000.000.001";

	private int port; // the port number for communication with this server
	private String rcvHost;
	private int rcvPort;
	private String networkHost;
	private int networkPort;
	private int seq = 0;

	private boolean _continueService;
	private int rdtSendState = 0; // rdt has four possible states for sender
	private boolean waitResponse = false;

	/**
	 * Constructs a UDPserver object.
	 */
	public Sender() {
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

	private String getMessage(DatagramPacket dp) {
		String message = new String(dp.getData());
		return message.substring(43, 53);
	}

	public void run() {
		_continueService = true;
//		for (String str : messagePackets) {
//			sendResponse(message, LOCALHOST, rcvPort);
//		}

		while (_continueService) {

			System.out.println("Please input message to send out: ");
			Scanner scan = new Scanner(System.in);
			String message = scan.nextLine();

			if (message.equals("<shutdown/>")) {
				sendResponse(message, LOCALHOST, rcvPort);
				_continueService = false;
			}

			String[] messagePackets = message.split("(?<=\\G.{8})");
			int count = messagePackets.length;
			int i = 0;

			while (i < count) {
				switch (rdtSendState) {
				case 0:
					if (i < messagePackets.length) {
						seq = 0;
						sendResponse(messagePackets[i], LOCALHOST, rcvPort);
						rdtSendState++;
						break;
					} else {
						break;
					}
				case 1:
					if (i < messagePackets.length) {
						try {
							_socket.setSoTimeout(1000);
							DatagramPacket receive = receiveRequest();
							String result = getMessage(receive);
							// result first char represents ACK0 or ACK1, second char represents corrupt
							if (result.substring(0, 1) == "0" && result.substring(1, 2) == "0") {
								rdtSendState++;
								i++;
							}

						} catch (SocketException e) {
							// if socket times out send packet again
							System.err.println("socket timed out");
							sendResponse(messagePackets[i], LOCALHOST, rcvPort);
						}
						break;
					} else {
						break;
					}
				case 2:
					if (i < messagePackets.length) {
						seq = 1;
						sendResponse(messagePackets[i], LOCALHOST, rcvPort);
						rdtSendState++;
						break;
					} else {
						break;
					}
				case 3:
					if (i < messagePackets.length) {
						try {
							_socket.setSoTimeout(1000);
							DatagramPacket receive = receiveRequest();
							String result = getMessage(receive);
							if (result.substring(0, 1) == "1" && result.substring(1, 2) == "0") {
								rdtSendState = 0;
								i++;
							}
						} catch (SocketException e) {
							System.err.println("socket timed out");
							sendResponse(messagePackets[i], LOCALHOST, rcvPort);
						}
						break;
					} else {
						break;
					}
				}
			}
		}
		_socket.close();
//			
//			
//			
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
//		}
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
				System.err.println("Sender unable to send message to server");
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
		} catch (SocketTimeoutException ex) {
			System.err.println("socket has timed out");
			return null;
		} catch (IOException e) {
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
		Sender sender = new Sender();

		String serverName;

		String req;

//        if (args.length != 5) {
//            System.err.println("Usage: Sender <port number> <rcvHost> <rcvPort> <networkHost> <networkPort>\n");
//            return;
//        }

		try {
			sender.port = 0;
			sender.rcvHost = LOCALHOST;
			sender.rcvPort = 3000;
			sender.networkHost = LOCALHOST;
			sender.networkPort = 2999;

//        	sender.port = Integer.parseInt(args[0]);
//        	sender.rcvHost = args[1];
//        	sender.rcvPort = Integer.parseInt(args[2]);
//        	sender.networkHost = args[3];
//        	sender.networkPort = Integer.parseInt(args[2]);
		} catch (NumberFormatException xcp) {
			System.err.println("Usage: Sender <port number> <rcvHost> <rcvPort> <networkHost> <networkPort>\n");
			return;
		}

		// construct client and client socket

		if (sender.createSocket(sender.port) < 0) {
			return;
		}

		sender.run();
		sender.closeSocket();
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

	private DatagramPacket createDatagramPacket(String request, String hostname, int port) {
		byte buffer[] = new byte[BUFFER_SIZE];

		// empty message into buffer
		for (int i = 0; i < BUFFER_SIZE; i++) {
			buffer[i] = '\0';
		}
		// format is srcIP 16 bytes, srcPort 6 bytes, destIP 16 bytes, destPort 6 bytes
		String networkLayer = LOCALHOST + padLeftZeros(String.valueOf(this.port), 6) + LOCALHOST
				+ padLeftZeros(String.valueOf(port), 6);
		// format is seq# 1 byte, checkSum 1 byte, message 8 bytes
		String transportLayer = String.valueOf(this.seq) + "0" + request;
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
