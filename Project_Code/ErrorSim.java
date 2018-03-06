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
        
        //testmode variables
        int mode = 0;	//0=normal,1=lose a packet,2=delay a packet,3=duplicate a packet and choose when to send it
        byte testOpcode = 0; //RRQ,WRQ,DATA,ACK
        int testBlockNum = -1; //0 to 65335 - for lost/delayed/duplicate DATA/ACK
        long delay = 0; //sleep in milliseconds
        boolean isLost = false; //is current packet lost
        DatagramPacket dupePack = null;
        byte delayOpcode = 0; //DATA,ACK delayed packet opcode to replace 
        int delayBkNum = 2; //block number to replace
        
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

        int tempPort = 0;

        try {
            clientSocket = new DatagramSocket(CLIENT_PORT);
            //clientSocket.setSoTimeout(5000);
            serverSocket = new DatagramSocket();
        } catch (SocketException se) {
            se.printStackTrace();
            System.exit(1);
        }

        byte[] rxData = new byte[DATA_SIZE];

        boolean lastDataPkt = false;

        while(true){
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
                
                if(mode==1 && rxData[1]==testOpcode) {//Test Mode 1: Lost Packet
                	if(rxData[1]==1 || rxData[1]==2) {//Checks if packet is RRQ or WRQ
                		isLost=true;
                	}
                	else if(blockNumToBytes(testBlockNum)[0]==rxData[2] && blockNumToBytes(testBlockNum)[1]==rxData[3]) {//Otherwise check the block number
                		isLost=true;
                	}
                }else if(mode==2 && rxData[1]==testOpcode) {//Test Mode 2: Delay Packet
                	if(rxData[1]==1 || rxData[1]==2) {//Checks if packet is RRQ or WRQ
                		Thread.sleep(delay);
                	}
                	else if(blockNumToBytes(testBlockNum)[0]==rxData[2] && blockNumToBytes(testBlockNum)[1]==rxData[3]) {//Otherwise check the block number
                		Thread.sleep(delay);
                	}
                }else if(mode==3 && rxData[1]==testOpcode) {//Test Mode 3: Duplicate Packet
                	if(blockNumToBytes(testBlockNum)[0]==rxData[2] && blockNumToBytes(testBlockNum)[1]==rxData[3]) {//Duplicate packet
                		dupePack = txPacket;
                	}
                }else if(mode==3 && rxData[1]==delayOpcode) {//Test Mode 3: Replace packet
                	if(blockNumToBytes(delayBkNum)[0]==rxData[2] && blockNumToBytes(delayBkNum)[1]==rxData[3]) {//Replace packet with duplicate
                		Thread.sleep(delay);
                		txPacket = dupePack;
                	}
                }
                
                
                if(rxData[1]==3||rxData[1]==4) {
                    txPacket.setPort(tempPort);
                }
                else {
                    txPacket.setPort(SERVER_PORT);
                }
                
                if(!isLost) {//Don't send lost packet
                	serverSocket.send(txPacket);
                	outputText(txPacket, direction.OUT, endhost.SERVER, verboseOutput);
                }
                isLost=false;

                if (!isOOB(txPacket)) {
                	//outputText(txPacket, direction.OUT, endhost.SERVER, verboseOutput);
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
                    
                    if(mode==1 && rxData[1]==testOpcode) {//Test Mode 1: Lost Packet
                    	if(rxData[1]==1 || rxData[1]==2) {//Checks if packet is RRQ or WRQ
                    		isLost=true;
                    	}
                    	else if(blockNumToBytes(testBlockNum)[0]==rxData[2] && blockNumToBytes(testBlockNum)[1]==rxData[3]) {//Otherwise check the block number
                    		isLost=true;
                    	}
                    }else if(mode==2 && rxData[1]==testOpcode) {//Test Mode 2: Delay Packet
                    	if(rxData[1]==1 || rxData[1]==2) {//Checks if packet is RRQ or WRQ
                    		Thread.sleep(delay);
                    	}
                    	else if(blockNumToBytes(testBlockNum)[0]==rxData[2] && blockNumToBytes(testBlockNum)[1]==rxData[3]) {//Otherwise check the block number
                    		Thread.sleep(delay);
                    	}
                    }else if(mode==3 && rxData[1]==testOpcode) {//Test Mode 3: Duplicate Packet
                    	if(blockNumToBytes(testBlockNum)[0]==rxData[2] && blockNumToBytes(testBlockNum)[1]==rxData[3]) {//Duplicate packet
                    		dupePack = txPacket;
                    	}
                    }else if(mode==3 && rxData[1]==delayOpcode) {//Test Mode 3: Replace packet
                    	if(blockNumToBytes(delayBkNum)[0]==rxData[2] && blockNumToBytes(delayBkNum)[1]==rxData[3]) {//Replace packet with duplicate
                    		Thread.sleep(delay);
                    		txPacket = dupePack;
                    	}
                    }
                    
                    if(!isLost) {//Don't send lost packet
                    	clientSocket.send(txPacket);
                    	outputText(txPacket, direction.OUT, endhost.CLIENT, verboseOutput);
                    }
                    isLost=false;
                    
                    
                }
            }
            catch (SocketTimeoutException ste)
            {
                System.out.println("Did not receive a packet from CLIENT or SERVER");
            }
        }
    }
}
