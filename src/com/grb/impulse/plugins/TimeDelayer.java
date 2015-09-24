package com.grb.impulse.plugins;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

import com.grb.impulse.BaseTransform;
import com.grb.impulse.Connection;
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
 * This transform delays data by the specified "delayInMS" parameter.
 * Can be specified as a single value; or as a range in the form of
 * min-max.
 */
public class TimeDelayer extends BaseTransform {
    /**
     * Delay for the incoming data. After the delay the data is sent
     * to the it's outputs.
     * Can be specified as a single value; or as a range in the form of
     * min-max. If a range is given a random value in that range will 
     * be chosen.
     */
    public static final String DELAY_IN_MS_PROPERTY = "delayInMS";

    /**
     * Default delay for incoming data in milliseconds.
     * Can be specified as a single value; or as a range in the form of
     * min-max. If a range is given a random value in that range will 
     * be chosen.
     * Specifiable in the configuration file or command line. 
     */
    public static final String DELAY_IN_MS_PROPERTY_DEFAULT = "1000";

    /**
     * Whether to use persistent connections.
     * The use case for this property is that when the timer eventually fires the 
     * transform may have already been disposed. If set to true, the connections
     * are saved and called regardless of the transform being disposed or not.
     * Be careful that the other end of the connection still exists. This really 
     * should only be used when the other end of the connection is auto started
     * and therefore persistent.
     */
    public static final String USE_PERSISTENT_CONNECTIONS_PROPERTY = "usePersistentConnections";
    
    /**
     * Default value for using persistent connections.
     */
    public static final boolean USE_PERSISTENT_CONNECTIONS_PROPERTY_DEFAULT = false;

	protected class DelayerTimerTask extends TimerTask {
		private TimeDelayer _delayer;
		private Map<String, Object> _argMap;
		private Set<Connection> _conns;
		
		private DelayerTimerTask(TimeDelayer delayer, Map<String, Object> argMap,  boolean usePersistentConnections) {
			_delayer = delayer;
			_argMap = argMap;
			if (usePersistentConnections) {
			    _conns = _delayer.getConnections("delayerOut");
			    // create a copy because on dispose this set is cleared
			    if (_conns != null) {
			        _conns = new TreeSet<Connection>(_conns); 
			    }
			} else {
                _conns = null;
			}
		}
		
		@Override
		public void run() {
		    if (_logger.isDebugEnabled()) {
		        _logger.debug("Finished delaying " + Impulse.format(_argMap));
		    }
		    if (_conns != null) {
	            Iterator<Connection> it = _conns.iterator();
	            while(it.hasNext()) {
	                Connection c = it.next();
	                next(c, _argMap);
	            }
		    } else {
	            _delayer.delayerOut(_argMap);
		    }
		}		
	}
	
	static protected Timer _timer = new Timer(true);
	protected int _delayInMS = 0;
	protected int _minDelayInMS = 0;
	protected int _maxDelayInMS = 0;
	protected boolean _usePersistentConnections;
	
	public TimeDelayer(String transformName, String instanceName, TransformCreationContext transformCreationContext, Object... args) {
		super(transformName, instanceName, transformCreationContext);
	}

	@Override
    public void init() throws Exception {
        super.init();
        String delayInMSStr = getStringProperty(DELAY_IN_MS_PROPERTY);
        parseDelay(delayInMSStr);
        _usePersistentConnections = getBooleanProperty(USE_PERSISTENT_CONNECTIONS_PROPERTY);
    }

	/**
	 * [input] Incoming data Object.
	 * 
	 * @param argMap Data to be delayed.
	 */
    @Input("delayerIn")
	public void delayerIn(Map<String, Object> argMap) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("delayerIn: \r\n" + Impulse.format(argMap));
        }
        if (_delayInMS > 0) {
            _timer.schedule(new DelayerTimerTask(this, argMap, _usePersistentConnections), _delayInMS);
        } else if (_minDelayInMS > 0) {
            int delay = _minDelayInMS + (int)((_maxDelayInMS - _minDelayInMS) * Math.random());
            _timer.schedule(new DelayerTimerTask(this, argMap, _usePersistentConnections), delay);
        }
	}	

    /**
     * [output] Delayed output. Data is unmodified.
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

        Property<String> p = new Property<String>(DELAY_IN_MS_PROPERTY, DELAY_IN_MS_PROPERTY_DEFAULT, true,
                new PropertySource<String>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<String>(ctx.getPropertyString(DELAY_IN_MS_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID),
                new MapPropertySource<String>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(DELAY_IN_MS_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID));
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "Delay for the incoming data");
        props.put(p.getId(), p);
        
        Property<Boolean> p1 = new Property<Boolean>(USE_PERSISTENT_CONNECTIONS_PROPERTY, USE_PERSISTENT_CONNECTIONS_PROPERTY_DEFAULT, true,
                new PropertySource<Boolean>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<Boolean>(ctx.getPropertyString(USE_PERSISTENT_CONNECTIONS_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Boolean.class)),
                new MapPropertySource<Boolean>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(USE_PERSISTENT_CONNECTIONS_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Boolean.class)));
        p1.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "Whether to use persistent connections");
        props.put(p1.getId(), p1);        
    }
}
