package com.grb.impulse.parsers.tl1.parser;

/**
 * Created by gbromfie on 11/13/15.
 */
public class ParseContext {
    final public byte[] buffer;
    final public int index;
    final public int length;
    public int mark;

    public ParseContext(byte[] buf) {
        this(buf, 0, buf.length);
    }

    public ParseContext(byte[] buf, int off, int len) {
        buffer = buf;
        index = off;
        length = len;
        mark = index;
    }
}
