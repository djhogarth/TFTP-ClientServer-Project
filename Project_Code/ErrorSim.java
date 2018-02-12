/*
  FILENAME - ErrorSim.java
  ASSIGNMENT - Final Project - SYSC 3303
  AUTHOR - GROUP 3 - W18
  DETAILS - A program that will receives DatagramPackets from Client or Server and forwards them to the opposite host.
*/

import java.net.*;
import java.nio.charset.Charset;

class ErrorSim extends CommonMethods
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
}
