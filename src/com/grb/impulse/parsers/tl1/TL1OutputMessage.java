package com.grb.impulse.parsers.tl1;

/**
 * Created by gbromfie on 11/4/15.
 */
abstract public class TL1OutputMessage extends TL1Message {
    /**
     * By spec the largest TL1 segment size can be 4096.
     * Doubled for buffer.
     */
    public static int MAX_SIZE = 8192;

    final static public byte[] responseCode = "\r\nM ".getBytes();
    final static public byte[] criticalAlarmCode = "\r\n*C".getBytes();
    final static public byte[] majorAlarmCode = "\r\n**".getBytes();
    final static public byte[] minorAlarmCode = "\r\n* ".getBytes();
    final static public byte[] nonAlarmCode = "\r\nA ".getBytes();

    private static final int INITIAL_BUFFER_SIZE = 500;

    protected TL1OutputMessage() {
        super(INITIAL_BUFFER_SIZE);
    }
}
