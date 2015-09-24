package com.grb.impulse.parsers.tl1;

import java.nio.ByteBuffer;

public class TL1AckMessage implements TL1Message {

	private TL1AckMessageType _type;
	private com.grb.util.ByteBuffer _buffer;
	
	public TL1AckMessage(TL1AckMessageType type, String prtclStr) {
		_type = type;
		_buffer = new com.grb.util.ByteBuffer(10);
		_buffer.write(prtclStr.getBytes());
	}

	public boolean parse(ByteBuffer readBuffer) {
        while(readBuffer.hasRemaining()) {
            byte b = readBuffer.get();
            _buffer.writeByte(b);
            if (b == '<') {
            	return true;
            }
        }
        return false;
	}
	
	public TL1AckMessageType getType() {
		return _type;
	}
	
	public com.grb.util.ByteBuffer getBuffer() {
		return _buffer;
	}
	
	static public TL1AckMessage fingerprint(com.grb.util.ByteBuffer prtclBytes) {
		String prtclStr = new String(prtclBytes.getBackingArray());
		TL1AckMessageType ackMsgType = TL1AckMessageType.getType(prtclStr);
		if (ackMsgType == null) {
			return null;
		} else {
			return new TL1AckMessage(ackMsgType, prtclStr);
		}
	}
}
