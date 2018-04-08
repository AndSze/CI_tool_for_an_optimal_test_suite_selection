package networking.Tcp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * The interface which all of your code base use for TCP connections 
 * instead of directly depending on the java.net.* classes.
 */
public interface StreamPair {
    public InputStream in();
    public OutputStream out();
}
  
/*
 * This is an implementation of the StreamPair interface on top of a TCP socket. I.e. production code.
 
public class SocketStreamPair implements StreamPair {
  
    private Socket socket;
  
    public SocketStreamPair(Socket s) {
        this.socket = s;
    }
  
    public InputStream in() {
        // return an InputStream that reads from the socket
    }
  
    public OutputStream out() {
        // return an OutputStream that writes to the socket
    }
}
  

 * This is a mock implementation of the StreamPair interface, letting you
 * test stuff that operates a network connection without actually going 
 * to the network layer.
 
public class MockStreamPair implements StreamPair {
  
    private InputStream data;
    private OutputStream output;
  
    public MockStreamPair(byte[] data) {
        this.data = new ByteArrayInputStream(data);
        this.output = new ByteArrayOutputStream();
    }
  
    public InputStream in() {
        return data;
    }
  
    public OutputStream out() {
        return output;
    }
  

     * Exposes what was written to this StreamPair for the tests' purposes.
     
    public byte[] getInput() {
        return ((ByteArrayOutputStream) output).toByteArray();
    }
}
*/