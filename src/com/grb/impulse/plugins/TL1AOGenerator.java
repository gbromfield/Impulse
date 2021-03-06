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
import java.util.*;

/**
 * Created by gbromfie on 8/19/16.
 */
public class TL1AOGenerator extends BaseTransform {

    public class AORunnable extends TimerTask {
        private int _index;
        private Map<String, Object> _argMap;
        private ArrayList<ByteBuffer> _buffers;
        private long _rate;
        private Timer _timer;
        private long _timestamp;
        private long _startTimestamp;

        public AORunnable(Map<String, Object> argMap, ArrayList<ByteBuffer> aoBuffers) {
            _argMap = argMap;
            _buffers = aoBuffers;
            _index = 0;
            _rate = (long) (((double)(1.0 / _aoRate)) * 1000);
            _timer = new Timer("AO Timer", true);
            _timestamp = 0;
        }

        public void start() {
            if (_aoRate > 1000) {
                if (_logger.isInfoEnabled()) {
                    _logger.info(String.format("Starting %d message AO Stream at the fastest possible, reporting after %d messages\r\n", _aoNum, _reportInterval));
                }
                _timer.schedule(this, 1);
            } else {
                if (_logger.isInfoEnabled()) {
                    _logger.info(String.format("Starting %d message AO Stream at %d messages/second, reporting after %d messages\r\n", _aoNum, _aoRate, _reportInterval));
                }
                _timer.scheduleAtFixedRate(this, 0, _rate);
            }
            _startTimestamp = System.currentTimeMillis();
            _timestamp = _startTimestamp;
        }

        @Override
        public void run() {
            if (_aoRate > 1000) {
                long ts = System.currentTimeMillis();
                long intervalts = ts;
                for(int i = 0; i < _buffers.size(); i++) {
                    onAO(_argMap, _buffers.get(i));
                    incrementAtag();
                    if (((i+1) % _reportInterval) == 0) {
                        long current = System.currentTimeMillis();
                        long totalElapsed = current - ts;
                        long rate = (long)((double)i / ((double)totalElapsed / 1000.0));
                        long intervalElapsed = current - intervalts;
                        long intervalRate = (long)((double)_reportInterval / ((double)intervalElapsed / 1000.0));
                        _logger.info(String.format("Finished %d message AO Stream at average %d messages/second, interval %d messages/second\r\n", i+1, rate, intervalRate));
                        intervalts = System.currentTimeMillis();
                    }
                }
                long elapsed = System.currentTimeMillis() - ts;
                long rate = (long)((double)_aoNum / ((double)elapsed / 1000.0));
                _logger.info(String.format("Finished %d message AO Stream at %d messages/second\r\n", _aoNum, rate));
            } else {
                if (_index >= _buffers.size()) {
                    _timer.cancel();
                    _logger.info(String.format("Finished %d message AO Stream at %d messages/second\r\n", _aoNum, _aoRate));
                } else {
                    onAO(_argMap, _buffers.get(_index));
                    _index++;
                    incrementAtag();
                    if ((_index % _reportInterval) == 0) {
                        long ts = System.currentTimeMillis();
                        long deltaInMS = ts - _timestamp;
                        double deltaInS = (double)deltaInMS / 1000.0;
                        long totalInMS = ts - _startTimestamp;
                        double totalInS = (double)totalInMS / 1000.0;
                        double totalRate = (double)_index / totalInS;
                        double deltaRate = (double)_reportInterval / deltaInS;
                        _logger.info(String.format("Finished %d messages in %.2f seconds - %.2f msgs/sec (%d message delta is %.2f seconds - %.2f msgs/sec)\r\n", _index, totalInS, totalRate, _reportInterval, deltaInS, deltaRate));
                        _timestamp = ts;
                    }
                }
            }
        }

        private void incrementAtag() {
            try {
                _atagJMXSource.setValue(_atagProp.getValue() + 1);
            } catch (PropertyVetoException e) {
                // shouldn't happen
            }
        }
    }

    /**
     * This variable is a place holder for a transform argument.
     * Argument syntax must follow <transform simple class name>.<type simple class name>.<field name>
     * This transforms outputs data into the argMap using this as a key.
     */
    @Argument
    final public static Object[] bytesTL1Message = {"TL1AOGenerator.ByteBuffer.bytesTL1Message", ByteBuffer.class};

    /**
     * This variable is a place holder for a transform argument.
     * Argument syntax must follow <transform simple class name>.<type simple class name>.<field name>
     * This transforms outputs data into the argMap using this as a key.
     */
//    @Argument
//    final public static Object[] stringTL1Message = {"TL1AOGenerator.String.stringTL1Message", String.class};

    /**
     * This variable is a place holder for a transform argument.
     * Argument syntax must follow <transform simple class name>.<type simple class name>.<field name>
     * This transforms outputs data into the argMap using this as a key.
     */
//    @Argument
//    final public static Object[] tl1Message = {"TL1AOGenerator.TL1Message.tl1Message", TL1Message.class};

    /**
     * Number of AOs to publish.
     */
    public static final String AO_NUMBER_PROPERTY = "aoNumber";

    /**
     * Rate in AOs per second to send.
     */
    public static final String AO_RATE_IN_MSGS_PER_SEC_PROPERTY = "aoRate";

    /**
     * AO 1 to send.
     */
    public static final String AO_1_PROPERTY = "ao_1";

    /**
     * AO 2 to send.
     */
    public static final String AO_2_PROPERTY = "ao_2";

    /**
     * AO_1 to AO_2 proportion to send.
     * For example:
     * 1:0, send all AO_1
     * 1:1, send AO_1 and AO_2 alternating
     * 100:1, send 100 AO_1 to 1 AO_2
     */
    public static final String AO_PROPORTION_PROPERTY = "ao_proportion";

    /**
     * ATAG to send.
     */
    public static final String ATAG_PROPERTY = "atag";

    /**
     * ATAG sequence to send.
     * Example: 1,2,4
     * Means first ao will have atag 1, second 2, third 4
     * Subsequence values will be the last value incremented by 1
     */
    public static final String ATAG_SEQUENCE_PROPERTY = "atagSequence";

    /**
     * Interval between reports.
     */
    public static final String REPORT_INTERVAL_PROPERTY = "reportInterval";

    /**
     * Default regex verb
     * Specifiable in the configuration file or command line.
     */
    public static final int AO_NUMBER_PROPERTY_DEFAULT = 1;

    /**
     * Default regex verb
     * Specifiable in the configuration file or command line.
     */
    public static final int AO_RATE_IN_MSGS_PER_SEC_PROPERTY_DEFAULT = 1;

    /**
     * Default ATAG
     * Specifiable in the configuration file or command line.
     */
    public static final int ATAG_PROPERTY_DEFAULT = 1;

    /**
     * Default Report Interval
     * Specifiable in the configuration file or command line.
     */
    public static final int REPORT_INTERVAL_PROPERTY_DEFAULT = 1000;

    private int _aoNum;
    private int _aoRate;
    private String[] _aos;
    private int[] _aoProportions;
    private Property<Integer> _atagProp;
    private PropertySource<Integer> _atagJMXSource;
    private int _reportInterval;
    private int[] _atagSequence;

//    private TL1AgentDecoder _aoDecoder;

    public TL1AOGenerator(String transformName, String instanceName,
                         TransformCreationContext transformCreationContext, Object... args) {
        super(transformName, instanceName, transformCreationContext);
//        _aoDecoder = new TL1AgentDecoder();
    }

    @Override
    public void init() throws Exception {
        super.init();
        _aoNum = getIntegerProperty(AO_NUMBER_PROPERTY);
        _aoRate = getIntegerProperty(AO_RATE_IN_MSGS_PER_SEC_PROPERTY);
        _aos = new String[2];
        _aos[0] = getStringProperty(AO_1_PROPERTY);
        _aos[1] = getStringProperty(AO_2_PROPERTY);
        String aoProportionStr = getStringProperty(AO_PROPORTION_PROPERTY);
        String[] aoProportionArr = aoProportionStr.split(":");
        _aoProportions = new int[2];
        _aoProportions[0] = Integer.parseInt(aoProportionArr[0]);
        _aoProportions[1] = Integer.parseInt(aoProportionArr[1]);
        _reportInterval = getIntegerProperty(REPORT_INTERVAL_PROPERTY);
        String atagSeqStr = getStringProperty(ATAG_SEQUENCE_PROPERTY);
        if (atagSeqStr.isEmpty()) {
            _atagSequence = null;
        } else {
            String[] atagSeqArr = atagSeqStr.split(",");
            _atagSequence = new int[atagSeqArr.length];
            for(int i = 0; i < atagSeqArr.length; i++) {
                _atagSequence[i] = Integer.parseInt(atagSeqArr[i]);
            }
        }
        // get global template property
        Iterator<Property<?>> it = getTransformContext().getProperties().values().iterator();
        while(it.hasNext()) {
            Property<?> p = it.next();
            if (p.getId().equals(ATAG_PROPERTY)) {
                _atagProp = (Property<Integer>)p;
            }
        }
        // remove local count property
        _properties.remove(ATAG_PROPERTY);
        _atagJMXSource = _atagProp.getSource(JMXUtils.JMX_SOURCE);
    }

    /**
     * [input] Indicates to start sending AOs.
     *
     * @param argMap Data to be delayed.
     */
    @Input("onStart")
    public void onStart(Map<String, Object> argMap) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("onStart: \r\n" + Impulse.format(argMap));
        }
        int aoAtag = _atagProp.getValue();
        ArrayList<ByteBuffer> buffers = new ArrayList<ByteBuffer>();
        int index = 0;
        for(int i = aoAtag; i < _aoNum + aoAtag; i++) {
            int rem = index % (_aoProportions[0] + _aoProportions[1]);
            String ao;
            if (rem < _aoProportions[0]) {
                ao = _aos[0];
            } else {
                ao = _aos[1];
            }
            String aoToSend;
            if (_atagSequence == null) {
                aoToSend = ao.replaceAll("<ATAG>", String.valueOf(i));
            } else {
                if (index < _atagSequence.length) {
                    aoToSend = ao.replaceAll("<ATAG>", String.valueOf(_atagSequence[index]));
                } else {
                    aoToSend = ao.replaceAll("<ATAG>", String.valueOf(_atagSequence[_atagSequence.length-1] +
                            (index - _atagSequence.length + 1)));
                }
            }
            ByteBuffer buffer = ByteBuffer.allocate(aoToSend.length());
            buffer.put(aoToSend.getBytes());
            buffer.flip();
            buffers.add(buffer);
            index++;
        }
        AORunnable aoRunnable = new AORunnable(argMap, buffers);
        aoRunnable.start();
    }

    /**
     * [output]
     *
     * @param argMap
     */
    @Output("onAO")
    public void onAO(Map<String, Object> argMap, ByteBuffer buffer) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("onAO: \r\n" + Impulse.format(argMap));
        }
        next("onAO", argMap, bytesTL1Message[0], buffer);
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

        RangeConstraint<Integer> ZeroToMax = new RangeConstraint<Integer>(0, Integer.MAX_VALUE);
        RangeConstraint<Integer> OneToMax = new RangeConstraint<Integer>(1, Integer.MAX_VALUE);

        Property<Integer> p = new Property<Integer>(AO_NUMBER_PROPERTY, AO_NUMBER_PROPERTY_DEFAULT, true,
                new PropertySource<Integer>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<Integer>(ctx.getPropertyString(AO_NUMBER_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Integer.class)),
                new MapPropertySource<Integer>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(AO_NUMBER_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Integer.class)));
        ((Property<Integer>)p).addVetoableListener(ZeroToMax);
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "Number of AOs to send");
        props.put(p.getId(), p);

        p = new Property<Integer>(AO_RATE_IN_MSGS_PER_SEC_PROPERTY, AO_RATE_IN_MSGS_PER_SEC_PROPERTY_DEFAULT, true,
                new PropertySource<Integer>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<Integer>(ctx.getPropertyString(AO_RATE_IN_MSGS_PER_SEC_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Integer.class)),
                new MapPropertySource<Integer>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(AO_RATE_IN_MSGS_PER_SEC_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Integer.class)));
        ((Property<Integer>)p).addVetoableListener(OneToMax);
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "Rate to send the AOs in messages per second");
        props.put(p.getId(), p);

        p = new Property<Integer>(ATAG_PROPERTY, ATAG_PROPERTY_DEFAULT, true,
                new PropertySource<Integer>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<Integer>(ctx.getPropertyString(ATAG_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Integer.class)),
                new MapPropertySource<Integer>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(ATAG_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Integer.class)));
        ((Property<Integer>)p).addVetoableListener(OneToMax);
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "ATAG value");
        props.put(p.getId(), p);

        p = new Property<Integer>(REPORT_INTERVAL_PROPERTY, REPORT_INTERVAL_PROPERTY_DEFAULT, true,
                new PropertySource<Integer>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<Integer>(ctx.getPropertyString(REPORT_INTERVAL_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Integer.class)),
                new MapPropertySource<Integer>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(REPORT_INTERVAL_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Integer.class)));
        ((Property<Integer>)p).addVetoableListener(OneToMax);
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "Report Interval value");
        props.put(p.getId(), p);

        Property<String> ps = new Property<String>(AO_1_PROPERTY, "", false,
                new PropertySource<String>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<String>(ctx.getPropertyString(AO_1_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID),
                new MapPropertySource<String>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(AO_1_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID));
        ps.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "AO 1 to send");
        props.put(ps.getId(), ps);

        ps = new Property<String>(AO_2_PROPERTY, "", false,
                new PropertySource<String>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<String>(ctx.getPropertyString(AO_2_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID),
                new MapPropertySource<String>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(AO_2_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID));
        ps.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "AO 2 to send");
        props.put(ps.getId(), ps);

        ps = new Property<String>(ATAG_SEQUENCE_PROPERTY, "", true,
                new PropertySource<String>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<String>(ctx.getPropertyString(ATAG_SEQUENCE_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID),
                new MapPropertySource<String>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(ATAG_SEQUENCE_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID));
        ps.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "ATAG sequnce");
        props.put(ps.getId(), ps);

        ps = new Property<String>(AO_PROPORTION_PROPERTY, "1:0", false,
                new PropertySource<String>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<String>(ctx.getPropertyString(AO_PROPORTION_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID),
                new MapPropertySource<String>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(AO_PROPORTION_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID));
        ps.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "AO proportion [AO_1 : AO_2], for example 1:1 for equal proportion to both AOs");
        props.put(ps.getId(), ps);
    }
}
