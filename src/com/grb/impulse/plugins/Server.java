package com.grb.impulse.plugins;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;

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
import com.grb.transport.tcp.TCPTransportClient;
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
 * This transform is a server that receives data from a socket.
 *
 */
public class Server extends BaseTransform implements TransportEventListener, Transport {
    /**
     * This variable is a place holder for a transform argument.
     * Argument syntax must follow <transform simple class name>.<type simple class name>.<field name>
     * This transforms outputs data into the argMap using this as a key.
     */
    @Argument
    final public static Object[] inputBytes = {"Server.ByteBuffer.inputBytes", ByteBuffer.class};

    /**
     * This property indicates the read buffer size.
     */
    public static final String READ_BUFFER_SIZE_PROPERTY = "readBufferSize";

    /**
     * Default read buffer size.
     */
    public static final int READ_BUFFER_SIZE_PROPERTY_DEFAULT = 100000;

    /**
     * This property indicates whether to clone incoming buffers.
     * If you want to manipulate the buffer or save it for future use
     * you must clone the buffer because the one that is passed to 
     * connections will be cleared when the connection return so that
     * more data can be read. 
     */
    public static final String CLONE_READ_BUFFER_PROPERTY = "cloneReadBuffer";

    /**
     * Default clone read buffer value.
     */
    public static final boolean CLONE_READ_BUFFER_PROPERTY_DEFAULT = false;

    /**
     * This property indicates whether to start reading on the socket 
     * at server creation.
     */
    public static final String AUTO_START_READ_PROPERTY = "autoStartRead";

    /**
     * Default auto start reading value.
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
     * Closes the server and disconnects the client.
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

    public Server(String transformName, String instanceName,
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
		_transport = this;
    }

    @Override
    public void init() throws Exception {
        super.init();
        _clientCloneReadBuffer = getBooleanProperty(CLONE_READ_BUFFER_PROPERTY);
                        
        _clientSideClient.addEventListener(this);
    }

    @Override
    public void start() throws Exception {
        super.start();
        try {
            if (getBooleanProperty(AUTO_START_READ_PROPERTY)) {
                startReading();
            }
        } catch (TransportException e) {
            if (_logger.isErrorEnabled()) {
                _logger.error("Error starting to read", e);
            }
            close();
        }
    }

    /**
     * [operation] Start reading from the socket.
     * 
     * @throws TransportException
     */
    public void startReading() throws TransportException {
        if (_logger.isInfoEnabled()) {
            _logger.info("startReading");
        }
        _clientSideClient.startReading(_clientReadListener, ByteBuffer.allocate(getIntegerProperty(READ_BUFFER_SIZE_PROPERTY)));
    }

    /**
     * [operation] Stops reading from the socket.
     * 
     * @throws TransportException
     */
    public void stopReading() throws TransportException {
        if (_logger.isInfoEnabled()) {
            _logger.info("stopReading");
        }
        _clientSideClient.stopReading();
    }
    
    /**
     * [input] Call when closing the server.
     */
    @Input("onClose")
    public void close() {
        if (_logger.isInfoEnabled()) {
            _logger.info("onClose");
        }
        if (_clientSideClient != null) {
            _clientSideClient.close();
        }
    }

    /**
     * [input] Call when accepting a new connection.
     * 
     * @param argMap Client.
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
     * [input] Call when writing to the client.
     * 
     * @param argMap Buffer to write.
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
     * [output] Called when new data has arrived.
     * 
     * @param [output]
     */
    @Output("onClientRead")
    public void onClientRead(ByteBuffer readBuffer) {
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
     * [output] Called when the server is closed.
     */
    @Output("onClientClose")
    public void onClientClose() {
        if (_logger.isInfoEnabled()) {
            _logger.info("onClientClose");
        }
        next("onClientClose", null);
    }
        
    @Override
    public String toString() {
        StringBuilder bldr = new StringBuilder();
        bldr.append("client= ");
        bldr.append(_clientSideClient);
        return bldr.toString();
    }

    @Override
    public void onTransportEvent(TransportEvent event) {
        if (event instanceof TransportStateChangeEvent) {
            TransportStateChangeEvent stateChangeEvt = (TransportStateChangeEvent)event;
            if (stateChangeEvt.getNewState().equals(TransportState.Disconnected)) {
                    onClientClose();
                    dispose();
            }
        }
        if (_logger.isDebugEnabled()) {
            _logger.debug(event);
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
                new MBeanOperationInfo(CLOSE_OPERATION, "closes the server and disconnects the client", null, null, MBeanOperationInfo.ACTION),
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

        RangeConstraint<Integer> OneToMax = new RangeConstraint<Integer>(1, Integer.MAX_VALUE);
        
        Property<?> p;        
        p = new Property<Integer>(READ_BUFFER_SIZE_PROPERTY, READ_BUFFER_SIZE_PROPERTY_DEFAULT, true,
                new PropertySource<Integer>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<Integer>(ctx.getPropertyString(READ_BUFFER_SIZE_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Integer.class)),
                new MapPropertySource<Integer>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(READ_BUFFER_SIZE_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Integer.class)));
        ((Property<Integer>)p).addVetoableListener(OneToMax);
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "This property indicates the read buffer size");
        props.put(p.getId(), p);

        p = new Property<Boolean>(CLONE_READ_BUFFER_PROPERTY, CLONE_READ_BUFFER_PROPERTY_DEFAULT, true,
                new PropertySource<Boolean>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<Boolean>(ctx.getPropertyString(CLONE_READ_BUFFER_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Boolean.class)),
                new MapPropertySource<Boolean>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(CLONE_READ_BUFFER_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Boolean.class)));
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "This property indicates whether to clone incoming buffers");
        props.put(p.getId(), p);

        p = new Property<Boolean>(AUTO_START_READ_PROPERTY, AUTO_START_READ_PROPERTY_DEFAULT, true,
                new PropertySource<Boolean>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<Boolean>(ctx.getPropertyString(AUTO_START_READ_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Boolean.class)),
                new MapPropertySource<Boolean>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(AUTO_START_READ_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Boolean.class)));
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "This property indicates whether to start reading on the socket at server creation");
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
        return null;
    }

    @Override
    public InetSocketAddress getServerRemoteAddress() {
        return null;
    }
}
