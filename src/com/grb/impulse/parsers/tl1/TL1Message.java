package com.grb.impulse.parsers.tl1;

import com.grb.impulse.parsers.tl1.parser.CharacterList;
import com.grb.impulse.parsers.tl1.parser.TextParser;

import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.ArrayList;

abstract public class TL1Message {

    protected static final TextParser optionalSpacesParser = new TextParser()
            .setAllowedChars(CharacterList.NO_CHARS)
            .addAllowedChar(' ')
            .setDelimiterChars(CharacterList.ALL_CHARS)
            .removeDelimeterChar(' ')
            .includeDelimiter(false);

    protected static final TextParser manadatorySpacesParser = new TextParser(optionalSpacesParser)
            .setLengths(1, Integer.MAX_VALUE);

    protected static final TextParser optionalWhitespaceParser = new TextParser()
            .setAllowedChars(CharacterList.WHITESPACE_CHARS)
            .setDelimiterChars(CharacterList.ALL_CHARS)
            .removeDelimeterChars(CharacterList.WHITESPACE_CHARS)
            .includeDelimiter(false);

    protected static final TextParser manadatoryWhitespaceParser = new TextParser(optionalWhitespaceParser)
            .setLengths(1, Integer.MAX_VALUE);

    protected static final TextParser ctagParser = new TextParser()
            .setAllowedChars(CharacterList.ALPHABETIC_MINUS_WHITESPACE_CHARS)
            .setDelimiterChars(CharacterList.WHITESPACE_CHARS)
            .includeDelimiter(false)
            .setLengths(1, Integer.MAX_VALUE);

    protected enum StackElement {
        QUOTE,
        COMMENT
    };

    protected com.grb.util.ByteBuffer _buffer;
    protected ArrayList<StackElement> _stack;

    protected TL1Message(int initialCapacity) {
        _buffer = new com.grb.util.ByteBuffer(initialCapacity);
        _stack = new ArrayList<>();
    }

    abstract public boolean parse(ByteBuffer readBuffer) throws TL1MessageMaxSizeExceededException, ParseException;

    protected boolean parse(byte[] preamble, int offset, int length) throws TL1MessageMaxSizeExceededException, ParseException {
        ByteBuffer readBuffer = ByteBuffer.allocate(preamble.length);
        readBuffer.put(preamble, offset, length);
        readBuffer.flip();
        return parse(readBuffer);
    }

    public com.grb.util.ByteBuffer getBuffer() {
        return _buffer;
    }

    public String toString() {
        return new String(_buffer.getBackingArray(), 0, _buffer.getLength());
    }
}
