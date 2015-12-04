import java.net.*;
import java.lang.*;
import java.io.*;
import java.util.*;

public class UDPClient {

  public static final int MAGICNUM_LSB = 0xA5 & 0x00FF;
  public static final int MAGICNUM_MSB = 0xA5 & 0x00FF;
  public static final int GROUP_ID = 11;
  public static final String quitMsg = "bye bye birdie";
  public static final byte[] quit = quitMsg.getBytes();

  public static void main(String[] args) {
    if (args.length == 3) {
        String server_name = args[0];
        int server_port = Integer.parseInt(args[1]);
        int my_port = Integer.parseInt(args[2]);

        System.out.println("\nConnecting to server...");
        run(server_name, server_port, my_port);
    }
    else {
      System.out.println("\nPlease enter:"
        + "\n\tserverName: the server's hostname"
        + "\n\tserverPort: the server's port number"
        + "\n\tmyPort: port number you want to chat on");
    }
  }

  public static void run(String server, int port, int myPort) {
    try {
        DatagramSocket clientSocket = new DatagramSocket();
        InetAddress ipAddress = InetAddress.getByName(server);
        int port_MSB = myPort >> 8 & 0xFF;
        int port_LSB = myPort >> 0 & 0xFF;

        byte[] sendData = new byte[5];
        sendData[0] = (byte) MAGICNUM_LSB;
        sendData[1] = (byte) MAGICNUM_MSB;
        sendData[2] = (byte) port_MSB;
        sendData[3] = (byte) port_LSB;
        sendData[4] = GROUP_ID;

        DatagramPacket sendPacket =
          new DatagramPacket(sendData, sendData.length, ipAddress, port);
        clientSocket.send(sendPacket);

        byte[] buffer = new byte[255];
        DatagramPacket incoming = new DatagramPacket(buffer, buffer.length);
        System.out.println("\nReceiving data...");
        clientSocket.receive(incoming);
        InetAddress IPAddress = incoming.getAddress();
        String host = IPAddress.getHostAddress();
        System.out.println("Connected to: " + host);

        byte[] data = incoming.getData();
        int magicNum = 0xA5;
        int one = 0x01;

        boolean correctMagic = (data[0] == (byte) magicNum
                                && data[1] == (byte) magicNum);
        boolean correctGID = (data[2] == GROUP_ID);
        boolean invalidReq = (data[3] == 0x00);

        //Check if invalid request
        if (invalidReq) {
          if (data[4] == (byte) one){
            System.out.print("Request does not contain a magic Number");
          }
          else if (data[5] == (byte) one) {
            System.out.print("Length is not correct in request");
          }
          else {
            System.out.print("Port number is out of range");
          }
        }
        //If not invalid check if magic number returned is correct and whether GroupID
        //or IP address is the next bit
        else {
          if(correctMagic) {
            if (correctGID) {
                System.out.print("Magic number: " + (((data[0] & 0x00FF) << 8 | + (data[1] & 0x00FF))));
                //These print statements are for debuggin purposes
                System.out.print("\nGroup ID: " + data[2]);
                int portN = ((data[3] & 0xFF) << 8) | (data[4] & 0xFF);
                System.out.print("\nPort Number: " + portN);
                System.out.print("\nPort Number: " + incoming.getPort() + "\n");
                TCPServer(myPort);
            }
            else {
                System.out.print("Magic number: " + (((data[0] & 0x00FF) << 8 | + (data[1] & 0x00FF))));
                System.out.println("\nIP Address: " + host + " on Port " + incoming.getPort());
                //String ipIn = data[2] + data[3] + data[4] + data[5];
                int portN = ((data[6] & 0xFF) << 8) | (data[7] & 0xFF);
                TCPClient(data);
            }
          }
          else {
              System.out.println("\nInvalid response, error occurred, bad magic Number.");
              System.exit(0);
          }
        }
    }
    catch (Exception e) {
        System.out.println(e);
    }
  }

  public static void TCPServer(int port) {
    //String quitMsg = "Bye bye birdie";
    //byte[] quit = quitMsg.getBytes();

    try {
      ServerSocket servSock = new ServerSocket(port);
      System.out.println("\nWaiting for a partner to connect...");
      Socket socket = servSock.accept();
      String clientIP = socket.getInetAddress().getHostName();
      System.out.println("Connection established with " + clientIP);

      BufferedReader user = new BufferedReader(new InputStreamReader(System.in));
      DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
      DataInputStream dis = new DataInputStream(socket.getInputStream());

      while(true) {
        byte[] buffer = new byte[256];
        System.out.println("\nEnter message: ");
        String msg = user.readLine();
        String quitCheck = msg.toLowerCase();
        byte[] output = msg.getBytes();
        int length = output.length;

        if(quitCheck.equals(quitMsg)) {
            dos.write(output, 0, length);
            System.out.println("\nChat ended.");
            socket.close();
            System.exit(0);
        }

        System.out.println("\nYou: " + msg);
        dos.write(output, 0, length);
        dis.read(buffer);
        String receivedMsg = new String(buffer).trim();
        byte[] incoming = receivedMsg.getBytes();
        String received = new String(incoming);
        String receivedCheck = received.toLowerCase();

        if(receivedCheck.equals(quitMsg)) {
            System.out.println("\nChat ended.");
            socket.close();
            System.exit(0);
        }

        System.out.println(clientIP + ": " + received);

      }
    }
    catch (Exception e) {
        System.out.println(e);
    }
  }

  public static void TCPClient(byte[] data) {
    int magicNum = ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);
    byte[] variable = new byte[4];
    int servPort = ((data[6] & 0xFF) << 8) | (data[7] & 0xFF);

    int j = 0;
    for(int i = 2; i < 6; i++) {
        variable[j] = data[i];
        j++;
    }

    try{
      InetAddress servIP = InetAddress.getByAddress(variable);
      Socket socket = new Socket(servIP, servPort);

      while(true) {
        BufferedReader user = new BufferedReader(new InputStreamReader(System.in));
        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
        DataInputStream dis = new DataInputStream(socket.getInputStream());

        byte[] buffer = new byte[256];
        System.out.println("\nEnter message: ");
        String msg = user.readLine();
        String quitCheck = msg.toLowerCase();
        byte[] output = msg.getBytes();
        int length = output.length;

        if(quitCheck.equals(quitMsg)) {
            dos.write(output, 0, length);
            System.out.println("\nChat ended.");
            socket.close();
            System.exit(0);
        }

        System.out.println("\nYou: " + msg);
        dos.write(output, 0, length);
        dis.read(buffer);
        String receivedMsg = new String(buffer).trim();
        byte[] incoming = receivedMsg.getBytes();
        String received = new String(incoming);
        String receivedCheck = received.toLowerCase();

        if(receivedCheck.equals(quitMsg)) {
            System.out.println("\nChat ended.");
            socket.close();
            System.exit(0);
        }

        System.out.println(servIP + ": " + received);
      }
    }
    catch (Exception e) {
        System.out.println(e);
    }
  }
}
