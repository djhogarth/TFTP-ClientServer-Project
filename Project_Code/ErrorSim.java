/*
  FILENAME - ErrorSim.java
  ASSIGNMENT - Final Project - SYSC 3303
  AUTHOR - GROUP 3 - W18
  DETAILS - A program that will receives DatagramPackets from Client or Server and forwards them to the opposite host.
*/

import java.net.*;
import java.util.Scanner;

class ErrorSim extends CommonMethods
{
    //private static final int CLIENT_PORT = 23;
    //private static final int SERVER_PORT = 69;
    private static final int CLIENT_PORT = 9923;
    private static final int SERVER_PORT = 9969;
    private static final int DATA_SIZE = 516;

    //Used to determine if a packet is inbound or outbound when displaying its text
    //public enum direction
    //{
    //    IN, OUT;
    //}

    //Waits to receive packet from Client.java
    //Upon receipt, forwards it to Server.java
    //Waits to receive packet from Server.java
    //Upon receipt, forwards it to Client.java
    public static void main(String args[]) throws Exception
    {
        boolean inputValid = false;
        String input = "";
        boolean verboseOutput = false;
        boolean showMenu = true;
        boolean testMode = false;
        Scanner reader = new Scanner(System.in); // Reading from System.in

        System.out.println("TFTP ErrorSim is running.\n");
        DatagramSocket clientSocket = new DatagramSocket();
        DatagramSocket serverSocket = new DatagramSocket();
        //serverSocket.setSoTimeout(5000);


        while (showMenu) {
            inputValid = false;

            while (!inputValid) {
                System.out.print("Would you like to run the ErrorSim in (t)est or (n)ormal mode? ");
                input = reader.nextLine();
                input = input.toLowerCase();

                switch (input) {
                    case "t":
                        inputValid = true;
                        showMenu = false;
                        testMode = true; //testing operation
                        break;
                    case "n":
                        inputValid = true;
                        showMenu = false;
                        testMode = false; //normal operation
                        break;
                    default:
                        System.out.println("The input is not valid.");
                }
            }

            if (testMode)
            {
                System.out.println("This will need to be implemented.");
            }
            else
            {
                System.out.println("You've chosen normal operation.");
            }

        }

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

                if (!isOOB(rxPacket)) {
                    outputText(rxPacket, direction.IN, endhost.CLIENT, verboseOutput);
                }
                else
                {
                    lastDataPkt = true;
                    if (rxPacket.getData()[2] == 0)
                        verboseOutput = false;
                    else
                        verboseOutput = true;
                }

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

                if (!isOOB(txPacket)) {

                    outputText(txPacket, direction.OUT, endhost.SERVER, verboseOutput);
                }
                else
                {
                    lastDataPkt = true;
                }


                if (!lastDataPkt) {
                    //Receive from SERVER
                    rxPacket = new DatagramPacket(rxData, rxData.length);
                    serverSocket.receive(rxPacket);
                    rxPacket = resizePacket(rxPacket);
                    outputText(rxPacket, direction.IN, endhost.SERVER, verboseOutput);
                    
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
                    outputText(txPacket, direction.OUT, endhost.CLIENT, verboseOutput);
                }
            }
            catch (SocketTimeoutException ste)
            {
                System.out.println("Did not receive a packet from CLIENT or SERVER");
            }
        }
    }
}
