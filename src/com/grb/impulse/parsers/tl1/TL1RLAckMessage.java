package com.grb.impulse.parsers.tl1;

/**
 * Created by gbromfie on 11/4/15.
 */
public class TL1RLAckMessage extends TL1AckMessage {

    final static public String PREAMBLE_STR = "RL";
    final static public byte[] PREAMBLE = (PREAMBLE_STR + " ").getBytes();

    public TL1RLAckMessage() throws TL1MessageMaxSizeExceededException {
        super();
    }

    public TL1RLAckMessage(String ctag) throws TL1MessageMaxSizeExceededException {
        super("RL", ctag);
    }

    @Override
    public String getAckCode() {
        return PREAMBLE_STR;
    }
}
