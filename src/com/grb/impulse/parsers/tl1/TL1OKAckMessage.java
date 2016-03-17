package com.grb.impulse.parsers.tl1;

/**
 * Created by gbromfie on 11/4/15.
 */
public class TL1OKAckMessage extends TL1AckMessage {

    final static public String PREAMBLE_STR = "OK";
    final static public byte[] PREAMBLE = (PREAMBLE_STR + " ").getBytes();

    public TL1OKAckMessage() throws TL1MessageMaxSizeExceededException {
        super();
    }

    public TL1OKAckMessage(String ctag) throws TL1MessageMaxSizeExceededException {
        super("OK", ctag);
    }

    @Override
    public String getAckCode() {
        return PREAMBLE_STR;
    }
}
