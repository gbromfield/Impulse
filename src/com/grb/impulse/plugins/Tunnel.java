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
 * creates a connection to another server. This can be used as both ends 
 * of a tunnel with transforms modifying the data passed between them.
 * <p>
 * Terminology:
 * <li> Client Side - the side of the tunnel that an application has initiated a connection to.
 * <li> Server Side - the side of the tunnel that the tunnel initiates connection with. 
 */
public class Tunnel extends BaseTransform implements TransportEventListener, Transport {
    /**
     * This variable is a place holder for a transform argument.
     * Argument syntax must follow <transform simple class name>.<type simple class name>.<field name>
     * This transforms outputs data into the argMap using this as a key.
     */
    @Argument
    final public static Object[] inputBytes = {"Tunnel.ByteBuffer.inputBytes", ByteBuffer.class};

    /**
     * Server side write timeout.
     */
    public static final String SERVER_WRITE_TIMEOUT_IN_SECS_PROPERTY = "serverWriteTimeoutInSecs";
    
    /**
     * Default server side write timeout.
     */
    public static final int SERVER_WRITE_TIMEOUT_IN_SECS_PROPERTY_DEFAULT = 30;

    /**
     * Server side connect timeout. 
     */
    public static final String SERVER_CONNECT_TIMEOUT_IN_SECS_PROPERTY = "serverConnectTimeoutInSecs";

    /**
     * Default server side connect timeout. 
     */
    public static final int SERVER_CONNECT_TIMEOUT_IN_SECS_PROPERTY_DEFAULT = 30;

    /**
     * Server side write buffer size.
     */
    public static final String SERVER_WRITE_BUFFER_SIZE_PROPERTY = "serverWriteBufferSize";

    /**
     * Default server side write buffer size.
     */
    public static final int SERVER_WRITE_BUFFER_SIZE_PROPERTY_DEFAULT = 100000;

    /**
     * Server side read buffer size.
     */
    public static final String SERVER_READ_BUFFER_SIZE_PROPERTY = "serverReadBufferSize";

    /**
     * Default server side read buffer size.
     */
    public static final int SERVER_READ_BUFFER_SIZE_PROPERTY_DEFAULT = 100000;

    /**
     * Server side property to clone read buffers.
     * If you want to manipulate the buffer or save it for future use
     * you must clone the buffer because the one that is passed to 
     * connections will be cleared when the connection return so that
     * more data can be read. 
     */
    public static final String SERVER_CLONE_READ_BUFFER_PROPERTY = "serverCloneReadBuffer";
    
    /**
     * Default server side property to clone read buffers.
     */
    public static final boolean SERVER_CLONE_READ_BUFFER_PROPERTY_DEFAULT = false;

    /**
     * IP address and port to connect to in order to complete the tunnel.
     * Specifiable in the configuration file or command line. 
     */
    public static final String SERVER_IP_PORT_PROPERTY = "serverIPPort";

    /**
     * IP address to connect to in order to complete the tunnel.
     * Specifiable through JMX only.
     * Used in conjuction with {@link #SERVER_PORT_PROPERTY}
     * This overrrides the property {@link #SERVER_IP_PORT_PROPERTY} 
     */
    public static final String SERVER_IP_ADDRESS_PROPERTY = "ipAddress";

    /**
     * Port to connect to in order to complete the tunnel.
     * Specifiable through JMX only.
     * Used in conjuction with {@link #SERVER_IP_ADDRESS_PROPERTY}
     * This overrrides the property {@link #SERVER_IP_PORT_PROPERTY} 
     */
    public static final String SERVER_PORT_PROPERTY = "port";
	
    /**
     * Client side read buffer size.
     */
    public static final String CLIENT_READ_BUFFER_SIZE_PROPERTY = "clientReadBufferSize";

    /**
     * Default client side read buffer size.
     */
    public static final int CLIENT_READ_BUFFER_SIZE_PROPERTY_DEFAULT = 100000;

    /**
     * Client side property to clone read buffers.
     * If you want to manipulate the buffer or save it for future use
     * you must clone the buffer because the one that is passed to 
     * connections will be cleared when the connection return so that
     * more data can be read. 
     */
    public static final String CLIENT_CLONE_READ_BUFFER_PROPERTY = "clientCloneReadBuffer";

    /**
     * Default client side property to clone read buffers.
     */
    public static final boolean CLIENT_CLONE_READ_BUFFER_PROPERTY_DEFAULT = false;

    /**
     * Client side auto start reading on new connections.
     */
    public static final String CLIENT_AUTO_START_READ_PROPERTY = "clientAutoStartRead";

    /**
     * Default client side auto start reading on new connections.
     */
    public static final boolean CLIENT_AUTO_START_READ_PROPERTY_DEFAULT = true;
		
    /**
     * Starts reading from the client side socket.
     */
    public static final String START_CLIENT_READING_OPERATION = "startClientReading";	

    /**
     * Stops reading from the cleint side socket.
     */
    public static final String STOP_CLIENT_READING_OPERATION = "stopClientReading";

    /**
     * Starts reading from the server side socket.
     */
    public static final String START_SERVER_READING_OPERATION = "startServerReading";	

    /**
     * Stops reading from the server side socket.
     */
    public static final String STOP_SERVER_READING_OPERATION = "stopServerReading";

    /**
     * Closes the tunnel and disconnects the parties.
     */
    public static final String CLOSE_OPERATION = "close";

	/**
	 * Client side properties
	 */
	private TCPTransportClient _clientSideClient;
	private TransportReadListener _clientReadListener;
	private boolean _clientCloneReadBuffer;
	private TransportSendResult _clientSendResult;
	private InetSocketAddress _clientLocalAddress;
    private InetSocketAddress _clientRemoteAddress;
	
	/**
	 * Server side properties
	 */
	private TCPTransportClient _serverSideClient;
	private TransportReadListener _serverReadListener;
	private boolean _serverCloneReadBuffer;
    private TransportSendResult _serverSendResult;
    private InetSocketAddress _serverLocalAddress;
    private InetSocketAddress _serverRemoteAddress;
	
	public Tunnel(String transformName, String instanceName,
			TransformCreationContext transformCreationContext, Object... args) {
		super(transformName, instanceName, transformCreationContext);
		if ((args == null) || (args.length != 1) || (!(args[0] instanceof TCPTransportClient))) {
		    throw new IllegalArgumentException(String.format("Null or invalid argument provided to constructor of transform \"%s\", instance \"%s\"", transformName, instanceName));
		}
		_clientSideClient = (TCPTransportClient)args[0];
		_clientSideClient.setLoggingContext(this);
		_clientSendResult = new TransportSendResult();
		_clientLocalAddress = _clientSideClient.getLocalAddress();
		_clientRemoteAddress = _clientSideClient.getRemoteAddress();
		_serverSendResult = new TransportSendResult();

		_clientReadListener = new TransportReadListener() {
			@Override
			public boolean onTransportRead(ByteBuffer readBuffer, int numBytesRead) {
				if (numBytesRead <= 0) {
					_clientSideClient.close();
					return false;
				} else {
					onClientRead(readBuffer);
					return true;
				}
			}
		};
		_serverReadListener = new TransportReadListener() {
			@Override
			public boolean onTransportRead(ByteBuffer readBuffer, int numBytesRead) {
				if (numBytesRead <= 0) {
					_serverSideClient.close();
					return false;
				} else {
					onServerRead(readBuffer);
					return true;
				}
			}
		};
		_transport = this;
	}

	@Override
	public void init() throws Exception {
		super.init();
		_clientCloneReadBuffer = getBooleanProperty(CLIENT_CLONE_READ_BUFFER_PROPERTY);
		_serverCloneReadBuffer = getBooleanProperty(SERVER_CLONE_READ_BUFFER_PROPERTY);
						
        _clientSideClient.addEventListener(this);

        InetSocketAddress addr = new InetSocketAddress(InetAddress.getByName(getStringProperty(SERVER_IP_ADDRESS_PROPERTY)), 
        		getIntegerProperty(SERVER_PORT_PROPERTY));
        TCPTransportClientProperties clientProps = new TCPTransportClientProperties(
                addr, Impulse.getEventExecutorService(), Impulse.getReactorThread(), new DefaultSocketChannelFactory(), 
                ByteBuffer.allocate(getIntegerProperty(SERVER_WRITE_BUFFER_SIZE_PROPERTY)), 
                getIntegerProperty(SERVER_WRITE_TIMEOUT_IN_SECS_PROPERTY), TimeUnit.SECONDS);
        _serverSideClient = new TCPTransportClient(clientProps);
        _serverSideClient.setLoggingContext(this);
        _serverSideClient.addEventListener(this);
        _serverSideClient.connectAsync(getIntegerProperty(SERVER_CONNECT_TIMEOUT_IN_SECS_PROPERTY), TimeUnit.SECONDS);        
	}

	@Output("start")
	@Override
    public void start() throws Exception {
        super.start();
        next("start", null);
    }

    /**
	 * [input] Start the client side reading from the socket.
	 * 
	 * @throws TransportException
	 */
	@Input("startClientReading")
	public void startClientReading() throws TransportException {
	    if (_logger.isInfoEnabled()) {
	         _logger.info("startClientReading");
	    }
	    _clientSideClient.startReading(_clientReadListener, ByteBuffer.allocate(getIntegerProperty(CLIENT_READ_BUFFER_SIZE_PROPERTY)));
	}
	
	/**
	 * [input] Stops the client side reading from the socket.
	 * 
	 * @throws TransportException
	 */
    @Input("stopClientReading")
	public void stopClientReading() throws TransportException {
        if (_logger.isInfoEnabled()) {
            _logger.info("stopClientReading");
       }
		_clientSideClient.stopReading();
	}

    /**
     * [input] Starts the server side reading from the socket.
     * 
     * @throws TransportException
     */
    @Input("startServerReading")
	public void startServerReading() throws TransportException {
        if (_logger.isInfoEnabled()) {
            _logger.info("startServerReading");
       }
        _serverSideClient.startReading(_serverReadListener, ByteBuffer.allocate(getIntegerProperty(SERVER_READ_BUFFER_SIZE_PROPERTY)));
	}
	
    /**
     * [input] Stops the server side reading from the socket.
     * 
     * @throws TransportException
     */
    @Input("stopServerReading")
	public void stopServerReading() throws TransportException {
        if (_logger.isInfoEnabled()) {
            _logger.info("stopServerReading");
       }
		_serverSideClient.stopReading();
	}

    /**
     * [input] Closes the client and server side sockets.
     */
	@Input("onClose")
    public void close() {
        if (_logger.isInfoEnabled()) {
            _logger.info("onClose");
        }
        if (_serverSideClient != null) {
            _serverSideClient.close();
        }
        if (_clientSideClient != null) {
            _clientSideClient.close();
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
            Log log = LogFactory.getLog(Tunnel.class.getName());
			if (log.isErrorEnabled()) {
				log.error("onNewConnection", e);
			}
        }
    }
	
	/**
	 * [input] Writes data to the client side socket.
	 * 
	 * @param argMap
	 */
	@Input("writeToClient")
	public void writeToClient(ByteBuffer buffer) {
		if (_logger.isDebugEnabled()) {
			_logger.debug("writeToClient: \r\n" + Impulse.GlobalByteArrayFormatter.format(buffer));
		}
		try {
			_clientSideClient.writeAsync(buffer, _clientSendResult);
		} catch (TransportException e) {
		    _clientSideClient.close(TransportOperation.Sending, e);
		}
	}

	/**
	 * [output] Called on reading data from the client side.
	 * 
	 * @param readBuffer
	 */
	@Output("onClientRead")
	public void onClientRead(ByteBuffer readBuffer) {
//	    byte[] data = new byte[readBuffer.limit()];
//	    System.arraycopy(readBuffer.array(), 0, data, 0, readBuffer.limit());
		// happens on the reactor thread
		ByteBuffer cloneBuf = readBuffer;
		if (_clientCloneReadBuffer) {
			cloneBuf = ByteBuffer.allocate(readBuffer.limit());
			cloneBuf.put(readBuffer);
			cloneBuf.flip();
		}
		if (_logger.isDebugEnabled()) {
			_logger.debug("onClientRead: \r\n" + Impulse.GlobalByteArrayFormatter.format(cloneBuf));
		}
		next("onClientRead", null, inputBytes[0], cloneBuf);
		readBuffer.clear();
	}

	/**
	 * [output] Called on the client side socket being closed.
	 */
	@Output("onClientClose")
	public void onClientClose() {
        if (_logger.isInfoEnabled()) {
            _logger.info("onClientClose");
        }
	    next("onClientClose", null);
	}
	
	/**
	 * [input] Writes data to the server side socket.
	 * 
	 * @param argMap
	 */
	@Input("writeToServer")
	public void writeToServer(ByteBuffer buffer) {
		if (_logger.isDebugEnabled()) {
			_logger.debug("writeToServer: \r\n" + Impulse.GlobalByteArrayFormatter.format(buffer));
		}
		try {
			_serverSideClient.writeAsync(buffer, _serverSendResult);
		} catch (TransportException e) {
		    _serverSideClient.close(TransportOperation.Sending, e);
		}
	}

	/**
	 * [output] Called on reading data from the server side.
	 * 
	 * @param readBuffer
	 */
	@Output("onServerRead")
	public void onServerRead(ByteBuffer readBuffer) {
		// happens on the reactor thread
		ByteBuffer cloneBuf = readBuffer;
		if (_serverCloneReadBuffer) {
			cloneBuf = ByteBuffer.allocate(readBuffer.limit());
			cloneBuf.put(readBuffer);
			cloneBuf.flip();
		}
		if (_logger.isDebugEnabled()) {
			_logger.debug("onServerRead: \r\n" + Impulse.GlobalByteArrayFormatter.format(cloneBuf));
		}
		next("onServerRead", null, inputBytes[0], cloneBuf);
		readBuffer.clear();
	}

	/**
	 * [output] Called on the server side socket being closed.
	 */
	@Output("onServerClose")
	public void onServerClose() {
        if (_logger.isInfoEnabled()) {
            _logger.info("onServerClose");
        }
	    next("onServerClose", null);
	}
	
	@Override
	public String toString() {
	    StringBuilder bldr = new StringBuilder();
	    bldr.append("client= ");
	    bldr.append(_clientSideClient);
	    bldr.append(", server= ");
	    bldr.append(_serverSideClient);
		return bldr.toString();
	}

    @Override
    public void onTransportEvent(TransportEvent event) {
        if (event instanceof TransportStateChangeEvent) {
            TransportStateChangeEvent stateChangeEvt = (TransportStateChangeEvent)event;
            if ((stateChangeEvt.getOldState().equals(TransportState.Connecting)) &&
                (stateChangeEvt.getNewState().equals(TransportState.Connected)) &&
                (stateChangeEvt.getTransportClient() == _serverSideClient)) {
                _serverLocalAddress = _serverSideClient.getLocalAddress();
                _serverRemoteAddress = _serverSideClient.getRemoteAddress();
                try {
                    _serverSideClient.startReading(_serverReadListener, ByteBuffer.allocate(getIntegerProperty(SERVER_READ_BUFFER_SIZE_PROPERTY)));
                    if (getBooleanProperty(CLIENT_AUTO_START_READ_PROPERTY)) {
                    	startClientReading();
                    }
                } catch (TransportException e) {
                	if (_logger.isErrorEnabled()) {
                		_logger.error("Error starting to read", e);
                	}
                	close();
                }
            } else if (stateChangeEvt.getNewState().equals(TransportState.Disconnected)) {
            	if (stateChangeEvt.getTransportClient() == _clientSideClient) {
            		onClientClose();
            		if (_serverSideClient.isDisconnected()) {
            			dispose();
            		} else {
            		    close();
            		}
            	} else if (stateChangeEvt.getTransportClient() == _serverSideClient) {
            	    if (stateChangeEvt.getOldState().equals(TransportState.Connecting)) {
            	        if (_logger.isErrorEnabled()) {
            	            _logger.error("Error connecting to server: " + stateChangeEvt);
            	        }
            	    }
            		onServerClose();
            		if (_clientSideClient.isDisconnected()) {
            			dispose();
            		} else {
            		    close();
            		}
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
            if (actionName.equals(START_CLIENT_READING_OPERATION)) {
                startClientReading();
            } else if (actionName.equals(STOP_CLIENT_READING_OPERATION)) {
                stopClientReading();
            } else if (actionName.equals(START_SERVER_READING_OPERATION)) {
                startServerReading();
            } else if (actionName.equals(STOP_SERVER_READING_OPERATION)) {
                stopServerReading();
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
                new MBeanOperationInfo(START_CLIENT_READING_OPERATION, "starts reading from the client socket", null, null, MBeanOperationInfo.ACTION),
                new MBeanOperationInfo(STOP_CLIENT_READING_OPERATION, "stops reading from the client socket", null, null, MBeanOperationInfo.ACTION),
                new MBeanOperationInfo(START_SERVER_READING_OPERATION, "starts reading from the server socket", null, null, MBeanOperationInfo.ACTION),
                new MBeanOperationInfo(STOP_SERVER_READING_OPERATION, "stops reading from the server socket", null, null, MBeanOperationInfo.ACTION),
                new MBeanOperationInfo(CLOSE_OPERATION, "closes the tunnel and disconnects the parties", null, null, MBeanOperationInfo.ACTION),
        };
        return opers;
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
        p = new Property<String>(SERVER_IP_ADDRESS_PROPERTY,
                new PropertySource<String>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<String>(ctx.getPropertyString(SERVER_IP_PORT_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID, 
                        new PropertyConverter() {
                            @Override
                            public Object convert(Object fromValue) throws PropertyConversionException {
                                String[] args = ((String)fromValue).split(":");
                                return args[0];
                            }
                        }),
                new MapPropertySource<String>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(SERVER_IP_PORT_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID,
                        new PropertyConverter() {
                            @Override
                            public Object convert(Object fromValue) throws PropertyConversionException {
                                String[] args = ((String)fromValue).split(":");
                                return args[0];
                            }
                        }),
                new SystemPropertySource<String>(SystemPropertySource.PROPERTY_NAME + " (wildcard)", ctx.getWildcardPropertyString(SERVER_IP_PORT_PROPERTY), PropertySource.PRIORITY_4, PropertySource.NULL_INVALID, 
                        new PropertyConverter() {
                            @Override
                            public Object convert(Object fromValue) throws PropertyConversionException {
                                String[] args = ((String)fromValue).split(":");
                                return args[0];
                            }
                        }),
                new MapPropertySource<String>(Impulse.CONFIG_FILE_SOURCE + " (wildcard)", ctx.getWildcardPropertyString(SERVER_IP_PORT_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_5, PropertySource.NULL_INVALID,
                        new PropertyConverter() {
                            @Override
                            public Object convert(Object fromValue) throws PropertyConversionException {
                                String[] args = ((String)fromValue).split(":");
                                return args[0];
                            }
                        }));

        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "IP address to connect to in order to complete the tunnel; derived from property " + ctx.getPropertyString(SERVER_IP_PORT_PROPERTY));
        props.put(p.getId(), p);

        p = new Property<Integer>(SERVER_PORT_PROPERTY,
                new PropertySource<Integer>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<Integer>(ctx.getPropertyString(SERVER_IP_PORT_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID, 
                        new PropertyConverter() {
                            @Override
                            public Object convert(Object fromValue) throws PropertyConversionException {
                                String[] args = ((String)fromValue).split(":");
                                if (args.length != 2) {
                                    throw new PropertyConversionException(String.format("Illegal format for property %s value %s, expecting server:port format",
                                            SERVER_PORT_PROPERTY, fromValue));
                                }
                                return Integer.valueOf(args[1]);
                            }
                        }),
                new MapPropertySource<Integer>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(SERVER_IP_PORT_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID, 
                        new PropertyConverter() {
                            @Override
                            public Object convert(Object fromValue) throws PropertyConversionException {
                                String[] args = ((String)fromValue).split(":");
                                if (args.length != 2) {
                                    throw new PropertyConversionException(String.format("Illegal format for property %s value %s, expecting server:port format",
                                            SERVER_PORT_PROPERTY, fromValue));
                                }
                                return Integer.valueOf(args[1]);
                            }
                        }),
                new SystemPropertySource<Integer>(SystemPropertySource.PROPERTY_NAME + " (wildcard)", ctx.getWildcardPropertyString(SERVER_IP_PORT_PROPERTY), PropertySource.PRIORITY_4, PropertySource.NULL_INVALID, 
                        new PropertyConverter() {
                            @Override
                            public Object convert(Object fromValue) throws PropertyConversionException {
                                String[] args = ((String)fromValue).split(":");
                                if (args.length != 2) {
                                    throw new PropertyConversionException(String.format("Illegal format for property %s value %s, expecting server:port format",
                                            SERVER_PORT_PROPERTY, fromValue));
                                }
                                return Integer.valueOf(args[1]);
                            }
                        }),
                new MapPropertySource<Integer>(Impulse.CONFIG_FILE_SOURCE + " (wildcard)", ctx.getWildcardPropertyString(SERVER_IP_PORT_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_5, PropertySource.NULL_INVALID, 
                        new PropertyConverter() {
                            @Override
                            public Object convert(Object fromValue) throws PropertyConversionException {
                                String[] args = ((String)fromValue).split(":");
                                if (args.length != 2) {
                                    throw new PropertyConversionException(String.format("Illegal format for property %s value %s, expecting server:port format",
                                            SERVER_PORT_PROPERTY, fromValue));
                                }
                                return Integer.valueOf(args[1]);
                            }
                        }));

        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "Port to connect to in order to complete the tunnel; derived from property " + ctx.getPropertyString(SERVER_IP_PORT_PROPERTY));
        props.put(p.getId(), p);

        p = new Property<Integer>(SERVER_WRITE_TIMEOUT_IN_SECS_PROPERTY, SERVER_WRITE_TIMEOUT_IN_SECS_PROPERTY_DEFAULT, true,
                new PropertySource<Integer>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<Integer>(ctx.getPropertyString(SERVER_WRITE_TIMEOUT_IN_SECS_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Integer.class)),
                new MapPropertySource<Integer>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(SERVER_WRITE_TIMEOUT_IN_SECS_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Integer.class)));
        ((Property<Integer>)p).addVetoableListener(OneToMax);
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "Server side write timeout");
        props.put(p.getId(), p);
        
        p = new Property<Integer>(SERVER_CONNECT_TIMEOUT_IN_SECS_PROPERTY, SERVER_CONNECT_TIMEOUT_IN_SECS_PROPERTY_DEFAULT, true,
                new PropertySource<Integer>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<Integer>(ctx.getPropertyString(SERVER_CONNECT_TIMEOUT_IN_SECS_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Integer.class)),
                new MapPropertySource<Integer>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(SERVER_CONNECT_TIMEOUT_IN_SECS_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Integer.class)));
        ((Property<Integer>)p).addVetoableListener(ZeroToMax);
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "Server side connect timeout");
        props.put(p.getId(), p);

        p = new Property<Integer>(SERVER_WRITE_BUFFER_SIZE_PROPERTY, SERVER_WRITE_BUFFER_SIZE_PROPERTY_DEFAULT, true,
                new PropertySource<Integer>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<Integer>(ctx.getPropertyString(SERVER_WRITE_BUFFER_SIZE_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Integer.class)),
                new MapPropertySource<Integer>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(SERVER_WRITE_BUFFER_SIZE_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Integer.class)));
        ((Property<Integer>)p).addVetoableListener(OneToMax);
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "Server side write buffer size");
        props.put(p.getId(), p);

        p = new Property<Integer>(SERVER_READ_BUFFER_SIZE_PROPERTY, SERVER_READ_BUFFER_SIZE_PROPERTY_DEFAULT, true,
                new PropertySource<Integer>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<Integer>(ctx.getPropertyString(SERVER_READ_BUFFER_SIZE_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Integer.class)),
                new MapPropertySource<Integer>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(SERVER_READ_BUFFER_SIZE_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Integer.class)));
        ((Property<Integer>)p).addVetoableListener(OneToMax);
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "Server side read buffer size");
        props.put(p.getId(), p);

        p = new Property<Boolean>(SERVER_CLONE_READ_BUFFER_PROPERTY, SERVER_CLONE_READ_BUFFER_PROPERTY_DEFAULT, true,
                new PropertySource<Boolean>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<Boolean>(ctx.getPropertyString(SERVER_CLONE_READ_BUFFER_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Boolean.class)),
                new MapPropertySource<Boolean>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(SERVER_CLONE_READ_BUFFER_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Boolean.class)));
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "Server side property to clone read buffers");
        props.put(p.getId(), p);
        
        p = new Property<Integer>(CLIENT_READ_BUFFER_SIZE_PROPERTY, CLIENT_READ_BUFFER_SIZE_PROPERTY_DEFAULT, true,
                new PropertySource<Integer>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<Integer>(ctx.getPropertyString(CLIENT_READ_BUFFER_SIZE_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Integer.class)),
                new MapPropertySource<Integer>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(CLIENT_READ_BUFFER_SIZE_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Integer.class)));
        ((Property<Integer>)p).addVetoableListener(OneToMax);
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "Client side read buffer size");
        props.put(p.getId(), p);

        p = new Property<Boolean>(CLIENT_CLONE_READ_BUFFER_PROPERTY, CLIENT_CLONE_READ_BUFFER_PROPERTY_DEFAULT, true,
                new PropertySource<Boolean>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<Boolean>(ctx.getPropertyString(CLIENT_CLONE_READ_BUFFER_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Boolean.class)),
                new MapPropertySource<Boolean>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(CLIENT_CLONE_READ_BUFFER_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Boolean.class)));
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "Client side property to clone read buffers");
        props.put(p.getId(), p);

        p = new Property<Boolean>(CLIENT_AUTO_START_READ_PROPERTY, CLIENT_AUTO_START_READ_PROPERTY_DEFAULT, true,
                new PropertySource<Boolean>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<Boolean>(ctx.getPropertyString(CLIENT_AUTO_START_READ_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Boolean.class)),
                new MapPropertySource<Boolean>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(CLIENT_AUTO_START_READ_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Boolean.class)));
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "Client side auto start reading on new connections");
        props.put(p.getId(), p);        
    }

    @Override
    public InetSocketAddress getClientLocalAddress() {
        return _clientLocalAddress;
    }

    @Override
    public InetSocketAddress getClientRemoteAddress() {
        return _clientRemoteAddress;
    }

    @Override
    public InetSocketAddress getServerLocalAddress() {
        return _serverLocalAddress;
    }

    @Override
    public InetSocketAddress getServerRemoteAddress() {
        return _serverRemoteAddress;
    }
}
