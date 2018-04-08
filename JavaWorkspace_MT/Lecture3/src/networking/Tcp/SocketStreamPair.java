package networking.Tcp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * This is an implementation of the StreamPair interface on top of a TCP socket. I.e. production code.
 */
public class SocketStreamPair implements StreamPair {
  
    private Socket socket;
    
    public SocketStreamPair(Socket s) {
        this.socket = s;
    }
  
    public DataInputStream in() throws IOException {
        // return an InputStream that reads from the socket
		return (DataInputStream) socket.getInputStream();
    }
  
    public DataOutputStream out() throws IOException {
    	 // return an OutputStream that writes to the socket
    	return (DataOutputStream) socket.getOutputStream();
    }
}
  

