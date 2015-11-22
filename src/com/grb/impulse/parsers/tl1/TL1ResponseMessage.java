package com.grb.impulse.parsers.tl1;

import com.grb.impulse.parsers.tl1.parser.CharacterList;
import com.grb.impulse.parsers.tl1.parser.ParseContext;
import com.grb.impulse.parsers.tl1.parser.TextParser;

import java.nio.ByteBuffer;
import java.text.ParseException;

/**
 * Created by gbromfie on 11/4/15.
 */
public class TL1ResponseMessage extends TL1OutputMessage {

    private static final TextParser completonCodeParser = new TextParser()
            .setAllowedChars(CharacterList.ALPHABETIC_MINUS_WHITESPACE_CHARS)
            .setDelimiterChars(CharacterList.WHITESPACE_CHARS)
            .includeDelimiter(false);

    private String _tid;
    private String _ctag;
    private String _complCode;

    public TL1ResponseMessage(byte[] preamble, int offset, int messageStartIdx, int length) throws TL1MessageMaxSizeExceededException {
        super(preamble, offset, messageStartIdx, length);
        _tid = null;
        _ctag = null;
        _complCode = null;
    }

    public String getTid() { return _tid; }

    public String getCTAG() {
        return _ctag;
    }

    public String getComplCode() { return _complCode; }

    @Override
    public boolean parse(ByteBuffer readBuffer) throws TL1MessageMaxSizeExceededException, ParseException {
        while(readBuffer.hasRemaining()) {
            byte b = readBuffer.get();
            _buffer.writeByte(b);
            if (_buffer.getLength() > MAX_SIZE) {
                throw new TL1MessageMaxSizeExceededException(String.format("Error: maximum %d character size of output message reached", TL1OutputMessage.MAX_SIZE));
            }
            if ((b == ';') && (_stack.size() == 0)) {
                parseTL1();
                return true;
            }
        }
        return false;
    }

    private void parseTL1() throws ParseException {
        ParseContext pc = new ParseContext(_buffer.getBackingArray(), _messageStartIdx, _buffer.getLength() - _messageStartIdx);
        optionalWhitespaceParser.parse(pc);
        try {
            _tid = quotedsidParser.parse(pc);
        } catch(ParseException e) {
            _tid = sidParser.parse(pc);
        }
        manadatorySpacesParser.parse(pc);
        dateParser.parse(pc);
        manadatorySpacesParser.parse(pc);
        timeParser.parse(pc);
        manadatoryWhitespaceParser.parse(pc);
        responseCodeParser.parse(pc, 2);
        manadatorySpacesParser.parse(pc);
        _ctag = ctagParser.parse(pc);
        manadatorySpacesParser.parse(pc);
        _complCode = completonCodeParser.parse(pc);
    }
}
