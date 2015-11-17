package com.grb.impulse.parsers.tl1;

import java.nio.ByteBuffer;

abstract public class TL1AckMessage extends TL1OutputMessage {

    public static final int PREAMBLE_FINGERPRINT_SIZE = 3;

    public static final byte[] PROLOGUE = "\r\n<".getBytes();

    private static final int INITIAL_BUFFER_SIZE = 10;

    private enum ParseState {

    }

    private String _ctag;

    protected TL1AckMessage() throws TL1MessageMaxSizeExceededException {
        super(INITIAL_BUFFER_SIZE);
        _ctag = null;
    }

	public boolean parse(ByteBuffer readBuffer) {
        while(readBuffer.hasRemaining()) {
            byte b = readBuffer.get();
            _buffer.writeByte(b);
            if (_buffer.getLength() >= getMessageStartIdx()) {
                if (_buffer.getLength() > _messageStartIdx) {
                    if (b == '<') {
                        return true;
                    }
                }
            }
        }
        return false;
	}

    public String getCTAG() {
        return _ctag;
    }
}
