/*
  FILENAME - Client.java
  ASSIGNMENT - Final Project - SYSC 3303
  AUTHOR - GROUP 3 - W18
  DETAILS - A program that will generate valid TFTP datagram packets and sends them to ErrorSim
*/

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.Vector;

class Client extends CommonMethods {

	DatagramPacket txPacket, rxPacket; // Two datagrams for tx/rx
	DatagramSocket socket; // Only need one socket since we never tx/rx simultaneously

	// private static final int ERRORSIM_PORT = 23;
	private static final int ERRORSIM_PORT = 9923;
	private static final int DATA_SIZE = 516;

	public InetAddress clientIP, errorSimIP, serverIP;
	public String pathname, filename, operation, quit;
	public Vector<byte[]> receivedFile;
	public int fileSize;//size of received file vector or temp vector
	private boolean verboseOutput = false; // Quiet output when false

	// Used to determine if a packet is inbound or outbound when displaying its text
	// public enum direction {
	// IN, OUT;
	// }

	public Client() {

		pathname = System.getProperty("user.dir") + "/ClientFiles/";

		try {
			clientIP = InetAddress.getLocalHost();
			errorSimIP = clientIP;
		} catch (UnknownHostException he) {
			he.printStackTrace();
		}

		try {
			socket = new DatagramSocket();
			// socket.setSoTimeout(5000); // socket
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
	}

	// Main -> userInput -> newDatagram -> makeRequest -> send
	public static void main(String args[]) throws Exception {

		// Thread.sleep(3000); //Allows INTHOST and SERVER to load first
		System.out.println("TFTP Client is running.\n");
		Client c = new Client();
		userInput(c);
	}

	// A function to create FTFP REQUEST headers
	private static synchronized DatagramPacket makeRequest(String mode, String filename, OPCodes rq, InetAddress ip) {
		// HEADER ==> OPCODE = 2B | FILENAME | 0x0 | MODE | 0x0
		byte[] header = new byte[DATA_SIZE];

		// OPCODE
		header[0] = 0;
		if (rq == OPCodes.READ)
			header[1] = 1;
		else if (rq == OPCodes.WRITE)
			header[1] = 2;
		else
			header[1] = 0; // This should never happen.

		// FILENAME
		byte[] temp = filename.getBytes();
		int j = 2; // byte placeholder for header

		for (int i = 0; i < filename.getBytes().length; i++) {
			header[j++] = temp[i];
		}

		// Add a 0x0
		header[j++] = 0;

		// MODE
		temp = mode.getBytes();

		for (int i = 0; i < mode.getBytes().length; i++) {
			header[j++] = temp[i];
		}

		// Add a 0x0
		header[j++] = 0;

		// Write header to txPacket
		DatagramPacket packet = new DatagramPacket(header, j, ip, ERRORSIM_PORT);
		packet = resizePacket(packet);

		return packet;
	}

	// gets file path, file name and operation (read/write) from the user
	public static void userInput(Client c) throws IOException {

		boolean startTransfer = false;
		boolean showMainMenu = true;
		boolean showOptionsMenu = false;
		boolean inputValid = false;
		boolean firstTime = true;
		Scanner reader = new Scanner(System.in); // Reading from System.in
		String input = "";
		String errorMessage;

		String mainMenu = "Commands:\n(s)tart   - Start a file transfer"
				+ "\n(o)ptions - Change parameters for this application"
				+ "\n(q)uit    - Ends the program after all file transfers are completed" + "\n";

		String optionsMenu = "\nOptions:\n(pwd)       - Print Working Directory" + "\n(cd)        - Change Directory"
				+ "\n(dir)       - List files in working directory" + "\n(v)erbose   - Verbose Output"
				+ "\n(q)uiet     - Quiet Output" + "\n(ip)        - Change the server's IP address"
				+ "\n(m)ode      - Normal or Test Modes" + "\n(b)ack      - Back to main menu" + "\n";

		while (true) {
			while (showMainMenu) {
				inputValid = false;

				System.out.println(mainMenu);

				while (!inputValid) {
					System.out.print("Please choose an option: ");
					input = reader.nextLine();
					input = input.toLowerCase();

					switch (input) {
					case "s":
						startTransfer = true;
						inputValid = true;
						firstTime = true;
						break;
					case "o":
						showOptionsMenu = true;
						inputValid = true;
						break;
					case "q":
						c.quit = "q";
						inputValid = true;
						break;
					default:
						System.out.println("The input is not valid.");
					}
				}

				showMainMenu = false;

				if (c.quit == "q") {
					System.out.println("Ending application");
					System.exit(1);
				}
			}

			while (showOptionsMenu) {
				inputValid = false;

				System.out.println(optionsMenu);

				while (!inputValid) {
					System.out.print("Please choose an option: ");
					input = reader.nextLine();
					input = input.toLowerCase();

					switch (input) {
					case "pwd":
						System.out.println("Working Directory = " + c.pathname);
						inputValid = true;
						break;
					case "cd":
						System.out.print("New directory path: ");
						System.out.println("-- Not implemented yet --");
						// System.out.print("Enter full file path: ");
						// c.pathname = reader.nextLine();
						inputValid = true;
						break;
					case "dir":
						System.out.print("Displaying contents of " + c.pathname + ":");
						System.out.println("-- Not implemented yet --");
						inputValid = true;
						break;
					case "v":
						System.out.println("Toggled: Verbose Output");
						c.verboseOutput = true;
						try {
							sendOOB(true);
						} catch (Exception e) {
						}
						inputValid = true;
						break;
					case "q":
						System.out.println("Toggled: Quiet Output");
						c.verboseOutput = false;
						try {
							sendOOB(false);
						} catch (Exception e) {
						}
						inputValid = true;
						break;
					case "ip":
						System.out.print("Enter Server's IP Address: ");
						System.out.println("-- Not implemented yet --");
						inputValid = true;
						break;
					case "m":
						System.out.print("Choose (n)ormal or (t)est mode: ");
						System.out.println("-- Not implemented yet --");
						inputValid = true;
						break;
					case "b":
						showMainMenu = true;
						showOptionsMenu = false;
						inputValid = true;
						break;
					default:
						System.out.println("The input is not valid.");
					}
				}
			}

			while (startTransfer) {

				inputValid = false;

				if (!firstTime) {
					while (!inputValid) {
						System.out.println("Would you like to go back to the menu? (y/n) ");
						input = reader.nextLine();

						switch (input) {
						case "y":
							showMainMenu = true;
							startTransfer = false;
							inputValid = true;
						case "n":
							inputValid = true;
							break;
						default:
							System.out.println("The input is not valid.");
						}
					}
				}
				firstTime = false;

				if (!showMainMenu) {
					System.out.print("Enter file name: ");
					input = reader.nextLine();
					c.filename = input; // Scans the next token of the input as an int.

					System.out.print("Is it a (r)ead or (w)rite?: ");
					input = reader.nextLine();
					c.operation = input;
					Boolean filePathValid = false;

					if ((c.operation).equals("write") || (c.operation).equals("w")) {
						c.txPacket = newDatagram(c.errorSimIP, OPCodes.WRITE, c.filename);
						errorMessage = c.checkError(c.txPacket);
						while (errorMessage.equals("File not found.")) {
							System.out.print("File Not Found, please enter valid file name: ");
							input = reader.nextLine();
							c.filename = input; // Scans the next token of the input as an int.
							c.txPacket = newDatagram(c.errorSimIP, OPCodes.WRITE, c.filename);
							errorMessage = c.checkError(c.txPacket);
						}
						System.out.println("\n--Writing " + c.filename + " to Server.--\n");

						try {
							c.socket.send(c.txPacket);
							outputText(c.txPacket, direction.OUT, endhost.ERRORSIM, c.verboseOutput);
							c.writeRequest(getPort(c.txPacket), c.filename);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					if ((c.operation).equals("read") || c.operation.equals("r")) {
						c.txPacket = newDatagram(c.errorSimIP, OPCodes.READ, c.filename);
						errorMessage = c.checkError(c.txPacket);
						if(errorMessage =="No Error") {
							System.out.println("\n--Reading " + c.filename + " from Server.--\n");
							try {
								c.socket.send(c.txPacket);
								outputText(c.txPacket, direction.OUT, endhost.ERRORSIM, c.verboseOutput);
								c.receivedFile = c.readRequest(getPort(c.txPacket));
	
								// CHECK ERRORS HERE
								if (c.receivedFile.size() != 0) {
									saveFile(c.receivedFile, c.filename, c);
								}
	
							} catch (Exception e) {
								e.printStackTrace();
							}
						}else {
							System.out.println(errorMessage);
						}
					}

					// startTransfer = true;
				}

			}
		}
	}

	public synchronized String checkError(DatagramPacket packet) {
		String errorMessage = "No Error";
		String[] msg = new String[8];
		msg[0] = "Not defined, see error message (if any).";
		msg[1] = "File not found."; // -- Iteration 2	   -Done in user input WRQ
		msg[2] = "Access violation."; // -- Iteration 2
		msg[3] = "Disk full or allocation exceeded."; // -- Iteration 2
		msg[4] = "Illegal TFTP operation.";
		msg[5] = "Unknown transfer ID.";
		msg[6] = "File already exists."; // -- Iteration 2 -Done in user input RRQ
		msg[7] = "No such user.";

		byte[] data = packet.getData();

		File path = new File("./ClientFiles/");
		//path = new File("e:/");//used for testing error 3 - path of full drive
		long diskSpace = path.getUsableSpace();// returns free space on path in bytes
		File f;
		f = new File("./ClientFiles/" + getFilename(packet));
		
		// Can do error 2 (access violation)
		// Can do error 3 (Disk full or allocation exceeded.)
		// Can do error 6 (file already exists)
		if (data[0] == 0 && data[1] == 1) {//RRQ
			if (diskSpace == 0) {
				System.out.println(msg[3]);
				errorMessage = msg[3];
			}
			if (f.exists() && !f.isDirectory()) {
				errorMessage = msg[6];
			}			
			if (path.canRead() == false) {
				errorMessage = msg[2];
			}
		}
		
		// Can do error 1 (file not found)
		// Can do error 2 (access violation)
		if (data[0] == 0 && data[1] == 2) {//WRQ
			if (f.exists() && !f.isDirectory()) {
				// System.out.println("File Exists!");
			} else {
				errorMessage = msg[1];
				System.out.println(msg[1]);
			}
		}

		// Can do error 2 (access violation)
		// Can do error 3 (Disk full or allocation exceeded.)
		if (data[0] == 0 && data[1] == 3) {//DATA
			if (diskSpace == 0 || diskSpace < fileSize) {// 100 is placeholder for vector size()
				System.out.println(msg[3]);
				errorMessage = msg[3];
			}
		}

		// If we receive an Error Packet
		if (data[0] == 0 && data[1] == 5) {
			String ascii = new String(data, Charset.forName("UTF-8"));
			ascii = ascii.substring(4, ascii.length() - 1);
			
			for (int i = 0; i < msg.length; i++) {
				if (ascii.equals(msg[i]))
					errorMessage = ascii;
			}
		}

		return errorMessage;

	}

	public void sendError(DatagramPacket packet) {
		String error = checkError(packet);

		byte[] sendData = new byte[DATA_SIZE];
		sendData[0] = 0;
		sendData[1] = 5;
		sendData[2] = 0;
		sendData[3] = errorMap.get(error).byteValue(); // Map the error code to the corresponding number

		// Error Code
		byte[] temp = error.getBytes();
		int j = 4; // byte placeholder for header

		for (int i = 0; i < error.getBytes().length; i++) {
			sendData[j++] = temp[i];
		}

		// Add 0x0
		sendData[j++] = 0;

		// Resizing packet here for now
		byte[] sendSmallData = new byte[j];
		for (int i = 0; i < j; i++) {
			sendSmallData[i] = sendData[i];
		}

		// send DATA packet to client
		txPacket = new DatagramPacket(sendSmallData, sendSmallData.length, packet.getAddress(), getPort(packet));
		txPacket = resizePacket(txPacket);
		
		try {
			socket.send(txPacket);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		outputText(txPacket, direction.OUT, endhost.ERRORSIM, verboseOutput);

		System.out.println("ERROR Complete: TERMINATING SOCKET");
		socket.close();
		System.exit(0);// shutdown after error
	}

	// A function that takes a vector of any size and writes its bytes to a file
	public static void saveFile(Vector<byte[]> receivedFile, String filename, Client c) {
		byte[] tempArray;
		int charCount = 0;

		for (int i = 0; i < receivedFile.size(); i++) {
			charCount += receivedFile.elementAt(i).length;
		}

		tempArray = new byte[charCount];

		String path = c.pathname; // "./"
		String outputName = filename;

		int tempCount = 0;

		for (byte[] bytes : receivedFile) {
			for (byte b : bytes) {
				tempArray[tempCount] = b;
				tempCount++;
			}
		}

		try (FileOutputStream fileOutputStream = new FileOutputStream(path + outputName)) {
			fileOutputStream.write(tempArray);
			fileOutputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// A function to create a new Datagram
	// Future updates to this code will implement the ability to create other types
	// of TFTP packets
	public static DatagramPacket newDatagram(InetAddress errorSimIP, OPCodes op, String filename) {
		String mode = "NETascii";
		DatagramPacket newPacket = makeRequest(mode, filename, op, errorSimIP);
		return newPacket;
	}

	/*
	 * RRQ FLOW Client -> RRQ -> Server Server -> DATA BLK 1 -> Client Client -> ACK
	 * BLK 1 -> Server Repeats until Server sends last DATA pkt, Client sends a
	 * final ACK
	 */

	// A function that implements the RRQ of TFTP client, takes as input the port
	// that the server uses for handling requests and name of the requested file
	// isValidPkt is used to validate if received packet is a DATA packet
	// buffer is used to temporarily store the byte array from the received DATA
	// packet
	// receivedFile is a vector that stores all byte[] for a file stream; this will
	// be converted back to a file later
	public synchronized Vector<byte[]> readRequest(int port) throws Exception {
		boolean isValidPkt = true;
		Vector<byte[]> tempVector = new Vector<byte[]>();

		while (isValidPkt) {// Loop to receive DATA and send ACK until received DATA<512 bytes
			// receive and create DATA
			byte[] receiveData = new byte[DATA_SIZE];
			rxPacket = new DatagramPacket(receiveData, receiveData.length);
			socket.receive(rxPacket);
			rxPacket = resizePacket(rxPacket);
			outputText(rxPacket, direction.IN, endhost.ERRORSIM, verboseOutput);
			byte[] buffer = new byte[rxPacket.getLength() - 4];

			// CHECK FOR ERRORS HERE
			String error = checkError(rxPacket);
			fileSize=tempVector.size();
			if (error == "No Error") {

				for (int i = 4; i < rxPacket.getLength(); i++) {
					buffer[i - 4] = rxPacket.getData()[i];
				}

				tempVector.addElement(buffer);

				// stop if received packet does not have DATA opcode or DATA is less then 512
				// bytes
				if (receiveData[1] != 3)
					isValidPkt = false;
				else {
					// create and send Ack packet with block # of received packet
					byte[] sendData = new byte[] { 0, 4, receiveData[2], receiveData[3] };
					txPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getLocalHost(), port);
					socket.send(txPacket);
					outputText(txPacket, direction.OUT, endhost.ERRORSIM, verboseOutput);

					if (rxPacket.getLength() < DATA_SIZE)
						isValidPkt = false;
				}
			} else {
				isValidPkt = false;
			}
		}

		return tempVector;
	}

	/*
	 * WRQ FLOW Client -> WRQ -> Server Server -> ACK BLK 0 -> Client Client -> DATA
	 * BLK 1 -> Server Server -> ACK BLK 1 -> Client Repeats until Client sends last
	 * DATA pkt, Server sends a final ACK
	 */

	// A function that implements the WRQ of TFTP client, takes as input the port
	// that the server uses for handling requests and name of the requested file
	public synchronized void writeRequest(int port, String filename) throws IOException {
		int blockNum = 1;

		Path path = Paths.get(pathname + filename);
		byte[] file = Files.readAllBytes(path);
		int totalBlocksRequired = (file.length / 512) + 1;
		int remainderLastBlock = (file.length % 512);
		boolean onLastBlock = false;
		int j = 0;

		while (true) {// Loop to receive ACK and send DATA until sent DATA<512 bytes
			// create and receive ACK
			byte[] receiveData = new byte[DATA_SIZE];
			rxPacket = new DatagramPacket(receiveData, receiveData.length);
			socket.receive(rxPacket);
			rxPacket = resizePacket(rxPacket);
			outputText(rxPacket, direction.IN, endhost.ERRORSIM, verboseOutput);
			
			
			if (receiveData[1]==5) return; //Stop Client if it receives an error
			if (checkError(rxPacket) != "No Error") {//check for error from server
	        	sendError(rxPacket);
	        }
			
			// create send DATA
			byte[] blockNumBytes = blockNumToBytes(blockNum++);
			byte[] sendData = new byte[DATA_SIZE];
			sendData[0] = 0;
			sendData[1] = 3;
			sendData[2] = blockNumBytes[0];
			sendData[3] = blockNumBytes[1];

			// Stop after final ACK
			if (onLastBlock)
				break;

			// Check if its the last block
			if (totalBlocksRequired == 1 || file.length - j < 512)
				onLastBlock = true;

			if (!onLastBlock) {
				for (int i = 4; i < DATA_SIZE && j < file.length; i++) {// 4-515 are for 512 bytes of data
					sendData[i] = file[j++];
				}
			} else {
				sendData = new byte[remainderLastBlock + 4];
				sendData[0] = 0;
				sendData[1] = 3;
				sendData[2] = blockNumBytes[0];
				sendData[3] = blockNumBytes[1];
				for (int i = 4; i < remainderLastBlock + 4; i++) {// 4-515 are for 512 bytes of data
					sendData[i] = file[j++];
				}
			}

			txPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getLocalHost(), port);
			socket.send(txPacket);
			txPacket = resizePacket(txPacket);
			outputText(txPacket, direction.OUT, endhost.ERRORSIM, verboseOutput);
		}
	}

	// Function for sending management packets out of band
	// These packets will not be displayed in the output for any of the programs
	// Used to synchronize parameters between all three programs
	// Verbose/Quiet output, according to requirements, must be the same across all
	// running apps
	public static void sendOOB(boolean verbose) throws Exception {
		// HEADER ==> OPCODE = 0x99 | BOOLEAN VERBOSE
		byte[] header = new byte[3];

		// OPCODE
		header[0] = 9;
		header[1] = 9;

		if (verbose)
			header[2] = 1; // verbose is toggled
		else
			header[2] = 0; // quiet is toggled

		DatagramPacket OOBPacket = new DatagramPacket(header, header.length, InetAddress.getLocalHost(), ERRORSIM_PORT);
		DatagramSocket OOBSocket = new DatagramSocket();
		OOBSocket.send(OOBPacket);
		OOBSocket.close();

	}
}
