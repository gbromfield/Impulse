package com.grb.impulse.parsers.tl1;

/**
 * Created by gbromfie on 11/4/15.
 */
public class TL1PFAckMessage extends TL1AckMessage {

    final static public byte[] PREAMBLE = "PF ".getBytes();

    public TL1PFAckMessage() throws TL1MessageMaxSizeExceededException {
        super();
    }
}
