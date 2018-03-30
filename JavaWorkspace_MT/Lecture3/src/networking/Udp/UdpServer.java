package networking.Udp;
 
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UdpServer {
    public static void main(String[] args) {
      try {
        // args contain message content and server hostname
        DatagramSocket aSocket = new DatagramSocket(9876);
        byte[] buffer = new byte[1024];
        while(true) {
          DatagramPacket request = new DatagramPacket(buffer, buffer.length);
          System.out.println("Waiting for request...");
          aSocket.receive(request);
          DatagramPacket reply = new DatagramPacket(request.getData(), 
	        request.getLength(), request.getAddress(), request.getPort());
          aSocket.send(reply); 
        }
      } catch (SocketException ex) {
        Logger.getLogger(UdpServer.class.getName()).log(Level.SEVERE, null, ex);
      } catch (IOException ex) {
        Logger.getLogger(UdpServer.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
}

