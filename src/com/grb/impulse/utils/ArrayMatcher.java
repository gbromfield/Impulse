package com.grb.impulse.utils;

/**
 * Created by gbromfie on 4/20/16.
 */
public class ArrayMatcher {
    private byte[] _matchBytes;
    private int _index;

    public ArrayMatcher(byte[] matchBytes) {
        _matchBytes = matchBytes;
        _index = 0;
    }

    public void reset() {
        _index = 0;
    }

    public int getLength() {
        return _matchBytes.length;
    }

    public boolean match(byte b) {
        if (_matchBytes[_index] == b) {
            _index++;
            if (_index == _matchBytes.length) {
                reset();
                return true;
            } else {
                return false;
            }
        } else {
            reset();
            if (_matchBytes[_index] == b) {
                _index++;
                if (_index == _matchBytes.length) {
                    reset();
                    return true;
                } else {
                    return false;
                }
            }
            return false;
        }
    }
}
