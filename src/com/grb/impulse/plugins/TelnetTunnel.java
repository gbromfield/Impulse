package com.grb.impulse.plugins;

import com.grb.impulse.*;
import com.grb.util.property.*;
import com.grb.util.property.impl.MapPropertySource;
import com.grb.util.property.impl.RangeConstraint;
import com.grb.util.property.impl.SystemPropertySource;
import com.grb.util.property.impl.ValueOfConverter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by gbromfie on 11/10/15.
 */
public class TelnetTunnel extends BaseTransform implements IoHandler, IoFutureListener {
    /**
     * This variable is a place holder for a transform argument.
     * Argument syntax must follow <transform simple class name>.<type simple class name>.<field name>
     * This transforms outputs data into the argMap using this as a key.
     */
    @Argument
    final public static Object[] inputIOBuffer = {"TelnetTunnel.IoBuffer.inputIOBuffer", IoBuffer.class};

    /**
     * This variable is a place holder for a transform argument.
     * Argument syntax must follow <transform simple class name>.<type simple class name>.<field name>
     * This transforms outputs data into the argMap using this as a key.
     */
    @Argument
    final public static Object[] inputByteBuffer = {"TelnetTunnel.ByteBuffer.inputByteBuffer", ByteBuffer.class};

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
    private IoSession _clientSession;

    /**
     * Server side properties
     */
    private IoSession _serverSession;
    private ConnectFuture _connFuture;

    /**
     *
     * @param transformName
     * @param instanceName
     * @param transformCreationContext
     * @param args
     */
    public TelnetTunnel(String transformName, String instanceName,
                  TransformCreationContext transformCreationContext, Object... args) {
        super(transformName, instanceName, transformCreationContext);
        if ((args == null) || (args.length != 1) || (!(args[0] instanceof IoSession))) {
            throw new IllegalArgumentException(String.format("Null or invalid argument provided to constructor of transform \"%s\", instance \"%s\"", transformName, instanceName));
        }
        _clientSession = (IoSession)args[0];
        _clientSession.setAttribute(TelnetServer.IOHANDLER_ATTRIBUTE_KEY, this);
        _serverSession = null;
    }

    @Override
    public void init() throws Exception {
        super.init();
        InetSocketAddress addr = new InetSocketAddress(InetAddress.getByName(getStringProperty(SERVER_IP_ADDRESS_PROPERTY)),
                getIntegerProperty(SERVER_PORT_PROPERTY));
        NioSocketConnector connector = new NioSocketConnector();
        SocketSessionConfig config = connector.getSessionConfig();
        config.setSendBufferSize(getIntegerProperty(SERVER_WRITE_BUFFER_SIZE_PROPERTY));
        config.setWriteTimeout(getIntegerProperty(SERVER_WRITE_TIMEOUT_IN_SECS_PROPERTY));
        connector.setConnectTimeoutMillis(getIntegerProperty(SERVER_CONNECT_TIMEOUT_IN_SECS_PROPERTY) * 1000);
        connector.setHandler(this);
        _connFuture = connector.connect(addr);
        _connFuture.addListener(this);
    }

    /**
     * [input] Closes the client and server side sockets.
     */
    @Input("onClose")
    public void close() {
        if (_logger.isInfoEnabled()) {
            _logger.info("onClose");
        }
        if (_clientSession != null) {
            _clientSession.close(true);
        }
        if (_serverSession != null) {
            _serverSession.close(true);
        }
    }

    /**
     * [input] Called on a new client connection.
     *
     * @param c
     */
    @Input("onNewConnection")
    static public void onNewConnection(Connection c, IoSession ioSession) {
        TransformCreationContext createCtx = new TransformCreationContext();
        try {
            createCtx.getInstance(getTransformContext(c.getConnectionDefinition().getInputTransformName()), ioSession);
        } catch (Exception e) {
            Log log = LogFactory.getLog(TelnetTunnel.class.getName());
            if (log.isErrorEnabled()) {
                log.error("onNewConnection", e);
            }
        }
    }

    /**
     * [input] Writes data to the client side socket.
     *
     * @param buffer
     */
    @Input("writeIOBufferToClient")
    public void writeIOBufferToClient(IoBuffer buffer) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("writeIOBufferToClient: \r\n" + Impulse.GlobalByteArrayFormatter.format(buffer.buf()));
        }
        _clientSession.write(buffer);
    }

    /**
     * [input] Writes data to the client side socket.
     *
     * @param buffer
     */
    @Input("writeByteBufferToClient")
    public void writeByteBufferToClient(ByteBuffer buffer) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("writeByteBufferToClient: \r\n" + Impulse.GlobalByteArrayFormatter.format(buffer));
        }
        IoBuffer iobuf = IoBuffer.allocate(buffer.remaining());
        iobuf.put(buffer);
        iobuf.flip();
        _clientSession.write(iobuf);
    }

    /**
     * [output] Called on reading data from the client side.
     *
     * @param readBuffer
     */
    @Output("onClientDataRead")
    public void onClientDataRead(IoBuffer readBuffer) {
        ByteBuffer byteBuf = ByteBuffer.allocate(readBuffer.remaining());
        byteBuf.put(readBuffer.array(), readBuffer.arrayOffset(), readBuffer.remaining());
        byteBuf.flip();
        next("onClientDataRead", null, inputIOBuffer[0], readBuffer, inputByteBuffer[0], byteBuf);
    }

    /**
     * [output] Called on reading control data from the client side.
     *
     * @param readBuffer
     */
    @Output("onClientControlRead")
    public void onClientControlRead(IoBuffer readBuffer) {
        next("onClientControlRead", null, inputIOBuffer[0], readBuffer);
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
     * @param buffer
     */
    @Input("writeIOBufferToServer")
    public void writeIOBufferToServer(IoBuffer buffer) {
        checkConnected();
        if (_logger.isDebugEnabled()) {
            _logger.debug("writeIOBufferToServer: \r\n" + Impulse.GlobalByteArrayFormatter.format(buffer.buf()));
        }
        _serverSession.write(buffer);
    }

    /**
     * [input] Writes data to the server side socket.
     *
     * @param buffer
     */
    @Input("writeByteBufferToServer")
    public void writeByteBufferToServer(ByteBuffer buffer) {
        checkConnected();
        if (_logger.isDebugEnabled()) {
            _logger.debug("writeByteBufferToServer: \r\n" + Impulse.GlobalByteArrayFormatter.format(buffer));
        }
        IoBuffer iobuf = IoBuffer.allocate(buffer.remaining());
        iobuf.put(buffer);
        iobuf.flip();
        _serverSession.write(iobuf);
    }

    /**
     * [output] Called on reading data from the server side.
     *
     * @param readBuffer
     */
    @Output("onServerDataRead")
    public void onServerDataRead(IoBuffer readBuffer) {
        ByteBuffer byteBuf = ByteBuffer.allocate(readBuffer.remaining());
        byteBuf.put(readBuffer.array(), readBuffer.arrayOffset(), readBuffer.remaining());
        byteBuf.flip();
        next("onServerDataRead", null, inputIOBuffer[0], readBuffer, inputByteBuffer[0], byteBuf);
    }

    /**
     * [output] Called on reading data from the server side.
     *
     * @param readBuffer
     */
    @Output("onServerControlRead")
    public void onServerControlRead(IoBuffer readBuffer) {
        next("onServerControlRead", null, inputIOBuffer[0], readBuffer);
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
/*
    @Override
    public String toString() {
        StringBuilder bldr = new StringBuilder();
        bldr.append("client= ");
        bldr.append(_clientSideClient);
        bldr.append(", server= ");
        bldr.append(_serverSideClient);
        return bldr.toString();
    }
*/
    @Override
    public void sessionCreated(IoSession ioSession) throws Exception {
        if (ioSession == _clientSession) {
            // happened already
        } else {
            _serverSession = ioSession;
        }
    }

    @Override
    public void sessionOpened(IoSession ioSession) throws Exception {
        if (ioSession == _clientSession) {
            // happened already
        } else {
            // Do nothing
        }
    }

    @Override
    public void sessionClosed(IoSession ioSession) throws Exception {
        close();
        dispose();
    }

    @Override
    public void sessionIdle(IoSession ioSession, IdleStatus idleStatus) throws Exception {
        // no idle handling
    }

    @Override
    public void exceptionCaught(IoSession ioSession, Throwable throwable) throws Exception {
        close();
    }

    @Override
    public void messageReceived(IoSession ioSession, Object o) throws Exception {
        if (o instanceof IoBuffer) {
            IoBuffer buffer = (IoBuffer)o;
            if (ioSession == _clientSession) {
                if (hasControlD(buffer)) {
                    close();
                } else if (isControlStatement(buffer)) {
                    onClientControlRead(buffer);
                } else {
                    onClientDataRead(buffer);
                }
            } else {
                if (isControlStatement(buffer)) {
                    onServerControlRead(buffer);
                    return;
                } else {
                    onServerDataRead(buffer);
                }
            }
        }
    }

    @Override
    public void messageSent(IoSession ioSession, Object o) throws Exception {
        // no send handling
    }

    @Override
    public void inputClosed(IoSession ioSession) throws Exception {
        close();
    }

    @Override
    public void operationComplete(IoFuture ioFuture) {
        if (ioFuture instanceof ConnectFuture) {
            // Must apply to the server connection
            ConnectFuture connFuture = (ConnectFuture)ioFuture;
            if (connFuture.isDone()) {
                if (connFuture.getException() != null) {
                    close();
                }
            }
        }
    }

    private void checkConnected() {
        if (_connFuture.isDone()) {
            if (_connFuture.isCanceled()) {
                if (_logger.isErrorEnabled()) {
                    _logger.error("Connection cancelled");
                }
                throw new IllegalStateException("Connection cancelled");
            } else if (_connFuture.getException() != null) {
                if (_logger.isErrorEnabled()) {
                    _logger.error("Connection failed", _connFuture.getException());
                }
                throw new IllegalStateException("Connection failed", _connFuture.getException());
            }
        } else {
            throw new IllegalStateException("Connection not yet connected");
        }
    }

    private boolean isControlStatement(IoBuffer buffer) {
        if ((buffer != null) && (buffer.array() != null) && (buffer.array().length > 0)) {
            if (buffer.array()[0] < 0) {
                return true;
            }
        }
        return false;
    }

    private boolean hasControlD(IoBuffer buffer) {
        if (buffer != null) {
            byte[] array = buffer.array();
            if ((array != null) && (array.length > 0)) {
                for(int i = 0; i < array.length; i++) {
                    if (array[i] == 0x04) {
                        return true;
                    }
                }
            }
        }
        return false;
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

/*
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
*/
}
