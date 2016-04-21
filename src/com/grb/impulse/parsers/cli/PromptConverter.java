package com.grb.impulse.parsers.cli;

import com.grb.util.property.PropertyConversionException;
import com.grb.util.property.PropertyConverter;

/**
 * Created by gbromfie on 4/19/16.
 */
public class PromptConverter implements PropertyConverter {
    @Override
    public Object convert(Object o) throws PropertyConversionException {
        if (o instanceof String) {
            StringBuilder bldr = new StringBuilder();
            byte[] ba = ((String)o).getBytes();
            for(int i = 0; i < ba.length; i++) {
                if ((i == 0) || (i == (ba.length - 1))) {
                    if (ba[i] == (byte)'"') {
                        // skip it
                    } else {
                        bldr.append((char)ba[i]);
                    }
                } else {
                    bldr.append((char)ba[i]);
                }
            }
            return bldr.toString();
        }
        return o;
    }
}
