import java.net.*;
import java.lang.*;
import java.io.*;
import java.util.*;

public class UDPClient {

  public static final int MAGICNUM_LSB = 0xA5;
  public static final int MAGICNUM_MSB = 0xA5;
  public static final int GROUP_ID = 11;
  public static final String quitMsg = "Bye bye Birdie";
  public static final byte[] quit = quitMsg.getBytes();

  public static void main(String[] args) {
    if (args.length == 3) {
        String server_name = args[0];
        int server_port = Integer.parseInt(args[1]);
        int my_port = Integer.parseInt(args[2]);

        System.out.println("\nConnecting to server...");
        run(server_name, server_port);
    }
    else {
      System.out.println("\nPlease enter:"
        + "\n\tserverName: the server's hostname"
        + "\n\tserverPort: the server's port number"
        + "\n\tmyPort: port number you want to chat on");
    }
  }

  public static void run(String server, int port) {
    try {
        DatagramSocket clientSocket = new DatagramSocket();
        InetAddress ipAddress = InetAddress.getByName(server);
        int port_LSB = port << 8;
        int port_MSB = port;

        byte[] sendData = new byte[5];
        sendData[0] = (byte) MAGICNUM_LSB;
        sendData[1] = (byte) MAGICNUM_MSB;
        sendData[2] = (byte) port_LSB;
        sendData[3] = (byte) port_MSB;
        sendData[4] = GROUP_ID;

        DatagramPacket sendPacket =
          new DatagramPacket(sendData, sendData.length, ipAddress, port);

        byte[] buffer = new byte[255];
        DatagramPacket incoming = new DatagramPacket(buffer, buffer.length);
        byte[] data = incoming.getData();

        if(data.length == 2) {
            TCPServer(port);
        }
        else if(data.length == 7){
            TCPClient(data);
        }
        else {
            System.out.println("\nSomething wrong happened.");
            System.exit(0);
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

      while(true) {

        BufferedReader user = new BufferedReader(new InputStreamReader(System.in));
        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
        DataInputStream dis = new DataInputStream(socket.getInputStream());

        byte[] output = new byte[1024];
        byte[] buffer = new byte[1024];
        System.out.println("Enter message: ");
        String msg = user.readLine();
        output = msg.getBytes();
        int length = output.length;

        if(Arrays.equals(output, quit)) {
            System.out.println("\nChat ended.");
            System.exit(0);
        }

        System.out.println("\nYou: " + msg);
        dos.write(output, 0, length);
        dis.read(buffer);
        String receivedMsg = new String(buffer);
        System.out.println(clientIP + ": " + receivedMsg);

      }
    }
    catch (Exception e) {
        System.out.println(e);
    }
  }

  public static void TCPClient(byte[] data) {
    int magicNum = ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);
    byte[] variable = new byte[4];
    int servPort = data[6];

    int j = 0;
    for(int i = 2; i < 6; i++) {
        variable[j] = data[i];
        j++;
    }
    String servIP = new String(variable);

    try{
      Socket socket = new Socket(servIP, servPort);

      while(true) {
        BufferedReader user = new BufferedReader(new InputStreamReader(System.in));
        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
        DataInputStream dis = new DataInputStream(socket.getInputStream());

        byte[] output = new byte[1024];
        byte[] buffer = new byte[1024];
        System.out.println("Enter message: ");
        String msg = user.readLine();
        output = msg.getBytes();
        int length = output.length;

        if(Arrays.equals(output, quit)) {
            System.out.println("\nChat ended.");
            System.exit(0);
        }

        System.out.println("\nYou: " + msg);
        dos.write(output, 0, length);
        dis.read(buffer);

        if(Arrays.equals(buffer, quit)) {
            System.out.println("\nChat ended.");
            System.exit(0);
        }
        String receivedMsg = new String(buffer);
        System.out.println(servIP + ": " + receivedMsg);
      }
    }
    catch (Exception e) {
        System.out.println(e);
    }
  }
}
