package com.grb.impulse.parsers.tl1;

import java.nio.ByteBuffer;

/**
 * Created by gbromfie on 11/4/15.
 */
public class TL1AOMessage extends TL1OutputMessage {

    public TL1AOMessage(byte[] preamble, int offset, int length) throws TL1MessageMaxSizeExceededException {
        super();
        parse(preamble, offset, length);
    }

    @Override
    public boolean parse(ByteBuffer readBuffer) throws TL1MessageMaxSizeExceededException {
        while(readBuffer.hasRemaining()) {
            byte b = readBuffer.get();
            _buffer.writeByte(b);
            if (_buffer.getLength() > MAX_SIZE) {
                throw new TL1MessageMaxSizeExceededException(String.format("Error: maximum %d character size of output message reached", TL1OutputMessage.MAX_SIZE));
            }
            if ((b == ';') && (_stack.size() == 0)) {
                return true;
            }
        }
        return false;
    }
}
