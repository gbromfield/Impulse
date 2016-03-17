package com.grb.impulse.parsers.tl1;

/**
 * Created by gbromfie on 11/4/15.
 */
public class TL1NGAckMessage extends TL1AckMessage {

    final static public String PREAMBLE_STR = "NG";
    final static public byte[] PREAMBLE = (PREAMBLE_STR + " ").getBytes();

    public TL1NGAckMessage() throws TL1MessageMaxSizeExceededException {
        super();
    }

    public TL1NGAckMessage(String ctag) throws TL1MessageMaxSizeExceededException {
        super("NG", ctag);
    }

    @Override
    public String getAckCode() {
        return PREAMBLE_STR;
    }
}
