package com.grb.impulse.parsers.tl1;

/**
 * Created by gbromfie on 11/4/15.
 */
public class TL1IPAckMessage extends TL1AckMessage {

    final static public byte[] preamble = "IP ".getBytes();

    public TL1IPAckMessage() {
        super(preamble);
    }
}
