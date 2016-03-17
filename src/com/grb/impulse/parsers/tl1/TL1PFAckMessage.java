package com.grb.impulse.parsers.tl1;

/**
 * Created by gbromfie on 11/4/15.
 */
public class TL1PFAckMessage extends TL1AckMessage {

    final static public String PREAMBLE_STR = "PF";
    final static public byte[] PREAMBLE = (PREAMBLE_STR + " ").getBytes();

    public TL1PFAckMessage() throws TL1MessageMaxSizeExceededException {
        super();
    }

    public TL1PFAckMessage(String ctag) throws TL1MessageMaxSizeExceededException {
        super("PF", ctag);
    }

    @Override
    public String getAckCode() {
        return PREAMBLE_STR;
    }
}
