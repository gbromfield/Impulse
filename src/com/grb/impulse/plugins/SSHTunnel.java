package com.grb.impulse.plugins;

import com.grb.impulse.*;
import com.grb.impulse.utils.SshServerClient;
import com.grb.transport.*;
import com.grb.transport.tcp.DefaultSocketChannelFactory;
import com.grb.transport.tcp.TCPTransportClient;
import com.grb.transport.tcp.TCPTransportClientProperties;
import com.grb.util.property.*;
import com.grb.util.property.impl.MapPropertySource;
import com.grb.util.property.impl.RangeConstraint;
import com.grb.util.property.impl.SystemPropertySource;
import com.grb.util.property.impl.ValueOfConverter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sshd.ClientChannel;
import org.apache.sshd.ClientSession;
import org.apache.sshd.SshClient;
import org.apache.sshd.SshServer;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.common.Session;
import org.apache.sshd.common.SessionListener;
import org.apache.sshd.common.SshException;
import org.apache.sshd.common.future.SshFutureListener;
import org.apache.sshd.common.io.IoInputStream;
import org.apache.sshd.common.io.IoOutputStream;
import org.apache.sshd.common.io.IoReadFuture;
import org.apache.sshd.common.util.Buffer;

import javax.management.MBeanException;
import javax.management.MBeanOperationInfo;
import javax.management.ReflectionException;
import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by gbromfie on 12/1/15.
 */
public class SSHTunnel extends BaseTransform implements SessionListener {

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

    private enum ReaderThreadOperation {
        START,
        STOP,
        CLOSE
    };

    /**
     * This variable is a place holder for a transform argument.
     * Argument syntax must follow <transform simple class name>.<type simple class name>.<field name>
     * This transforms outputs data into the argMap using this as a key.
     */
    @Argument
    final public static Object[] inputBytes = {"SSHTunnel.ByteBuffer.inputBytes", ByteBuffer.class};

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
    private SshServerClient _clientSideClient;
    private ArrayBlockingQueue<ReaderThreadOperation> _clientSideClientReadQ = new ArrayBlockingQueue<ReaderThreadOperation>(100);
    private boolean _clientCloneReadBuffer;

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

    public SSHTunnel(String transformName, String instanceName,
                  TransformCreationContext transformCreationContext, Object... args) {
        super(transformName, instanceName, transformCreationContext);
        if ((args == null) || (args.length != 1) || (!(args[0] instanceof SshServerClient))) {
            throw new IllegalArgumentException(String.format("Null or invalid argument provided to constructor of transform \"%s\", instance \"%s\"", transformName, instanceName));
        }
        _clientSideClient = (SshServerClient)args[0];
        _clientSideClient.setRunnable(new Runnable() {
            @Override
            public void run() {
                char[] data = new char[65535];
                ByteBuffer buffer = ByteBuffer.allocate(data.length);
                BufferedReader r = new BufferedReader(new InputStreamReader(_clientSideClient.getIn()));
                boolean enabled = false;
                try {
                    while (true) {
                        while ((!enabled) || (_clientSideClientReadQ.size() > 0)) {
                            ReaderThreadOperation oper = _clientSideClientReadQ.take();
                            if (oper.equals(ReaderThreadOperation.START)) {
                                enabled = true;
                            } else if (oper.equals(ReaderThreadOperation.STOP)) {
                                enabled = false;
                            } else {
                                return;
                            }
                        }
                        int numRead = r.read(data);
                        if (numRead == -1) {
                            close();
                            return;
                        }
                        while (_clientSideClientReadQ.size() > 0) {
                            ReaderThreadOperation oper = _clientSideClientReadQ.take();
                            if (oper.equals(ReaderThreadOperation.START)) {
                                enabled = true;
                            } else if (oper.equals(ReaderThreadOperation.STOP)) {
                                enabled = false;
                            } else {
                                return;
                            }
                        }
                        for(int i = 0; i < numRead; i++) {
                            buffer.put((byte) data[i]);
                        }
                        if (enabled) {
                            buffer.flip();
                            onClientRead(buffer);
                            buffer.clear();
                        }
                    }
                } catch(Exception e) {
                } finally {
                }
            }
        }, "Test"); // TODO:
    }

    @Override
    public void init() throws Exception {
        super.init();
        _clientCloneReadBuffer = getBooleanProperty(CLIENT_CLONE_READ_BUFFER_PROPERTY);
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

            if (getBooleanProperty(CLIENT_AUTO_START_READ_PROPERTY)) {
                startClientReading();
            }
        } catch(Exception e) {
            close();
        }
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
    public void startClientReading() {
        if (_logger.isInfoEnabled()) {
            _logger.info("startClientReading");
        }
        try {
            _clientSideClientReadQ.put(ReaderThreadOperation.START);
        } catch (InterruptedException e) {
        }
    }

    /**
     * [input] Stops the client side reading from the socket.
     *
     * @throws TransportException
     */
    @Input("stopClientReading")
    public void stopClientReading() {
        if (_logger.isInfoEnabled()) {
            _logger.info("stopClientReading");
        }
        try {
            _clientSideClientReadQ.put(ReaderThreadOperation.STOP);
        } catch (InterruptedException e) {
        }
    }

    /**
     * [input] Starts the server side reading from the socket.
     *
     * @throws TransportException
     */
    @Input("startServerReading")
    public void startServerReading() {
        if (_logger.isInfoEnabled()) {
            _logger.info("startServerReading");
        }
        _serverSideInReader.startReading();
        _serverSideErrReader.startReading();
    }

    /**
     * [input] Stops the server side reading from the socket.
     *
     * @throws TransportException
     */
    @Input("stopServerReading")
    public void stopServerReading() {
        if (_logger.isInfoEnabled()) {
            _logger.info("stopServerReading");
        }
//        _serverSideClient.stopReading();
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
            _serverSideClient.close(true);
        }
        if (_clientSideClient != null) {
            try {
                _clientSideClientReadQ.put(ReaderThreadOperation.CLOSE);
            } catch (InterruptedException e) {
            }
            _clientSideClient.close();
        }
    }

    /**
     * [input] Called on a new client connection.
     *
     * @param c
     */
    @Input("onNewConnection")
    static public void onNewConnection(Connection c, SshServerClient client) {
        TransformCreationContext createCtx = new TransformCreationContext();
        try {
            createCtx.getInstance(getTransformContext(c.getConnectionDefinition().getInputTransformName()), client);
        } catch (Exception e) {
            Log log = LogFactory.getLog(SSHTunnel.class.getName());
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
    @Input("writeToClient")
    public void writeToClient(ByteBuffer buffer) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("writeToClient: \r\n" + Impulse.GlobalByteArrayFormatter.format(buffer));
        }
        try {
            _clientSideClient.getOut().write(buffer.array(), buffer.arrayOffset(), buffer.remaining());
            _clientSideClient.getOut().flush();
        } catch (Exception e) {
//            _clientSideClient.close(TransportOperation.Sending, e);
        }
    }

    /**
     * [output] Called on reading data from the client side.
     *
     * @param readBuffer
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
    @Input("writeToServer")
    public void writeToServer(ByteBuffer buffer) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("writeToServer: \r\n" + Impulse.GlobalByteArrayFormatter.format(buffer));
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
    public Object invoke(String actionName, Object[] params, String[] signature)
            throws MBeanException, ReflectionException {
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

    public InetSocketAddress getServerLocalAddress() {
        return _serverLocalAddress;
    }

    public InetSocketAddress getServerRemoteAddress() {
        return _serverRemoteAddress;
    }

}
