/*
  FILENAME - Client.java
  ASSIGNMENT - Assignment 1 - SYSC 3303
  AUTHOR - Alex Viman (100967379)
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
import java.util.Scanner;

class Client {

    DatagramPacket sendPacket, receivePacket; // Two datagrams for tx/rx
    DatagramSocket socket; // Only need one socket since we never tx/rx simultaneously

    //private static final int LOCAL_PORT = 9923;
    private static final int INTHOST_PORT = 23;
    //private static final int SERVER_PORT = 9969; //Client doesn't interface with the server directly

    public InetAddress clientIP, intHostIP, serverIP;
    public String pathName, FileName, Operation;

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
        if (rq == Client.OPCodes.READ)
            header[1] = 1;
        else if (rq == Client.OPCodes.WRITE)
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

        //Write header to sendPacket
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
    
    public void userInput(Client c) throws IOException
    {
    //Scanner reader = new Scanner(System.in);  // Reading from System.in
    //System.out.println("Enter file name: ");
    //c.FileName = reader.nextLine(); // Scans the next token of the input as an int.
    c.FileName = "test.txt";
    //System.out.println("Enter file path: ");
    //c.pathName = reader.nextLine();
    c.pathName = "C:\\Users\\Dariy\\Documents\\test.txt\n" ;
   // System.out.println("Is it a read or write?: ");
    //c.Operation = reader.nextLine();
c.Operation = "read";
    //reader.close(); 

    if((c.Operation).equals("write"))
    c.sendPacket = newDatagram(c.intHostIP, OPCodes.WRITE);

    if((c.Operation).equals("read"))
    c.sendPacket = newDatagram(c.intHostIP, OPCodes.READ);

    File file = new File(c.pathName);
    		FileInputStream filee = null;
    		   				
    		
    		try {
    			filee = new FileInputStream(file);
    			byte[] data = new byte[(int) file.length()];   			
    			filee.read(data); //read file into bytes[]
    						
    			System.out.println("File was successfully read in : ");
    			/*String s = new String(data);
    			 System.out.println("File content: " + s);
				*/
    			
    		}
    			catch (FileNotFoundException e) {
    				 System.out.println("File not found" + e);
    			}
    			
    		catch (IOException e) {
    			e.printStackTrace();
    		} finally {
    			try {
    				if (filee != null)
    					filee.close();
    			} catch (IOException ex) {
    				ex.printStackTrace();
    			}
    		}
    		 
    		 
    	}

    //Loops 11 times, creating a new packet each iteration and sending it to IntHost
    public static void sendReceiveLoop(Client c) throws IOException
    {
    	 
    		c.userInput(c);
            outputText(c.sendPacket, direction.OUT);
            
           
            //Sending Packet to IntHost
            try {
                c.socket.send(c.sendPacket);
            }
            catch (Exception e) {
                e.printStackTrace();
            }

           
            byte[] receiveData = new byte[516]; 
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            try {
            	System.out.println("done");
                c.socket.receive(receivePacket);
                receivePacket = resizePacket(receivePacket);
                outputText(receivePacket, direction.IN);
                
            }
            catch (SocketTimeoutException ste)
            {
                System.out.println("Did not receive a packet from IntHost");
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
        if (dir == Client.direction.IN)
            System.out.println("--Inbound Packet Data from IntHost--");
        else if (dir == Client.direction.OUT)
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
