package com.grb.impulse.parsers.tl1;

/**
 * Created by gbromfie on 11/4/15.
 */
public class TL1RLAckMessage extends TL1AckMessage {

    final static public byte[] preamble = "RL ".getBytes();

    public TL1RLAckMessage() {
        super(preamble);
    }
}
