package com.grb.impulse.parsers.tl1;

/**
 * Created by gbromfie on 11/4/15.
 */
public class TL1NGAckMessage extends TL1AckMessage {

    final static public byte[] PREAMBLE = "NG ".getBytes();

    public TL1NGAckMessage() throws TL1MessageMaxSizeExceededException {
        super();
    }
}
