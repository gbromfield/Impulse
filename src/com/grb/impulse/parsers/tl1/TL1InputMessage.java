package com.grb.impulse.parsers.tl1;

import com.grb.impulse.parsers.tl1.parser.CharacterList;
import com.grb.impulse.parsers.tl1.parser.ParseContext;
import com.grb.impulse.parsers.tl1.parser.TextParser;

import java.nio.ByteBuffer;
import java.text.ParseException;

public class TL1InputMessage extends TL1Message {
    /**
     * No max size indicated in the spec so arbitrarily capped at 16384
     * to have a limit for parsing.
     */
    public static int MAX_SIZE = 16384;

    private static final int INITIAL_BUFFER_SIZE = 100;

    private static final TextParser verbParser = new TextParser()
            .setAllowedChars(CharacterList.ALPHABETIC_CHARS)
            .setDelimiterChars(CharacterList.NO_CHARS)
            .addDelimeterChar('-')
            .addDelimeterChar(':')
            .setLengths(1, Integer.MAX_VALUE)
            .includeDelimiter(false);

    private static final TextParser cmdCodeDelParser = new TextParser()
            .setAllowedChars(CharacterList.NO_CHARS)
            .addAllowedChar('-')
            .addDelimeterChars(CharacterList.ALPHABETIC_CHARS)
            .removeDelimeterChar('-')
            .includeDelimiter(false);

    private static final TextParser modParser = new TextParser()
            .setAllowedChars(CharacterList.ALPHABETIC_CHARS)
            .setDelimiterChars(CharacterList.NO_CHARS)
            .addDelimeterChar('-')
            .addDelimeterChar(':')
            .includeDelimiter(false);

    private static final TextParser colonDelParser = new TextParser()
            .setAllowedChars(CharacterList.WHITESPACE_CHARS)
            .addAllowedChar(':')
            .addDelimeterChars(CharacterList.ALPHABETIC_MINUS_WHITESPACE_CHARS)
            .removeDelimeterChar(':')
            .includeDelimiter(false);

    private static final TextParser blockParser = new TextParser()
            .setAllowedChars(CharacterList.ALPHABETIC_CHARS)
            .removeAllowedChar(':')
            .removeAllowedChar(';')
            .setDelimiterChars(CharacterList.NO_CHARS)
            .addDelimeterChar(':')
            .addDelimeterChar(';')
            .includeDelimiter(false);

    private String _cmdCode;
    private String _verb;
    private String _mod1;
    private String _mod2;
    private String _tid;
    private String _aid;
    private String _ctag;

    public TL1InputMessage() throws TL1MessageMaxSizeExceededException {
        super(INITIAL_BUFFER_SIZE);
    }

    public String getCmdCode() {
        return _cmdCode;
    }

    public String getVerb() {
        return _verb;
    }

    public String getMod1() {
        return _mod1;
    }

    public String getMod2() {
        return _mod2;
    }

    public String getTid() { return _tid; }

    public String getAid() { return _aid; }

    public String getCTAG() {
        return _ctag;
    }

    public boolean parse(ByteBuffer readBuffer) throws TL1MessageMaxSizeExceededException, ParseException {
        while (readBuffer.hasRemaining()) {
            byte b = readBuffer.get();
            _buffer.writeByte(b);
            if (_buffer.getLength() > MAX_SIZE) {
                throw new TL1MessageMaxSizeExceededException(String.format("Error: maximum %d character size of input message reached", MAX_SIZE));
            }
            if ((b == ';') && (_stack.size() == 0)) {
                parseTL1();
                return true;
            } else if (b == 4) {    // EOT
                return true;
            }
        }
        return false;
    }

    private void parseTL1() throws ParseException {
        ParseContext pc = new ParseContext(_buffer.getBackingArray(), 0, _buffer.getLength());

        _verb = verbParser.parse(pc);
        if (_verb.equals(";")) {
            return;
        }
        cmdCodeDelParser.parse(pc);
        _mod1 = modParser.parse(pc);
        cmdCodeDelParser.parse(pc);
        _mod2 = modParser.parse(pc);
        StringBuilder bldr = new StringBuilder();
        bldr.append(_verb);
        if (_mod1.length() > 0) {
            bldr.append("-");
            bldr.append(_mod1);
        }
        if (_mod2.length() > 0) {
            bldr.append("-");
            bldr.append(_mod2);
        }
        _cmdCode = bldr.toString();
        colonDelParser.parse(pc, 1);
        _tid = blockParser.parse(pc);
        colonDelParser.parse(pc, 1);
        _aid = blockParser.parse(pc);
        colonDelParser.parse(pc, 1);
        _ctag = blockParser.parse(pc);
    }
}