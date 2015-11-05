package com.grb.impulse.parsers.http;

import java.util.Iterator;
import java.util.List;

public class HTTPMessage {

    public List<String> headers;
    public byte[] payload;
    private String method;
    private String path;

    public HTTPMessage() {
        headers = null;
        payload = null;
        method = null;
        path = null;
    }

    public String getMethod() {
        parseHeader();
        return method;
    }

    public String getPath() {
        parseHeader();
        return path;
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

    private void parseHeader() {
        if (method == null) {
            if (headers != null) {
                Iterator<String> it = headers.iterator();
                while (it.hasNext()) {
                    String header = it.next();
                    String[] fields = header.split(" ");
                    if ((fields[0].equalsIgnoreCase("OPTIONS")) ||
                            (fields[0].equalsIgnoreCase("GET")) ||
                            (fields[0].equalsIgnoreCase("HEAD")) ||
                            (fields[0].equalsIgnoreCase("POST")) ||
                            (fields[0].equalsIgnoreCase("PUT")) ||
                            (fields[0].equalsIgnoreCase("DELETE")) ||
                            (fields[0].equalsIgnoreCase("TRACE")) ||
                            (fields[0].equalsIgnoreCase("CONNECT"))) {
                        method = fields[0];
                        if (fields.length > 1) {
                            path = fields[1];
                        }
                    }
                }
            }
        }
    }
}
