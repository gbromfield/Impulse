package com.grb.impulse.plugins;

import com.grb.impulse.*;
import com.grb.impulse.parsers.tl1.TL1InputMessage;
import com.grb.impulse.parsers.tl1.TL1Message;
import com.grb.impulse.parsers.tl1.TL1ResponseMessage;
import com.grb.util.property.*;
import com.grb.util.property.impl.MapPropertySource;
import com.grb.util.property.impl.SystemPropertySource;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by gbromfie on 11/23/15.
 */
public class TL1Responder extends BaseTransform {

    /**
     * Completion code for the response message
     */
    public static final String COMPLETION_CODE_PROPERTY = "completionCode";

    /**
     * Error code for the response message
     */
    public static final String ERROR_CODE_PROPERTY = "errorCode";

    /**
     * Error Message for the response message
     */
    public static final String ERROR_MESSAGE_PROPERTY = "errorMessage";

    /**
     * Default completion code
     * Specifiable in the configuration file or command line.
     */
    public static final String COMPLETION_CODE_PROPERTY_DEFAULT = "DENY";

    /**
     * Default error code
     * Specifiable in the configuration file or command line.
     */
    public static final String ERROR_CODE_PROPERTY_DEFAULT = "IPNV";

    /**
     * Default error message
     * Specifiable in the configuration file or command line.
     */
    public static final String ERROR_MESSAGE_PROPERTY_DEFAULT = "";

    protected String _completionCode;
    protected String _errorCode;
    protected String _errorMessage;

    public TL1Responder(String transformName, String instanceName,
                        TransformCreationContext transformCreationContext, Object... args) {
        super(transformName, instanceName, transformCreationContext);
    }

    @Override
    public void init() throws Exception {
        super.init();
        _completionCode = getStringProperty(COMPLETION_CODE_PROPERTY);
        _errorCode = getStringProperty(ERROR_CODE_PROPERTY);
        _errorMessage = getStringProperty(ERROR_MESSAGE_PROPERTY);
    }

    /**
     * [input] Incoming TL1 Response.
     *
     * @param argMap Data to be delayed.
     */
    @Input("onTL1In")
    public void onTL1In(Map<String, Object> argMap, TL1Message message) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("onTL1In: \r\n" + Impulse.format(argMap));
        }
        if (message instanceof TL1ResponseMessage) {
            String output = null;
            TL1ResponseMessage resp = (TL1ResponseMessage)message;
            if (_completionCode.equalsIgnoreCase("DENY")) {
                if (_errorMessage.isEmpty()) {
                    output = String.format("\r\n\n   %s %s %s\r\nM  %s %s\r\n   %s\r\n;", resp.getTid(), resp.getDate(), resp.getTime(), resp.getCTAG(), _completionCode, _errorCode);
                } else {
                    output = String.format("\r\n\n   %s %s %s\r\nM  %s %s\r\n   %s\r\n   %s\r\n;", resp.getTid(), resp.getDate(), resp.getTime(), resp.getCTAG(), _completionCode, _errorCode, _errorMessage);
                }
            } else {
                output = String.format("\r\n\n   %s %s %s\r\nM  %s %s\r\n;", resp.getTid(), resp.getDate(), resp.getTime(), resp.getCTAG(), _completionCode);
            }
            argMap.put((String)TL1AgentFramer.stringTL1Message[0], output);
            ByteBuffer buf = ByteBuffer.allocate(output.length());
            buf.put(output.getBytes());
            buf.flip();
            argMap.put((String)TL1AgentFramer.bytesTL1Message[0], buf);
        }
        onTL1Out(argMap);
    }

    /**
     * [output] In progresses and responses.
     *
     * @param argMap
     */
    @Output("onTL1Out")
    public void onTL1Out(Map<String, Object> argMap) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("onTL1Out: \r\n" + Impulse.format(argMap));
        }
        next("onTL1Out", argMap);
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

        Property<String> p = new Property<String>(ERROR_CODE_PROPERTY, ERROR_CODE_PROPERTY_DEFAULT, true,
                new PropertySource<String>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<String>(ctx.getPropertyString(ERROR_CODE_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID),
                new MapPropertySource<String>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(ERROR_CODE_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID));
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "Error Code");
        props.put(p.getId(), p);

        p = new Property<String>(ERROR_MESSAGE_PROPERTY, ERROR_MESSAGE_PROPERTY_DEFAULT, true,
                new PropertySource<String>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<String>(ctx.getPropertyString(ERROR_MESSAGE_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID),
                new MapPropertySource<String>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(ERROR_MESSAGE_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID));
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "Error Message");
        props.put(p.getId(), p);

        p = new Property<String>(COMPLETION_CODE_PROPERTY, COMPLETION_CODE_PROPERTY_DEFAULT, true,
                new PropertySource<String>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<String>(ctx.getPropertyString(COMPLETION_CODE_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID),
                new MapPropertySource<String>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(COMPLETION_CODE_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID));
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "Completion Code");
        props.put(p.getId(), p);
    }
}
