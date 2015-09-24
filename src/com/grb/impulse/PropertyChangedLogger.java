package com.grb.impulse;

import org.apache.commons.logging.Log;

import com.grb.util.property.Property;
import com.grb.util.property.PropertyChangeEvent;
import com.grb.util.property.PropertyChangeListener;

public class PropertyChangedLogger implements PropertyChangeListener<Object> {

    private Log _logger;
    
    public PropertyChangedLogger(Log logger) {
        _logger = logger;
    }

    @Override
    public void propertyChanged(PropertyChangeEvent<Object> event) {
        if (_logger.isDebugEnabled()) {
            if (Impulse.logStackTrace()) {
                _logger.debug(event.toString() + "\n" + Property.getStackTraceForSetString());
            } else {
                _logger.debug(event.toString());   
            }
        }
    }
}
