/*
  FILENAME - Server.java
  ASSIGNMENT - Final Project - SYSC 3303
  AUTHOR - GROUP 3 - W18
  DETAILS - A program that will receives DatagramPackets from ErrorSim
          - Based on the packet type, sends a response to ErrorSim
*/

import javax.xml.crypto.Data;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Vector;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;

class Server implements Runnable
{

    //private static final int ERRORSIM_PORT = 69;
    private static final int ERRORSIM_PORT = 9969;
    private static final int DATA_SIZE = 516;

    private DatagramSocket socket;
    private DatagramPacket packet;
    private byte[] rxData, txData;
    private boolean isListener = false;
    private boolean quitSignal = false;

    //TFTP OPCODES
    public enum OPCodes {
        READ,   //0x01
        WRITE,  //0x02
        DATA,   //0x03
        ACK,    //0x04
        ERROR   //0x05
    }

    //Used to determine if a packet is inbound or outbound when displaying its text
    public enum direction {
        IN, OUT;
    }

    //Overloaded Constructor
    //Used to instantiate a LISTENER
    public Server(int port) throws Exception
    {
        socket = new DatagramSocket();

        if (port == ERRORSIM_PORT)
            isListener = true;

        try {
            socket = new DatagramSocket(port);
            //socket.setSoTimeout(30000); // LISTENER timeouts after 30 seconds
        }
        catch (SocketException se)
        {
            se.printStackTrace();
            System.exit(1);
        }
    }

    //Overloaded Constructor
    //Used to instantiate a SENDER
    public Server(DatagramPacket packet) throws Exception
    {
        this.txData = new byte[DATA_SIZE];
        this.socket = new DatagramSocket();
        this.packet = packet;
    }

    //Essentially a pseudo-main method that runs all logic for the threads
    //Two types of threads: LISTENERs and SENDERs
    //Listeners will listen for any packet on port 69
    //Senders are new threads that create new sockets to transmit data back to the ErrorSimulator
    public synchronized void run()
    {
        rxData = new byte[DATA_SIZE];

        //If thread is LISTENER
        while(isListener)
        {
            boolean receivedPkt = false;
            DatagramPacket rxPacket = new DatagramPacket(rxData, rxData.length);

            //Used to ensure that the server receives a real packet
            while (!receivedPkt) {
                try {
                    Thread.sleep(1000);
                    socket.receive(rxPacket);

                    if (rxPacket == (new DatagramPacket(rxData, rxData.length)))
                        receivedPkt = false;
                    else
                        receivedPkt = true;

                } catch (Exception e) {
                	System.out.println("Server has timed out: terminating...");
                    System.exit(1);
                }
            }
            rxPacket = resizePacket(rxPacket);
            outputText(rxPacket, direction.IN);

            //Ensures inbound packets are formatted correctly
            //Once validated, creates a new SENDER thread and runs it
            if (validatePacket(rxPacket)) {

                try {
                    Thread sendReply = new Thread(new Server(rxPacket), "SENDER");
                    sendReply.start();
                }
                catch (Exception e)
                {
                    System.out.println (e.getStackTrace());
                }

            }
            else {
                outputText(rxPacket, direction.IN);
                //throw new ValidationException("Packet is not valid.");
            }

            if (quitSignal)
                System.exit(1);
        }

        //If the thread is a SENDER, run this code
        if (!isListener)
        {
            //All sending logic is in sendReply()
            sendReply(this.packet, this.socket);

            //Close temp socket and thread

            //Code used for user input
            //Currently only accepts "q" to close the server process
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            String line = "";

            System.out.print("Would you like to (q)uit? ");
            while (line.equalsIgnoreCase("q") == false) {
                try { line = in.readLine(); }
                catch (Exception e) {}
            }
            if (line.equalsIgnoreCase("q") == true)
                quitSignal = true;

            try { in.close(); } catch (Exception e) {}

            socket.close();
            Thread.currentThread().interrupt();

            if (quitSignal)
                System.exit(1);
        }
    }

    //Waits to receive packet from ErrorSim.java
    //Upon receipt, validates the packet, creates a new thread and temp socket, creates a response packet and sends it back to ErrorSim.java
    public synchronized static void main(String args[]) throws Exception
    {
        //Creates a thread that listens to port 69
        Thread listener = new Thread(new Server(ERRORSIM_PORT), "LISTENER");
        System.out.println("TFTP Server is running.\n");

        listener.start();
    }

    //A function that reads the text in each packet and displays its contents in ASCII and BYTES
    //Future iterations will update this function for QUIET/VERBOSE options
    public synchronized static void outputText(DatagramPacket packet, direction dir)
    {
        byte[] data = packet.getData();

        if (dir == direction.IN)
            System.out.println("--Inbound Packet Data from ErrorSim--");
        else if (dir == direction.OUT)
            System.out.println("--Outbound Packet Data to ErrorSim--");

        //PACKET TYPE OUTPUT
        if (data[0] == 0 && data[1] == 1)
            System.out.println("OPCODE = READ [0x01]");
        if (data[0] == 0 && data[1] == 2)
            System.out.println("OPCODE = WRITE [0x02]");
        if (data[0] == 0 && data[1] ==  3)
            System.out.println("OPCODE = DATA [0x03]");
        if (data[0] == 0 && data[1] ==  4)
            System.out.println("OPCODE = ACK [0x04]");
        if (data[0] == 0 && data[1] ==  5)
            System.out.println("OPCODE = ERROR [0x05]");

        //MESSAGE OUTPUT
        String ascii = new String(data, Charset.forName("UTF-8"));
        ascii = ascii.substring(4, ascii.length());
        if (ascii.length() > 0) {
            System.out.println("MSG LENGTH = " + ascii.length());
            System.out.println("MESSAGE = ");
            System.out.println(ascii);
        }
        else
            System.out.println("MESSAGE = NULL");

        //BYTE OUTPUT
        //Confirm output with - https://www.branah.com/ascii-converter
        System.out.println("BYTES = ");
        for (int j = 0; j < data.length; j++) {
            System.out.print(data[j]);
            if (j % 1 == 0 && j != 0)
                System.out.print(" ");
            if (j == 0)
                System.out.print(" ");
        }
        System.out.println("\n-----------------------");
    }
    
    //A function that verifies that a packet is a valid TFTP packet
    public synchronized static boolean validatePacket(DatagramPacket packet)
    {
        //BYTES [9-126] WILL COUNT AS VALID CHARACTERS
        //BYTE 10 == LF == Line Feed
        //ANYTHING ELSE IS "INVALID"
        boolean isValid = false;
        boolean isRequest = false;
        boolean isError = false;
        boolean filenameIsValid = true;
        boolean modeIsValid = true;

        //Counts the number of 0x0 in the packet (size of Vector) and their indexes in the packet (index values in Vector)
        Vector<Integer> hasZero = new Vector<Integer>();

        //If Packet is a RRQ or WRQ
        if (packet.getData()[0] == 0 && (packet.getData()[1] == 1 || packet.getData()[1] == 2)) {
            isValid = true;
            isRequest = true;
        }

        if (packet.getData()[0] == 0 && packet.getData()[1] == 5) {
            isValid = true;
            isError = true;
        }

        if (isError)
        {
            System.out.println("Packet is Error");
        }

        if (isRequest)
        {
            System.out.println("Packet is Request");
        }

        if (isValid)
        {
            for (int i = 2; i < packet.getLength(); i++)
            {
                if (packet.getData()[i] == 0) {
                    hasZero.addElement(i);
                }
            }

            if (hasZero.size() >= 2)
            {
                for (int i = 2; i < hasZero.elementAt(0); i++)
                {
                    if ((packet.getData()[i] <= 8 && packet.getData()[i] != 0) || (packet.getData()[i] >= 127))
                        filenameIsValid = false;
                }

                for (int i = hasZero.elementAt(0) + 1; i < hasZero.elementAt(1); i++)
                {
                    if ((packet.getData()[i] <= 8 && packet.getData()[i] != 0) || (packet.getData()[i] >= 127))
                        modeIsValid = false;
                }
            }
            else
                isValid = false;

            if (isValid && modeIsValid && filenameIsValid)
                return true;
            else
                return false;
        }

        return isValid;
    }

    public synchronized static String checkError(DatagramPacket packet)
    {
        String errorMessage = "No Error";
        String msg0 = "Not defined, see error message (if any).";
        String msg1 = "File not found.";                            // -- Iteration 2
        String msg2 = "Access violation.";                          // -- Iteration 2
        String msg3 = "Disk full or allocation exceeded.";          // -- Iteration 2
        String msg4 = "Illegal TFTP operation.";
        String msg5 = "Unknown transfer ID.";
        String msg6 = "File already exists.";                       // -- Iteration 2
        String msg7 = "No such user.";

        byte[] data = packet.getData();

        //Can do error 1 (file not found)
        //Can do error 2 (access violation)
        if (data[0] == 0 && data[1] == 1)
        {
            File f = new File("./" + getFilename(packet));
            if(f.exists() && !f.isDirectory()) {
                //System.out.println("File Exists!");
            }
            else
            {
                System.out.println(msg1); //File not found.
                errorMessage = msg1;
            }
        }

        //Can do error 2 (access violation)
        //Can do error 3 (Disk full or allocation exceeded.)
        //Can do error 7 (file already exists)
        if (data[0] == 0 && data[1] == 2)
        {

        }

        //Not sure what to check here
        if (data[0] == 0 && data[1] == 3)
        {

        }

        return errorMessage;

    }
    
    //A function that sends a initiates a RRQ or WRQ based a the received packet
    //packet = packet received from ErrorSim
    //socket = socket ErrorSim used to send the packet to Server
    public synchronized void sendReply(DatagramPacket packet, DatagramSocket socket)
    {
        byte[] data = packet.getData();
        byte[] response = new byte[4];
        String filename = getFilename(packet);

        //Extract ErrorSim socket's port from packet
        InetSocketAddress temp_add = (InetSocketAddress)packet.getSocketAddress();
        int port = temp_add.getPort();
        DatagramPacket txPacket = new DatagramPacket(response, response.length, packet.getAddress(), port);

        if (data[0] == 0 && data[1] == 1)       //IF PACKET IS RRQ
        {
            try {
                if (checkError(packet) == "No Error")
                    readRequest(port, filename);
                else
                {
                    sendError(packet);
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        else if (data[0] == 0 && data[1] == 2)  //IF PACKET IS WRQ
        {
            try {
                if (checkError(packet) == "No Error")
                    writeRequest(port, filename);
                else
                {
                    sendError(packet);
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        else {                                //SEND 0x0000
            response[0] = 0;
            response[1] = 0;
            response[2] = 0;
            response[3] = 0;
        }
    }

    public void sendError(DatagramPacket packet) throws Exception
    {
        DatagramSocket writeSocket = new DatagramSocket();
        String error = checkError(packet);

        byte[] sendData = new byte[DATA_SIZE];
        sendData[0]=0;
        sendData[1]=5;
        sendData[2]=0;

        if (error == "File not found.")
            sendData[3]=1;

        //Error Code
        byte[] temp = error.getBytes();
        int j = 4; //byte placeholder for header

        for (int i = 0; i < error.getBytes().length; i++) {
            sendData[j++] = temp[i];
        }

        //Add 0x0
        sendData[j++] = 0;

        //Resizing packet here for now
        byte[] sendSmallData = new byte[j];
        for (int i = 0; i < j; i++)
        {
            sendSmallData[i] = sendData[i];
        }

        //send DATA packet to client
        DatagramPacket txPacket = new DatagramPacket(sendSmallData,sendSmallData.length,packet.getAddress(), getPort(packet));
        txPacket = resizePacket(txPacket);
        writeSocket.send(txPacket);
        outputText(txPacket, direction.OUT);

        System.out.println("ERROR Complete: TERMINATING SOCKET");
        writeSocket.close();
    }
    
    /*
	    WRQ FLOW
	    Client -> WRQ -> Server
	    Server -> ACK BLK 0 -> Client
	    Client -> DATA BLK 1 -> Server
	    Server -> ACK BLK 1 -> Client
	    Repeats until Client sends last DATA pkt, Server sends a final ACK
    */
    
    //A function that implements the WRQ of a TFTP server, takes as input the client's port and file name of requested file
    public synchronized void writeRequest(int port,String filename) throws Exception {
        boolean isValidFile = true;
        Vector<byte[]> fileVector = new Vector<byte[]>();
        DatagramSocket writeSocket = new DatagramSocket();//new socket for WRQ
        byte[] sendData = new byte[]{0,4,0,0};//block 0 ACK packet

        while(true) {//Loop to send ACK and receive DATA until DATA<512 bytes
            //send ACK packet to Client
            DatagramPacket txPacket = new DatagramPacket(sendData,sendData.length,InetAddress.getLocalHost(),port);
            writeSocket.send(txPacket);
            outputText(txPacket, direction.OUT);   
            
            if(isValidFile) {
	            //receive DATA packet from Client
	            byte[] receiveData = new byte[DATA_SIZE];
	            DatagramPacket rxPacket = new DatagramPacket(receiveData, receiveData.length);
	            writeSocket.receive(rxPacket);
	            rxPacket = resizePacket(rxPacket);
	            outputText(rxPacket, direction.IN);
	
	            byte[] buffer = new byte[rxPacket.getLength() - 4];
	
	            for (int i = 4; i < rxPacket.getLength(); i++)
	            {
	                buffer[i - 4] = rxPacket.getData()[i];
	            }
	            
	            fileVector.addElement(buffer);
	
	            //set Data of next ACK packet with received block #
	            sendData = new byte[]{0,4,receiveData[2],receiveData[3]};
	            //stop if received DATA packet is less then 512 bytes
	            if (rxPacket.getLength()<DATA_SIZE) isValidFile = false;
            }
            else break;
        }
        saveFile(fileVector,filename);
        System.out.println("WRQ Complete: TERMINATING SOCKET");
        writeSocket.close();
    }

    /*
       RRQ FLOW
       Client -> RRQ -> Server
       Server -> DATA BLK 1 -> Client
       Client -> ACK BLK 1 -> Server
       Repeats until Server sends last DATA pkt, Client sends a final ACK
     */

    //A function that implements the RRQ of a TFTP server, takes as input the client's port and file name of requested file
    public synchronized void readRequest(int port,String filename) throws Exception {

        int blockNum=1;
        DatagramSocket readSocket = new DatagramSocket();//new socket for RRQ

        Path path = Paths.get("./" + filename);
        byte[] file = Files.readAllBytes(path);

        int totalBlocksRequired = (file.length / 512) + 1;
        int remainderLastBlock = (file.length % 512);
        boolean onLastBlock = false;

        int j=0;

        while(!onLastBlock) {//Loop to send DATA and receive ACK until DATA<512 bytes
            byte[] blockNumBytes= blockNumToBytes(blockNum++);
            byte[] sendData = new byte[DATA_SIZE];
            sendData[0]=0;
            sendData[1]=3;
            sendData[2]=blockNumBytes[0];
            sendData[3]=blockNumBytes[1];

            if (totalBlocksRequired == 1 || file.length - j < 512)
                onLastBlock = true;

            if (!onLastBlock) {
                for (int i = 4; i < DATA_SIZE && j < file.length; i++) {//4-515 are for 512 bytes of data
                    sendData[i] = file[j++];
                }
            }
            else
            {
                sendData = new byte[remainderLastBlock + 4];
                sendData[0]=0;
                sendData[1]=3;
                sendData[2]=blockNumBytes[0];
                sendData[3]=blockNumBytes[1];
                for (int i = 4; i < remainderLastBlock + 4; i++) {//4-515 are for 512 bytes of data
                    sendData[i] = file[j++];
                }
            }

            //send DATA packet to client
            DatagramPacket txPacket = new DatagramPacket(sendData,sendData.length,InetAddress.getLocalHost(),port);
            txPacket = resizePacket(txPacket);
            readSocket.send(txPacket);
            outputText(txPacket, direction.OUT);

            byte[] receiveData = new byte[4];
            DatagramPacket rxPacket = new DatagramPacket(receiveData, receiveData.length);
            readSocket.receive(rxPacket);
            rxPacket = resizePacket(rxPacket);
            outputText(rxPacket, direction.IN);
        }
        System.out.println("RRQ Complete: TERMINATING SOCKET");
        readSocket.close();
    }
    
    //A function that writes a file with WRQ attached to its filename, takes a byte array and a filename, 
    public static void saveFile(Vector<byte[]> receivedFile, String filename)
    {
        byte[] tempArray;
        int charCount = 0;

        for (int i = 0; i < receivedFile.size(); i++)
        {
            charCount += receivedFile.elementAt(i).length;
        }

        tempArray = new byte[charCount];

        String path = "./WRQ";
        String outputName = " "+filename;

        int tempCount = 0;

        for (byte[] bytes : receivedFile) {
            for (byte b : bytes) {
                tempArray[tempCount] = b;
                tempCount++;
            }
        }

        //Can do error checks here

        try (FileOutputStream fileOuputStream = new FileOutputStream(path + outputName)) {
            fileOuputStream.write(tempArray);
            fileOuputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    
    //A function to convert an int into an array of 2 bytes
    public static byte[] blockNumToBytes(int blockNum) {
        int b1 = blockNum / 256;
        int b2 = blockNum % 256;
        return new byte[] {(byte)b1,(byte)b2};
    }

    //Packets are initialized with 100 Bytes of memory but don't actually use all the space
    //This function resizes a packet based on the length of its payload, conserving space
    public synchronized static DatagramPacket resizePacket(DatagramPacket packet)
    {
        InetSocketAddress temp_add = (InetSocketAddress)packet.getSocketAddress();
        int port = temp_add.getPort();
        InetAddress ip = packet.getAddress();
        int length = packet.getLength();

        byte[] tempData = new byte[length];

        for (int i = 0; i < length; i++)
        {
            tempData[i] = packet.getData()[i];
        }

        DatagramPacket resizedPacket = new DatagramPacket(tempData, tempData.length, ip, port);
        return resizedPacket;
    }

    public static String getFilename(DatagramPacket packet)
    {
        byte[] data = packet.getData();

        //Extract filename from packet
        int i=2;
        while(data[i]!=0) {
            i++;
        }
        String filename = new String(Arrays.copyOfRange(data, 2, i) , Charset.forName("UTF-8"));

        return filename;
    }

    //Returns the a packet's port number
    public static int getPort(DatagramPacket p)
    {
        InetSocketAddress temp_add = (InetSocketAddress) p.getSocketAddress();
        int port = temp_add.getPort();
        return port;
    }
}
