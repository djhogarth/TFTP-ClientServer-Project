/*
  FILENAME - Client.java
  ASSIGNMENT - Final Project - SYSC 3303
  AUTHOR -
  DETAILS - A program that will generate five RRQ, five WRQ, and one ERROR datagram packets and sends them to IntHost.java
*/

/*  FLOW
    c1 - form message
    c2 - create datagram
    c3 - send datagram
    s1 - receive datagram
    s2 - extract message
    s3 - create datagram
    s4 - send datagram
    c4 - receive datagram
    c5 - extract message
    c6 - print message
 */

//HOW TO LAUNCH ALL FILES SIMULTANEOUSLY
// File -> Import -> Run/Debug -> Launch Configurations -> Next
// Browse to Assignment1_AlexV/Launch Config -> Select Folder -> Select Assignment1_AlexV.launch -> Check "Overwrite" box -> Finish
// Run -> Run Configurations -> Launch Group -> Assignment1_AlexV -> Run

import java.io.*;
import java.net.*;
import java.nio.charset.Charset;

class Client {

    DatagramPacket txPacket, rxPacket; // Two datagrams for tx/rx
    DatagramSocket socket; // Only need one socket since we never tx/rx simultaneously

    //private static final int LOCAL_PORT = 9923;
    private static final int INTHOST_PORT = 23;
    //private static final int SERVER_PORT = 9969; //Client doesn't interface with the server directly

    public InetAddress clientIP, intHostIP, serverIP;

    //TFTP OPCODES
    public enum OPCodes
    {
        READ, WRITE, DATA, ACK, ERROR
    }

    //Used to determine if a packet is inbound or outbound when displaying its text
    public enum direction
    {
        IN, OUT;
    }

    public Client() {

        try {
            clientIP = InetAddress.getLocalHost();
            intHostIP = clientIP;
        } catch (UnknownHostException he)
        {
            he.printStackTrace();
        }

        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(5000); // socket
        } catch (SocketException se) {
            se.printStackTrace();
            System.exit(1);
        }

    }

    //A function to create FTFP REQUEST headers
    private static synchronized DatagramPacket makeRequest(String mode, String filename, OPCodes rq, InetAddress ip)
    {
        //HEADER ==> OPCODE = 2B | FILENAME | 0x0 | MODE | 0x0
        byte[] header = new byte[100];

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

        for (int i = 0; i < filename.getBytes().length; i++)
        {
            header[j++] = temp[i];
        }

        //Add a 0x0
        header[j++] = 0;

        //MODE
        temp = mode.getBytes();

        for (int i = 0; i < mode.getBytes().length; i++)
        {
            header[j++] = temp[i];
        }

        //Add a 0x0
        header[j++] = 0;

        //Write header to txPacket
        DatagramPacket packet = new DatagramPacket(header, j, ip, INTHOST_PORT);
        packet = resizePacket(packet);

        return packet;
    }

    // Main -> sendReceiveLoop -> newDatagram -> makeRequest -> send
    public static void main(String args[]) throws Exception {

        Thread.sleep(3000); //Allows INTHOST and SERVER to load first
        System.out.println("TFTP Client is running.\n");
        Client c = new Client();
        sendReceiveLoop(c);
    }

    //Loops 11 times, creating a new packet each iteration and sending it to ErrorSim
    public static void sendReceiveLoop(Client c) throws IOException
    {
        for (int i = 0; i < 11; i++)
        {
            if (i%2 == 0 && i != 10)
                c.txPacket = newDatagram(c.intHostIP, OPCodes.READ);
            else if (i == 10)
                c.txPacket = newDatagram(c.intHostIP, OPCodes.ERROR); //Last packet is "invalid"
            else
                c.txPacket = newDatagram(c.intHostIP, OPCodes.WRITE);

            outputText(c.txPacket, direction.OUT);

            //Sending Packet to ErrorSim
            try {
                c.socket.send(c.txPacket);
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            byte[] receiveData = new byte[100];
            DatagramPacket rxPacket = new DatagramPacket(receiveData, receiveData.length);
            try {
                c.socket.receive(rxPacket);
                rxPacket = resizePacket(rxPacket);
                outputText(rxPacket, direction.IN);
            }
            catch (SocketTimeoutException ste)
            {
                System.out.println("Did not receive a packet from ErrorSim");
            }
        }
    }

    //A function to create a new Datagram
    //Future updates to this code will implement the ability to create other types of TFTP packets
    public static DatagramPacket newDatagram(InetAddress intHostIP, OPCodes op) throws IOException {
        String mode = "NETascii";
        String filename = "README.txt";

        DatagramPacket newPacket = makeRequest(mode, filename, op, intHostIP);
        return newPacket;
    }

    //A function that reads the text in each packet and displays its contents in ASCII and BYTES
    public static void outputText(DatagramPacket packet, direction dir)
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

    //Packets are initialized with 100 Bytes of memory but don't actually use all the space
    //This function resizes a packet based on the length of its payload, conserving space
    public static DatagramPacket resizePacket(DatagramPacket packet)
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
