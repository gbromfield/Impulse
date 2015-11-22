package com.grb.impulse.plugins;

import com.grb.impulse.*;
import com.grb.impulse.parsers.tl1.*;
import com.grb.util.property.*;
import com.grb.util.property.impl.MapPropertySource;
import com.grb.util.property.impl.SystemPropertySource;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * Created by gbromfie on 11/21/15.
 */
public class TL1InProgress extends BaseTransform {
    /**
     * This variable is a place holder for a transform argument.
     * Argument syntax must follow <transform simple class name>.<type simple class name>.<field name>
     * This transforms outputs data into the argMap using this as a key.
     */
    @Argument
    final public static Object[] bytesTL1Message = {"TL1InProgress.ByteBuffer.bytesTL1Message", ByteBuffer.class};

    /**
     * This variable is a place holder for a transform argument.
     * Argument syntax must follow <transform simple class name>.<type simple class name>.<field name>
     * This transforms outputs data into the argMap using this as a key.
     */
    @Argument
    final public static Object[] stringTL1Message = {"TL1InProgress.String.stringTL1Message", String.class};

    /**
     * This variable is a place holder for a transform argument.
     * Argument syntax must follow <transform simple class name>.<type simple class name>.<field name>
     * This transforms outputs data into the argMap using this as a key.
     */
    @Argument
    final public static Object[] tl1Message = {"TL1InProgress.TL1Message.tl1Message", TL1Message.class};

    /**
     * Delay for the TL1 response. After the delay the data is sent
     * to the it's outputs.
     * Can be specified as a single value; or as a range in the form of
     * min-max. If a range is given a random value in that range will
     * be chosen.
     */
    public static final String RESPONSE_DELAY_IN_MS_PROPERTY = "respDelayInMS";

    /**
     * Default delay for the TL1 response.
     * Can be specified as a single value; or as a range in the form of
     * min-max. If a range is given a random value in that range will
     * be chosen.
     * Specifiable in the configuration file or command line.
     */
    public static final String COMMAND_DELAY_IN_MS_PROPERTY_DEFAULT = "10000";

    /**
     * Interval of the IP messages.
     * Specifiable in the configuration file or command line.
     */
    public static final String IN_PROGRESS_INTERVAL_IN_MS_PROPERTY = "ipIntervalInMS";

    /**
     * Default Interval of the IP messages.
     * Specifiable in the configuration file or command line.
     */
    public static final String IN_PROGRESS_INTERVAL_IN_MS_PROPERTY_DEFAULT = "1000";

    static protected Timer _timer = new Timer(true);

    protected class DelayerTimerTask extends TimerTask {
        private TL1InProgress _delayer;
        private Map<String, Object> _argMap;
        private TL1IPAckMessage _ackMsg;
        private TL1ResponseMessage _msg;
        private int _delayInMS;
        private int _ipIntervalInMS;
        private int _count;

        private DelayerTimerTask(TL1InProgress delayer, Map<String, Object> argMap, TL1IPAckMessage ackMsg, TL1ResponseMessage msg,
                                 int delayInMS, int ipIntervalInMS, int count) {
            _delayer = delayer;
            _argMap = argMap;
            _ackMsg = ackMsg;
            _msg = msg;
            _delayInMS = delayInMS;
            _ipIntervalInMS = ipIntervalInMS;
            _count = count;
        }

        @Override
        public void run() {
            if (_count < 1) {
                if (_logger.isDebugEnabled()) {
                    _logger.debug("Finished delaying " + Impulse.format(_argMap));
                }
                _delayer.onTL1Out(_argMap, _msg);
            } else {
                _delayer.onTL1Out(_argMap, _ackMsg);
                _timer.schedule(new DelayerTimerTask(_delayer, _argMap, _ackMsg, _msg, _delayInMS,
                        _ipIntervalInMS, _count - 1), _ipIntervalInMS);
            }
        }
    }

    protected int _delayInMS = 0;
    protected int _minDelayInMS = 0;
    protected int _maxDelayInMS = 0;
    protected int _ipIntervalInMS = 0;
    protected String _ctag;

    public TL1InProgress(String transformName, String instanceName,
                          TransformCreationContext transformCreationContext, Object... args) {
        super(transformName, instanceName, transformCreationContext);
    }

    @Override
    public void init() throws Exception {
        super.init();
        String cmdDelayInMSStr = getStringProperty(RESPONSE_DELAY_IN_MS_PROPERTY);
        parseDelay(cmdDelayInMSStr);
        String ipIntervalInMSStr = getStringProperty(IN_PROGRESS_INTERVAL_IN_MS_PROPERTY);
        _ipIntervalInMS = Integer.parseInt(ipIntervalInMSStr);
        _ctag = null;
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
        if ((_ctag != null) && (message instanceof TL1ResponseMessage)) {
            if (_ctag.equals(((TL1ResponseMessage)message).getCTAG())) {
                int count = (int)(_delayInMS / _ipIntervalInMS);
                if (count < 1) {
                    onTL1Out(argMap, message);
                } else {
                    try {
                        TL1IPAckMessage ackMsg = new TL1IPAckMessage(_ctag);
                        _timer.schedule(new DelayerTimerTask(this, argMap, ackMsg, (TL1ResponseMessage)message,
                                _delayInMS, _ipIntervalInMS, count), _ipIntervalInMS);
                    } catch (TL1MessageMaxSizeExceededException e) {
                        // Not happening
                    }
                }
            }
        }
    }

    /**
     * [output] In progresses and responses.
     *
     * @param argMap
     */
    @Output("onTL1Out")
    public void onTL1Out(Map<String, Object> argMap, TL1Message message) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("onTL1Out: \r\n" + Impulse.format(argMap));
        }
        ByteBuffer buffer = ByteBuffer.allocate(message.getBuffer().getLength());
        buffer.put(message.getBuffer().getBackingArray(), message.getBuffer().getBackingArrayOffset(), message.getBuffer().getLength());
        buffer.flip();
        next("onTL1Out", argMap, bytesTL1Message[0], buffer, stringTL1Message[0], message.toString(), tl1Message[0], message);
    }

    /**
     * [input] Setting of the ctag.
     *
     * @param argMap Data to be delayed.
     */
    @Input("onSetCtag")
    public void onSetCtag(Map<String, Object> argMap, TL1Message message) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("onSetCtag: \r\n" + Impulse.format(argMap));
        }
        if (message instanceof TL1InputMessage) {
            _ctag = ((TL1InputMessage)message).getCTAG();
        }
    }

    public void parseDelay(String delayValue) {
        String[] delays = delayValue.split("-");
        if (delays.length == 1) {
            _delayInMS = Integer.parseInt(delayValue);
        } else if (delays.length == 2) {
            _minDelayInMS = Integer.parseInt(delays[0]);
            _maxDelayInMS = Integer.parseInt(delays[1]);
        } else {
            throw new IllegalArgumentException("Illegal format of a range: " + delayValue);
        }
        if (_delayInMS < 0) {
            throw new IllegalArgumentException("delay must be greater than or equal to 0: " + _delayInMS);
        }
        if (_minDelayInMS < 0) {
            throw new IllegalArgumentException("minimum delay must be greater than or equal to 0: " + _minDelayInMS);
        }
        if (_maxDelayInMS < 0) {
            throw new IllegalArgumentException("maximum delay must be greater than or equal to 0: " + _maxDelayInMS);
        }
        if (_minDelayInMS > _maxDelayInMS) {
            throw new IllegalArgumentException("minimum delay must be less than or equal to maximum delay: " + delayValue);
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

        Property<String> p = new Property<String>(RESPONSE_DELAY_IN_MS_PROPERTY, COMMAND_DELAY_IN_MS_PROPERTY_DEFAULT, true,
                new PropertySource<String>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<String>(ctx.getPropertyString(RESPONSE_DELAY_IN_MS_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID),
                new MapPropertySource<String>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(RESPONSE_DELAY_IN_MS_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID));
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "Delay for the incoming data");
        props.put(p.getId(), p);

        p = new Property<String>(IN_PROGRESS_INTERVAL_IN_MS_PROPERTY, IN_PROGRESS_INTERVAL_IN_MS_PROPERTY_DEFAULT, true,
                new PropertySource<String>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<String>(ctx.getPropertyString(IN_PROGRESS_INTERVAL_IN_MS_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID),
                new MapPropertySource<String>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(IN_PROGRESS_INTERVAL_IN_MS_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID));
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "Interval between In Progress messages");
        props.put(p.getId(), p);
    }
}
