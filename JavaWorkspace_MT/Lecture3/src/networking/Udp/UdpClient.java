package networking.Udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UdpClient {
  public static void main(String[] args) {
    try {
      // args contain server hostname and message content
      if(args.length < 1) {
    	  System.out.println("Usage: UDPClient <server host name>");
    	  System.exit(-1);
      }
   	  byte[] buffer = new byte[1024];
      InetAddress aHost = InetAddress.getByName(args[0]);
      int serverPort = 9876;
      DatagramSocket aSocket = new DatagramSocket();
      Scanner scan = new Scanner(System.in);
      String line = "";
      while(true) {
    	  System.out.println("Enter your request: ");
    	  if(scan.hasNextLine())
    		  line = scan.nextLine();
    	  DatagramPacket request = new DatagramPacket(line.getBytes(), line.length(), aHost, serverPort);
    	  aSocket.send(request);
    	  DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
    	  aSocket.receive(reply);
    	  System.out.println("Reply: " + new String(reply.getData(), 0, reply.getLength()));
      }
    } catch (SocketException ex) {
      Logger.getLogger(UdpClient.class.getName()).log(Level.SEVERE, null, ex);
    } catch (UnknownHostException ex) {
      Logger.getLogger(UdpClient.class.getName()).log(Level.SEVERE, null, ex);
    } catch (IOException ex) {
      Logger.getLogger(UdpClient.class.getName()).log(Level.SEVERE, null, ex);
    }
  } 
}