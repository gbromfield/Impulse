package com.grb.impulse.parsers.tl1;

/**
 * Created by gbromfie on 11/4/15.
 */
public class TL1NAAckMessage extends TL1AckMessage {

    final static public String PREAMBLE_STR = "NA";
    final static public byte[] PREAMBLE = (PREAMBLE_STR + " ").getBytes();

    public TL1NAAckMessage() throws TL1MessageMaxSizeExceededException {
        super();
    }

    public TL1NAAckMessage(String ctag) throws TL1MessageMaxSizeExceededException {
        super("NA", ctag);
    }

    @Override
    public String getAckCode() {
        return PREAMBLE_STR;
    }
}
