package com.grb.impulse.parsers.http;

import java.util.List;

public class HTTPMessage {

    public List<String> headers;
    public byte[] payload;
    
    public HTTPMessage() {
        headers = null;
        payload = null;
    }

    @Override
    public String toString() {
        StringBuilder bldr = new StringBuilder();
        if (headers != null) {
            for(String hdr : headers) {
                bldr.append(hdr);
                bldr.append("\n");
            }
        }
        if (payload != null) {
            bldr.append(new String(payload));
            bldr.append("\n");
        }
        return bldr.toString();
    }
}
