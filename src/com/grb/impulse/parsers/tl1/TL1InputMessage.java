package com.grb.impulse.parsers.tl1;

import java.nio.ByteBuffer;

public class TL1InputMessage extends TL1Message {
    /**
     * No max size indicated in the spec so arbitrarily capped at 16384
     * to have a limit for parsing.
     */
    public static int MAX_SIZE = 16384;

    private static final int INITIAL_BUFFER_SIZE = 100;

    public TL1InputMessage(byte[] preamble, int offset, int length) throws TL1MessageMaxSizeExceededException {
        super(INITIAL_BUFFER_SIZE);
        parse(preamble, offset, length);
    }

    public boolean parse(ByteBuffer readBuffer) throws TL1MessageMaxSizeExceededException {
        while(readBuffer.hasRemaining()) {
            byte b = readBuffer.get();
            _buffer.writeByte(b);
            if (_buffer.getLength() > MAX_SIZE) {
                throw new TL1MessageMaxSizeExceededException(String.format("Error: maximum %d character size of input message reached", MAX_SIZE));
            }
            if ((b == ';') && (_stack.size() == 0)) {
                return true;
            }
        }
        return false;
    }
}
