package networking.Tcp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * The interface which all of your code base use for TCP connections 
 * instead of directly depending on the java.net.* classes.
 */
public interface StreamPair {
    public DataInputStream in() throws IOException;
    public DataOutputStream out() throws IOException;
}
  
