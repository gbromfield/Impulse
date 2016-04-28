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
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.apache.sshd.ClientChannel;
import org.apache.sshd.ClientSession;
import org.apache.sshd.SshClient;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.common.Session;
import org.apache.sshd.common.SessionListener;
import org.apache.sshd.common.SshException;
import org.apache.sshd.common.future.SshFutureListener;
import org.apache.sshd.common.io.IoInputStream;
import org.apache.sshd.common.io.IoOutputStream;
import org.apache.sshd.common.io.IoReadFuture;
import org.apache.sshd.common.util.Buffer;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;

/**
 * Created by gbromfie on 11/10/15.
 */
public class TelnetToSSHTunnel extends BaseTransform implements IoHandler, IoFutureListener, SessionListener {

    @Override
    public void sessionCreated(Session session) {

    }

    @Override
    public void sessionEvent(Session session, Event event) {

    }

    @Override
    public void sessionClosed(Session session) {
        close();
        dispose();
    }

    public class SshReader implements SshFutureListener<IoReadFuture> {
        private String _host;
        private IoInputStream _ioInStr;
        private Buffer _buffer;

        public SshReader(String host, IoInputStream ioInStr) {
            _host = host;
            _ioInStr = ioInStr;
            _buffer = new Buffer(10000);
        }

        /**
         * Register for reading
         */
        public void startReading() {
            _ioInStr.read(_buffer).addListener(this);
        }

        /**
         * Called when data is ready to be read.
         * This will execute in one of the nio threads from the thread pool.
         *
         * @param ioReadFuture
         */
        @Override
        public void operationComplete(IoReadFuture ioReadFuture) {
            Throwable t = ioReadFuture.getException();
            if (t instanceof SshException) {
                SshException sshEx = (SshException)t;
                if (_logger.isErrorEnabled()) {
                    _logger.error(String.format("Exception on read from host \"%s\", disconnect code=%d - \"%s\"", _host, sshEx.getDisconnectCode(), sshEx.getMessage()), t);
                }
                close();
            }
            Buffer buffer = ioReadFuture.getBuffer();
            if (buffer.available() > 0) {
                byte[] inData = new byte[buffer.available()];
                buffer.getRawBytes(inData);
                ByteBuffer b = ByteBuffer.wrap(inData);
                onServerRead(b);
                buffer.clear();
                startReading();
            }
        }
    }

    /**
     * This variable is a place holder for a transform argument.
     * Argument syntax must follow <transform simple class name>.<type simple class name>.<field name>
     * This transforms outputs data into the argMap using this as a key.
     */
    @Argument
    final public static Object[] inputIOBuffer = {"TelnetToSSHTunnel.IoBuffer.inputIOBuffer", IoBuffer.class};

    /**
     * This variable is a place holder for a transform argument.
     * Argument syntax must follow <transform simple class name>.<type simple class name>.<field name>
     * This transforms outputs data into the argMap using this as a key.
     */
    @Argument
    final public static Object[] inputByteBuffer = {"TelnetToSSHTunnel.ByteBuffer.inputByteBuffer", ByteBuffer.class};

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
    private SshClient _serverSideClient;
    private ClientSession _serverSideSession;
    private ChannelShell _serverSideChannel;
    private IoOutputStream _serverSideOutput;
    private SshReader _serverSideInReader;
    private SshReader _serverSideErrReader;
    private boolean _serverCloneReadBuffer;
    private InetSocketAddress _serverLocalAddress;
    private InetSocketAddress _serverRemoteAddress;

    /**
     *
     * @param transformName
     * @param instanceName
     * @param transformCreationContext
     * @param args
     */
    public TelnetToSSHTunnel(String transformName, String instanceName,
                             TransformCreationContext transformCreationContext, Object... args) {
        super(transformName, instanceName, transformCreationContext);
        if ((args == null) || (args.length != 1) || (!(args[0] instanceof IoSession))) {
            throw new IllegalArgumentException(String.format("Null or invalid argument provided to constructor of transform \"%s\", instance \"%s\"", transformName, instanceName));
        }
        _clientSession = (IoSession)args[0];
        _clientSession.setAttribute(TelnetServer.IOHANDLER_ATTRIBUTE_KEY, this);
    }

    @Override
    public void init() throws Exception {
        super.init();
        _serverCloneReadBuffer = getBooleanProperty(SERVER_CLONE_READ_BUFFER_PROPERTY);

        _serverSideClient = SshClient.setUpDefaultClient();
        // Sets the number of threads in the NIO thread pool (default is 9)
        _serverSideClient.setNioWorkers(1);
        // Sets the SSH heartbeat interval that will stop the inactivity
        // timeout on the client or server side from occurring
        _serverSideClient.getProperties().put(SshClient.HEARTBEAT_INTERVAL, "60000");
        System.out.println(_serverSideClient.getProperties());
        _serverSideClient.start();

        try {
            String host = getStringProperty(SERVER_IP_ADDRESS_PROPERTY);
            int port = getIntegerProperty(SERVER_PORT_PROPERTY);
            _serverSideSession = _serverSideClient.connect("", host, port).await().getSession();
            _serverSideSession.addListener(this);
            _serverSideSession.auth().verify();
            _serverSideChannel = (ChannelShell)_serverSideSession.createChannel("shell");
            _serverSideChannel.setAgentForwarding(false);
            _serverSideChannel.setStreaming(ClientChannel.Streaming.Async);
            _serverSideChannel.open().await();

            _serverSideOutput = _serverSideChannel.getAsyncIn();
            _serverSideInReader = new SshReader(host, _serverSideChannel.getAsyncOut());
            _serverSideInReader.startReading();
            _serverSideErrReader = new SshReader(host, _serverSideChannel.getAsyncErr());
            _serverSideErrReader.startReading();
        } catch(Exception e) {
            close();
        }
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
        if (_serverSideClient != null) {
            _serverSideClient.close(true);
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
            Log log = LogFactory.getLog(TelnetToSSHTunnel.class.getName());
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
        if (_logger.isDebugEnabled()) {
            _logger.debug("writeIOBufferToServer: \r\n" + Impulse.GlobalByteArrayFormatter.format(buffer.buf()));
        }
        ByteBuffer byteBuf = ByteBuffer.allocate(buffer.remaining());
        byteBuf.put(buffer.array(), buffer.arrayOffset(), buffer.remaining());
        byteBuf.flip();
        _serverSideOutput.write(new Buffer(buffer.array(), buffer.arrayOffset(), buffer.remaining()));
    }

    /**
     * [input] Writes data to the server side socket.
     *
     * @param buffer
     */
    @Input("writeByteBufferToServer")
    public void writeByteBufferToServer(ByteBuffer buffer) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("writeByteBufferToServer: \r\n" + Impulse.GlobalByteArrayFormatter.format(buffer));
        }
        _serverSideOutput.write(new Buffer(buffer.array(), buffer.arrayOffset(), buffer.remaining()));
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
        next("onServerRead", null, inputByteBuffer[0], cloneBuf);
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
    }

    @Override
    public void sessionOpened(IoSession ioSession) throws Exception {
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
            if (hasControlD(buffer)) {
                close();
            } else if (isControlStatement(buffer)) {
                onClientControlRead(buffer);
            } else {
                onClientDataRead(buffer);
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
