package com.grb.impulse.plugins;

import java.util.Map;

import com.grb.impulse.BaseTransform;
import com.grb.impulse.Impulse;
import com.grb.impulse.Input;
import com.grb.impulse.Output;
import com.grb.impulse.TransformContext;
import com.grb.impulse.TransformCreationContext;
import com.grb.util.property.JMXUtils;
import com.grb.util.property.Property;
import com.grb.util.property.PropertyConversionException;
import com.grb.util.property.PropertySource;
import com.grb.util.property.PropertyVetoException;
import com.grb.util.property.impl.MapPropertySource;
import com.grb.util.property.impl.ValueOfConverter;

/**
 * This transform switches between two outputs based on the
 * switch state property modifiable through JMX
 */
public class Switcher extends BaseTransform {
    /*
     * Property that indicates the switch state (on/off).
     */
    public static final String SWITCH_STATE_PROPERTY = "isOn";

    /**
     * Default switch state. True means On.
     */
    public static final boolean SWITCH_STATE_PROPERTY_DEFAULT = true;
    
    private PropertySource<Boolean> _switchJMXSource;
    
    public Switcher(String transformName, String instanceName,
            TransformCreationContext transformCreationContext, Object... args) {
        super(transformName, instanceName, transformCreationContext);
    }

    @Override
    public void init() throws Exception {
        super.init();
        @SuppressWarnings("unchecked")
        Property<Boolean> switchProp = (Property<Boolean>)getProperty(SWITCH_STATE_PROPERTY);
        _switchJMXSource = switchProp.getSource(JMXUtils.JMX_SOURCE);
    }

    /**
     * [input] Object to be switched.
     * 
     * @param argMap Object to be switched.
     */
    @Input("onData")
    synchronized public void onData(Map<String, Object> argMap) {
        if (getBooleanProperty(SWITCH_STATE_PROPERTY)) {
            on(argMap);
        } else {
            off(argMap);
        }
    }   

    /**
     * [input] To switch on.
     * 
     */
    @Input("onOn")
    synchronized public void onOn() {
        try {
            _switchJMXSource.setValue(true);
        } catch (PropertyVetoException e) {}
    }   

    /**
     * [input] To switch off.
     * 
     */
    @Input("onOff")
    synchronized public void onOff() {
        try {
            _switchJMXSource.setValue(false);
        } catch (PropertyVetoException e) {}
    }   

    /**
     * [output] Where the data goes when the switch is on.
     * 
     * @param argMap
     */
    @Output("on")
    public void on(Map<String, Object> argMap) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("on: \r\n" + Impulse.format(argMap));
        }
        next("on", argMap);
    }  

    /**
     * [output] Where the data goes when the switch is off.
     * 
     * @param argMap
     */
    @Output("off")
    public void off(Map<String, Object> argMap) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("off: \r\n" + Impulse.format(argMap));
        }
        next("off", argMap);
    }  
        
    /**
     * This method is for declaring this transform's properties. This method is called
     * using reflection at application startup by the transform's context. These properties
     * will be initialized after this call to validate their values. The TransformContext
     * for this Transform owns the properties and then clones the properties
     * for the transform when the transform is created so that each Transform has its
     * own copy. 
     * 
     * @param ctx the transform context.
     * @throws PropertyVetoException
     * @throws PropertyConversionException
     */
    public static void getProperties(TransformContext ctx) throws PropertyVetoException, PropertyConversionException {
        Map<String, Property<?>> props = ctx.getProperties();

        @SuppressWarnings("unchecked")
        Property<Boolean> p1 = new Property<Boolean>(SWITCH_STATE_PROPERTY, SWITCH_STATE_PROPERTY_DEFAULT, true,
                new PropertySource<Boolean>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new MapPropertySource<Boolean>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(SWITCH_STATE_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_2, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Boolean.class)));
        p1.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "Property that indicates the switch state (on/off)");
        props.put(p1.getId(), p1);        
    }
}
