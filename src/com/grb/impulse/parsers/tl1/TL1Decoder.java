package com.grb.impulse.parsers.tl1;

import java.nio.ByteBuffer;

public class TL1Decoder {
	static private final int ACK_MESSAGE_SAMPLE_SIZE = 3;
	
	private com.grb.util.ByteBuffer _buffer;
	private int _bytesLeftToRead;
	private TL1Message _message;
	
    public TL1Decoder() {
    	_buffer = new com.grb.util.ByteBuffer(ACK_MESSAGE_SAMPLE_SIZE);
    }
    
    public TL1Message decodeTL1Message(ByteBuffer readBuffer) {
        int available = readBuffer.limit() - readBuffer.position();
        if (available == 0) {
            return null;
        }
        if (_bytesLeftToRead > 0) {
            int bytesToRead = Math.min(_bytesLeftToRead, available);
            byte[] data = new byte[bytesToRead];
            readBuffer.get(data);
            _buffer.write(data);               
            _bytesLeftToRead -= bytesToRead;
            if (_bytesLeftToRead == 0) {
            	_message = TL1AckMessage.fingerprint(_buffer);
            	if (_message == null) {
//            		if ()
            	}
            }
        }
        return null;
    }
 
    public void reset() {
    	_bytesLeftToRead = ACK_MESSAGE_SAMPLE_SIZE;
    	_buffer.reset();
    }
    
    /**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
