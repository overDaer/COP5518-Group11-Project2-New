
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/* Dae Sung & Mukesh Rathore 
 * Receiver.java
 * Systems and Networks II
 * Project 2
 *
 * This file implements the receiver class for RDT3.0 protocol
 * As the receiver, this class only has two states in its FSM, and 
 * for the most part is listening for incoming packets. Every time
 * the receiver receives a packet, it will send confirmation message back to Sender.
 * When the correct packet is received with correct checksum, it will iterate it's sequence
 * and send confirmation.
 */

public class Receiver {
	private boolean                _continueService;
	private int                    rdtReceiveState = 0; // RDT has two possible states for Receiver
	private static final String    LOCALHOST = "127.000.000.001";
	private static final int       BUFFER_SIZE = 52;
	private DatagramSocket         _socket; // The socket for communication with a Server
	private int                    networkPort;

	/**
	 * Constructs a TCPclient object.
	 */
	public Receiver() {
	}

	/**
	 * Creates a Datagram socket and binds it to a free port.
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
			System.err.println("Unable to create and bind Socket");
			return -1;
		} catch (UnknownHostException ex) {
			System.err.println("Host unknown, Socket not bound");
			return -1;
		}
		return 0;
	}

	/**
	 * Sends a request for service to the server. Do not wait for a reply in this
	 * function. This will be an asynchronous call to the server.
	 *
	 * @param request  - The request to be sent
	 * @param hostAddr - The IP or Hostname of the Server
	 * @param port     - The Port Number of the Server
	 *
	 * @return - 0, if no error; otherwise, a negative number indicating the error
	 */
	public int sendResponse(String request, String hostAddr, String ack) {

		DatagramPacket newDatagramPacket = createDatagramPacket(request, hostAddr, ack);
		if (newDatagramPacket != null) {
			try {
				_socket.send(newDatagramPacket);
			} catch (IOException ex) {
				System.err.println("Receiver unable to send message to server");
				return -1;
			}

			return 0;
		}

		System.err.println("Unable to create message");
		return -1;
	}

	/**
	 * Receives the server's response following a previously sent request.
	 *
	 * @return - the server's response or NULL if an error occurred
	 */
	public DatagramPacket receiveResponse() {
		System.out.println("Receiver waiting to receive packet.");
		byte[] buffer = new byte[BUFFER_SIZE];
		DatagramPacket receivedDatagramPacket = new DatagramPacket(buffer, BUFFER_SIZE);

		try {
			_socket.receive(receivedDatagramPacket);
		} catch (IOException ex) {
			System.err.println("Unable to receive message from server");
			return null;
		}
		networkPort = receivedDatagramPacket.getPort();
		return receivedDatagramPacket;

	}

	/*
	 * Prints the response to the screen in a formatted way. Response - the server's
	 * response as an XML formatted string
	 */
	public static void printResponse(String response) {
		System.out.println("RESPONSE FROM SERVER: " + response);
	}

	/**
	 * Closes an open socket.
	 *
	 * @return - 0, if no error; otherwise, a negative number indicating the error
	 */
	public int closeSocket() {
		_socket.close();

		return 0;
	}

	private String getMessage(DatagramPacket dp) {
		String message = new String(dp.getData());
		return message.substring(42, 52);
	}

	public void run() {
//		Run server until gracefully shut down
		_continueService = true;
		StringBuilder sb = new StringBuilder();

		while (_continueService) {
			DatagramPacket receivedDatagram = receiveResponse();

			String message = new String(receivedDatagram.getData());
			message = message.substring(0, 52);
			String messageContent = getMessage(receivedDatagram);

			if (messageContent.toLowerCase().contains("shutdown")) {
				System.out.println(sb);
				System.out.println("Shutting Down.");
				_continueService = false;
			}

			System.out.println(
					"Received message with ACK" + messageContent.charAt(0) + " Corrupted: " + messageContent.charAt(1));

			switch (rdtReceiveState) {

			case 0:

//				If ACK1 and not corrupt
				if (messageContent.charAt(0) == '1' && messageContent.charAt(1) == '0') {
//					Response with "1" = ACK1
					sendResponse(message, LOCALHOST, "1");
					break;
				} else if (messageContent.charAt(0) == '0' && messageContent.charAt(1) == '0') {
					System.out.println("rdt state(0) - Received packet content : " + messageContent.substring(2));
					sb.append(messageContent.substring(2));
					sendResponse(message, LOCALHOST, "0");
//					Move to next state
					rdtReceiveState = 1;
					break;
				}
				break;

			case 1:

				if (messageContent.charAt(0) == '0' && messageContent.charAt(1) == '0') {
//					Response with "1" = ACK1
					sendResponse(message, LOCALHOST, "0");
					break;
				} else if (messageContent.charAt(0) == '1' && messageContent.charAt(1) == '0') {
					System.out.println("rdt state(1) - Received packet content : " + messageContent.substring(2));
					sb.append(messageContent.substring(2));
					sendResponse(message, LOCALHOST, "1");
//					Move to next state
					rdtReceiveState = 0;
					break;
				}
				break;
			}
		}
		System.out.println("Message accumulated : " + sb);
	}

	/**
	 * The main function. Use this function for testing your code. We will provide a
	 * new main function on the day of the lab demo.
	 */
	public static void main(String[] args) {
		Receiver client;
		int portNum;

		if (args.length != 1) {
			System.err.println("Usage: UDPreceiver <port number>\n");
			return;
		}
		try {
			portNum = Integer.parseInt(args[0]);
		} catch (NumberFormatException xcp) {
			System.err.println("Usage: UDPreceiver <port number>\n");
			return;
		}

//      Construct client and client socket
		client = new Receiver();
		if (client.createSocket(portNum) < 0) {
			return;
		}

		System.out.println("Receiver running at port # " + portNum);
		client.run();

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
	private DatagramPacket createDatagramPacket(String request, String hostname, String ack) {
		byte buffer[] = new byte[BUFFER_SIZE];

		// Empty message into buffer
		for (int i = 0; i < BUFFER_SIZE; i++) {
			buffer[i] = '\0';
		}
		String addresses = request.substring(0, 42);
		// The substrings are flipped here so srcPort and destPorts swapped
		int srcPort = Integer.parseInt(addresses.substring(36, 42));
		int destPort = Integer.parseInt(addresses.substring(15, 21));

		// Format is srcIP 16 bytes, srcPort 6 bytes, destIP 16 bytes, destPort 6 bytes
		String networkLayer = LOCALHOST + padLeftZeros(String.valueOf(srcPort), 6) + LOCALHOST
				+ padLeftZeros(String.valueOf(destPort), 6);
		// Format is seq# 1 byte, checkSum 1 byte, message 8 bytes
		String transportLayer = ack + "0        ";
		String message = networkLayer + transportLayer;
		// Copy message into buffer
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
