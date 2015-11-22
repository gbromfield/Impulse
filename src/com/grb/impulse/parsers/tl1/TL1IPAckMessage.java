package com.grb.impulse.parsers.tl1;

/**
 * Created by gbromfie on 11/4/15.
 */
public class TL1IPAckMessage extends TL1AckMessage {

    final static public byte[] PREAMBLE = "IP ".getBytes();

    public TL1IPAckMessage() throws TL1MessageMaxSizeExceededException {
        super();
    }

    public TL1IPAckMessage(String ctag) throws TL1MessageMaxSizeExceededException {
        super("IP", ctag);
    }
}
