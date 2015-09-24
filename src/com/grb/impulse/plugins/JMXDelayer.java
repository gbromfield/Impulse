package com.grb.impulse.plugins;

import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.management.MBeanException;
import javax.management.MBeanOperationInfo;
import javax.management.ReflectionException;

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
 * This transform stores all pieces of data (the entire argument
 * map) given to it and they are only released to the next 
 * transform by JMX actions. 
 */
public class JMXDelayer extends BaseTransform {
    /**
     * This property contains the current count of delayed
     * pieces of data.
     */
    public static final String NUM_ITEMS_QUEUED_PROPERTY = "numItemsQueued";

    /**
     * This property indicates whether delaying is on or not.
     * If delaying is off then data is passed through.
     */
    public static final String DELAY_PROPERTY = "delay";

    /**
     * Default delay value.
     */
    public static final boolean DELAY_PROPERTY_DEFAULT = true;

    /**
     * Sends one (the oldest) piece of data from the delayed data.
     */
    public static final String SEND_ONE_OPERATION = "send one";    

    /**
     * Sends all delayed data.
     */
    public static final String SEND_ALL_OPERATION = "send all";

    /**
     * Clears (deletes) all the cleared data.
     */
    public static final String CLEAR_ALL_OPERATION = "clear all";

    /**
     * Starts the delaying of data. If delaying is off then data is passed through.
     */
    public static final String START_DELAYING_OPERATION = "start delaying";

    /**
     * Stops the delaying of data. If delaying is off then data is passed through.
     */
    public static final String STOP_DELAYING_OPERATION = "stop delaying";

    private LinkedList<Map<String, Object>> _delayedItems;
    private Property<Integer> _numItems;
    
    public JMXDelayer(String transformName, String instanceName,
            TransformCreationContext transformCreationContext, Object... args) {
        super(transformName, instanceName, transformCreationContext);
        _delayedItems = new LinkedList<Map<String, Object>>();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void init() throws Exception {
        super.init();
        _numItems = (Property<Integer>)getProperty(NUM_ITEMS_QUEUED_PROPERTY);
        _numItems.setValue(0);
    }

    /**
     * [input] Object to be delayed.
     *  
     * @param argMap Object to be delayed.
     */
    @Input("delayerIn")
    synchronized public void delayerIn(Map<String, Object> argMap) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("delayerIn: \r\n" + Impulse.format(argMap));
        }
        if (getBooleanProperty(DELAY_PROPERTY)) {
            _delayedItems.add(argMap);
            try {
                _numItems.setValue(_delayedItems.size());
            } catch (Exception e) {}
        } else {
            delayerOut(argMap);
        }
    }   

    /**
     * [output] Called when activated from JMX to deliver delayed objects.
     * 
     * @param argMap
     */
    @Output("delayerOut")
    public void delayerOut(Map<String, Object> argMap) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("delayerOut: \r\n" + Impulse.format(argMap));
        }
        next("delayerOut", argMap);
    }  

    /**
     * [operation] Starts delaying data.
     */
    synchronized public void startDelaying() {
        if (_logger.isInfoEnabled()) {
            _logger.info("startDelaying");
        }
        @SuppressWarnings("unchecked")
        Property<Boolean> p = (Property<Boolean>)getProperty(DELAY_PROPERTY);
        try {
            p.getSource(JMXUtils.JMX_SOURCE).setValue(true);
        } catch (PropertyVetoException e) {}
    }
    
    /**
     * [operation] Stops delaying data and sends what has been delayed.
     */
    synchronized public void stopDelaying() {
        if (_logger.isInfoEnabled()) {
            _logger.info("stopDelaying");
        }
        @SuppressWarnings("unchecked")
        Property<Boolean> p = (Property<Boolean>)getProperty(DELAY_PROPERTY);
        try {
            p.getSource(JMXUtils.JMX_SOURCE).setValue(false);
        } catch (PropertyVetoException e) {}
        sendAll();
    }
    
    /**
     * [operation] Sends one piece of delayed data.
     */
    synchronized public void sendOne() {
        if (_logger.isInfoEnabled()) {
            _logger.info("sendOne");
        }
        Map<String, Object> argMap;
        try {
            argMap = _delayedItems.removeFirst();
            delayerOut(argMap);
        } catch(NoSuchElementException e) {}
    }

    /**
     * [operation] Sends all delayed data.
     */
    synchronized public void sendAll() {
        if (_logger.isInfoEnabled()) {
            _logger.info("sendAll");
        }
        Map<String, Object> argMap;
        try {
            while(true) {
                argMap = _delayedItems.removeFirst();
                delayerOut(argMap);
            }
        } catch(NoSuchElementException e) {}
    }

    /**
     * [operation] Clears all the delayed data.
     */
    synchronized public void clearAll() {
        if (_logger.isInfoEnabled()) {
            _logger.info("clearAll");
        }
        _delayedItems.clear();
    }
    
    @Override
    synchronized public Object invoke(String actionName, Object[] params, String[] signature)
            throws MBeanException, ReflectionException {
        if (actionName.equals(SEND_ONE_OPERATION)) {
            sendOne();
        } else if (actionName.equals(SEND_ALL_OPERATION)) {
            sendAll();
        } else if (actionName.equals(CLEAR_ALL_OPERATION)) {
            clearAll();
        } else if (actionName.equals(START_DELAYING_OPERATION)) {
            startDelaying();
        } else if (actionName.equals(STOP_DELAYING_OPERATION)) {
            stopDelaying();
        }
        try {
            _numItems.setValue(_delayedItems.size());
        } catch (Exception e) {}
        return null;
    }

    @Override
    protected MBeanOperationInfo[] createOperInfo() {
        MBeanOperationInfo[] opers = {
                new MBeanOperationInfo(START_DELAYING_OPERATION, "clears all peices of data without sending to next", null, null, MBeanOperationInfo.ACTION),
                new MBeanOperationInfo(STOP_DELAYING_OPERATION, "clears all peices of data without sending to next", null, null, MBeanOperationInfo.ACTION),
                new MBeanOperationInfo(SEND_ONE_OPERATION, "sends one peice of data to next", null, null, MBeanOperationInfo.ACTION),
                new MBeanOperationInfo(SEND_ALL_OPERATION, "sends all peices of data to next", null, null, MBeanOperationInfo.ACTION),
                new MBeanOperationInfo(CLEAR_ALL_OPERATION, "clears all peices of data without sending to next", null, null, MBeanOperationInfo.ACTION),
        };
        return opers;
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

        Property<Integer> p = new Property<Integer>(NUM_ITEMS_QUEUED_PROPERTY, 0);
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "This property contains the current count of delayed pieces of data");
        props.put(p.getId(), p);

        @SuppressWarnings("unchecked")
        Property<Boolean> p1 = new Property<Boolean>(DELAY_PROPERTY, DELAY_PROPERTY_DEFAULT, true,
                new PropertySource<Boolean>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new MapPropertySource<Boolean>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(DELAY_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_2, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Boolean.class)));
        p1.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "This property indicates whether delaying is on or not");
        props.put(p1.getId(), p1);        
    }
}
