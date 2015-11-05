package com.grb.impulse.parsers.tl1;

import java.nio.ByteBuffer;
import java.util.ArrayList;

abstract public class TL1Message {

    protected enum StackElement {
        QUOTE,
        COMMENT
    };

    protected com.grb.util.ByteBuffer _buffer;
    protected ArrayList<StackElement> _stack;

    protected TL1Message(int initialCapacity) {
        _buffer = new com.grb.util.ByteBuffer(initialCapacity);
        _stack = new ArrayList<>();
    }

    abstract public boolean parse(ByteBuffer readBuffer) throws TL1MessageMaxSizeExceededException;

    protected boolean parse(byte[] preamble, int offset, int length) throws TL1MessageMaxSizeExceededException {
        ByteBuffer readBuffer = ByteBuffer.allocate(preamble.length);
        readBuffer.put(preamble, offset, length);
        readBuffer.flip();
        return parse(readBuffer);
    }

    public com.grb.util.ByteBuffer getBuffer() {
        return _buffer;
    }

    public String toString() {
        return new String(_buffer.getBackingArray(), 0, _buffer.getLength());
    }
}
