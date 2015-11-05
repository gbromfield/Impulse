package com.grb.impulse.parsers.tl1;

/**
 * Created by gbromfie on 11/4/15.
 */
public class TL1PFAckMessage extends TL1AckMessage {

    final static public byte[] preamble = "PF ".getBytes();

    public TL1PFAckMessage() {
        super(preamble);
    }
}
