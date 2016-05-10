package com.grb.impulse.parsers.tl1.parser;

import java.text.ParseException;

/**
 * Created by gbromfie on 11/16/15.
 */
public class TextParser {

    private byte[] _beginDelimiter;
    private byte[] _endDelimiter;
    private boolean _includeDelimiter;
    private CharacterList _allowedChars;
    private CharacterList _delimiterChars;
    private boolean _endOfBufferDelimeter;
    private int _minLength;
    private int _maxLength;

    public TextParser() {
        _beginDelimiter = null;
        _endDelimiter = null;
        _includeDelimiter = true;
        _allowedChars = new CharacterList(CharacterList.ALL_CHARS);
        _delimiterChars = new CharacterList(CharacterList.NO_CHARS);
        _endOfBufferDelimeter = false;
        _minLength = 0;
        _maxLength = Integer.MAX_VALUE;
    }

    public TextParser(TextParser copy) {
        _beginDelimiter = copy._beginDelimiter;
        _endDelimiter = copy._endDelimiter;
        _includeDelimiter = copy._includeDelimiter;
        _allowedChars = new CharacterList(copy._allowedChars);
        _delimiterChars = new CharacterList(copy._delimiterChars);
        _endOfBufferDelimeter = copy._endOfBufferDelimeter;
        _minLength = copy._minLength;
        _maxLength = copy._maxLength;
    }

    public TextParser addDelimeterChars(CharacterList delimiterChars) {
        _delimiterChars.add(delimiterChars);
        return this;
    }

    public TextParser addDelimeterChar(char c) {
        _delimiterChars.add(c);
        return this;
    }

    public TextParser removeDelimeterChars(CharacterList delimiterChars) {
        _delimiterChars.remove(delimiterChars);
        return this;
    }

    public TextParser removeDelimeterChar(char c) {
        _delimiterChars.remove(c);
        return this;
    }

    public TextParser addEOBDelimeter() {
        _endOfBufferDelimeter = true;
        return this;
    }

    public TextParser removeEOBDelimeter() {
        _endOfBufferDelimeter = false;
        return this;
    }

    public TextParser setDelimiterChars(CharacterList delimiterChars) {
        _delimiterChars.set(delimiterChars);
        return this;
    }

    public CharacterList getDelimiterChars() {
        return _delimiterChars;
    }

    public TextParser setDelimiterStrings(String begin, String end) {
        _beginDelimiter = begin.getBytes();
        _endDelimiter = end.getBytes();
        return this;
    }

    public TextParser includeDelimiter(boolean includeDelimiter) {
        _includeDelimiter = includeDelimiter;
        return this;
    }

    public TextParser addAllowedChars(CharacterList allowedChars) {
        _allowedChars.add(allowedChars);
        return this;
    }

    public TextParser addAllowedChar(char c) {
        _allowedChars.add(c);
        return this;
    }

    public TextParser setAllowedChars(CharacterList allowedChars) {
        _allowedChars.set(allowedChars);
        return this;
    }

    public TextParser removeAllowedChars(CharacterList allowedChars) {
        _allowedChars.remove(allowedChars);
        return this;
    }

    public TextParser removeAllowedChar(char c) {
        _allowedChars.remove(c);
        return this;
    }

    public CharacterList getAllowedChars() {
        return _allowedChars;
    }

    public TextParser setLengths(int min, int max) {
        _minLength = min;
        _maxLength = max;
        return this;
    }

    public String parse(ParseContext ctx) throws ParseException {
        return parse(ctx, Integer.MAX_VALUE);
    }

    public String parse(ParseContext ctx, int length) throws ParseException {
        if (ctx.mark >= (ctx.index + ctx.length)) {
            throw new ParseException("Buffer Underflow", ctx.mark);
        }
        StringBuilder bldr = new StringBuilder();
        if (_beginDelimiter != null) {
            if (ctx.length >= _beginDelimiter.length) {
                for(int i = 0; i < _beginDelimiter.length; i++) {
                    if (_beginDelimiter[i] != ctx.buffer[ctx.mark]) {
                        throw new ParseException("No Delimiter Found", ctx.mark);
                    }
                    if (_includeDelimiter) {
                        bldr.append((char)(ctx.buffer[ctx.mark]));
                        if (bldr.length() == length) {
                            ctx.mark = ctx.mark + 1;
                            validateLength(bldr.length());
                            return bldr.toString();
                        }
                    }
                    ctx.mark = ctx.mark + 1;
                }
            }
        }
        while((ctx.mark - ctx.index) < ctx.length) {
            if (_endDelimiter != null) {
                boolean found = true;
                if ((ctx.mark - ctx.index + 1) >= _endDelimiter.length) {
                    for (int i = 0; i < _endDelimiter.length; i++) {
                        if (ctx.buffer[ctx.mark - i] != _endDelimiter[_endDelimiter.length - i - 1]) {
                            found = false;
                            break;
                        }
                    }
                    if (found) {
                        if (_includeDelimiter) {
                            bldr.append((char)(ctx.buffer[ctx.mark]));
                        } else {
                            bldr.setLength(bldr.length() - _endDelimiter.length + 1);
                        }
                        ctx.mark = ctx.mark + 1;
                        validateLength(bldr.length());
                        return bldr.toString();
                    }
                }
            }
            if (_delimiterChars.matches(ctx.buffer[ctx.mark])) {
                if (_includeDelimiter) {
                    bldr.append((char)(ctx.buffer[ctx.mark]));
                    ctx.mark = ctx.mark + 1;
                }
                validateLength(bldr.length());
                return bldr.toString();
            }
            if (!_allowedChars.matches(ctx.buffer[ctx.mark])) {
                throw new ParseException(
                        String.format("Character \"%s\" is not allowed, must be one of %s",
                                CharacterList.CHARACTER_STRINGS[ctx.buffer[ctx.mark]], _allowedChars), ctx.mark);
            }
            bldr.append((char)(ctx.buffer[ctx.mark]));
            ctx.mark = ctx.mark + 1;
            if (bldr.length() == length) {
                validateLength(bldr.length());
                return bldr.toString();
            }
        }
        if (_delimiterChars.equals(CharacterList.NO_CHARS)) {
            return bldr.toString();
        } else {
            if (_endOfBufferDelimeter) {
                return bldr.toString();
            }
        }
        throw new ParseException("Buffer Underflow - end of buffer reached without a delimeter", ctx.mark);
    }

    private void validateLength(int length) throws ParseException {
        if (length < _minLength) {
            throw new ParseException(String.format("Size of %d does not meet minimum size of %d", length, _minLength), length);
        }
        if (length > _maxLength) {
            throw new ParseException(String.format("Size of %d exceeds maximum size of %d", length, _maxLength), length);
        }
    }
}