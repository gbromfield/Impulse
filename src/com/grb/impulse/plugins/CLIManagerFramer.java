package com.grb.impulse.plugins;

import com.grb.impulse.*;
import com.grb.impulse.parsers.cli.CLIManagerDecoder;
import com.grb.impulse.parsers.cli.CLIMessage;
import com.grb.impulse.parsers.cli.CLIMessageMaxSizeExceededException;
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
public class CLIManagerFramer extends BaseTransform {
    /**
     * This variable is a place holder for a transform argument.
     * Argument syntax must follow <transform simple class name>.<type simple class name>.<field name>
     * This transforms outputs data into the argMap using this as a key.
     */
    @Argument
    final public static Object[] bytesCLIMessage = {"CLIManagerFramer.ByteBuffer.bytesCLIMessage", ByteBuffer.class};

    /**
     * This variable is a place holder for a transform argument.
     * Argument syntax must follow <transform simple class name>.<type simple class name>.<field name>
     * This transforms outputs data into the argMap using this as a key.
     */
    @Argument
    final public static Object[] stringCLIMessage = {"CLIManagerFramer.String.stringCLIMessage", String.class};

    /**
     * This variable is a place holder for a transform argument.
     * Argument syntax must follow <transform simple class name>.<type simple class name>.<field name>
     * This transforms outputs data into the argMap using this as a key.
     */
    @Argument
    final public static Object[] cliMessage = {"CLIManagerFramer.CLIMessage.cliMessage", CLIMessage.class};

    /**
     * Maximum CLI input message message.
     */
    public static final String MAX_MESSAGE_SIZE_IN_CHARS_PROPERTY = "maxMessageSizeInChars";

    /**
     * Default maximum CLI input message message.
     */
    public static final int MAX_MESSAGE_SIZE_IN_CHARS_PROPERTY_DEFAULT = 10000;

    /**
     * CLI Command Completion Character.
     */
    public static final String COMMAND_COMPLETED_STRINGS_PROPERTY = "commandCompletionStrings";

    /**
     * Default CLI Command Completion Character.
     */
    public static final String COMMAND_COMPLETED_STRINGS_PROPERTY_DEFAULT = "\r";

    CLIManagerDecoder _decoder;
    String _commandCompletionStrings;

    public CLIManagerFramer(String transformName, String instanceName,
                            TransformCreationContext transformCreationContext, Object... args) {
        super(transformName, instanceName, transformCreationContext);
        _decoder = null;
        _commandCompletionStrings = null;
    }

    @Override
    public void init() throws Exception {
        super.init();
        _commandCompletionStrings = getStringProperty(COMMAND_COMPLETED_STRINGS_PROPERTY);
        String[] commandCompletionStringsArray = _commandCompletionStrings.split(",");
        int maxBufferSize = getIntegerProperty(MAX_MESSAGE_SIZE_IN_CHARS_PROPERTY);
        _decoder = new CLIManagerDecoder(maxBufferSize, commandCompletionStringsArray);
    }

    /**
     * [input] Data from the socket.
     *
     * @param argMap
     */
    @Input("onData")
    public void onData(Map<String, Object> argMap, ByteBuffer readBuffer) throws CLIMessageMaxSizeExceededException, ParseException {
        if (_logger.isDebugEnabled()) {
            _logger.debug("onData: \r\n" + Impulse.GlobalByteArrayFormatter.format(readBuffer));
        }
        while(readBuffer.hasRemaining()) {
            CLIMessage msg = _decoder.decodeCLIMessage(readBuffer);
            if (msg != null) {
                System.out.println("Manager = \"" + msg.getMessageStr().substring(0, msg.getMessageStr().length()-1) + "\"");
                com.grb.util.ByteBuffer buffer = msg.getMessage();
                ByteBuffer out = ByteBuffer.allocate(buffer.getLength());
                out.put(buffer.getBackingArray(), buffer.getBackingArrayOffset(), buffer.getLength());
                out.flip();
                onCLIMessage(argMap, out, msg.toString(), msg);
            }
        }
    }

    /**
     * [output] Called on a framed http message.
     *
     * @param msg
     */
    @Output("onCLIMessage")
    public void onCLIMessage(Map<String, Object> argMap, ByteBuffer buffer, String msg, CLIMessage cliMsg) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("onCLIMessage: \r\n" + msg);
        }
        next("onCLIMessage", argMap, bytesCLIMessage[0], buffer, stringCLIMessage[0], msg, cliMessage[0], cliMsg);
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
        p = new Property<Integer>(MAX_MESSAGE_SIZE_IN_CHARS_PROPERTY, MAX_MESSAGE_SIZE_IN_CHARS_PROPERTY_DEFAULT, true,
                new PropertySource<Integer>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<Integer>(ctx.getPropertyString(MAX_MESSAGE_SIZE_IN_CHARS_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Integer.class)),
                new MapPropertySource<Integer>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(MAX_MESSAGE_SIZE_IN_CHARS_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Integer.class)));
        ((Property<Integer>)p).addVetoableListener(OneToMax);
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "Maximum input message size in characters");
        props.put(p.getId(), p);

        p = new Property<String>(COMMAND_COMPLETED_STRINGS_PROPERTY, COMMAND_COMPLETED_STRINGS_PROPERTY_DEFAULT,
                new PropertySource<String>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<String>(ctx.getPropertyString(COMMAND_COMPLETED_STRINGS_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID),
                new MapPropertySource<String>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(COMMAND_COMPLETED_STRINGS_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID));
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "Command Completion String");
        props.put(p.getId(), p);
    }
}
