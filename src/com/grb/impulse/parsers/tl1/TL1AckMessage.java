package com.grb.impulse.parsers.tl1;

import java.nio.ByteBuffer;

abstract public class TL1AckMessage extends TL1Message {

    private static final int INITIAL_BUFFER_SIZE = 10;

    protected TL1AckMessage(byte[] preamble) {
        super(INITIAL_BUFFER_SIZE);
        _buffer.writeBytes(preamble);
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
}
