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
import com.grb.util.property.impl.RangeConstraint;
import com.grb.util.property.impl.SystemPropertySource;
import com.grb.util.property.impl.ValueOfConverter;

/**
 * This transform fragments the input data into "fragmentSize"
 * chunks. A fragment delay "fragmentDelay" can be introduced
 * to give a small delay between fragments.
 */
public class Fragger extends BaseTransform {
    /**
     * This variable is a place holder for a transform argument.
     * Argument syntax must follow <transform simple class name>.<type simple class name>.<field name>
     * This transforms outputs data into the argMap using this as a key.
     */
    @Argument
    final public static Object[] fragment = {"Fragger.ByteBuffer.fragment", ByteBuffer.class};

    /**
     * Fragment size for the fragments. With a fragment size of 100
     * a 1000 byte buffer will be converted to 10 fragments. A single
     * integer can be specified in which case the fragments will all 
     * be the same size, or a range where each fragment will be a 
     * random value between the minimum and maximum.
     */
    public static final String FRAGMENT_SIZE_PROPERTY = "fragmentSize";
    
    /**
     * Default fragment size.
     */
    public static final String FRAGMENT_SIZE_PROPERTY_DEFAULT = "100";

    /**
     * Time to delay between sending of fragments. The first fragment
     * is sent immediately. 0 means no delay. A sleep is performed
     * to get the delay not a timer.
     */
    public static final String FRAGMENT_DELAY_IN_MS_PROPERTY = "fragmentDelay";

    /**
     * Default fragment delay in milliseconds.
     */
    public static final int FRAGMENT_DELAY_IN_MS_PROPERTY_DEFAULT = 0;

    private String _fragmentSize;
    private int _minFragmentSize;
    private int _maxFragmentSize;
    private int _fragmentDelay;
    
    public Fragger(String transformName, String instanceName,
            TransformCreationContext transformCreationContext, Object... args) {
        super(transformName, instanceName, transformCreationContext);
    }

    @Override
    public void init() throws Exception {
        super.init();
        _fragmentSize = getStringProperty(FRAGMENT_SIZE_PROPERTY);
        parseFragmentSizeStr();
        _fragmentDelay = getIntegerProperty(FRAGMENT_DELAY_IN_MS_PROPERTY);
    }

    /**
     * [input] Buffer to be fragmented.
     * 
     * @param argMap Buffer to be fragmented.
     */
    @Input("fraggerIn")
    public void fraggerIn(Map<String, Object> argMap, ByteBuffer buffer) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("fraggerIn: \r\n" + Impulse.GlobalByteArrayFormatter.format(buffer));
        }
        if (_minFragmentSize > 0) {            
            int fragmentSize = _minFragmentSize;
            if (_minFragmentSize != _maxFragmentSize) {
                fragmentSize = _minFragmentSize + (int)((_maxFragmentSize - _minFragmentSize) * Math.random());
            }
            if (buffer.limit() <= fragmentSize) {
                fraggerOut(argMap, buffer);
            } else {
                byte[] bufferArray = buffer.array();
                int offset = 0;
                // fragment the buffer
                while(offset < buffer.limit()) {
                    ByteBuffer output = ByteBuffer.allocate(fragmentSize);
                    output.put(bufferArray, offset, Math.min(fragmentSize, (buffer.limit() - offset)));
                    output.flip();
                    fraggerOut(argMap, output);
                    if (_fragmentDelay > 0) {
                        try {
                            Thread.sleep(_fragmentDelay);
                        } catch (InterruptedException e) {
                            if (_logger.isErrorEnabled()) {
                                _logger.error("fragment delay sleep was interrupted", e);
                            }
                        }
                    }
                    offset += fragmentSize;
                }
            }
        } else {
            fraggerOut(argMap, buffer);
        }
    }

    /**
     * [output] This is called once for each fragment. 
     * 
     * @param buffer Fragmented buffer.
     */
    @Output("fraggerOut")
    public void fraggerOut(Map<String, Object> argMap, ByteBuffer buffer) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("fraggerOut: \r\n" + Impulse.GlobalByteArrayFormatter.format(buffer));
        }
        next("fraggerOut", argMap, fragment[0], buffer);
    }
    
    private void parseFragmentSizeStr() {
        final String exceptionStr = String.format("Illegal format of fragment size, must be a single positive integer value or dash separated range: %s", _fragmentSize);
        try {
            String[] args = _fragmentSize.trim().split("-");
            if (args.length == 1) {
                _minFragmentSize = Integer.parseInt(args[0]);
                _maxFragmentSize = _minFragmentSize;
            } else if (args.length == 2) {
                _minFragmentSize = Integer.parseInt(args[0]);
                _maxFragmentSize = Integer.parseInt(args[1]);
            } else {
                throw new IllegalArgumentException(exceptionStr);                
            }
        } catch(IllegalArgumentException e) {
            throw e;
        } catch(Exception e) {
            throw new IllegalArgumentException(exceptionStr, e);
        }
        if ((_minFragmentSize > _maxFragmentSize) || (_minFragmentSize < 0) || (_maxFragmentSize < 0)) {
            throw new IllegalArgumentException(exceptionStr);                
        }
        if ((_minFragmentSize == 0) && (_maxFragmentSize != 0)) {
            throw new IllegalArgumentException(exceptionStr);                
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

        Property<String> p = new Property<String>(FRAGMENT_SIZE_PROPERTY, FRAGMENT_SIZE_PROPERTY_DEFAULT, true,
                new PropertySource<String>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<String>(ctx.getPropertyString(FRAGMENT_SIZE_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID),
                new MapPropertySource<String>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(FRAGMENT_SIZE_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID));
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "Fragment size for the fragments as a single integer or range (min-max)");
        props.put(p.getId(), p);

        Property<Integer> ip = new Property<Integer>(FRAGMENT_DELAY_IN_MS_PROPERTY, FRAGMENT_DELAY_IN_MS_PROPERTY_DEFAULT, true,
                new PropertySource<Integer>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<Integer>(ctx.getPropertyString(FRAGMENT_DELAY_IN_MS_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Integer.class)),
                new MapPropertySource<Integer>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(FRAGMENT_DELAY_IN_MS_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Integer.class)));
        ip.addVetoableListener(new RangeConstraint<Integer>(0, Integer.MAX_VALUE));
        ip.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "Time to delay between sending of fragments");
        props.put(ip.getId(), ip);

    }
}
