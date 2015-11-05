package com.grb.impulse.parsers.tl1;

/**
 * Created by gbromfie on 11/4/15.
 */
public class TL1OKAckMessage extends TL1AckMessage {

    final static public byte[] preamble = "OK ".getBytes();

    public TL1OKAckMessage() {
        super(preamble);
    }
}
