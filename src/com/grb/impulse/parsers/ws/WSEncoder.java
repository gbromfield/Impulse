package com.grb.impulse.parsers.ws;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.bind.DatatypeConverter;

public class WSEncoder {
    public static final String SERVER_KEY = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    public WSEncoder() {
    }

    /**
     * Very simple encoder 
     * 
     * @return Flipped ByteBuffer ready for sending
     */
    public ByteBuffer toByteBuffer(WSMessage msg) {
        ByteBuffer buffer = ByteBuffer.allocate(msg.payload.length + 100);
        byte b1 = (byte)(msg.fin << 7);
        b1 = (byte)(b1 | (msg.rsv1 << 6));
        b1 = (byte)(b1 | (msg.rsv2 << 5));
        b1 = (byte)(b1 | (msg.rsv3 << 4));
        b1 = (byte)(b1 | msg.opcode);
        buffer.put(b1);  
        byte b2 = (byte)(msg.maskPresent << 7);
        if (msg.payloadLength <= WSDecoder.MAX_SIMPLE_PAYLOAD_LENGTH) {
            b2 = (byte)(b2 | msg.payloadLength);
            buffer.put(b2);
        } else {
            throw new IllegalArgumentException("message too large");
        }
        buffer.put(msg.payload);
        buffer.flip();
        return buffer;
    }

    public String encodeAcceptToken(String key) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        String c = key + SERVER_KEY;
        MessageDigest cript = MessageDigest.getInstance("SHA-1");
        cript.reset();
        cript.update(c.getBytes("UTF-8"));
        byte[] data = cript.digest();
        String encoded = DatatypeConverter.printBase64Binary(data);
        return encoded;
    }

}
