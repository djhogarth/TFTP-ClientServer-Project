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
import java.io.ByteArrayOutputStream;
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
    private InetSocketAddress expectedTID = null;
	
    private String pathname;
    private int fileSize;//file size of written file

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
        this.expectedTID = (InetSocketAddress) packet.getSocketAddress();
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
    public synchronized static boolean validatePacket(DatagramPacket packet){
        //BYTES [9-126] WILL COUNT AS VALID CHARACTERS
        //BYTE 10 == LF == Line Feed
        //ANYTHING ELSE IS "INVALID"
        boolean isValid = true;
        boolean isRequest = false;
        boolean isError = false;
        boolean filenameIsValid = true;
        boolean modeIsValid = true;

        //Counts the number of 0x0 in the packet (size of Vector) and their indexes in the packet (index values in Vector)
        Vector<Integer> hasZero = new Vector<Integer>();
        
        //check opcode
        if (packet.getData()[0] == 0 && (packet.getData()[1] == 1 || packet.getData()[1] == 2)) {
            isRequest = true;
        }else if (packet.getData()[0] == 0 && packet.getData()[1] == 5) {
            isError = true;
        }else if(packet.getData()[0] != 0 || (packet.getData()[1] < 1 || packet.getData()[1] > 5)) {
        	isValid = false;
        }
		 
        //check errorcode
        if (isError) {
        	int length = packet.getData().length;
        	if (packet.getData()[2] != 0 || (packet.getData()[3] < 1 && packet.getData()[3] > 7)) {
        		isValid = false;
    		}
        	if(!(packet.getData()[length-1] == 0)) {
        		isValid = false;
        	}
        }
        
        
        //check request
        if (isRequest){
        	ByteArrayOutputStream mode = new ByteArrayOutputStream(); // to store mode 
        	int length = packet.getData().length; // 
        	byte [] data = packet.getData();
        	int i;
        	
        	//Go until first 0 seperator
        	for (i = 2; data[i] != 0 && i < length; ++i) {
    			// we could get fileName here
    		}
        	
        	//Go until second 0 seperator
        	for (i += 1; data[i] != 0 && i < length; ++i) {
    			mode.write(data[i]); 
    		}
        	
        	//check if mode is valid
        	if (!(mode.toString().toLowerCase().equals("netascii") || mode.toString().toLowerCase().equals("octet"))) {
        		isValid=false;
        		modeIsValid = false;
    		}
        	if(!(packet.getData()[length-1] == 0)) {
        		isValid = false;
        	}
        }
       
        // doesnt work
        /*if (isValid){
            for (int i = 2; i < packet.getLength(); i++){
                if (packet.getData()[i] == 0) {
                    hasZero.addElement(i);
                }
            }
            if (hasZero.size() >= 2){
                for (int i = 2; i < hasZero.elementAt(0); i++){
                    if ((packet.getData()[i] <= 8 && packet.getData()[i] != 0) || (packet.getData()[i] >= 127))
                        filenameIsValid = false;
                }

                for (int i = hasZero.elementAt(0) + 1; i < hasZero.elementAt(1); i++){
                    if ((packet.getData()[i] <= 8 && packet.getData()[i] != 0) || (packet.getData()[i] >= 127))
                        modeIsValid = false;
                }
            }
            else
                isValid = false;
            if (isValid && modeIsValid && filenameIsValid && isRequest && isError)
                return true;
            else
                return false;
        }*/

        return isValid;
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

        String path = "./ServerFiles/";
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
    
    //Essentially a pseudo-main method that runs all logic for the threads
    //Two types of threads: LISTENERs and SENDERs
    //Listeners will listen for any packet on port 69
    //Senders are new threads that create new sockets to transmit data back to the ErrorSimulator
    public synchronized void run()
    {
        byte[] rxData = new byte[DATA_SIZE];
        DatagramPacket lastPkReceived = new DatagramPacket(rxData, 100);
        DatagramPacket lastRqReceived = new DatagramPacket(rxData, 100);
        lastPkReceived.setData(new byte[2]);
        lastRqReceived.setData(new byte[2]);

        //If thread is LISTENER
        while(isListener)
        {
            boolean receivedPkt = false;
            DatagramPacket rxPacket = new DatagramPacket(rxData, rxData.length);

            //Used to ensure that the server receives a real packet
            while (!receivedPkt) {
                try {
                    Thread.sleep(500);
                    socket.receive(rxPacket);

                    //EXPERIMENTAL CODE - NOT YET FINISHED
                    if (rxPacket == (new DatagramPacket(rxData, rxData.length)) && areTheSame(rxPacket, lastRqReceived)) { // || rxPacket.getData() == lastPkReceived.getData() || lastRqReceived.getData() == rxPacket.getData()) {
                        receivedPkt = false;
                        System.out.println("They were the same!!!");
                    }
                    else {
                        receivedPkt = true;
                        lastPkReceived = deepCopy(rxPacket);

                        //EXPERIMENTAL CODE - NOT YET FINISHED
                        //If the last received packet is a REQUEST, we "copy" the data from rxPacket to lastRqReceived
                        //This is the only way I could "deep copy" a Datagram Packet
                        if (rxPacket.getData()[1] == 1 || rxPacket.getData()[1] == 2) {
                            //lastRqReceived.setData(rxPacket.getData());
                            lastRqReceived = deepCopy(rxPacket);
                        }

                    }

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
            //creates a new SENDER thread and runs it
            if (!isOOB(rxPacket)) {

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
            }

            if (quitSignal)
                System.exit(1);
        }

        //If the thread is a SENDER, run this code
        if (!isListener)
        {
            if (verboseOutput)
                System.out.println("*** NEW SENDER THREAD! ***");

            //All sending logic is in sendReply()
            sendReply(this.packet, this.socket);

            //Close temp socket and thread

            //Code used for user input
            //Currently only accepts "q" to close the server process
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            String line = "";

            System.out.println("Would you like to (q)uit? ");
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
    
    //Function takes a packet and returns a corresponding error message
    public synchronized String checkError(DatagramPacket packet)
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
        File path = new File("./ServerFiles/");
        long diskSpace = path.getUsableSpace();//returns free space on Server in bytes

        //Can do error 1 (file not found)
        //Can do error 2 (access violation)
        if (data[0] == 0 && data[1] == 1)//RRQ
        {
            f = new File("./ServerFiles/" + getFilename(packet));
            //System.out.print("./ServerFiles/" + getFilename(packet));
            if(f.exists() && !f.isDirectory()) {
                //System.out.println("File Exists!");
            	if (f.canRead()==false) {
	            	errorMessage = msg[2];
	            	//System.out.println(msg[2]);   //access violation.
	            }
            }
            else
            {
                //System.out.println(msg[1]); //File not found.
                errorMessage = msg[1];
            }
        }

        //Can do error 2 (access violation)
        //Can do error 3 (Disk full or allocation exceeded.)
        //Can do error 6 (file already exists)
        if (data[0] == 0 && data[1] == 2)//WRQ
        {
        	f = new File("./ServerFiles/" + getFilename(packet));
            if(f.exists() && !f.isDirectory()) {
            	//System.out.println(msg[6]); //File already exists.
                errorMessage = msg[6];
            }

            f = new File("./ServerFiles/");
            if (!f.canWrite()) {
            	errorMessage = msg[2];
            	//System.out.println(msg[2]);   //access violation.
            }

		    if(diskSpace==0) {
		    	//System.out.println(msg[3]); //Disk full or allocation exceeded
		    	errorMessage = msg[3];
		    }
        }

        //Not sure what to check here
        if (data[0] == 0 && data[1] == 3)//DATA
        {

		    if(diskSpace==0 || diskSpace < fileSize) {//size of file being written
		    	//System.out.println(msg[3]); //Disk full or allocation exceeded
		    	errorMessage = msg[3];
		    }
        }
		
        //Error 05 Unknown TID
		InetSocketAddress packetTID = (InetSocketAddress) packet.getSocketAddress();
        if (this.expectedTID != null && !this.expectedTID.equals(packetTID)) {
        	errorMessage = msg[5];
        }
        
        if(!validatePacket(packet)) {
        	errorMessage = msg[4];
        }

        return errorMessage;
    }
    
    //A function that sends and initiates a RRQ or WRQ based on the received packet
    //packet = packet received from ErrorSim
    //socket = socket ErrorSim used to send the packet to Server
    public synchronized void sendReply(DatagramPacket packet, DatagramSocket socket)
    {
        byte[] data = packet.getData();
        String filename = "";
        if(validatePacket(packet)){
        	filename = getFilename(packet);
        }
        //Extract ErrorSim socket's port from packet
        InetSocketAddress temp_add = (InetSocketAddress)packet.getSocketAddress();
        int port = temp_add.getPort();

        if (data[0] == 0 && data[1] == 1)       //IF PACKET IS RRQ
        {
            try {
                readRequest(port, filename);
            } catch (Exception e) {
            	//e.printStackTrace();  
            }
        }
        else  //IF PACKET IS WRQ or anything else
        {
            try {
                writeRequest(port, filename);
            } catch (Exception e) {
            	//e.printStackTrace();
            }
        }
    }
    
    /*
	    WRQ FLOW
	    Client -> WRQ -> Server
	    Server -> ACK BLK 0 -> Client
	    Client -> DATA BLK 1 -> Server
	    Server -> ACK BLK 1 -> Client
	    Repeats until Client sends last DATA pkt, Server sends a final ACK
    */
    
  //Takes a packet with a file error and sends an ERROR packet back
    public void sendError(DatagramPacket packet) throws Exception
    {
        String error = checkError(packet);

        byte[] sendData = new byte[DATA_SIZE];
        sendData[0]=0;
        sendData[1]=5;
        sendData[2]=0;
        sendData[3]=errorMap.get(error).byteValue(); //Map the error code to the corresponding number

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
        socket.send(txPacket);
        outputText(txPacket, direction.OUT, endhost.ERRORSIM, verboseOutput);
        
        if (!error.equals("Unknown transfer ID.")) { //Do not close socket if error 5 (Unknown Transfer ID) occurs
        	System.out.println("ERROR Complete: TERMINATING SOCKET");
        	socket.close();
        	//System.exit(0);//shutdown after error
        }
    }

    /*
       RRQ FLOW
       Client -> RRQ -> Server
       Server -> DATA BLK 1 -> Client
       Client -> ACK BLK 1 -> Server
       Repeats until Server sends last DATA pkt, Client sends a final ACK
     */

    //A function that implements the WRQ of a TFTP server, takes as input the client's port and file name of requested file
    public synchronized void writeRequest(int port,String filename) throws Exception {
        boolean isValidFile = true;
        Vector<byte[]> fileVector = new Vector<byte[]>();
        
        DatagramPacket txPacket = null;
        byte[] sendData = new byte[]{0,4,0,0};//block 0 ACK packet
        boolean gotResponse = false;
        boolean isUnknown = false;//placeholder for not sending ack when receiving unknown TID error
        int dataCounter = 1;
        int numResentPkt = 0;

        fileSize=fileVector.size();
        String errorMsg = checkError(packet);
        if (!errorMsg.equals("No Error")) {//initial WRQ file error check
        	sendError(packet);
        	return;
        }

        while(true) {//Loop to send ACK and receive DATA until DATA<512 bytes
        
        	//send ACK packet to Client
        	if(!isUnknown) {
	            txPacket = new DatagramPacket(sendData,sendData.length,InetAddress.getLocalHost(),port);
	            socket.setSoTimeout(10000);
	            socket.send(txPacket);
	            outputText(txPacket, direction.OUT, endhost.ERRORSIM, verboseOutput);
        	}
        	
            if(isValidFile) {
        		//receive DATA packet from Client
	            byte[] receiveData = new byte[DATA_SIZE];
	            DatagramPacket rxPacket = new DatagramPacket(receiveData, receiveData.length);
                gotResponse = false;
            
                while (!gotResponse) {// Loop receive from server until either Valid DATA or ERROR is received
	                isUnknown = false;
                    socket.setSoTimeout(5000);
                    try {
                        socket.receive(rxPacket);
                        receiveData = rxPacket.getData();
                        if(receiveData[1]==5 || (receiveData[1] == 3 && (receiveData[2] == blockNumToBytes(dataCounter)[0] && receiveData[3] == blockNumToBytes(dataCounter)[1]) ) 
                        		|| !checkError(rxPacket).equals("No Error")){
    						gotResponse = true;
    						dataCounter++;
    					}
                    }
                    catch (SocketTimeoutException ste)
                    {
                        if (numResentPkt >= 3) {
                            System.out.println("\n*** No response from Client. ***\n");
                            return;
                        }

                        numResentPkt++;
                        //Don't resend acks
                        //System.out.println("\n*** No response received from Client... Re-sending packet. ***\n");
                        //socket.send(txPacket);
                        //outputText(txPacket, direction.OUT, endhost.ERRORSIM, verboseOutput);
                        socket.setSoTimeout(0); //infinite socket wait time
                        gotResponse = false;
                    }
                }


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

	            fileSize=fileVector.size();
	            errorMsg = checkError(rxPacket);
	            if (!errorMsg.equals("No Error")) {//check received packet
	            	sendError(rxPacket);
	            	if (errorMsg.equals("Unknown transfer ID.")) {
	            		isUnknown = true;
	            		isValidFile=true;
	            		dataCounter--;
	            	}else {
	            		return;
	            	}
	            }
	            if (receiveData[1]==5)//STOP if received ERROR
	            	return;
            }
            else break;
        }
        saveFile(fileVector,filename);
        System.out.println("WRQ Complete: TERMINATING SOCKET");
        socket.close();
    }
    
    //A function that implements the RRQ of a TFTP server, takes as input the client's port and file name of requested file
    public synchronized void readRequest(int port,String filename) throws Exception {

        int blockNum=1;

        String errorMsg = checkError(packet);
        if (checkError(packet) != "No Error") {//initial RRQ file error check
        	sendError(packet);
        	return;
        }

        Path path = Paths.get(pathname + filename);
        byte[] file = Files.readAllBytes(path);

        int totalBlocksRequired = (file.length / 512) + 1;
        int remainderLastBlock = (file.length % 512);
        boolean onLastBlock = false;
        int numResentPkt = 0;

        int j=0;

        boolean gotResponse = false;

        while(!onLastBlock) {//Loop to send DATA and receive ACK until DATA<512 bytes
            gotResponse = false;
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
            socket.setSoTimeout(5000);
            socket.send(txPacket);
            outputText(txPacket, direction.OUT, endhost.ERRORSIM, verboseOutput);

            //receive ACK packet from client
            byte[] receiveData = new byte[4];
            DatagramPacket rxPacket = new DatagramPacket(receiveData, receiveData.length);

            while (!gotResponse) {// Loop receive from server until either Valid ACK or ERROR is received
                socket.setSoTimeout(5000);
                try {
                    socket.receive(rxPacket);

                    if (receiveData[1] == 5 || (sendData[2] == receiveData[2] && sendData[3] == receiveData[3]) || !checkError(rxPacket).equals("No Error")) {
                        gotResponse = true;
                    }
                } catch (SocketTimeoutException ste) {

                    if (numResentPkt >= 3) {
                        System.out.println("\n*** No response from Client after three attempts. ***\n");
                        return;
                    }

                    numResentPkt++;
                    System.out.println("\n*** No response received from Client... Re-sending packet. ***\n");
                    socket.send(txPacket);
                    outputText(txPacket, direction.OUT, endhost.ERRORSIM, verboseOutput);
                    socket.setSoTimeout(0); //infinite socket wait time
                    gotResponse = false;
                }
            }

            if (gotResponse) {
                rxPacket = resizePacket(rxPacket);
                outputText(rxPacket, direction.IN, endhost.ERRORSIM, verboseOutput);
                errorMsg = checkError(rxPacket);
	            if (!errorMsg.equals("No Error")) {//check received packet
	            	sendError(rxPacket);
	            	if (!errorMsg.equals("Unknown transfer ID.")) {
	            		return;
	            	}
	            }
            }
        }

        System.out.println("RRQ Complete: TERMINATING SOCKET");
        socket.close();
    }
}
