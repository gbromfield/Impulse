package com.grb.impulse.utils;

public class Array {
    static public String toString(byte[] data) {
        StringBuilder bldr = new StringBuilder();
        bldr.append("[");
        for(int i = 0; i < data.length; i++) {
            if (i > 0) {
                bldr.append(",");
            }
            bldr.append(data[i]);
        }
        bldr.append("]");
        return bldr.toString();
    }

    static public String toString(Object[] data) {
        StringBuilder bldr = new StringBuilder();
        bldr.append("[");
        for(int i = 0; i < data.length; i++) {
            if (i > 0) {
                bldr.append(",");
            }
            bldr.append(data[i]);
        }
        bldr.append("]");
        return bldr.toString();
    }
}
