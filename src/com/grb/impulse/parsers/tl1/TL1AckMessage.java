package com.grb.impulse.parsers.tl1;

import com.grb.impulse.parsers.tl1.parser.CharacterList;
import com.grb.impulse.parsers.tl1.parser.ParseContext;
import com.grb.impulse.parsers.tl1.parser.TextParser;

import java.nio.ByteBuffer;
import java.text.ParseException;

abstract public class TL1AckMessage extends TL1OutputMessage {

    public static final int PREAMBLE_FINGERPRINT_SIZE = 3;

    public static final byte[] PROLOGUE = "\r\n<".getBytes();

    private static final int INITIAL_BUFFER_SIZE = 10;

    private static final TextParser ackCodeParser = new TextParser()
            .setAllowedChars(CharacterList.ALPHABETIC_MINUS_WHITESPACE_CHARS)
            .includeDelimiter(false)
            .setLengths(2, 2);

    protected String _ctag;

    protected TL1AckMessage(String ackCode, String ctag) throws TL1MessageMaxSizeExceededException {
        super(INITIAL_BUFFER_SIZE);
        _ctag = ctag;
        String ackMsg = ackCode + " " + ctag + "\r\n<";
        _buffer.writeBytes(ackMsg.getBytes());
    }

    protected TL1AckMessage() throws TL1MessageMaxSizeExceededException {
        super(INITIAL_BUFFER_SIZE);
        _ctag = null;
    }

	public boolean parse(ByteBuffer readBuffer) throws ParseException {
        while(readBuffer.hasRemaining()) {
            byte b = readBuffer.get();
            _buffer.writeByte(b);
            if (_buffer.getLength() >= getMessageStartIdx()) {
                if (_buffer.getLength() > _messageStartIdx) {
                    if (b == '<') {
                        parseTL1();
                        return true;
                    }
                }
            }
        }
        return false;
	}

    public String getCTAG() {
        return _ctag;
    }

    abstract public String getAckCode();
    
    private void parseTL1() throws ParseException {
        ParseContext pc = new ParseContext(_buffer.getBackingArray(), _messageStartIdx, _buffer.getLength() - _messageStartIdx);
        ackCodeParser.parse(pc, 2);
        manadatorySpacesParser.parse(pc);
        _ctag = ctagParser.parse(pc);
    }
}
