package testTcpServ;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import networking.Tcp.StreamPair;

/**
 * This is a mock implementation of the StreamPair interface, letting you
 * test stuff that operates a network connection without actually going 
 * to the network layer.
 */
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
  
    /**
     * Exposes what was written to this StreamPair for the tests' purposes.
     */
    public byte[] getInput() {
        return ((ByteArrayOutputStream) output).toByteArray();
    }
}

