
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

/* Dae Sung & Mukesh Rathore 
 * Sender.java
 * Systems and Networks II
 * Project 2
 *
 * This file implements the Sender class for RDT3.0 protocol
 * As the sender, this class has four states in its FSM. It
 * strictly follows the protocol and will continue to wait for
 * correct confirmation before changing sequence number(seq) and sending a new
 * message. The sender resends the message if it listens for
 * confirmation and the socket times out. This will continue until all
 * messages are sent, and then prompt user for another message.
 */

public class Sender {

	private static final int      BUFFER_SIZE = 52;
	private DatagramSocket        _socket; // the socket for communication with clients
	private static final String   LOCALHOST = "127.000.000.001";

	private int                   port; // the port number for communication with this server
	private String                rcvHost;
	private int                   rcvPort;
	private String                networkHost;
	private int                   networkPort;
	private int                   seq = 0;

	private boolean               _continueService;
	private int                   rdtSendState = 0; // RDT has four possible states for sender
	private boolean               waitResponse = false;

	/**
	 * Constructs a UDPserver object.
	 */
	public Sender() {
	}

	/**
	 * Creates a Datagram Socket and binds it to a free port.
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

	private String getMessage(DatagramPacket dp) {
		String message = new String(dp.getData());
		return message.substring(42, 52);
	}

	public void run() {
		_continueService = true;
		while (_continueService) {

			System.out.println("Please input message to send out (shutdown to end): ");
			Scanner scan = new Scanner(System.in);
			String message = scan.nextLine();

			if (message.equalsIgnoreCase("shutdown")) {
				scan.close();
				sendResponse(message, LOCALHOST, rcvPort);
				_continueService = false;
			}

//			Extracting and sending only 8 characters each time from the input message 
			String[] messagePackets = message.split("(?<=\\G.{8})");
			int count = messagePackets.length;
			int i = 0;

			while (i < count && _continueService) {

				switch (rdtSendState) {

				case 0:

					seq = 0;
					System.out.println("Sending: " + i);
					sendResponse(messagePackets[i], LOCALHOST, rcvPort);
					rdtSendState++;
					break;

				case 1:

					try {
						System.out.println("Waiting on Receiver ACK0 reply");
						_socket.setSoTimeout(300);
						DatagramPacket receive = receiveRequest();
						String result = getMessage(receive);
						System.out.println("ACK" + result.charAt(0) + " received" + " SEQ:" + this.seq + " Corruption: "
								+ result.charAt(1));
//						result first char represents ACK0 or ACK1, second char represents corrupt
						if (result.charAt(0) == '0' && result.charAt(1) == '0') {
							rdtSendState++;
							i++;
						}

					} catch (SocketException e) {
//						if socket times out send packet again
						System.err.println("Unable to receive message from Server");
						break;
					} catch (SocketTimeoutException ex) {
						System.out.println("Sender socket timed out case 1, resending packet");
						sendResponse(messagePackets[i], LOCALHOST, rcvPort);
						break;
					}
					break;

				case 2:
					System.out.println("Sending: " + i);
					seq = 1;
					sendResponse(messagePackets[i], LOCALHOST, rcvPort);
					rdtSendState++;
					break;

				case 3:
					try {
						System.out.println("Waiting on Receiver ACK1 reply");
						_socket.setSoTimeout(300);
						DatagramPacket receive = receiveRequest();
						String result = getMessage(receive);
						System.out.println("ACK" + result.charAt(0) + " received" + " SEQ:" + this.seq + " Corruption: "
								+ result.charAt(1));
						// if ACK1 and corrupt = 0
						if (result.charAt(0) == '1' && result.charAt(1) == '0') {
							rdtSendState = 0;
							i++;
						}
					} catch (SocketException e) {
						System.err.println("Unable to receive message from server");
						break;
					} catch (SocketTimeoutException ex) {
						System.out.println("Sender socket timed out case 3, resending packet");
						sendResponse(messagePackets[i], LOCALHOST, rcvPort);
						break;
					}
					break;
				}
			}
		}
		_socket.close();
	}

	/**
	 * Sends a request for service to the server. Do not wait for a reply in this
	 * function. This will be an asynchronous call to the server.
	 *
	 * @param response - The response to be sent
	 * @param hostAddr - The IP or Hostname of the Server
	 * @param port     - The port number of the Server
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

		System.err.println("Unable to create message");
		return -1;
	}

	/**
	 * Receives a client's request.
	 *
	 * @return - the Datagram containing the client's request or NULL if an error
	 *         occurred
	 */
	public DatagramPacket receiveRequest() throws SocketTimeoutException {
		byte[] buffer = new byte[BUFFER_SIZE];
		DatagramPacket newDatagramPacket = new DatagramPacket(buffer, BUFFER_SIZE);
		try {
			_socket.receive(newDatagramPacket);
		} catch (SocketTimeoutException ex) {
			System.out.println("Timout: No Ack Received");
//        	throw exception to caller
			throw ex;
		} catch (IOException e) {
			System.err.println("Unable to receive message from server");
			return null;
		}

		return newDatagramPacket;
	}

	/**
	 * Prints the response to the screen in a formatted way.
	 *
	 * response - the server's response as an XML formatted string
	 *
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

	/**
	 * The main function. Use this function for testing your code. We will provide a
	 * new main function on the day of the lab demo.
	 */
	public static void main(String[] args) {
		Sender sender = new Sender();

		if (args.length != 5) {
			System.err.println("Usage: Sender <port number> <rcvHost> <rcvPort> <networkHost> <networkPort>\n");
			return;
		}

		try {

			sender.port = Integer.parseInt(args[0]);
			sender.rcvHost = args[1];
			sender.rcvPort = Integer.parseInt(args[2]);
			sender.networkHost = args[3];
			sender.networkPort = Integer.parseInt(args[4]);

		} catch (NumberFormatException xcp) {
			System.err.println("Usage: Sender <port number> <rcvHost> <rcvPort> <networkHost> <networkPort>\n");
			return;
		}

//      construct client and client socket        
		if (sender.createSocket(sender.port) < 0) {
			return;
		}
		System.out.println("Sender running at port # " + sender.port);
		sender.run();
		sender.closeSocket();
	}

	/**
	 * Creates a Datagram from the specified request and destination host and port
	 * information.
	 *
	 * @param request  - The request to be submitted to the server
	 * @param hostname - The Hostname of the host receiving this Datagram
	 * @param port     - The port number of the host receiving this Datagram
	 *
	 * @return a complete Datagram or null if an error occurred creating the
	 *         Datagram
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
			System.err.println("Invalid Host Address");
			return null;
		}

		return new DatagramPacket(buffer, BUFFER_SIZE, hostAddr, this.networkPort);
	}
}
