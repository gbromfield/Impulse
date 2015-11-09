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
     * Transform Output Port Name
     */
    public String outputPortName;

    /**
     * Input arguments to the Transform
     */
    public Map<String, Object> argMap;

    /**
     * Output arguments from the transform
     */
    public Object[] args;

    /**
     * List of connection names associated with
     * the Transform's output
     */
    public String[] connectionNames;

    public String toString() {
        StringBuilder bldr = new StringBuilder();
        bldr.append("outputPortName=");
        bldr.append(outputPortName);
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
        bldr.append("}, connectionNames={");
        if (args == null) {
            bldr.append("null");
        } else {
            for(int i = 0; i < connectionNames.length; i++) {
                if (i > 0) {
                    bldr.append(", ");
                }
                bldr.append(connectionNames[i]);
            }
        }
        bldr.append("}");
        return bldr.toString();
    }
}
