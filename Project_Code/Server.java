/*
  FILENAME - Server.java
  ASSIGNMENT - Final Project - SYSC 3303
  AUTHOR - GROUP 3 - W18
  DETAILS - A program that will receives DatagramPackets from ErrorSim
          - Based on the packet type, sends a response to ErrorSim
*/

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.Vector;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;

class Server extends CommonMethods implements Runnable
{

    //private static final int ERRORSIM_PORT = 69;
    private static final int ERRORSIM_PORT = 9969;
    private static final int DATA_SIZE = 516;

    private DatagramSocket socket;
    private DatagramPacket packet;
    private boolean isListener = false;
    private boolean quitSignal = false;
    private boolean verboseOutput = false;
    private static String[] error = new String[2]; // error[0] = errorMessage error[1] = errorCode

    private String pathname;

    //Used to determine if a packet is inbound or outbound when displaying its text
    //public enum direction {
    //    IN, OUT;
    //}

    //Overloaded Constructor
    //Used to instantiate a LISTENER
    public Server(int port) throws Exception
    {
        this.pathname = System.getProperty("user.dir") + "/ServerFiles/";
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
    public Server(DatagramPacket packet, boolean v) throws Exception
    {
        this.pathname = System.getProperty("user.dir") + "/ServerFiles/";
        this.socket = new DatagramSocket();
        this.packet = packet;
        this.verboseOutput = v;
    }

    //Essentially a pseudo-main method that runs all logic for the threads
    //Two types of threads: LISTENERs and SENDERs
    //Listeners will listen for any packet on port 69
    //Senders are new threads that create new sockets to transmit data back to the ErrorSimulator
    public synchronized void run()
    {
        byte[] rxData = new byte[DATA_SIZE];

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

            if (!isOOB(rxPacket)) {
                outputText(rxPacket, direction.IN, endhost.ERRORSIM, verboseOutput);
            }
            else
            {
                if (rxPacket.getData()[0] == 9 && rxPacket.getData()[1] == 9 && rxPacket.getData()[2] == 0)
                    verboseOutput = false;
                if (rxPacket.getData()[0] == 9 && rxPacket.getData()[1] == 9 && rxPacket.getData()[2] == 1)
                    verboseOutput = true;
            }

            //Ensures inbound packets are formatted correctly
            //Once validated, creates a new SENDER thread and runs it
            if (validatePacket(rxPacket) && !isOOB(rxPacket)) {

                try {
                    Thread sendReply = new Thread(new Server(rxPacket, verboseOutput), "SENDER");
                    sendReply.start();
                }
                catch (Exception e)
                {
                    System.out.println (e.getStackTrace());
                }

            }
            else {
                if (!isOOB(rxPacket)) {
                    outputText(rxPacket, direction.IN, endhost.ERRORSIM, verboseOutput);
                }
                else {

                    if (rxPacket.getData()[0] == 9 && rxPacket.getData()[1] == 9 && rxPacket.getData()[2] == 0)
                        verboseOutput = false;
                    if (rxPacket.getData()[0] == 9 && rxPacket.getData()[1] == 9 && rxPacket.getData()[2] == 1)
                        verboseOutput = true;
                }
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
            //System.out.println("Packet is Error");
        }

        if (isRequest)
        {
            //System.out.println("Packet is Request");
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
    	String[] msg = new String[8];
        msg[0] = "Not defined, see error message (if any).";
        msg[1] = "File not found.";                            // -- Iteration 2
        msg[2] = "Access violation.";                          // -- Iteration 2
        msg[3] = "Disk full or allocation exceeded.";          // -- Iteration 2
        msg[4] = "Illegal TFTP operation.";
        msg[5] = "Unknown transfer ID.";
        msg[6] = "File already exists.";                       // -- Iteration 2
        msg[7] = "No such user.";

        byte[] data = packet.getData();
        
        File f;
        
        //Can do error 1 (file not found)
        //Can do error 2 (access violation)
        if (data[0] == 0 && data[1] == 1)//RRQ
        {
            f = new File("./" + getFilename(packet));
            if(f.exists() && !f.isDirectory()) {
                //System.out.println("File Exists!");
            }
            
            else if (f.canRead()==false) {
            	error[0] = msg[2];
            	error[1] = "2";   
            	System.out.println(msg[2]);   //access violation.
            }

            else
            {
                System.out.println(msg[1]); //File not found.
                errorMessage = msg[1];
            }
        }
        
<<<<<<< HEAD
=======
       

>>>>>>> 4c2537641e0ea3d788ade88bc93ed1376f3829ec
        //Can do error 2 (access violation)
        //Can do error 3 (Disk full or allocation exceeded.)
        //Can do error 6 (file already exists)
        if (data[0] == 0 && data[1] == 2)//WRQ
        {
        	f = new File("./ServerFiles/WRQ/" + getFilename(packet));
            if(f.exists() && !f.isDirectory()) {
            	System.out.println(msg[6]); //File already exists.
                errorMessage = msg[6];
            }
        }

        //Not sure what to check here
        if (data[0] == 0 && data[1] == 3)//DATA
        {

        }
        
        //Error 3 Disk full or allocation exceeded
        File path = new File("./ServerFiles/WRQ");
        long diskSpace = path.getFreeSpace();//returns free space on Server in bytes
        if(diskSpace==0 || diskSpace < 100) {//100 is placeholder for vector size()
        	System.out.println(msg[3]);
        	errorMessage = msg[3];
        }
        
        //Not sure what to check here

        return errorMessage;

    }
    
    //A function that sends and initiates a RRQ or WRQ based on the received packet
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

        if (data[0] == 0 && data[1] == 1)       //IF PACKET IS RRQ
        {
            try {
                    readRequest(port, filename);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        else if (data[0] == 0 && data[1] == 2)  //IF PACKET IS WRQ
        {
            try {
                    writeRequest(port, filename);
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
        sendData[3]=errorMap.get(error).byteValue(); //Map the error code to the corresponding number

        //if (error == "File not found.")
        //    sendData[3]=1;

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
        outputText(txPacket, direction.OUT, endhost.ERRORSIM, verboseOutput);

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
        
        byte[] sendData = new byte[]{0,4,0,0};//block 0 ACK packet
        
        if (checkError(packet) != "No Error") {//initial WRQ file error check
        	sendError(packet);
        	socket.close();//lazy quit for now
        	System.exit(0);
        }
        
        while(true) {//Loop to send ACK and receive DATA until DATA<512 bytes
            //send ACK packet to Client
        	
            DatagramPacket txPacket = new DatagramPacket(sendData,sendData.length,InetAddress.getLocalHost(),port);
            socket.send(txPacket);
            outputText(txPacket, direction.OUT, endhost.ERRORSIM, verboseOutput);
            
            
            
            if(isValidFile) {
	            //receive DATA packet from Client
	            byte[] receiveData = new byte[DATA_SIZE];
	            DatagramPacket rxPacket = new DatagramPacket(receiveData, receiveData.length);
	            socket.receive(rxPacket);
	            rxPacket = resizePacket(rxPacket);
	            outputText(rxPacket, direction.IN, endhost.ERRORSIM, verboseOutput);
	
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
	            
	            if (checkError(packet) != "No Error") {//check received data
	            	sendError(packet);
	            	socket.close();//lazy quit for now
	            	System.exit(0);
	            }
	            
            }
            else break;
        }
        saveFile(fileVector,filename);
        System.out.println("WRQ Complete: TERMINATING SOCKET");
        socket.close();
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
        
        if (checkError(packet) != "No Error") {//initial RRQ file error check
        	sendError(packet);
        	socket.close();//lazy quit for now
        	System.exit(0);
        }
        
        //Path path = Paths.get("./" + filename);
        Path path = Paths.get(pathname + filename);
        byte[] file = Files.readAllBytes(path);

        int totalBlocksRequired = (file.length / 512) + 1;
        int remainderLastBlock = (file.length % 512);
        boolean onLastBlock = false;

        int j=0;
        
        while(!onLastBlock) {//Loop to send DATA and receive ACK until DATA<512 bytes
        	
        	if (checkError(packet) != "No Error") {
        		sendError(packet);
        	}
        	
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
            socket.send(txPacket);
            outputText(txPacket, direction.OUT, endhost.ERRORSIM, verboseOutput);

            byte[] receiveData = new byte[4];
            DatagramPacket rxPacket = new DatagramPacket(receiveData, receiveData.length);
            socket.receive(rxPacket);
            rxPacket = resizePacket(rxPacket);
            outputText(rxPacket, direction.IN, endhost.ERRORSIM, verboseOutput);
        }
        System.out.println("RRQ Complete: TERMINATING SOCKET");
        socket.close();
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

        //String path = "./WRQ";
        String path = "./ServerFiles/WRQ/";
        String outputName = filename;

        int tempCount = 0;

        for (byte[] bytes : receivedFile) {
            for (byte b : bytes) {
                tempArray[tempCount] = b;
                tempCount++;
            }
        }

        //Can do error checks here

        try (FileOutputStream fileOutputStream = new FileOutputStream(path + outputName)) {
            fileOutputStream.write(tempArray);
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
