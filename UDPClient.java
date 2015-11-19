import java.net.*;
import java.lang.*;
import java.io.*;

public class UDPClient {

  public static final int MAGICNUMBER = 0xA5A5;
  public static final int GROUP_ID = 11;

  public static void main(String[] args) {
    if (args.length == 3) {
        String server_name = args[0];
        int server_port = Integer.parseInt(args[1]);
        int my_port = Integer.parseInt(args[2]);

        System.out.println("\nConnecting to server...");
        sendToServer(server_name, server_port);
    }
    else {
      System.out.println("\nPlease enter:"
        + "\n\tserverName: the server's hostname"
        + "\n\tserverPort: the server's port number"
        + "\n\tmyPort: port number you want to chat on");
    }
  }
}
