package com.grb.impulse.parsers.tl1;

/**
 * Created by gbromfie on 11/4/15.
 */
public class TL1IPAckMessage extends TL1AckMessage {

    final static public String PREAMBLE_STR = "IP";
    final static public byte[] PREAMBLE = (PREAMBLE_STR + " ").getBytes();

    public TL1IPAckMessage() throws TL1MessageMaxSizeExceededException {
        super();
    }

    public TL1IPAckMessage(String ctag) throws TL1MessageMaxSizeExceededException {
        super("IP", ctag);
    }

    @Override
    public String getAckCode() {
        return PREAMBLE_STR;
    }
}
