import javax.xml.crypto.Data;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CommonMethods {

	public static final Map<String, Integer> errorMap;
    static
    {
    	errorMap = new HashMap<String, Integer>();
    	errorMap.put("Not defined, see error message (if any).", 0);
    	errorMap.put("File not found.", 1);
    	errorMap.put("Access violation.", 2);
    	errorMap.put("Disk full or allocation exceeded.", 3);
    	errorMap.put("Illegal TFTP operation.", 4);
    	errorMap.put("Unknown transfer ID.", 5);
    	errorMap.put("File already exists.", 6);
    	errorMap.put("No such user.", 7);
    }

    public enum direction {
        IN, OUT;
    }

    //Used to determine which host sent a packet to whom
    public enum endhost
    {
        CLIENT, ERRORSIM, SERVER;
    }

    //TFTP OPCODES
    public enum OPCodes {
        READ,   //0x01
        WRITE,  //0x02
        DATA,   //0x03
        ACK,    //0x04
        ERROR   //0x05
    }

    //Returns the a packet's port number
    public static int getPort(DatagramPacket p)
    {
        InetSocketAddress temp_add = (InetSocketAddress) p.getSocketAddress();
        int port = temp_add.getPort();
        return port;
    }

    //Packets are initialized with 100 Bytes of memory but don't actually use all the space
    //This function resizes a packet based on the length of its payload, conserving space
    public static DatagramPacket resizePacket(DatagramPacket packet) {
        int port = getPort(packet);
        InetAddress ip = packet.getAddress();
        int length = packet.getLength();

        byte[] tempData = new byte[length];

        for (int i = 0; i < length; i++) {
            tempData[i] = packet.getData()[i];
        }

        DatagramPacket resizedPacket = new DatagramPacket(tempData, tempData.length, ip, port);
        return resizedPacket;
    }

    //A function that reads the text in each packet and displays its contents in ASCII and BYTES
    /*Verbose == Inbound/Outbound Packet
                 Timeouts/Re-transmissions       //Iteration 3
                 Inbound/Outbound Socket
                 Packet Type
                 Mode (netascii, octet, or mail)
                 Filename
                 Block Number
                 Number of Data Bytes
                 Error Code/Message
     */

    //A function that reads the text in each packet and displays its contents in ASCII and BYTES
    public static void outputText(DatagramPacket packet, direction dir, endhost host, boolean verbose)
    {
        if (verbose) {
            byte[] data = packet.getData();

            if (dir == direction.IN)
                System.out.println("DIRECTION \t\t= Inbound Packet Data from " + host);
            else if (dir == direction.OUT)
                System.out.println("DIRECTION \t\t= Outbound Packet Data to " + host);

            //For Iteration 3
            //System.out.println("TIMEOUTS = N/A");
            //System.out.println("RE-TRANSMISSIONS = N/A");

            if (dir == direction.IN)
                System.out.println("INBOUND FROM \t= " + packet.getAddress() + ":" + getPort(packet));
            else if (dir == direction.OUT)
                System.out.println("OUTBOUND TO \t= " + packet.getAddress() + ":" + getPort(packet));

            //PACKET TYPE OUTPUT
            if (data[0] == 0 && data[1] == 1)
                System.out.println("OPCODE \t\t\t= READ [0x01]");
            if (data[0] == 0 && data[1] == 2)
                System.out.println("OPCODE \t\t\t= WRITE [0x02]");
            if (data[0] == 0 && data[1] == 3)
                System.out.println("OPCODE \t\t\t= DATA [0x03]");
            if (data[0] == 0 && data[1] == 4)
                System.out.println("OPCODE \t\t\t= ACK [0x04]");
            if (data[0] == 0 && data[1] == 5)
                System.out.println("OPCODE \t\t\t= ERROR [0x05]");

            //REQUEST MODE
            if (data[0] == 0 && (data[1] == 1 && data[1] == 2)) {
                String mode = new String(data, Charset.forName("UTF-8"));
                mode = mode.substring(4, mode.length() - 1);
                mode = mode.toLowerCase();
                System.out.println("REQUEST MODE \t= " + mode);
            }

            if (data[0] == 0 && (data[1] == 1 || data[1] == 2 || data[1] == 3))
            {
                String fn = getFilename(packet);
                System.out.println("FILENAME \t\t= " + fn);
            }

            if (data[0] == 0 && data[1] == 3)
            {
                String ascii = new String(data, Charset.forName("UTF-8"));
                ascii = ascii.substring(4, ascii.length());
                System.out.println("MESSAGE LENGTH \t= " + ascii.length());
            }

            if (data[0] == 0 && (data[1] == 3 || data[1] == 4))
            {
                String blockNum = Integer.toString(data[2]) + Integer.toString(data[3]);
                System.out.println("BLOCK NUMBER \t= " + blockNum);
            }

            if (data[0] == 0 && data[1] == 5)
            {
                String error = Server.checkError(packet)[0]; //using this for now
                System.out.println("ERROR CODE \t= X = " + error);
            }


            /*
            ---- DEPRECATED CODE ----

            //MESSAGE OUTPUT
            String ascii = new String(data, Charset.forName("UTF-8"));
            ascii = ascii.substring(4, ascii.length());
            if (ascii.length() > 0) {
                System.out.println("MSG LENGTH = " + ascii.length());
                System.out.println("MESSAGE = ");
                System.out.println(ascii);
            } else
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
            */

            System.out.println("-----------------------");
        }
        else
        {
            System.out.println("So quiet...");
        }
    }

    //A function to convert an int into an array of 2 bytes
    public static byte[] blockNumToBytes(int blockNum) {
        int b1 = blockNum / 256;
        int b2 = blockNum % 256;
        return new byte[]{(byte) b1, (byte) b2};
    }

    //Determines if an inbound packet is a W/R RQ packet
    public static boolean isRequest(DatagramPacket p)
    {
        byte[] header = p.getData();

        if (header[0] == 0 && (header[1] == 1 || header[1] == 2))
            return true;
        else
            return false;
    }

    public static String getFilename(DatagramPacket packet)
    {
        byte[] data = packet.getData();

        //Extract filename from packet
        int i=2;
        while(data[i]!=0) {
            i++;
        }
        String filename = new String(Arrays.copyOfRange(data, 2, i) , Charset.forName("UTF-8"));

        return filename;
    }

    public static boolean isOOB(DatagramPacket pkt)
    {
        byte[] data = pkt.getData();

        if (data.length <= 3)
        {
            if (data[0] == 9 && data[1] == 9)
            {
                return true;
            }
        }

        return false;
    }
}
