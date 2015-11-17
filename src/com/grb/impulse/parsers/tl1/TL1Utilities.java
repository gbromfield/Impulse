package com.grb.impulse.parsers.tl1;

/**
 * Created by gbromfie on 11/13/15.
 */
public class TL1Utilities {
    public static boolean arrayContains(byte[] source, byte[] comparison, int offset, int length) {
        if ((source.length >= length) && (comparison.length >= length)) {
            for(int i = 0; i < length; i++) {
                if (source[offset+i] != comparison[i]) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
