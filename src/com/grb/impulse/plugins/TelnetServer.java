package com.grb.impulse.plugins;

import com.grb.impulse.*;
import com.grb.util.property.*;
import com.grb.util.property.impl.MapPropertySource;
import com.grb.util.property.impl.RangeConstraint;
import com.grb.util.property.impl.SystemPropertySource;
import com.grb.util.property.impl.ValueOfConverter;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Map;

/**
 * Created by gbromfie on 11/10/15.
 */
public class TelnetServer extends BaseTransform implements IoHandler {

    public final static String IOHANDLER_ATTRIBUTE_KEY = "IOHandler";

    /**
     * This variable is a place holder for a transform argument.
     * Argument syntax must follow <transform simple class name>.<type simple class name>.<field name>
     * This transforms outputs data into the argMap using this as a key.
     */
    @Argument
    final public static Object[] client = {"TelnetServer.IoSession.client", IoSession.class};

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
    private Property<Boolean> _acceptingProp;
    private PropertySource<Boolean> _acceptingJMXSource;
    private InetAddress _listenInetAddr;
    private NioSocketAcceptor _acceptor;

    public TelnetServer(String transformName, String instanceName,
                        TransformCreationContext transformCreationContext, Object... args) {
        super(transformName, instanceName, transformCreationContext);
    }

    @Override
    public void init() throws Exception {
        super.init();
        _acceptingProp = (Property<Boolean>)getProperty(ACCEPTING_PROPERTY);
        _acceptingJMXSource = _acceptingProp.getSource(JMXUtils.JMX_SOURCE);

        Property<String> addressProp = (Property<String>)getProperty(LISTEN_ADDRESS_PROPERTY);
        _listenInetAddr = null;
        if (addressProp.isSet()) {
            _listenInetAddr = (Inet4Address)InetAddress.getByName(addressProp.getValue());
        }
        _minList = new ArrayList<Integer>();
        _maxList = new ArrayList<Integer>();
        parsePortStr();
        _acceptor = new NioSocketAcceptor();
        _acceptor.setHandler(this);
        _acceptor.getSessionConfig().setSendBufferSize(getIntegerProperty(CLIENT_WRITE_BUFFER_SIZE_PROPERTY));
        _acceptor.getSessionConfig().setWriteTimeout(getIntegerProperty(CLIENT_WRITE_TIMEOUT_IN_SECS_PROPERTY));
//        _acceptor.getFilterChain().addLast( "codec", new ProtocolCodecFilter( new TextLineCodecFactory( Charset.forName("UTF-8"))));
    }

    @Override
    public void start() throws Exception {
        super.start();
        if (_acceptingProp.getValue()) {
            startAccepting();
        }
    }

    /**
     * [input] Starts accepting new connections.
     *
     * @throws
     */
    @Input("startAccepting")
    public void startAccepting() throws IOException {
        if (_logger.isInfoEnabled()) {
            _logger.info("startAccepting");
        }
        for(int i = 0; i < _minList.size(); i++) {
            int minPort = _minList.get(i);
            int maxPort = _maxList.get(i);
            while(minPort <= maxPort) {
                if (_listenInetAddr == null) {
                    _acceptor.bind( new InetSocketAddress(minPort));
                } else {
                    _acceptor.bind( new InetSocketAddress(_listenInetAddr, minPort));
                }
                if (_logger.isInfoEnabled()) {
                    if (_listenInetAddr == null) {
                        _logger.info("Listening on port " + minPort);
                    } else {
                        _logger.info(String.format("Listening on addr:port %s:%d", _listenInetAddr, minPort));
                    }
                }
                minPort++;
            }
        }
        try {
            _acceptingJMXSource.setValue(true);
        } catch (PropertyVetoException e) {}
    }

    /**
     * [input] Stops accepting new connections.
     *
     * @throws
     */
    @Input("stopAccepting")
    public void stopAccepting() {
        if (_logger.isInfoEnabled()) {
            _logger.info("stopAccepting");
        }
        _acceptor.unbind();
        try {
            _acceptingJMXSource.setValue(false);
        } catch (PropertyVetoException e) {}
    }

    /**
     * [output] Called with new connections.
     */
    @Output("onNewConnection")
    public void onNewConnection(IoSession ioSession) {
        if (_logger.isInfoEnabled()) {
            _logger.info("onNewConnection: " + ioSession);
        }
        // On Reactor Thread
        next("onNewConnection", null, client[0], ioSession);
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

    @Override
    public void sessionCreated(IoSession ioSession) throws Exception {
        // do nothing
    }

    @Override
    public void sessionOpened(IoSession ioSession) throws Exception {
        onNewConnection(ioSession);
        IoHandler hndlr = (IoHandler)ioSession.getAttribute(IOHANDLER_ATTRIBUTE_KEY);
        if (hndlr != null) {
            hndlr.sessionOpened(ioSession);
        }
    }

    @Override
    public void sessionClosed(IoSession ioSession) throws Exception {
        IoHandler hndlr = (IoHandler)ioSession.getAttribute(IOHANDLER_ATTRIBUTE_KEY);
        if (hndlr != null) {
            hndlr.sessionClosed(ioSession);
        }
    }

    @Override
    public void sessionIdle(IoSession ioSession, IdleStatus idleStatus) throws Exception {
        IoHandler hndlr = (IoHandler)ioSession.getAttribute(IOHANDLER_ATTRIBUTE_KEY);
        if (hndlr != null) {
            hndlr.sessionIdle(ioSession, idleStatus);
        }
    }

    @Override
    public void exceptionCaught(IoSession ioSession, Throwable throwable) throws Exception {
        IoHandler hndlr = (IoHandler)ioSession.getAttribute(IOHANDLER_ATTRIBUTE_KEY);
        if (hndlr != null) {
            hndlr.exceptionCaught(ioSession, throwable);
        }
    }

    @Override
    public void messageReceived(IoSession ioSession, Object o) throws Exception {
        IoHandler hndlr = (IoHandler)ioSession.getAttribute(IOHANDLER_ATTRIBUTE_KEY);
        if (hndlr != null) {
            hndlr.messageReceived(ioSession, o);
        }
    }

    @Override
    public void messageSent(IoSession ioSession, Object o) throws Exception {
        IoHandler hndlr = (IoHandler)ioSession.getAttribute(IOHANDLER_ATTRIBUTE_KEY);
        if (hndlr != null) {
            hndlr.messageSent(ioSession, o);
        }
    }

    @Override
    public void inputClosed(IoSession ioSession) throws Exception {
        IoHandler hndlr = (IoHandler)ioSession.getAttribute(IOHANDLER_ATTRIBUTE_KEY);
        if (hndlr != null) {
            hndlr.inputClosed(ioSession);
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
