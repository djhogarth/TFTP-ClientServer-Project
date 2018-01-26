/*
  FILENAME - Server.java
  ASSIGNMENT - Final Project - SYSC 3303
  AUTHOR -
  DETAILS - A program that will receives DatagramPackets from IntHost
          - Based on the packet type, sends a response to IntHost
*/

import javax.xml.bind.ValidationException;
import java.net.*;
import java.nio.charset.Charset;
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
        this.txData = packet.getData();
        byte[] response = new byte[4];

        //Extract ErrorSim socket's port from packet
        InetSocketAddress temp_add = (InetSocketAddress)packet.getSocketAddress();
        int port = temp_add.getPort();
        DatagramPacket txPacket = new DatagramPacket(response, response.length, packet.getAddress(), port);

        if (txData[0] == 0 && txData[1] == 1)       //IF PACKET IS RRQ
        {
            //DATA PACKET
            response[0] = 0;
            response[1] = 3;

            //BLOCK NUMBER
            response[2] = 0;
            response[3] = 1;
        }
        else if (txData[0] == 0 && txData[1] == 2)  //IF PACKET IS WRQ
        {
            //ACK PACKET
            response[0] = 0;
            response[1] = 4;

            //BLOCK NUMBER
            response[2] = 0;
            response[3] = 0;
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
