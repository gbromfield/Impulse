package com.grb.impulse.parsers.tl1.parser;

/**
 * Created by gbromfie on 11/13/15.
 */
public class CharacterList {

    public static final int NUMBER_CHARACTERS = 128;

    public static final CharacterList ALL_CHARS = createAllList();
    public static final CharacterList NO_CHARS = createNoList();
    public static final CharacterList CONTROL_CHARS = createControlCharList();
    public static final CharacterList ALPHABETIC_CHARS = createAlphabeticCharList();
    public static final CharacterList ALPHABETIC_MINUS_WHITESPACE_CHARS = createAlphabeticMinusWhitespaceCharList();
    public static final CharacterList WHITESPACE_CHARS = createWhitespaceCharList();
    public static final CharacterList SPACE_TAB_CHARS = createSpaceTabCharList();
    public static final CharacterList CR_LF_CHARS = createCRLFCharList();
    public static final CharacterList NUMBER_CHARS = createNumberCharList();

    private static CharacterList createAllList() {
        boolean[] charList = new boolean[NUMBER_CHARACTERS];
        for(int i = 0; i < NUMBER_CHARACTERS; i++) {
            charList[i] = true;
        }
        return new CharacterList(charList, true);
    }

    private static CharacterList createNoList() {
        boolean[] charList = new boolean[NUMBER_CHARACTERS];
        for(int i = 0; i < NUMBER_CHARACTERS; i++) {
            charList[i] = false;
        }
        return new CharacterList(charList, true);
    }

    private static CharacterList createControlCharList() {
        boolean[] charList = new boolean[NUMBER_CHARACTERS];
        for(int i = 0; i < 32; i++) {
            charList[i] = true;
        }
        for(int i = 32; i < 127; i++) {
            charList[i] = false;
        }
        charList[127] = true;
        return new CharacterList(charList, true);
    }

    private static CharacterList createAlphabeticCharList() {
        boolean[] charList = new boolean[NUMBER_CHARACTERS];
        for(int i = 0; i < 32; i++) {
            charList[i] = false;
        }
        for(int i = 32; i < 127; i++) {
            charList[i] = true;
        }
        charList[127] = false;
        return new CharacterList(charList, true);
    }

    private static CharacterList createNumberCharList() {
        boolean[] charList = new boolean[NUMBER_CHARACTERS];
        for(int i = 0; i < NUMBER_CHARACTERS; i++) {
            charList[i] = false;
        }
        for(int i = '0'; i <= '9'; i++) {
            charList[i] = true;
        }
        return new CharacterList(charList, true);
    }

    private static CharacterList createAlphabeticMinusWhitespaceCharList() {
        boolean[] charList = new boolean[NUMBER_CHARACTERS];
        for(int i = 0; i < 32; i++) {
            charList[i] = false;
        }
        for(int i = 32; i < 127; i++) {
            charList[i] = true;
        }
        charList[127] = false;
        charList[' '] = false;
        return new CharacterList(charList, true);
    }

    private static CharacterList createWhitespaceCharList() {
        boolean[] charList = new boolean[NUMBER_CHARACTERS];
        for(int i = 0; i < NUMBER_CHARACTERS; i++) {
            charList[i] = false;
        }
        charList[9] = true;
        charList[10] = true;
        charList[11] = true;
        charList[12] = true;
        charList[13] = true;
        charList[32] = true;
        return new CharacterList(charList, true);
    }

    private static CharacterList createSpaceTabCharList() {
        boolean[] charList = new boolean[NUMBER_CHARACTERS];
        for(int i = 0; i < NUMBER_CHARACTERS; i++) {
            charList[i] = false;
        }
        charList[9] = true;
        charList[32] = true;
        return new CharacterList(charList, true);
    }

    private static CharacterList createCRLFCharList() {
        boolean[] charList = new boolean[NUMBER_CHARACTERS];
        for(int i = 0; i < NUMBER_CHARACTERS; i++) {
            charList[i] = false;
        }
        charList[10] = true;
        charList[13] = true;
        return new CharacterList(charList, true);
    }

    private static final String[] ControlCodeStrs = {
            "NUL", "SOH", "STX", "ETX", "EOT", "ENQ", "ACK", "BEL", "BS", "TAB", "LF", "VT", "FF", "CR", "SO",
            "SI", "DLE", "DC1", "DC2", "DC3", "DC4", "NAK", "SYN", "ETB", "CAN", "EM", "SUB", "ESC", "FS", "GS", "RS", "US"
    };

    public static final String[] CHARACTER_STRINGS = createCodes();

    private static String[] createCodes() {
        String[] codes = new String[NUMBER_CHARACTERS];
        for(int i = 0; i < 32; i++) {
            codes[i] = ControlCodeStrs[i];
        }
        for(int i = 32; i < 127; i++) {
            codes[i] = String.valueOf((char)i);
        }
        codes[127] = "DEL";
        return codes;
    }

    private boolean[] _charList;
    private boolean _readOnly;

    public CharacterList() {
        this(NO_CHARS, false);
    }

    public CharacterList(CharacterList charList) {
        this(charList._charList, false);
    }

    public CharacterList(CharacterList charList, boolean readOnly) {
        this(charList._charList, readOnly);
    }

    public CharacterList(boolean[] charList) {
        this(charList, false);
    }

    public CharacterList(boolean[] charList, boolean readOnly) {
        if ((charList == null) || (charList.length != NUMBER_CHARACTERS)) {
            throw new IllegalArgumentException("CharList must be of size " + NUMBER_CHARACTERS);
        }
        _charList = new boolean[NUMBER_CHARACTERS];
        for(int i = 0; i < NUMBER_CHARACTERS; i++) {
            _charList[i] = charList[i];
        }
        _readOnly = readOnly;
    }

    public CharacterList add(CharacterList charList) {
        checkReadOnly();
        for (int i = 0; i < NUMBER_CHARACTERS; i++) {
            if (charList._charList[i]) {
                _charList[i] = true;
            }
        }
        return this;
    }

    public CharacterList add(char c) {
        checkReadOnly();
        _charList[c] = true;
        return this;
    }

    public CharacterList remove(CharacterList charList) {
        checkReadOnly();
        for (int i = 0; i < NUMBER_CHARACTERS; i++) {
            if (charList._charList[i]) {
                _charList[i] = false;
            }
        }
        return this;
    }

    public CharacterList remove(char c) {
        checkReadOnly();
        _charList[c] = false;
        return this;
    }

    public CharacterList set(CharacterList charList) {
        checkReadOnly();
        for (int i = 0; i < NUMBER_CHARACTERS; i++) {
            _charList[i] = charList.matches((byte)i);
        }
        return this;
    }

    public boolean matches(char c) {
        return _charList[c];
    }

    public boolean matches(byte b) {
        return _charList[b];
    }

    public int getSize() {
        int count = 0;
        for(int i = 0; i < NUMBER_CHARACTERS; i++) {
            if (_charList[i]) {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CharacterList) {
            CharacterList other = (CharacterList)obj;
            for (int i = 0; i < NUMBER_CHARACTERS; i++) {
                if (_charList[i] != other._charList[i]) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public String toString() {
        StringBuilder bldr = new StringBuilder("[");
        for(int i = 0; i < NUMBER_CHARACTERS; i++) {
            if (_charList[i]) {
                if ((bldr.length() > 1) && (i < 32)) {
                    bldr.append(",");
                }
                bldr.append(CHARACTER_STRINGS[i]);
            }
        }
        bldr.append("]");
        return bldr.toString();
    }

    private void checkReadOnly() {
        if (_readOnly) {
            throw new IllegalStateException("This object is read only");
        }
    }
}
