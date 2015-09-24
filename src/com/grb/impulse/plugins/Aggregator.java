package com.grb.impulse.plugins;

import java.nio.ByteBuffer;
import java.util.Map;

import com.grb.impulse.Argument;
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
import com.grb.util.property.impl.SystemPropertySource;
import com.grb.util.property.impl.ValueOfConverter;

/**
 * This transform takes input, stores it  until it has "aggregate" number
 * of data items and then outputs all the data to the output.
 */
public class Aggregator extends BaseTransform {
    /**
     * This variable is a place holder for a transform argument.
     * Argument syntax must follow <transform simple class name>.<type simple class name>.<field name>
     * This transforms outputs data into the argMap using this as a key.
     */
    @Argument()
    final public static Object[] aggregate = {"Aggregator.ByteBuffer.aggregate", ByteBuffer.class};
    
    /**
     * The number of buffers of data to save before releasing.
     */
    public static final String AGGREGATE_PROPERTY = "aggregate";

    /**
     * Default aggregate value. 1 means don't aggregate just pass 
     * everything through.
     */
    public static final int AGGREGATE_PROPERTY_DEFAULT = 1;

    /**
     * This property keeps the value of the instance parameter.
     */
    public static final String COUNT_PROPERTY = "count";
    
    /**
     * Default count value.
     */
    public static final int COUNT_PROPERTY_DEFAULT = 0;

    /**
     * This property defines whether to be cyclic or not.
     * If true, the aggregate buffer will be sent every
     * nth (aggregate property) time. If false, the aggregate 
     * buffer will be sent once and then data will just be 
     * passed through. 
     */
    public static final String CYCLIC_PROPERTY = "cyclic";
    
    /**
     * Default cyclic value.
     */
    public static final boolean CYCLIC_PROPERTY_DEFAULT = false;

    private int _aggregateCount;
    private ByteBuffer _aggregateBuffer;
    private Property<Integer> _countProp;
    private PropertySource<Integer> _countJMXSource;
    private boolean _cyclic;
    
    public Aggregator(String transformName, String instanceName,
            TransformCreationContext transformCreationContext, Object ... args) {
        super(transformName, instanceName, transformCreationContext);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void init() throws Exception {
        super.init();
        _aggregateCount = getIntegerProperty(AGGREGATE_PROPERTY);
        _aggregateBuffer = null;
        _countProp = (Property<Integer>)getProperty(COUNT_PROPERTY);
        _countJMXSource = _countProp.getSource(JMXUtils.JMX_SOURCE);
        _cyclic = getBooleanProperty(CYCLIC_PROPERTY);
    }

    /**
     * [input] The buffer that is passed will be saved and passed to {@link Aggregator#onOutput(Map, ByteBuffer)}
     * when the aggregate number has been achieved.
     * 
     * @param argMap The buffer to add to the aggregate.
     */
    @Input("onData")
    public void onData(Map<String, Object> argMap, ByteBuffer buffer) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("onData: \r\n" + Impulse.GlobalByteArrayFormatter.format(buffer));
        }
        int count = _countProp.getValue();        
        if ((_aggregateCount <= 1) || (count >= _aggregateCount)) {
            try {
                _countJMXSource.setValue(count + 1);
            } catch (PropertyVetoException e) {}
            onOutput(argMap, buffer);
            return;
        }
        appendBuffer(buffer);
        if ((count + 1) == _aggregateCount) {
            if (_cyclic) {
                try {
                    _countJMXSource.setValue(0);
                } catch (PropertyVetoException e) {}
            } else {
                try {
                    _countJMXSource.setValue(count + 1);
                } catch (PropertyVetoException e) {}
            }
            _aggregateBuffer.flip();
            onOutput(argMap, _aggregateBuffer);
            _aggregateBuffer.clear();
        } else {
            try {
                _countJMXSource.setValue(count + 1);
            } catch (PropertyVetoException e) {}
        }
    }

    /**
     * [output] The aggregate buffers.
     * 
     * @param buffer The aggregate buffer.
     */
    @Output("onOutput")
    public void onOutput(Map<String, Object> argMap, ByteBuffer buffer) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("onOutput: \r\n" + Impulse.GlobalByteArrayFormatter.format(buffer));
        }
        next("onOutput", argMap, aggregate[0], buffer);
    }
    
    private void appendBuffer(ByteBuffer buffer) {
        if (buffer != null) {
            if (_aggregateBuffer == null) {
                _aggregateBuffer = ByteBuffer.allocate(buffer.limit());
            }
            if (buffer.limit() > (_aggregateBuffer.capacity() - _aggregateBuffer.position())) {
                ByteBuffer newBuffer = ByteBuffer.allocate(_aggregateBuffer.capacity() + buffer.limit());
                _aggregateBuffer.flip();
                newBuffer.put(_aggregateBuffer);
                _aggregateBuffer = newBuffer;
            }
            _aggregateBuffer.put(buffer);
        }
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
    @SuppressWarnings("unchecked")
    public static void getProperties(TransformContext ctx) throws PropertyVetoException, PropertyConversionException {
        Map<String, Property<?>> props = ctx.getProperties();
        
        Property<Integer> ip = new Property<Integer>(AGGREGATE_PROPERTY, AGGREGATE_PROPERTY_DEFAULT,
                new PropertySource<Integer>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1, ValueOfConverter.getConverter(Integer.class)),
                new SystemPropertySource<Integer>(ctx.getPropertyString(AGGREGATE_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Integer.class)),
                new MapPropertySource<Integer>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(AGGREGATE_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Integer.class)));
        ip.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "The number of buffers of data to save before releasing.");
        props.put(ip.getId(), ip);

        ip = new Property<Integer>(COUNT_PROPERTY, COUNT_PROPERTY_DEFAULT, 
                new PropertySource<Integer>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1, ValueOfConverter.getConverter(Integer.class)));
        ip.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "This property keeps the value of the instance parameter.");
        props.put(ip.getId(), ip); 
        
        Property<Boolean> bp = new Property<Boolean>(CYCLIC_PROPERTY, CYCLIC_PROPERTY_DEFAULT,
                new PropertySource<Boolean>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<Boolean>(ctx.getPropertyString(CYCLIC_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Boolean.class)),
                new MapPropertySource<Boolean>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(CYCLIC_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Boolean.class)));
        bp.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "This property indicates whether the aggregation is cyclical");
        props.put(bp.getId(), bp);        
    }
}
