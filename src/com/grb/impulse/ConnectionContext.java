package com.grb.impulse;

import org.apache.commons.logging.Log;

import java.util.Map;

/**
 * Created by gbromfie on 11/9/15.
 */
public class ConnectionContext {
    /**
     * Transform Logger
     */
    public Log log;

    /**
     * Connection
     */
    public Connection connection;

    /**
     * Input arguments to the Transform
     */
    public Map<String, Object> argMap;

    /**
     * Output arguments from the transform
     */
    public Object[] args;

    public String toString() {
        StringBuilder bldr = new StringBuilder();
        bldr.append("connection=");
        bldr.append(connection);
        bldr.append(", argMap={");
        bldr.append(argMap);
        bldr.append("}, args={");
        if (args == null) {
            bldr.append("null");
        } else {
            for(int i = 0; i < args.length; i++) {
                if (i > 0) {
                    bldr.append(", ");
                }
                bldr.append(args[i]);
            }
        }
        return bldr.toString();
    }
}
