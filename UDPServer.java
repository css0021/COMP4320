import java.io.*;
import java.net.*;
import java.lang.*;
import java.nio.*;

public class UDPServer {

   public static final int MAGICCHECK = 0x1234;
   public static final int GROUP_ID = 11;
   public static final int ERROR_PACKET_LENGTH = 7;

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
         DatagramSocket servSock = new DatagramSocket(port);
         
         
         while(true) {              
            System.out.println("\nServer waiting for connections...");
            byte[] buffer = new byte[255];  
            DatagramPacket incoming = new DatagramPacket(buffer, buffer.length);
         
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
         	
            short sum = getChecksum(data);
            
            byte errorCode = 0;
            if(magicNum != 4660 || tml <= 7 || sum != 0xFF) {
               if(magicNum != 4660) {
                  errorCode = getByteErrorCode(2);
                  System.out.println("\nMessage is invalid: No magic number.");
               
               }
               else if(tml <= 7) {
                  errorCode = getByteErrorCode(0);
                  System.out.println("\nMessage is invalid: Request too short.");
               
               }
               else if(sum != 0xFF) {
                  errorCode = getByteErrorCode(1);
                  System.out.println("\nMessage is invalid: Bad checksum.");
               
               }
               
               byte[] sendData = new byte[ERROR_PACKET_LENGTH];
               sendData[0] = 0x12;
               sendData[1] = 0x34;
               sendData[2] = (byte) ((ERROR_PACKET_LENGTH >> 8) & 0x00FF);
               sendData[3] = (byte) ((ERROR_PACKET_LENGTH >> 0) & 0x00FF);
               sendData[4] = 0;
               sendData[5] = 11;
               sendData[6] = errorCode;
               sendData[4] = (byte) ~(getChecksum(sendData));
               DatagramPacket sendPacket = 
                  new DatagramPacket(sendData, sendData.length, incoming.getAddress(), incoming.getPort());
               servSock.send(sendPacket);
               incoming = null;
               System.out.println("Sent error packet to " + host + " successfully.");
            }
            else {
               
               int totalHosts = 0;
               int i = 7;
               while (i < tml - 1) {
                  int bytesToJump = data[i] + 1;
                  totalHosts += 1;
                  i += bytesToJump;
               }
               int newTML = 7 + totalHosts * 4;
               byte[] send = new byte[newTML];
               
               send[0] = 0x12;
               send[1] = 0x34;
               send[2] = (byte) ((newTML >> 8)& 0x00FF);
               send[3] = (byte) ((newTML >> 0) & 0x00FF);
               send[4] = 0;
               send[5] = 11;
               send[6] = reqID;
               
               
               int length = 0;
               int msgLength = 0;
               int counter = 7;
               int factor = 0;
               for(int n = 7; factor < totalHosts; n += length) {
                  length = data[n + factor];
                  byte[] variable = new byte[length];
                  msgLength = n+length;
                  
                  for(int j = n+1; j < msgLength;) {
                     for(int k = 0; k < length; k++) {
                        variable[k] = data[j + factor];
                        j++;
                     }
                  }
                  
                  String hostname = new String(variable);
                  byte[] bytes = InetAddress.getByName(hostname).getAddress();
                  int ip = IPtoInt(bytes);
                  
                  ByteBuffer bb = ByteBuffer.allocate(4);
                  bb.putInt(ip);
                
                  System.arraycopy(bb.array(), 0, send, counter, 4);
                  counter = counter + 4;
                  factor++;
                  
               }
               send[4] = (byte) ~(getChecksum(send));
               DatagramPacket sendPacket = 
                  new DatagramPacket(send, send.length, incoming.getAddress(), incoming.getPort());
               servSock.send(sendPacket);
               incoming = null;
               System.out.println("Sent packet to " + host + " successfully.");
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
   
   public static byte getByteErrorCode(int type) {
      byte bec = 0;
      switch (type) {
         case 0:
            bec = (byte) (bec | (1 << 0));
            break;
         case 1:
            bec = (byte) (bec | (1 << 1));
            break;
         case 2:
            bec = (byte) (bec | (1 << 2));
            break;
         default:
            break;    
      }        
      return bec;
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
