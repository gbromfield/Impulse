package com.grb.impulse.plugins;

import com.grb.impulse.*;
import com.grb.impulse.parsers.tl1.*;
import com.grb.util.property.*;
import com.grb.util.property.impl.MapPropertySource;
import com.grb.util.property.impl.RangeConstraint;
import com.grb.util.property.impl.SystemPropertySource;
import com.grb.util.property.impl.ValueOfConverter;

import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.Map;

/**
 * Created by gbromfie on 11/9/15.
 */
public class TL1AgentFramer extends BaseTransform {
    /**
     * This variable is a place holder for a transform argument.
     * Argument syntax must follow <transform simple class name>.<type simple class name>.<field name>
     * This transforms outputs data into the argMap using this as a key.
     */
    @Argument
    final public static Object[] bytesTL1Message = {"TL1AgentFramer.ByteBuffer.bytesTL1Message", ByteBuffer.class};

    /**
     * This variable is a place holder for a transform argument.
     * Argument syntax must follow <transform simple class name>.<type simple class name>.<field name>
     * This transforms outputs data into the argMap using this as a key.
     */
    @Argument
    final public static Object[] stringTL1Message = {"TL1AgentFramer.String.stringTL1Message", String.class};

    /**
     * This variable is a place holder for a transform argument.
     * Argument syntax must follow <transform simple class name>.<type simple class name>.<field name>
     * This transforms outputs data into the argMap using this as a key.
     */
    @Argument
    final public static Object[] tl1Message = {"TL1AgentFramer.TL1Message.tl1Message", TL1Message.class};

    /**
     * Maximum TL1 input message message.
     */
    public static final String MAX_OUTPUT_MESSAGE_SIZE_IN_CHARS_PROPERTY = "maxOutputMessageSizeInChars";

    /**
     * Default maximum TL1 input message message.
     */
    public static final int MAX_OUTPUT_MESSAGE_SIZE_IN_CHARS_PROPERTY_DEFAULT = TL1OutputMessage.MAX_SIZE;

    TL1AgentDecoder _decoder;

    public TL1AgentFramer(String transformName, String instanceName,
                     TransformCreationContext transformCreationContext, Object... args) {
        super(transformName, instanceName, transformCreationContext);
        _decoder = new TL1AgentDecoder();
    }

    /**
     * [input] Data from the socket.
     *
     * @param argMap
     */
    @Input("onData")
    public void onData(Map<String, Object> argMap, ByteBuffer readBuffer) throws TL1MessageMaxSizeExceededException, ParseException {
        if (_logger.isDebugEnabled()) {
            _logger.debug("onData: \r\n" + Impulse.GlobalByteArrayFormatter.format(readBuffer));
        }
        while(readBuffer.hasRemaining()) {
            TL1Message msg = _decoder.decodeTL1Message(readBuffer);
            if (msg != null) {
                com.grb.util.ByteBuffer buffer = msg.getBuffer();
                ByteBuffer out = ByteBuffer.allocate(buffer.getLength());
                out.put(buffer.getBackingArray(), buffer.getBackingArrayOffset(), buffer.getLength());
                out.flip();
                onTL1Message(argMap, out, msg.toString(), msg);
            }
        }
    }

    /**
     * [output] Called on a framed http message.
     *
     * @param msg
     */
    @Output("onTL1Message")
    public void onTL1Message(Map<String, Object> argMap, ByteBuffer buffer, String msg, TL1Message tl1Msg) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("onTL1Message: \r\n" + msg);
        }
        next("onTL1Message", argMap, bytesTL1Message[0], buffer, stringTL1Message[0], msg, tl1Message[0], tl1Msg);
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

        RangeConstraint<Integer> OneToMax = new RangeConstraint<Integer>(1, Integer.MAX_VALUE);

        Property<?> p;
        p = new Property<Integer>(MAX_OUTPUT_MESSAGE_SIZE_IN_CHARS_PROPERTY, MAX_OUTPUT_MESSAGE_SIZE_IN_CHARS_PROPERTY_DEFAULT, true,
                new PropertySource<Integer>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<Integer>(ctx.getPropertyString(MAX_OUTPUT_MESSAGE_SIZE_IN_CHARS_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Integer.class)),
                new MapPropertySource<Integer>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(MAX_OUTPUT_MESSAGE_SIZE_IN_CHARS_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Integer.class)));
        ((Property<Integer>)p).addVetoableListener(OneToMax);
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "Maximum output message size in characters");
        props.put(p.getId(), p);
    }}
