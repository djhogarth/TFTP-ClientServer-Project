/*
  FILENAME - Server.java
  ASSIGNMENT - Assignment 1 - SYSC 3303
  AUTHOR - Alex Viman (100967379)
  DETAILS - A program that will receives DatagramPackets from IntHost
          - Based on the packet type, sends a response to IntHost
*/

import javax.xml.bind.ValidationException;
import java.net.*;
import java.nio.charset.Charset;
import java.util.Vector;

class Server
{

    private static final int INTHOST_PORT = 69;

    //Used to determine if a packet is inbound or outbound when displaying its text
    public enum direction
    {
        IN, OUT;
    }

    //Waits to receive packet from IntHost.java
    //Upon receipt, validates the packet, creates a response packet and sends it to IntHost.java
    public static void main(String args[]) throws Exception
    {
        System.out.println("TFTP Server is running.\n");
        DatagramSocket intHostSocket = new DatagramSocket();

        try {
            intHostSocket = new DatagramSocket(INTHOST_PORT);
            intHostSocket.setSoTimeout(5000); // socket
        } catch (SocketException se) {
            se.printStackTrace();
            System.exit(1);
        }

        byte[] rxData = new byte[100];

        while(true)
        {
            DatagramPacket rxPacket = new DatagramPacket(rxData, rxData.length);

            intHostSocket.receive(rxPacket);
            rxPacket = resizePacket(rxPacket);
            outputText(rxPacket, direction.IN);

            if (validatePacket(rxPacket)) {
                outputText(rxPacket, direction.IN);
                sendReply(rxPacket);
            }
            else {
                outputText(rxPacket, direction.IN);
                //System.exit(1);
                intHostSocket.close();
                throw new ValidationException("Packet is not valid.");
            }

        }
    }

    //A function that reads the text in each packet and displays its contents in ASCII and BYTES
    public static void outputText(DatagramPacket packet, direction dir)
    {
        if (dir == Server.direction.IN)
            System.out.println("--Inbound Packet Data from IntHost--");
        else if (dir == Server.direction.OUT)
            System.out.println("--Outbound Packet Data to IntHost--");

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

    public static boolean validatePacket(DatagramPacket packet)
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

    public static void sendReply(DatagramPacket packet)
    {
        DatagramSocket tempSocket;
        byte[] temp = packet.getData();
        byte[] response = new byte[4];

        //Extract IntHost socket's port from packet
        InetSocketAddress temp_add = (InetSocketAddress)packet.getSocketAddress();
        int port = temp_add.getPort();
        DatagramPacket txPacket = new DatagramPacket(response, response.length, packet.getAddress(), port);

        if (temp[0] == 0 && temp[1] == 1)       //IF PACKET IS RRQ
        {
            response[0] = 0;
            response[1] = 3;
            response[2] = 0;
            response[3] = 1;
        }
        else if (temp[0] == 0 && temp[1] == 2)  //IF PACKET IS WRQ
        {
            response[0] = 0;
            response[1] = 4;
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
            tempSocket = new DatagramSocket();

            try {
                tempSocket.send(txPacket);
                tempSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (SocketException se) {
            se.printStackTrace();
        }

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
