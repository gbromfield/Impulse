package com.grb.impulse.plugins;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanException;
import javax.management.MBeanOperationInfo;
import javax.management.ReflectionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.grb.impulse.Argument;
import com.grb.impulse.BaseTransform;
import com.grb.impulse.Connection;
import com.grb.impulse.Impulse;
import com.grb.impulse.Input;
import com.grb.impulse.Output;
import com.grb.impulse.TransformContext;
import com.grb.impulse.TransformCreationContext;
import com.grb.impulse.Transport;
import com.grb.transport.TransportEvent;
import com.grb.transport.TransportEventListener;
import com.grb.transport.TransportException;
import com.grb.transport.TransportOperation;
import com.grb.transport.TransportReadListener;
import com.grb.transport.TransportSendResult;
import com.grb.transport.TransportState;
import com.grb.transport.TransportStateChangeEvent;
import com.grb.transport.tcp.DefaultSocketChannelFactory;
import com.grb.transport.tcp.TCPTransportClient;
import com.grb.transport.tcp.TCPTransportClientProperties;
import com.grb.util.property.JMXUtils;
import com.grb.util.property.Property;
import com.grb.util.property.PropertyConversionException;
import com.grb.util.property.PropertyConverter;
import com.grb.util.property.PropertySource;
import com.grb.util.property.PropertyVetoException;
import com.grb.util.property.impl.MapPropertySource;
import com.grb.util.property.impl.RangeConstraint;
import com.grb.util.property.impl.SystemPropertySource;
import com.grb.util.property.impl.ValueOfConverter;

/**
 * This transform when given a new connection from "onNewConnection"
 * creates a connection to a client. 
 * Alternately, this socket can initiate a connection to a server. 
 * But both cannot be done using the same instance. If you want a 
 * tunnel it is better to use the Tunnel transform, or 2 of these 
 * sockets would be needed that are linked together (one for the 
 * client side, and one for the server side). See socketTunnel.properties.
 * This transform has the exact same functionality as the 
 * Tunnel transform but 1 Tunnel transform is needed where 2 Socket 
 * transforms would be used.
 * <p>
 * Terminology:
 * <li> Client Side - the side of the tunnel that an application has initiated a connection to.
 * <li> Server Side - the side of the tunnel that the tunnel initiates connection with. 
 */
public class Socket extends BaseTransform implements TransportEventListener, Transport {

    /**
     * This variable is a place holder for a transform argument.
     * Argument syntax must follow <transform simple class name>.<type simple class name>.<field name>
     * This transforms outputs data into the argMap using this as a key.
     */
    @Argument
    final public static Object[] inputBytes = {"Socket.ByteBuffer.inputBytes", ByteBuffer.class};

    /**
     * Write timeout.
     */
    public static final String WRITE_TIMEOUT_IN_SECS_PROPERTY = "writeTimeoutInSecs";
    
    /**
     * Default Write timeout.
     */
    public static final int WRITE_TIMEOUT_IN_SECS_PROPERTY_DEFAULT = 30;

    /**
     * Connect timeout. 
     */
    public static final String CONNECT_TIMEOUT_IN_SECS_PROPERTY = "connectTimeoutInSecs";

    /**
     * Default connect timeout. 
     */
    public static final int CONNECT_TIMEOUT_IN_SECS_PROPERTY_DEFAULT = 30;

    /**
     * Write buffer size.
     */
    public static final String WRITE_BUFFER_SIZE_PROPERTY = "writeBufferSize";

    /**
     * Default write buffer size.
     */
    public static final int WRITE_BUFFER_SIZE_PROPERTY_DEFAULT = 100000;

    /**
     * Read buffer size.
     */
    public static final String READ_BUFFER_SIZE_PROPERTY = "readBufferSize";

    /**
     * Default read buffer size.
     */
    public static final int READ_BUFFER_SIZE_PROPERTY_DEFAULT = 100000;

    /**
     * Property to clone read buffers.
     * If you want to manipulate the buffer or save it for future use
     * you must clone the buffer because the one that is passed to 
     * connections will be cleared when the connection return so that
     * more data can be read. 
     */
    public static final String CLONE_READ_BUFFER_PROPERTY = "cloneReadBuffer";
    
    /**
     * Default property to clone read buffers.
     */
    public static final boolean CLONE_READ_BUFFER_PROPERTY_DEFAULT = false;

    /**
     * IP address and port to connect to.
     * Specifiable in the configuration file or command line. 
     */
    public static final String IP_PORT_PROPERTY = "ipPort";

    /**
     * IP address to connect to.
     * Specifiable through JMX only.
     * Used in conjuction with {@link #PORT_PROPERTY}
     * This overrrides the property {@link #IP_PORT_PROPERTY} 
     */
    public static final String IP_ADDRESS_PROPERTY = "ipAddress";

    /**
     * Port to connect to.
     * Specifiable through JMX only.
     * Used in conjuction with {@link #IP_ADDRESS_PROPERTY}
     * This overrrides the property {@link #IP_PORT_PROPERTY} 
     */
    public static final String PORT_PROPERTY = "port";

    /**
     * Auto start reading on new connections.
     */
    public static final String AUTO_START_READ_PROPERTY = "autoStartRead";

    /**
     * Default auto start reading on new connections.
     */
    public static final boolean AUTO_START_READ_PROPERTY_DEFAULT = true;

    /**
     * Starts reading from the socket.
     */
    public static final String START_READING_OPERATION = "startReading";   

    /**
     * Stops reading from the socket.
     */
    public static final String STOP_READING_OPERATION = "stopReading";

    /**
     * Closes the socket and disconnects the parties.
     */
    public static final String CLOSE_OPERATION = "close";
    
    /**
     * Transport properties
     */
    private TCPTransportClient _client;
    private TransportReadListener _readListener;
    private boolean _cloneReadBuffer;
    private TransportSendResult _sendResult;
    private InetSocketAddress _localAddress;
    private InetSocketAddress _remoteAddress;
    private boolean _clientSide;
    
    public Socket(String transformName, String instanceName,
            TransformCreationContext transformCreationContext, Object... args) {
        super(transformName, instanceName, transformCreationContext);
        if ((args == null) || (args.length != 1) || (!(args[0] instanceof TCPTransportClient))) {
            // ok - server side socket
            _client = null;
            _sendResult = new TransportSendResult();
            _localAddress = null;
            _remoteAddress = null;
            _clientSide = false;
        } else {
            _client = (TCPTransportClient)args[0];
            _client.setLoggingContext(this);
            _sendResult = new TransportSendResult();
            _localAddress = _client.getLocalAddress();
            _remoteAddress = _client.getRemoteAddress();
            _clientSide = true;
        }

        _readListener = new TransportReadListener() {
            @Override
            public boolean onTransportRead(ByteBuffer readBuffer, int numBytesRead) {
                if (numBytesRead <= 0) {
                    _client.close();
                    return false;
                } else {
                    onRead(readBuffer);
                    return true;
                }
            }
        };
        _transport = this;
    }

    @Override
    public void init() throws Exception {
        super.init();
        _cloneReadBuffer = getBooleanProperty(CLONE_READ_BUFFER_PROPERTY);

        if (_client == null) {
            String ipAddress = getStringProperty(IP_ADDRESS_PROPERTY);
            if ((ipAddress == null) || (ipAddress.trim().length() == 0)) {
                throw new IllegalArgumentException(String.format("Server IP Address not specified. Must specify properties \"%s\" or \"%s\"", IP_PORT_PROPERTY, IP_ADDRESS_PROPERTY));
            }
            int port = getIntegerProperty(PORT_PROPERTY);
            if (port == 0) {
                throw new IllegalArgumentException(String.format("Server port not specified. Must specify properties \"%s\" or \"%s\"", IP_PORT_PROPERTY, PORT_PROPERTY));
            }
            InetSocketAddress addr = new InetSocketAddress(InetAddress.getByName(ipAddress), port);
            TCPTransportClientProperties clientProps = new TCPTransportClientProperties(
                    addr, Impulse.getEventExecutorService(), Impulse.getReactorThread(), new DefaultSocketChannelFactory(), 
                    ByteBuffer.allocate(getIntegerProperty(WRITE_BUFFER_SIZE_PROPERTY)), 
                    getIntegerProperty(WRITE_TIMEOUT_IN_SECS_PROPERTY), TimeUnit.SECONDS);
            _client = new TCPTransportClient(clientProps);
            _client.setLoggingContext(this);
            _client.addEventListener(this);
            _client.connectAsync(getIntegerProperty(CONNECT_TIMEOUT_IN_SECS_PROPERTY), TimeUnit.SECONDS);        
        } else {
            _client.addEventListener(this);
            if (getBooleanProperty(AUTO_START_READ_PROPERTY)) {
                startReading();
            }
        }
    }

    /**
     * [input] Start reading from the socket.
     * 
     * @throws TransportException
     */
    @Input("startReading")
    public void startReading() throws TransportException {
        if (_logger.isInfoEnabled()) {
             _logger.info("startReading");
        }
        _client.startReading(_readListener, ByteBuffer.allocate(getIntegerProperty(READ_BUFFER_SIZE_PROPERTY)));
    }
    
    /**
     * [input] Stops reading from the socket.
     * 
     * @throws TransportException
     */
    @Input("stopReading")
    public void stopReading() throws TransportException {
        if (_logger.isInfoEnabled()) {
            _logger.info("stopReading");
       }
        _client.stopReading();
    }
    
    /**
     * [input] Closes the sockets.
     */
    @Input("close")
    public void close() {
        if (_logger.isInfoEnabled()) {
            _logger.info("onClose");
        }
        if (_client != null) {
            _client.close();
        }
    }

    /**
     * [input] Called on a new client connection.
     * 
     * @param argMap
     */
    @Input("onNewConnection")
    static public void onNewConnection(Connection c, TCPTransportClient client) {
        TransformCreationContext createCtx = new TransformCreationContext();
        try {
            createCtx.getInstance(getTransformContext(c.getConnectionDefinition().getInputTransformName()), client);
        } catch (Exception e) {
            Log log = LogFactory.getLog(Socket.class.getName());
            if (log.isErrorEnabled()) {
                log.error("onNewConnection", e);
            }
        }
    }
    
    /**
     * [input] Writes data to the socket.
     * 
     * @param argMap
     */
    @Input("write")
    public void write(ByteBuffer buffer) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("write: \r\n" + Impulse.GlobalByteArrayFormatter.format(buffer));
        }
        try {
            _client.writeAsync(buffer, _sendResult);
        } catch (TransportException e) {
            _client.close(TransportOperation.Sending, e);
        }
    }

    /**
     * [output] Called on reading data from the client side.
     * 
     * @param readBuffer
     */
    @Output("onRead")
    public void onRead(ByteBuffer readBuffer) {
        // happens on the reactor thread
        ByteBuffer cloneBuf = readBuffer;
        if (_cloneReadBuffer) {
            cloneBuf = ByteBuffer.allocate(readBuffer.limit());
            cloneBuf.put(readBuffer);
            cloneBuf.flip();
        }
        if (_logger.isDebugEnabled()) {
            _logger.debug("onRead: \r\n" + Impulse.GlobalByteArrayFormatter.format(cloneBuf));
        }
        next("onRead", null, inputBytes[0], cloneBuf);
        readBuffer.clear();
    }

    /**
     * [output] Called on the socket being connected.
     */
    @Output("onConnected")
    public void onConnected() {
        if (_logger.isInfoEnabled()) {
            _logger.info("onConnected");
        }
        next("onConnected", null);
    }
    
    /**
     * [output] Called on the socket being closed.
     */
    @Output("onClose")
    public void onClose() {
        if (_logger.isInfoEnabled()) {
            _logger.info("onClose");
        }
        next("onClose", null);
    }
        
    @Override
    public String toString() {
        StringBuilder bldr = new StringBuilder();
        bldr.append("client= ");
        bldr.append(_client);
        return bldr.toString();
    }

    @Override
    public void onTransportEvent(TransportEvent event) {
        if (event instanceof TransportStateChangeEvent) {
            TransportStateChangeEvent stateChangeEvt = (TransportStateChangeEvent)event;
            if ((stateChangeEvt.getOldState().equals(TransportState.Connecting)) &&
                (stateChangeEvt.getNewState().equals(TransportState.Connected))) {
                _localAddress = _client.getLocalAddress();
                _remoteAddress = _client.getRemoteAddress();
                try {
                    if (getBooleanProperty(AUTO_START_READ_PROPERTY)) {
                        startReading();
                    }
                    onConnected();
                } catch (TransportException e) {
                    if (_logger.isErrorEnabled()) {
                        _logger.error("Error starting to read", e);
                    }
                    close();
                }
            } else if (stateChangeEvt.getNewState().equals(TransportState.Disconnected)) {
                if (_clientSide) {
                    onClose();
                    dispose();
                } else {
                    if (stateChangeEvt.getOldState().equals(TransportState.Connecting)) {
                        if (_logger.isErrorEnabled()) {
                            _logger.error("Error connecting to server: " + stateChangeEvt);
                        }
                    }
                    onClose();
                    dispose();
                }
            }
        }
        if (_logger.isInfoEnabled()) {
            _logger.info(event);
        }
    }

    @Override
    public Object invoke(String actionName, Object[] params, String[] signature)
            throws MBeanException, ReflectionException {
        try {
            if (actionName.equals(START_READING_OPERATION)) {
                startReading();
            } else if (actionName.equals(STOP_READING_OPERATION)) {
                stopReading();
            } else if (actionName.equals(CLOSE_OPERATION)) {
                close();
            }
        } catch(TransportException e) {
            throw new MBeanException(null, e.getMessage());
        }
        return null;
    }

    @Override
    protected MBeanOperationInfo[] createOperInfo() {
        MBeanOperationInfo[] opers = {
                new MBeanOperationInfo(START_READING_OPERATION, "starts reading from the socket", null, null, MBeanOperationInfo.ACTION),
                new MBeanOperationInfo(STOP_READING_OPERATION, "stops reading from the socket", null, null, MBeanOperationInfo.ACTION),
                new MBeanOperationInfo(CLOSE_OPERATION, "closes the socket and disconnects the parties", null, null, MBeanOperationInfo.ACTION),
        };
        return opers;
    }

    @Override
    public InetSocketAddress getClientLocalAddress() {
        if (_clientSide) {
            return _localAddress;
        }
        return null;
    }

    @Override
    public InetSocketAddress getClientRemoteAddress() {
        if (_clientSide) {
            return _remoteAddress;
        }
        return null;
    }

    @Override
    public InetSocketAddress getServerLocalAddress() {
        if (!_clientSide) {
            return _localAddress;
        }
        return null;
    }

    @Override
    public InetSocketAddress getServerRemoteAddress() {
        if (!_clientSide) {
            return _remoteAddress;
        }
        return null;
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
        
        Property<?> p;
        p = new Property<String>(IP_ADDRESS_PROPERTY, "",
                new PropertySource<String>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<String>(ctx.getPropertyString(IP_PORT_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID, 
                        new PropertyConverter() {
                            @Override
                            public Object convert(Object fromValue) throws PropertyConversionException {
                                String[] args = ((String)fromValue).split(":");
                                return args[0];
                            }
                        }),
                new MapPropertySource<String>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(IP_PORT_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID,
                        new PropertyConverter() {
                            @Override
                            public Object convert(Object fromValue) throws PropertyConversionException {
                                String[] args = ((String)fromValue).split(":");
                                return args[0];
                            }
                        }),
                new SystemPropertySource<String>(SystemPropertySource.PROPERTY_NAME + " (wildcard)", ctx.getWildcardPropertyString(IP_PORT_PROPERTY), PropertySource.PRIORITY_4, PropertySource.NULL_INVALID, 
                        new PropertyConverter() {
                            @Override
                            public Object convert(Object fromValue) throws PropertyConversionException {
                                String[] args = ((String)fromValue).split(":");
                                return args[0];
                            }
                        }),
                new MapPropertySource<String>(Impulse.CONFIG_FILE_SOURCE + " (wildcard)", ctx.getWildcardPropertyString(IP_PORT_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_5, PropertySource.NULL_INVALID,
                        new PropertyConverter() {
                            @Override
                            public Object convert(Object fromValue) throws PropertyConversionException {
                                String[] args = ((String)fromValue).split(":");
                                return args[0];
                            }
                        }));

        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "IP address to connect to; derived from property " + ctx.getPropertyString(IP_PORT_PROPERTY));
        props.put(p.getId(), p);

        p = new Property<Integer>(PORT_PROPERTY, 0,
                new PropertySource<Integer>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<Integer>(ctx.getPropertyString(IP_PORT_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID, 
                        new PropertyConverter() {
                            @Override
                            public Object convert(Object fromValue) throws PropertyConversionException {
                                String[] args = ((String)fromValue).split(":");
                                if (args.length != 2) {
                                    throw new PropertyConversionException(String.format("Illegal format for property %s value %s, expecting server:port format",
                                            PORT_PROPERTY, fromValue));
                                }
                                return Integer.valueOf(args[1]);
                            }
                        }),
                new MapPropertySource<Integer>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(IP_PORT_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID, 
                        new PropertyConverter() {
                            @Override
                            public Object convert(Object fromValue) throws PropertyConversionException {
                                String[] args = ((String)fromValue).split(":");
                                if (args.length != 2) {
                                    throw new PropertyConversionException(String.format("Illegal format for property %s value %s, expecting server:port format",
                                            PORT_PROPERTY, fromValue));
                                }
                                return Integer.valueOf(args[1]);
                            }
                        }),
                new SystemPropertySource<Integer>(SystemPropertySource.PROPERTY_NAME + " (wildcard)", ctx.getWildcardPropertyString(IP_PORT_PROPERTY), PropertySource.PRIORITY_4, PropertySource.NULL_INVALID, 
                        new PropertyConverter() {
                            @Override
                            public Object convert(Object fromValue) throws PropertyConversionException {
                                String[] args = ((String)fromValue).split(":");
                                if (args.length != 2) {
                                    throw new PropertyConversionException(String.format("Illegal format for property %s value %s, expecting server:port format",
                                            PORT_PROPERTY, fromValue));
                                }
                                return Integer.valueOf(args[1]);
                            }
                        }),
                new MapPropertySource<Integer>(Impulse.CONFIG_FILE_SOURCE + " (wildcard)", ctx.getWildcardPropertyString(IP_PORT_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_5, PropertySource.NULL_INVALID, 
                        new PropertyConverter() {
                            @Override
                            public Object convert(Object fromValue) throws PropertyConversionException {
                                String[] args = ((String)fromValue).split(":");
                                if (args.length != 2) {
                                    throw new PropertyConversionException(String.format("Illegal format for property %s value %s, expecting server:port format",
                                            PORT_PROPERTY, fromValue));
                                }
                                return Integer.valueOf(args[1]);
                            }
                        }));

        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "Port to connect to; derived from property " + ctx.getPropertyString(IP_PORT_PROPERTY));
        props.put(p.getId(), p);

        p = new Property<Integer>(WRITE_TIMEOUT_IN_SECS_PROPERTY, WRITE_TIMEOUT_IN_SECS_PROPERTY_DEFAULT, true,
                new PropertySource<Integer>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<Integer>(ctx.getPropertyString(WRITE_TIMEOUT_IN_SECS_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Integer.class)),
                new MapPropertySource<Integer>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(WRITE_TIMEOUT_IN_SECS_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Integer.class)));
        ((Property<Integer>)p).addVetoableListener(OneToMax);
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "Server side write timeout");
        props.put(p.getId(), p);
        
        p = new Property<Integer>(CONNECT_TIMEOUT_IN_SECS_PROPERTY, CONNECT_TIMEOUT_IN_SECS_PROPERTY_DEFAULT, true,
                new PropertySource<Integer>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<Integer>(ctx.getPropertyString(CONNECT_TIMEOUT_IN_SECS_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Integer.class)),
                new MapPropertySource<Integer>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(CONNECT_TIMEOUT_IN_SECS_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Integer.class)));
        ((Property<Integer>)p).addVetoableListener(ZeroToMax);
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "Server side connect timeout");
        props.put(p.getId(), p);

        p = new Property<Integer>(WRITE_BUFFER_SIZE_PROPERTY, WRITE_BUFFER_SIZE_PROPERTY_DEFAULT, true,
                new PropertySource<Integer>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<Integer>(ctx.getPropertyString(WRITE_BUFFER_SIZE_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Integer.class)),
                new MapPropertySource<Integer>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(WRITE_BUFFER_SIZE_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Integer.class)));
        ((Property<Integer>)p).addVetoableListener(OneToMax);
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "Server side write buffer size");
        props.put(p.getId(), p);

        p = new Property<Integer>(READ_BUFFER_SIZE_PROPERTY, READ_BUFFER_SIZE_PROPERTY_DEFAULT, true,
                new PropertySource<Integer>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<Integer>(ctx.getPropertyString(READ_BUFFER_SIZE_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Integer.class)),
                new MapPropertySource<Integer>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(READ_BUFFER_SIZE_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Integer.class)));
        ((Property<Integer>)p).addVetoableListener(OneToMax);
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "Server side read buffer size");
        props.put(p.getId(), p);

        p = new Property<Boolean>(CLONE_READ_BUFFER_PROPERTY, CLONE_READ_BUFFER_PROPERTY_DEFAULT, true,
                new PropertySource<Boolean>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<Boolean>(ctx.getPropertyString(CLONE_READ_BUFFER_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Boolean.class)),
                new MapPropertySource<Boolean>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(CLONE_READ_BUFFER_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Boolean.class)));
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "Server side property to clone read buffers");
        props.put(p.getId(), p); 
        
        p = new Property<Boolean>(AUTO_START_READ_PROPERTY, AUTO_START_READ_PROPERTY_DEFAULT, true,
                new PropertySource<Boolean>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<Boolean>(ctx.getPropertyString(AUTO_START_READ_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Boolean.class)),
                new MapPropertySource<Boolean>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(AUTO_START_READ_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Boolean.class)));
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "Client side auto start reading on new connections");
        props.put(p.getId(), p);
    }

}
