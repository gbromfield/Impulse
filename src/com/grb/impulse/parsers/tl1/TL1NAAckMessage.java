package com.grb.impulse.parsers.tl1;

/**
 * Created by gbromfie on 11/4/15.
 */
public class TL1NAAckMessage extends TL1AckMessage {

    final static public byte[] PREAMBLE = "NA ".getBytes();

    public TL1NAAckMessage() throws TL1MessageMaxSizeExceededException {
        super();
    }
}
