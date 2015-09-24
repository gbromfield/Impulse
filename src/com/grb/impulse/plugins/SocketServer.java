package com.grb.impulse.plugins;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.ReflectionException;

import com.grb.impulse.Argument;
import com.grb.impulse.BaseTransform;
import com.grb.impulse.Impulse;
import com.grb.impulse.Input;
import com.grb.impulse.Output;
import com.grb.impulse.TransformContext;
import com.grb.impulse.TransformCreationContext;
import com.grb.transport.TransportException;
import com.grb.transport.tcp.TCPTransportClient;
import com.grb.transport.tcp.TCPTransportClientProperties;
import com.grb.transport.tcp.TCPTransportServer;
import com.grb.transport.tcp.TCPTransportServerConnectionListener;
import com.grb.transport.tcp.TCPTransportServerProperties;
import com.grb.util.logging.LoggingContext;
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
 * This transform listens on a port and calls the output "onNewConnection"
 * when new connections are made.
 */
public class SocketServer extends BaseTransform implements TCPTransportServerConnectionListener, LoggingContext {
    /**
     * This variable is a place holder for a transform argument.
     * Argument syntax must follow <transform simple class name>.<type simple class name>.<field name>
     * This transforms outputs data into the argMap using this as a key.
     */
    @Argument
    final public static Object[] client = {"SocketServer.TCPTransportClient.client", TCPTransportClient.class};

    /**
     * Address to listen on.
     */
    public static final String LISTEN_ADDRESS_PROPERTY = "listenAddress";

    /**
     * Port to listen on.
     * A comma separated list of integers and ranges of integers.
     * 
     * Example: 1,3-5,6,10 (listens on port 1,3,4,5,6,10) 
     */
    public static final String LISTEN_PORT_PROPERTY = "listenPort";

    /**
     * Write timeout for accepted connections.
     */
    public static final String CLIENT_WRITE_TIMEOUT_IN_SECS_PROPERTY = "clientWriteTimeoutInSecs";
    
    /**
     * Default write timeout for accepted connections.
     */
    public static final int CLIENT_WRITE_TIMEOUT_IN_SECS_PROPERTY_DEFAULT = 30;
	
    /**
     * Write buffer size for accepted connections.
     */
    public static final String CLIENT_WRITE_BUFFER_SIZE_PROPERTY = "clientWriteBufferSize";

    /**
     * Default write buffer size for accepted connections.
     */
    public static final int CLIENT_WRITE_BUFFER_SIZE_PROPERTY_DEFAULT = 100000;

    /**
     * Property that indicates whether the socket is accepting or not.
     */
    public static final String ACCEPTING_PROPERTY = "accepting";

    /**
     * Default value for accepting connections.
     */
    public static final Boolean ACCEPTING_PROPERTY_DEFAULT = Boolean.TRUE;

    private ArrayList<Integer> _minList;
    private ArrayList<Integer> _maxList;
	private ArrayList<TCPTransportServer> _servers;
	private Property<Boolean> _acceptingProp;
	private PropertySource<Boolean> _acceptingJMXSource;
	
	public SocketServer(String transformName, String instanceName, TransformCreationContext transformCreationContext, Object... args) {
		super(transformName, instanceName, transformCreationContext);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void init() throws Exception {
		super.init();
		_acceptingProp = (Property<Boolean>)getProperty(ACCEPTING_PROPERTY);
		_acceptingJMXSource = _acceptingProp.getSource(JMXUtils.JMX_SOURCE);
        TCPTransportClientProperties clientProps = new TCPTransportClientProperties(
        		null, Impulse.getEventExecutorService(), Impulse.getReactorThread(), null, 
        		ByteBuffer.allocate(getIntegerProperty(CLIENT_WRITE_BUFFER_SIZE_PROPERTY)), 
        		getIntegerProperty(CLIENT_WRITE_TIMEOUT_IN_SECS_PROPERTY), TimeUnit.SECONDS);

        Property<String> addressProp = (Property<String>)getProperty(LISTEN_ADDRESS_PROPERTY);
        String listenAddr = null;
        if (addressProp.isSet()) {
            listenAddr = addressProp.getValue();
        }
        _minList = new ArrayList<Integer>();
        _maxList = new ArrayList<Integer>();
        parsePortStr();
        _servers = new ArrayList<TCPTransportServer>();
        for(int i = 0; i < _minList.size(); i++) {
            int minPort = _minList.get(i);
            int maxPort = _maxList.get(i);
            while(minPort <= maxPort) {
                TCPTransportServerProperties serverProps = new TCPTransportServerProperties(listenAddr, minPort, 
                        Impulse.getReactorThread(), clientProps);
                TCPTransportServer server = new TCPTransportServer(serverProps, this);
                server.setLoggingContext(this);    
                if (_logger.isInfoEnabled()) {
                    if (listenAddr == null) {
                        _logger.info("Listening on port " + minPort);
                    } else {
                        _logger.info(String.format("Listening on addr:port %s:%d", listenAddr, minPort));
                    }
                }
                _servers.add(server);
                minPort++;
            }
        }
	}

	@Override
	public void start() throws Exception {
		super.start();
		if (_acceptingProp.getValue()) {
		    for(int i = 0; i < _servers.size(); i++) {
	            _servers.get(i).startAccepting();
		    }
		}
	}

	@Override
	public void setAttribute(Attribute attribute)
			throws AttributeNotFoundException, InvalidAttributeValueException,
			MBeanException, ReflectionException {
        if (attribute.getName().equals(ACCEPTING_PROPERTY)) {
			try {
				if (((Boolean)attribute.getValue()).booleanValue()) {
					startAccepting();
				} else {
					stopAccepting();
				}
			} catch (TransportException e) {
	            throw new MBeanException(null, e.getMessage());
			}
		} else {
			super.setAttribute(attribute);
		}
	}

	/**
	 * [input] Starts accepting new connections.
	 * 
	 * @throws TransportException
	 */
	@Input("startAccepting")
	public void startAccepting() throws TransportException {
        if (_logger.isInfoEnabled()) {
            _logger.info("startAccepting");
       }
        for(int i = 0; i < _servers.size(); i++) {
            _servers.get(i).startAccepting();
        }
	    try {
			_acceptingJMXSource.setValue(true);
		} catch (PropertyVetoException e) {}
	}

	/**
	 * [input] Stops accepting new connections.
	 * 
	 * @throws TransportException
	 */
    @Input("stopAccepting")
    public void stopAccepting() throws TransportException {
        if (_logger.isInfoEnabled()) {
            _logger.info("stopAccepting");
       }
        for(int i = 0; i < _servers.size(); i++) {
            _servers.get(i).stopAccepting();
        }
	    try {
			_acceptingJMXSource.setValue(false);
		} catch (PropertyVetoException e) {}
    }

    /**
     * [output] Called with new connections.
     */
	@Output("onNewConnection")
	@Override
	public void onNewConnection(TCPTransportClient tcpClient) {
        if (_logger.isInfoEnabled()) {
            _logger.info("onNewConnection: " + tcpClient);
       }
	    // On Reactor Thread
	    next("onNewConnection", null, client[0], tcpClient);
	}

	@Override
	public String toString() {
		return _servers.toString();
	}

    private void parsePortStr() {
        String portStr = getStringProperty(LISTEN_PORT_PROPERTY);
        _minList.clear();
        _maxList.clear();
        String[] commaList = portStr.split(",");
        for(int i = 0; i < commaList.length; i++) {
            String[] dashList = commaList[i].split("-");
            if (dashList.length == 1) {
                if (commaList[i].contains("-")) {
                    int value = Integer.parseInt(dashList[0]);
                    _minList.add(value);
                    _maxList.add(Integer.MAX_VALUE);
                } else {
                    int value = Integer.parseInt(dashList[0]);
                    _minList.add(value);
                    _maxList.add(value);
                }
            } else if (dashList.length == 2) {
                _minList.add(Integer.parseInt(dashList[0]));
                _maxList.add(Integer.parseInt(dashList[1]));
            } else {
                throw new IllegalArgumentException("Illegal format of a range: " + commaList[i]);
            }
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

        RangeConstraint<Integer> OneToMax = new RangeConstraint<Integer>(1, Integer.MAX_VALUE);

    	Property<?> p;
        p = new Property<String>(LISTEN_ADDRESS_PROPERTY, "",
                new SystemPropertySource<String>(ctx.getPropertyString(LISTEN_ADDRESS_PROPERTY), PropertySource.PRIORITY_1, PropertySource.NULL_INVALID),
                new MapPropertySource<String>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(LISTEN_ADDRESS_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_2, PropertySource.NULL_INVALID));
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "Address to listen on");
        props.put(p.getId(), p);

        p = new Property<String>(LISTEN_PORT_PROPERTY,
                new SystemPropertySource<String>(ctx.getPropertyString(LISTEN_PORT_PROPERTY), PropertySource.PRIORITY_1, PropertySource.NULL_INVALID),
                new MapPropertySource<String>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(LISTEN_PORT_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_2, PropertySource.NULL_INVALID));
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "A comma separated list of integers and ranges of Integers of ports to listen on");
        props.put(p.getId(), p);
        
        p = new Property<Integer>(CLIENT_WRITE_TIMEOUT_IN_SECS_PROPERTY, CLIENT_WRITE_TIMEOUT_IN_SECS_PROPERTY_DEFAULT, true,
                new SystemPropertySource<Integer>(ctx.getPropertyString(CLIENT_WRITE_TIMEOUT_IN_SECS_PROPERTY), PropertySource.PRIORITY_1, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Integer.class)),
                new MapPropertySource<Integer>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(CLIENT_WRITE_TIMEOUT_IN_SECS_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_2, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Integer.class)));
        ((Property<Integer>)p).addVetoableListener(OneToMax);
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "Write timeout for accepted connections");
        props.put(p.getId(), p);

        p = new Property<Integer>(CLIENT_WRITE_BUFFER_SIZE_PROPERTY, CLIENT_WRITE_BUFFER_SIZE_PROPERTY_DEFAULT, true,
                new SystemPropertySource<Integer>(ctx.getPropertyString(CLIENT_WRITE_BUFFER_SIZE_PROPERTY), PropertySource.PRIORITY_1, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Integer.class)),
                new MapPropertySource<Integer>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(CLIENT_WRITE_BUFFER_SIZE_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_2, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Integer.class)));
        ((Property<Integer>)p).addVetoableListener(OneToMax);
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "Write buffer size for accepted connections");
        props.put(p.getId(), p);

        p = new Property<Boolean>(ACCEPTING_PROPERTY, ACCEPTING_PROPERTY_DEFAULT,
                new PropertySource<Boolean>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1, ValueOfConverter.getConverter(Boolean.class)),
                new SystemPropertySource<Boolean>(ctx.getPropertyString(ACCEPTING_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Boolean.class)),
                new MapPropertySource<Boolean>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(ACCEPTING_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Boolean.class)));
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "Whether the socket is accepting or not");
        props.put(p.getId(), p);
    }
}
