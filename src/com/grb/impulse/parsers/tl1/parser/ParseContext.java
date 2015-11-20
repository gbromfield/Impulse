package com.grb.impulse.parsers.tl1.parser;

/**
 * Created by gbromfie on 11/13/15.
 */
public class ParseContext {
    public byte[] buffer;
    public int index;

    public ParseContext(byte[] buf) {
        this(buf, 0, buf.length);
    }

    public ParseContext(byte[] buf, int off, int length) {
        buffer = buf;
        index = off;
    }
}
