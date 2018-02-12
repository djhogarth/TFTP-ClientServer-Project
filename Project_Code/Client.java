/*
  FILENAME - Client.java
  ASSIGNMENT - Final Project - SYSC 3303
  AUTHOR - GROUP 3 - W18
  DETAILS - A program that will generate valid TFTP datagram packets and sends them to ErrorSim
*/

import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.Vector;


class Client extends CommonMethods{

    DatagramPacket txPacket, rxPacket; // Two datagrams for tx/rx
    DatagramSocket socket; // Only need one socket since we never tx/rx simultaneously

    //private static final int ERRORSIM_PORT = 23;
    private static final int ERRORSIM_PORT = 9923;
    private static final int DATA_SIZE = 516;

    public InetAddress clientIP, errorSimIP, serverIP;
    public String pathname, filename, operation, continueOrQuit;
    public Vector<byte[]> receivedFile;

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

    public Client() {

        try {
            clientIP = InetAddress.getLocalHost();
            errorSimIP = clientIP;
        } catch (UnknownHostException he) {
            he.printStackTrace();
        }

        try {
            socket = new DatagramSocket();
            //socket.setSoTimeout(5000); // socket
        } catch (SocketException se) {
            se.printStackTrace();
            System.exit(1);
        }
    }

    // Main -> userInput -> newDatagram -> makeRequest -> send
    public static void main(String args[]) throws Exception {

        //Thread.sleep(3000); //Allows INTHOST and SERVER to load first
        System.out.println("TFTP Client is running.\n");
        Client c = new Client();
        userInput(c);
    }
  
    //A function to create FTFP REQUEST headers
    private static synchronized DatagramPacket makeRequest(String mode, String filename, OPCodes rq, InetAddress ip) {
        //HEADER ==> OPCODE = 2B | FILENAME | 0x0 | MODE | 0x0
        byte[] header = new byte[DATA_SIZE];

        //OPCODE
        header[0] = 0;
        if (rq == OPCodes.READ)
            header[1] = 1;
        else if (rq == OPCodes.WRITE)
            header[1] = 2;
        else
            header[1] = 0; //This should never happen.

        //FILENAME
        byte[] temp = filename.getBytes();
        int j = 2; //byte placeholder for header

        for (int i = 0; i < filename.getBytes().length; i++) {
            header[j++] = temp[i];
        }

        //Add a 0x0
        header[j++] = 0;

        //MODE
        temp = mode.getBytes();

        for (int i = 0; i < mode.getBytes().length; i++) {
            header[j++] = temp[i];
        }

        //Add a 0x0
        header[j++] = 0;

        //Write header to txPacket
        DatagramPacket packet = new DatagramPacket(header, j, ip, ERRORSIM_PORT);
        packet = resizePacket(packet);

        return packet;
    }

    // gets file path, file name and operation (read/write) from the user
    public static void userInput(Client c) throws IOException {

        while (true) {
            System.out.println("Working Directory = " + System.getProperty("user.dir"));

            Scanner reader = new Scanner(System.in);  // Reading from System.in
            System.out.print("Enter file name: ");
            c.filename = reader.nextLine(); // Scans the next token of the input as an int.

            System.out.print("Enter full file path: ");
            c.pathname = reader.nextLine();

            System.out.print("Is it a (r)ead or (w)rite?: ");
            c.operation = reader.nextLine();

            System.out.print("If this is the last file, enter 'q'.  ");
            c.continueOrQuit = reader.nextLine();

            if ((c.operation).equals("write") || (c.operation).equals("w")) {
                System.out.println("Creating a WRQ Packet");
                c.txPacket = newDatagram(c.errorSimIP, OPCodes.WRITE, c.filename);
                try {
                    c.socket.send(c.txPacket);
                    c.writeRequest(getPort(c.txPacket), c.filename);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if ((c.operation).equals("read") || c.operation.equals("r")) {
                System.out.println("Creating a RRQ Packet");
                c.txPacket = newDatagram(c.errorSimIP, OPCodes.READ, c.filename);
                try {
                    c.socket.send(c.txPacket);
                    c.receivedFile = c.readRequest(getPort(c.txPacket));
                    saveFile(c.receivedFile, c.filename);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if ((c.continueOrQuit).equals("q"))
                break;
        }
    }

    //A function that takes a vector of any size and writes its bytes to a file
    public static void saveFile(Vector<byte[]> receivedFile, String filename)
    {
        byte[] tempArray;
        int charCount = 0;

        for (int i = 0; i < receivedFile.size(); i++)
        {
            charCount += receivedFile.elementAt(i).length;
        }

        tempArray = new byte[charCount];

        String path = "./";
        String outputName = "RRQ " + filename;

        int tempCount = 0;

        for (byte[] bytes : receivedFile) {
            for (byte b : bytes) {
                tempArray[tempCount] = b;
                tempCount++;
            }
        }

        try (FileOutputStream fileOuputStream = new FileOutputStream(path + outputName)) {
            fileOuputStream.write(tempArray);
            fileOuputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //A function to create a new Datagram
    //Future updates to this code will implement the ability to create other types of TFTP packets
    public static DatagramPacket newDatagram(InetAddress errorSimIP, OPCodes op, String filename) {
        String mode = "NETascii";
        DatagramPacket newPacket = makeRequest(mode, filename, op, errorSimIP);
        return newPacket;
    }


    /*
       RRQ FLOW
       Client -> RRQ -> Server
       Server -> DATA BLK 1 -> Client
       Client -> ACK BLK 1 -> Server
       Repeats until Server sends last DATA pkt, Client sends a final ACK
     */

    //A function that implements the RRQ of TFTP client, takes as input the port that the server uses for handling requests and name of the requested file
    //isValidPkt is used to validate if received packet is a DATA packet
    //buffer is used to temporarily store the byte array from the received DATA packet
    //receivedFile is a vector that stores all byte[] for a file stream; this will be converted back to a file later
    public synchronized Vector<byte[]> readRequest(int port) throws Exception {
        boolean isValidPkt = true;
        Vector<byte[]> tempVector = new Vector<byte[]>();

        while (isValidPkt) {//Loop to receive DATA and send ACK until received DATA<512 bytes
            //receive and create DATA
            byte[] receiveData = new byte[DATA_SIZE];
            rxPacket = new DatagramPacket(receiveData, receiveData.length);
            this.socket.receive(rxPacket);
            rxPacket = resizePacket(rxPacket);
            outputText(rxPacket, CommonMethods.direction.IN);
            byte[] buffer = new byte[rxPacket.getLength() - 4];

            for (int i = 4; i < rxPacket.getLength(); i++)
            {
                buffer[i - 4] = rxPacket.getData()[i];
            }

            tempVector.addElement(buffer);

            //stop if received packet does not have DATA opcode or DATA is less then 512 bytes
            if (receiveData[1] != 3) isValidPkt = false;
            else {
                //create and send Ack packet with block # of received packet
                byte[] sendData = new byte[]{0, 4, receiveData[2], receiveData[3]};
                txPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getLocalHost(), port);
                this.socket.send(this.txPacket);
                outputText(txPacket, CommonMethods.direction.OUT);

                if (rxPacket.getLength() < DATA_SIZE) isValidPkt = false;
            }
        }

        return tempVector;
    }

    /*
       WRQ FLOW
       Client -> WRQ -> Server
       Server -> ACK BLK 0 -> Client
       Client -> DATA BLK 1 -> Server
       Server -> ACK BLK 1 -> Client
       Repeats until Client sends last DATA pkt, Server sends a final ACK
     */

    //A function that implements the WRQ of TFTP client, takes as input the port that the server uses for handling requests and name of the requested file
    public synchronized void writeRequest(int port, String filename) throws IOException {
        boolean isValidFile = true;
        int blockNum = 1;
        
        Path path = Paths.get("./" + filename);
        byte[] file = Files.readAllBytes(path);
        int totalBlocksRequired = (file.length / 512) + 1;
        int remainderLastBlock = (file.length % 512);
        boolean onLastBlock = false;
        int j=0;
        
        while (true) {//Loop to receive ACK and send DATA until sent DATA<512 bytes
            //create and receive ACK
            byte[] receiveData = new byte[4];
            rxPacket = new DatagramPacket(receiveData, receiveData.length);
            this.socket.receive(rxPacket);
            rxPacket = resizePacket(rxPacket);
            outputText(rxPacket, CommonMethods.direction.IN);

            //create send DATA
            byte[] blockNumBytes = blockNumToBytes(blockNum++);
            byte[] sendData = new byte[DATA_SIZE];
            sendData[0] = 0;
            sendData[1] = 3;
            sendData[2] = blockNumBytes[0];
            sendData[3] = blockNumBytes[1];
            
            //Stop after final ACK
            if(onLastBlock) break;
            
            //Check if its the last block
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
            
            txPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getLocalHost(), port);
            this.socket.send(this.txPacket);
            txPacket = resizePacket(txPacket);
            outputText(txPacket, CommonMethods.direction.OUT);
        }
    }
}
