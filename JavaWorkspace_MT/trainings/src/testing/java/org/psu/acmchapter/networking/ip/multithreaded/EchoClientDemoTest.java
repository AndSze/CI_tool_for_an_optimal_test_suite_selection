package org.psu.acmchapter.networking.ip.multithreaded;

import static org.junit.Assert.*;
import org.junit.Test;

import junit.framework.Assert;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.net.Socket;

public class EchoClientDemoTest {

	    @Test
	    public void testSimplePayload() {
	        byte[] emptyPayload = new byte[1001];

	        // Using Mockito
	        final Socket socket = mock(Socket.class);
	        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
	        when(socket.getOutputStream()).thenReturn(byteArrayOutputStream);

	        EchoClientDemoTest text = new EchoClientDemoTest(emptyPayload) {
	            @Override
	            protected Socket createSocket() {
	                return socket;
	            }
	        };

	        Assert.assertTrue("Message sent successfully", text.sendTo("localhost", "1234"));
	        Assert.assertEquals("whatever you wanted to send".getBytes(), byteArrayOutputStream.toByteArray());
	    }
	}
}
