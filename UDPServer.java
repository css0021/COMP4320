import java.io.*;
import java.net.*;
import java.lang.*;
import java.nio.*;

public class UDPServer {

   public static final int MAGICCHECK = 0x1234;
   public static final int GROUP_ID = 11;

   public static void main(String[] args) {
      int port;
   
      if(args.length == 1) {
         port = Integer.parseInt(args[0]);
         run(port);
      }
      else {
         System.out.println("\nPlease enter the port number.");
      }
   }
	
   public static void run(int port) {
      try {
         System.out.println("\nServer waiting for connections...");
         DatagramSocket servSock = new DatagramSocket(port);
      	
         byte[] buffer = new byte[255];
         DatagramPacket incoming = new DatagramPacket(buffer, buffer.length);
      	
         while(true) {
            System.out.println("\nReceiving data...");
            servSock.receive(incoming);
            byte[] data = incoming.getData();
            InetAddress IPAddress = incoming.getAddress();
            String host = IPAddress.getHostAddress();
            System.out.println("Connected to: " + host);
         	
            int magicNum = ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);
            int tml = ((data[2] & 0xFF) << 8) | (data[3] & 0xFF);
            int checksum = data[4];
            byte gid = data[5];
            byte reqID = data[6];
         	
            byte[] newData = new byte[tml];
            for(int i = 0; i < tml; i++) {
               newData[i] = data[i];
            }
         	
            //newData[4] = 0;	
            short sum = getChecksum(newData);
            
            
            if(magicNum == 0 || tml <= 7 || sum != 0xFF) {
               if(magicNum == 0) {
                  System.out.println("\nMessage is invalid: No magic number.");
                  System.exit(0);
               }
               else if(tml <= 7) {
                  System.out.println("\nMessage is invalid: Request too short.");
                  System.exit(0);
               }
               else if(sum != 0xFF) {
                  System.out.println("\nMessage is invalid: Bad checksum.");
                  System.exit(0);
               }
            }
            else {
               int totalHosts = (tml - 7) / 2;
               int newTML = 7 + totalHosts * 4;
               byte[] send = new byte[newTML];
               
               send[0] = 0x34;
               send[1] = 0x12;
               send[2] = (byte) (newTML & 0xFF);
               send[3] = (byte) ((newTML >> 8) & 0xFF);
               send[4] = 0;
               send[5] = 11;
               send[6] = reqID;
               
               int length = 0;
               int msgLength = 0;
               int counter = 7;
               
               for(int i = 7; i < tml; i += length) {
                  length = data[i];
                  byte[] variable = new byte[length];
                  msgLength = i + length;
                  
                  for(int j = i+1; j < msgLength; j++) {
                     for(int k = 0; k < length; k++) {
                        variable[k] = data[j];
                     }
                  }
                  
                  String hostname = new String(variable);
                  byte[] bytes = InetAddress.getByName(hostname).getAddress();
                  int ip = IPtoInt(bytes);
                  
                  ByteBuffer bb = ByteBuffer.allocate(4);
                  bb.putInt(ip);
                
                  System.arraycopy(bb.array(), 0, send, counter, 4);
                  counter = counter + 4;
               }
            }
         					
         }
      }
      catch (IOException e) {
         System.out.println(e);
      }
   }
	
   public static short getChecksum(byte[] data) {
      short checksum = 0;
      for(int i = 0; i < data.length; i++) {
         checksum += (short) (data[i] & 0x00FF);
         checksum = (short) ((checksum & 0xFF) + (checksum >> 8));
      }
      return checksum;
   }
   
   public static int IPtoInt(byte[] bytes) {
      int val = 0;
      for(int i = 0; i < bytes.length; i++) {
         val <<= 8;
         val |= bytes[i] & 0xff;
      }
      
      return val;
   }
}
