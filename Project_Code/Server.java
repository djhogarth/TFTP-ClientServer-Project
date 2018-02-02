/*
  FILENAME - Server.java
  ASSIGNMENT - Final Project - SYSC 3303
  AUTHOR -
  DETAILS - A program that will receives DatagramPackets from IntHost
          - Based on the packet type, sends a response to IntHost
*/

import java.net.*;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Vector;

class Server implements Runnable
{

    //private static final int ERRORSIM_PORT = 69;
    private static final int ERRORSIM_PORT = 9969;
    private static final int DATA_SIZE = 516;

    private DatagramSocket socket;
    private DatagramPacket packet;
    private byte[] rxData, txData;
    private boolean isListener = false;

    //Used to determine if a packet is inbound or outbound when displaying its text
    public enum direction
    {
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
            //errorSimSocket.setSoTimeout(5000); // socket
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
                    System.out.println("failed to receive");
                    //e.printStackTrace();
                    //System.exit(1);
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
                    //resetVars();
                }
                catch (Exception e)
                {
                    System.out.println (e.getStackTrace());
                }

            }
            else {
                outputText(rxPacket, direction.IN);
                //System.exit(1);
                //socket.close();
                //throw new ValidationException("Packet is not valid.");
            }
        }

        //If the thread is a SENDER, run this code
        if (!isListener)
        {
            //All sending logic is in sendReply()
            sendReply(this.packet, this.socket);

            //Close temp socket and thread
            socket.close();
            Thread.currentThread().interrupt();
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
        if (dir == direction.IN)
            System.out.println("--Inbound Packet Data from ErrorSim--");
        else if (dir == direction.OUT)
            System.out.println("--Outbound Packet Data to ErrorSim--");

        //ASCII OUTPUT
        byte[] data = packet.getData();
        String ascii = new String(data, Charset.forName("UTF-8"));
        System.out.println(ascii);

        //BYTE OUTPUT
        //Confirm output with - https://www.branah.com/ascii-converter
        for (int j = 0; j < data.length; j++)
        {
            System.out.print(data[j]);
            if (j%1 == 0 && j != 0)
                System.out.print(" ");
        }
        System.out.println("\n-----------------------");
    }

    public synchronized static boolean validatePacket(DatagramPacket packet)
    {
        //BYTES [32-126] WILL COUNT AS VALID CHARACTERS
        //ANYTHING ELSE IS "INVALID"
        boolean isValid = false;
        boolean filenameIsValid = true;
        boolean modeIsValid = true;

        //Counts the number of 0x0 in the packet (size of Vector) and their indexes in the packet (index values in Vector)
        Vector<Integer> hasZero = new Vector<Integer>();

        if (packet.getData()[0] == 0 && (packet.getData()[1] == 1 || packet.getData()[1] == 2))
            isValid = true;

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
                    if ((packet.getData()[i] <= 31 && packet.getData()[i] != 0) || (packet.getData()[i] >= 127))
                        filenameIsValid = false;
                }

                for (int i = hasZero.elementAt(0) + 1; i < hasZero.elementAt(1); i++)
                {
                    if ((packet.getData()[i] <= 31 && packet.getData()[i] != 0) || (packet.getData()[i] >= 127))
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

    //packet = packet received from ErrorSim
    //socket = socket ErrorSim used to send the packet to Server
    public synchronized void sendReply(DatagramPacket packet, DatagramSocket socket)
    {
        byte[] data = packet.getData();
        byte[] response = new byte[4];
        String filename;

        //Extract filename from packet
        int i=2;
        while(data[i]!=0) {
            i++;
        }
        filename = new String(Arrays.copyOfRange(data, 2, i-1) , Charset.forName("UTF-8"));


        //Extract ErrorSim socket's port from packet
        InetSocketAddress temp_add = (InetSocketAddress)packet.getSocketAddress();
        int port = temp_add.getPort();
        DatagramPacket txPacket = new DatagramPacket(response, response.length, packet.getAddress(), port);

        if (txData[0] == 0 && txData[1] == 1)       //IF PACKET IS RRQ
        {
            try {
                this.readRequest(port, filename);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        else if (txData[0] == 0 && txData[1] == 2)  //IF PACKET IS WRQ
        {
            try {
                this.writeRequest(port, filename);
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

        outputText(txPacket, direction.OUT);

        try {
            socket.send(txPacket);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    //A function that implements the WRQ of a TFTP server, takes as input the client's port and file name of requested file
    public synchronized void writeRequest(int port,String filename) throws Exception {
        boolean isValidFile = true;
        byte[] file;
        DatagramSocket writeSocket = new DatagramSocket();//new socket for WRQ
        byte[] sendData = new byte[]{0,4,0,0};//block 0 ACK packet

        while(isValidFile) {//Loop to send ACK and receive DATA until DATA<512 bytes
            //send ACK packet to Client
            DatagramPacket txPacket = new DatagramPacket(sendData,sendData.length,InetAddress.getLocalHost(),port);
            writeSocket.send(txPacket);
            outputText(txPacket, direction.OUT);

            //receive DATA packet from Client
            byte[] receiveData = new byte[DATA_SIZE];
            DatagramPacket rxPacket = new DatagramPacket(receiveData, receiveData.length);
            writeSocket.receive(rxPacket);
            rxPacket = resizePacket(rxPacket);
            outputText(rxPacket, direction.IN);



            //buffer/write file data here



            //set Data of next ACK packet with received block #
            sendData = new byte[]{0,4,receiveData[2],receiveData[3]};

            //stop if received DATA packet is less then 512 bytes
            if (rxPacket.getLength()<DATA_SIZE) isValidFile = false;
        }
        writeSocket.close();
    }

    //A function that implements the RRQ of a TFTP server, takes as input the client's port and file name of requested file
    public synchronized void readRequest(int port,String filename) throws Exception {
        boolean isValidFile = true;
        byte[] file;
        int blockNum=1;
        DatagramSocket readSocket = new DatagramSocket();//new socket for RRQ

        while(isValidFile) {//Loop to send DATA and receive ACK until DATA<512 bytes
            byte[] blockNumBytes= blockNumToBytes(blockNum++);
            byte[] sendData = new byte[DATA_SIZE];
            sendData[0]=0;
            sendData[1]=3;
            sendData[2]=blockNumBytes[0];
            sendData[3]=blockNumBytes[1];

            //buffer file data here

            //set data in packet here
            for (int i=4;i<516;i++) {//4-515 are for 512 bytes of data
                sendData[i]=0;
            }

            //send DATA packet to client
            DatagramPacket txPacket = new DatagramPacket(sendData,sendData.length,InetAddress.getLocalHost(),port);
            readSocket.send(txPacket);
            txPacket = resizePacket(txPacket);
            outputText(txPacket, direction.OUT);

            //stop if sent DATA is less then 512 byte
            if (txPacket.getLength()<DATA_SIZE) isValidFile = false;
            else {
                //receive ACK packet from client, nothing is done with it yet
                byte[] receiveData = new byte[4];
                DatagramPacket rxPacket = new DatagramPacket(receiveData, receiveData.length);
                readSocket.receive(rxPacket);
                rxPacket = resizePacket(rxPacket);
                outputText(rxPacket, direction.IN);
            }
        }
        readSocket.close();
    }

    //A function to convert an int into an array of 2 bytes
    public static byte[] blockNumToBytes(int blockNum) {
        int b1 = blockNum / 256;
        int b2 = blockNum % 256;
        return new byte[] {(byte)b1,(byte)b2};
    }

    //Packets are initialized with 100 Bytes of memory but don't actually use all the space
    //This function resizes a packet based on the length of its payload, conserving space
    //**THIS FUNCTION MAY BE DEPRECATED IN FUTURE ITERATIONS**
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
}
