/*
  FILENAME - ErrorSim.java
  ASSIGNMENT - Final Project - SYSC 3303
  AUTHOR -
  DETAILS - A program that will receives DatagramPackets from Client or Server and forwards them to the opposite host.
*/

import java.net.*;
import java.nio.charset.Charset;

class ErrorSim
{
    //private static final int CLIENT_PORT = 23;
    //private static final int SERVER_PORT = 69;
    private static final int CLIENT_PORT = 9923;
    private static final int SERVER_PORT = 9969;
    private static final int DATA_SIZE = 516;

    //Used to determine if a packet is inbound or outbound when displaying its text
    public enum direction
    {
        IN, OUT;
    }

    //Used to detemine which host sent a packet to ErrorSim
    public enum endhost
    {
        CLIENT, SERVER;
    }

    //Waits to receive packet from Client.java
    //Upon receipt, forwards it to Server.java
    //Waits to receive packet from Server.java
    //Upon receipt, forwards it to Client.java
    public static void main(String args[]) throws Exception
    {
        System.out.println("TFTP ErrorSim is running.\n");
        DatagramSocket clientSocket = new DatagramSocket();
        DatagramSocket serverSocket = new DatagramSocket();
        //serverSocket.setSoTimeout(5000);
        int tempPort=0;

        try {
            clientSocket = new DatagramSocket(CLIENT_PORT);
            //clientSocket.setSoTimeout(5000);
        } catch (SocketException se) {
            se.printStackTrace();
            System.exit(1);
        }

        try {
            serverSocket = new DatagramSocket();
        } catch (SocketException se) {
            se.printStackTrace();
            System.exit(1);
        }

        byte[] rxData = new byte[DATA_SIZE];

        boolean lastDataPkt = false;

        while(true)
        {
            try {
                DatagramPacket rxPacket = new DatagramPacket(rxData, rxData.length);
                DatagramPacket txPacket;

                //Receive from CLIENT
                clientSocket.receive(rxPacket);

                if (isRequest(rxPacket))
                    lastDataPkt = false;

                rxPacket = resizePacket(rxPacket);
                outputText(rxPacket, direction.IN, endhost.CLIENT);

                InetSocketAddress temp_add = (InetSocketAddress) rxPacket.getSocketAddress();
                int client_port = temp_add.getPort();

                //Send to SERVER listener or last Thread
                txPacket = rxPacket;
                txPacket.setPort(SERVER_PORT);
                if(rxData[1]==3||rxData[1]==4) {
                    txPacket.setPort(tempPort);
                }
                else {
                    txPacket.setPort(SERVER_PORT);
                }
                serverSocket.send(txPacket);
                outputText(txPacket, direction.OUT, endhost.SERVER);

                if (!lastDataPkt) {
                    //Receive from SERVER
                    rxPacket = new DatagramPacket(rxData, rxData.length);
                    serverSocket.receive(rxPacket);
                    rxPacket = resizePacket(rxPacket);
                    outputText(rxPacket, direction.IN, endhost.SERVER);
                    
                    if (rxPacket.getData().length < 512 && rxData[1]==3)
                        lastDataPkt = true;

                    if (rxData[1] == 3 || rxData[1] == 4) {
                        tempPort = rxPacket.getPort();
                    }

                    //Send to CLIENT
                    txPacket = rxPacket;
                    txPacket.setPort(client_port);
                    txPacket.setAddress(InetAddress.getLocalHost());
                    clientSocket.send(txPacket);
                    outputText(txPacket, direction.OUT, endhost.CLIENT);
                }
            }
            catch (SocketTimeoutException ste)
            {
                System.out.println("Did not receive a packet from CLIENT or SERVER");
            }
        }
    }

    //A function that reads the text in each packet and displays its contents in ASCII and BYTES
    public static void outputText(DatagramPacket packet, direction dir, endhost host)
    {
        byte[] data = packet.getData();

        if (dir == direction.IN)
            System.out.println("--Inbound Packet Data from " + host + "--");
        else if (dir == direction.OUT)
            System.out.println("--Outbound Packet Data to " + host + "--");

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

    public static boolean isRequest(DatagramPacket p)
    {
        byte[] header = p.getData();

        if (header[0] == 0 && (header[1] == 1 || header[1] == 2))
            return true;
        else
            return false;
    }
}
