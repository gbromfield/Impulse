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

    public static final byte[] PREAMBLE = "\r\n\n   ".getBytes();

    public static final int FINGERPRINT_SIZE = 4;

    public static final byte[] RESPONSE_CODE = "\r\nM ".getBytes();
    public static final byte[] CRITICAL_ALARM_CODE = "\r\n*C".getBytes();
    public static final byte[] MAJOR_ALARM_CODE = "\r\n**".getBytes();
    public static final byte[] MINOR_ALARM_CODE = "\r\n* ".getBytes();
    public static final byte[] NON_ALARM_CODE = "\r\nA ".getBytes();

    public static int DEFAULT_INITIAL_BUFFER_SIZE = 1024;

    protected int _messageStartIdx;

    protected TL1OutputMessage(int bufferSize) throws TL1MessageMaxSizeExceededException {
        this(bufferSize, null, 0, -1, 0);
    }

    protected TL1OutputMessage(byte[] preamble, int offset, int messageStartIdx, int length) throws TL1MessageMaxSizeExceededException {
        this(DEFAULT_INITIAL_BUFFER_SIZE, preamble, offset, messageStartIdx, length);
    }

    protected TL1OutputMessage(int bufferSize, byte[] preamble, int offset, int messageStartIdx, int length) throws TL1MessageMaxSizeExceededException {
        super(bufferSize);
        _messageStartIdx = messageStartIdx;
        if (preamble != null) {
            _buffer.write(preamble, offset, length);
        }
    }

    protected boolean parse(byte[] preamble, int offset, int messageStartIdx, int length) throws TL1MessageMaxSizeExceededException {
        _messageStartIdx = messageStartIdx;
        return parse(preamble, offset, length);
    }

    public int getMessageStartIdx() {
        return _messageStartIdx;
    }
}
