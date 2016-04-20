package com.grb.impulse.plugins;

import com.grb.impulse.*;
import com.grb.impulse.parsers.cli.CLIAgentDecoder;
import com.grb.impulse.parsers.cli.CLIMessage;
import com.grb.impulse.parsers.cli.CLIMessageMaxSizeExceededException;
import com.grb.impulse.parsers.cli.PromptConverter;
import com.grb.util.property.*;
import com.grb.util.property.impl.MapPropertySource;
import com.grb.util.property.impl.RangeConstraint;
import com.grb.util.property.impl.SystemPropertySource;
import com.grb.util.property.impl.ValueOfConverter;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * Created by gbromfie on 11/9/15.
 */
public class CLIAgentFramer extends BaseTransform {
    /**
     * This variable is a place holder for a transform argument.
     * Argument syntax must follow <transform simple class name>.<type simple class name>.<field name>
     * This transforms outputs data into the argMap using this as a key.
     */
    @Argument
    final public static Object[] bytesCLIMessage = {"CLIAgentFramer.ByteBuffer.bytesCLIMessage", ByteBuffer.class};

    /**
     * This variable is a place holder for a transform argument.
     * Argument syntax must follow <transform simple class name>.<type simple class name>.<field name>
     * This transforms outputs data into the argMap using this as a key.
     */
    @Argument
    final public static Object[] stringCLIMessage = {"CLIAgentFramer.String.stringCLIMessage", String.class};

    /**
     * This variable is a place holder for a transform argument.
     * Argument syntax must follow <transform simple class name>.<type simple class name>.<field name>
     * This transforms outputs data into the argMap using this as a key.
     */
    @Argument
    final public static Object[] cliMessage = {"CLIAgentFramer.CLIMessage.cliMessage", CLIMessage.class};

    /**
     * This variable is a place holder for a transform argument.
     * Argument syntax must follow <transform simple class name>.<type simple class name>.<field name>
     * This transforms outputs data into the argMap using this as a key.
     */
    @Argument
    final public static Object[] bytesCLIPreamble = {"CLIAgentFramer.ByteBuffer.bytesCLIPreamble", ByteBuffer.class};

    /**
     * This variable is a place holder for a transform argument.
     * Argument syntax must follow <transform simple class name>.<type simple class name>.<field name>
     * This transforms outputs data into the argMap using this as a key.
     */
    @Argument
    final public static Object[] stringCLIPreamble = {"CLIAgentFramer.String.stringCLIPreamble", String.class};

    /**
     * This variable is a place holder for a transform argument.
     * Argument syntax must follow <transform simple class name>.<type simple class name>.<field name>
     * This transforms outputs data into the argMap using this as a key.
     */
    @Argument
    final public static Object[] bytesCLIPrompt = {"CLIAgentFramer.ByteBuffer.bytesCLIPrompt", ByteBuffer.class};

    /**
     * This variable is a place holder for a transform argument.
     * Argument syntax must follow <transform simple class name>.<type simple class name>.<field name>
     * This transforms outputs data into the argMap using this as a key.
     */
    @Argument
    final public static Object[] stringCLIPrompt = {"CLIAgentFramer.String.stringCLIPrompt", String.class};

    /**
     * Maximum CLI input message message.
     */
    public static final String MAX_MESSAGE_SIZE_IN_CHARS_PROPERTY = "maxMessageSizeInChars";

    /**
     * Default maximum CLI input message message.
     */
    public static final int MAX_MESSAGE_SIZE_IN_CHARS_PROPERTY_DEFAULT = 100000;

    /**
     * CLI Prompt. This is a comma separated array of prompts.
     */
    public static final String PROMPTS_PROPERTY = "prompts";

    /**
     * Default CLI Prompt.
     */
    public static final String PROMPT_PROPERTY_DEFAULT = "";

    private static final PromptConverter PROMPT_CONVERTER = new PromptConverter();

    CLIAgentDecoder _decoder;
    String[] _prompts;
    boolean _firstTime;

    public CLIAgentFramer(String transformName, String instanceName,
                          TransformCreationContext transformCreationContext, Object... args) {
        super(transformName, instanceName, transformCreationContext);
        _decoder = null;
        _prompts = null;
        _firstTime = true;
    }

    @Override
    public void init() throws Exception {
        super.init();
        int maxBufferSize = getIntegerProperty(MAX_MESSAGE_SIZE_IN_CHARS_PROPERTY);
        _decoder = new CLIAgentDecoder(maxBufferSize);
        String prompts = getStringProperty(PROMPTS_PROPERTY);
        if (prompts.length() > 0) {
            _prompts = prompts.split(",");
            _decoder.setPrompts(_prompts);
        }
    }

    /**
     * [input] Data from the socket.
     *
     * @param argMap
     */
    @Input("onData")
    public void onData(Map<String, Object> argMap, ByteBuffer readBuffer) throws CLIMessageMaxSizeExceededException {
        if (_logger.isDebugEnabled()) {
            _logger.debug("onData: \r\n" + Impulse.GlobalByteArrayFormatter.format(readBuffer));
        }
        while(readBuffer.hasRemaining()) {
            CLIMessage msg = _decoder.decodeCLIMessage(readBuffer);
            if (msg != null) {
                if (_firstTime) {
                    System.out.println("Agent Preamble = \"" + msg + "\"");
                } else {
                    System.out.println("Agent = \"" + msg + "\"");
                }
                System.out.println("Agent Prompt = \"" + msg.getPromptStr() + "\"");

                com.grb.util.ByteBuffer buffer = msg.getMessage();
                ByteBuffer out = ByteBuffer.allocate(buffer.getLength());
                out.put(buffer.getBackingArray(), buffer.getBackingArrayOffset(), buffer.getLength());
                out.flip();

//                ByteArrayFormatter fmter = new ByteArrayFormatter(ByteArrayFormatter.DEFAULT_FORMAT);
//                fmter.setLimit(10000, ByteArrayFormatter.LimitType.BYTES);
//                System.out.println(fmter.format(msg.getMessage().getBackingArray(), 0, msg.getMessage().getLength()));

                if (_firstTime) {
                    onCLIPreamble(argMap, out, msg.toString());
                    _firstTime = false;
                } else {
                    onCLIMessage(argMap, out, msg.toString(), msg);
                }

                buffer = msg.getPrompt();
                ByteBuffer outP = ByteBuffer.allocate(buffer.getLength());
                outP.put(buffer.getBackingArray(), buffer.getBackingArrayOffset(), buffer.getLength());
                outP.flip();
//                System.out.println(fmter.format(msg.getPrompt().getBackingArray(), 0, msg.getPrompt().getLength()));
                onCLIPrompt(argMap, outP, msg.getPromptStr());
            }
        }
    }

    /**
     * [output] Called on a framed CLI message.
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
     * [output] Called on a framed CLI preamble.
     *
     * @param preamble
     */
    @Output("onCLIPreamble")
    public void onCLIPreamble(Map<String, Object> argMap, ByteBuffer buffer, String preamble) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("onCLIPreamble: \r\n" + preamble);
        }
        next("onCLIPreamble", argMap, bytesCLIPreamble[0], buffer, stringCLIPreamble[0], preamble);
    }

    /**
     * [output] Called on a framed http message.
     *
     * @param prompt
     */
    @Output("onCLIPrompt")
    public void onCLIPrompt(Map<String, Object> argMap, ByteBuffer buffer, String prompt) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("onCLIPrompt: \r\n" + prompt);
        }
        next("onCLIPrompt", argMap, bytesCLIPrompt[0], buffer, stringCLIPrompt[0], prompt);
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
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "Maximum output message size in characters");
        props.put(p.getId(), p);

        p = new Property<String>(PROMPTS_PROPERTY, PROMPT_PROPERTY_DEFAULT,
                new PropertySource<String>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1, PROMPT_CONVERTER),
                new SystemPropertySource<String>(ctx.getPropertyString(PROMPTS_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID, PROMPT_CONVERTER),
                new MapPropertySource<String>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(PROMPTS_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID, PROMPT_CONVERTER));
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "Comma separated list of CLI Prompts");
        props.put(p.getId(), p);
    }
}
