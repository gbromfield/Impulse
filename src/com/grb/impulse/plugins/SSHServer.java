package com.grb.impulse.plugins;

import com.grb.impulse.*;
import com.grb.impulse.utils.SshServerClient;
import com.grb.transport.TransportException;
import com.grb.util.property.*;
import com.grb.util.property.impl.MapPropertySource;
import com.grb.util.property.impl.RangeConstraint;
import com.grb.util.property.impl.SystemPropertySource;
import com.grb.util.property.impl.ValueOfConverter;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.Factory;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.UserAuth;
import org.apache.sshd.server.auth.UserAuthNone;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;

import javax.management.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by gbromfie on 12/1/15.
 */
public class SSHServer extends BaseTransform {
    /**
     * This variable is a place holder for a transform argument.
     * Argument syntax must follow <transform simple class name>.<type simple class name>.<field name>
     * This transforms outputs data into the argMap using this as a key.
     */
    @Argument
    final public static Object[] client = {"SSHServer.SshServerClient.client", SshServerClient.class};

    /**
     * Host key filename.
     */
    public static final String HOST_KEY_FILENAME_PROPERTY = "hostKeyFilename";

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
    private ArrayList<SshServer> _servers;
    private Property<Boolean> _acceptingProp;
    private PropertySource<Boolean> _acceptingJMXSource;

    public SSHServer(String transformName, String instanceName, TransformCreationContext transformCreationContext, Object... args) {
        super(transformName, instanceName, transformCreationContext);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void init() throws Exception {
        super.init();
        _acceptingProp = (Property<Boolean>)getProperty(ACCEPTING_PROPERTY);
        _acceptingJMXSource = _acceptingProp.getSource(JMXUtils.JMX_SOURCE);
        String hostKeyFilename = getStringProperty(HOST_KEY_FILENAME_PROPERTY);
        if (hostKeyFilename.length() == 0) {
            hostKeyFilename = null;
        } else {
            File hostKeyFile = new File(hostKeyFilename);
            if (!hostKeyFile.exists()) {
                throw new IllegalArgumentException(String.format("Host key file %s does not exist", hostKeyFilename));
            }
        }
        Property<String> addressProp = (Property<String>)getProperty(LISTEN_ADDRESS_PROPERTY);
        String listenAddr = null;
        if (addressProp.isSet()) {
            listenAddr = addressProp.getValue();
        }
        _minList = new ArrayList<Integer>();
        _maxList = new ArrayList<Integer>();
        parsePortStr();

        _servers = new ArrayList<SshServer>();
        for(int i = 0; i < _minList.size(); i++) {
            int minPort = _minList.get(i);
            int maxPort = _maxList.get(i);
            while(minPort <= maxPort) {
                SshServer sshd = SshServer.setUpDefaultServer();
                sshd.setPort(minPort);
                sshd.getProperties().put(SshServer.IDLE_TIMEOUT, "0");
                sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(hostKeyFilename));
                sshd.setShellFactory(new Factory<Command>() {
                    @Override
                    public Command create() {
                        SshServerClient c = new SshServerClient();
                        onNewConnection(c);
                        return c;
                    }
                });
                List<NamedFactory<UserAuth>> userAuthFactories = new ArrayList<NamedFactory<UserAuth>>(
                        1);
                userAuthFactories.add(new UserAuthNone.Factory());
                sshd.setUserAuthFactories(userAuthFactories);
                if (_logger.isInfoEnabled()) {
                    if (listenAddr == null) {
                        _logger.info("Listening on port " + minPort);
                    } else {
                        _logger.info(String.format("Listening on addr:port %s:%d", listenAddr, minPort));
                    }
                }
                _servers.add(sshd);
                minPort++;
            }
        }
    }

    @Override
    public void start() throws Exception {
        super.start();
        if (_acceptingProp.getValue()) {
            for(int i = 0; i < _servers.size(); i++) {
                _servers.get(i).start();
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
            } catch (Exception e) {
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
    public void startAccepting() throws TransportException, IOException {
        if (_logger.isInfoEnabled()) {
            _logger.info("startAccepting");
        }
        for(int i = 0; i < _servers.size(); i++) {
            _servers.get(i).start();
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
    public void stopAccepting() throws TransportException, IOException, InterruptedException {
        if (_logger.isInfoEnabled()) {
            _logger.info("stopAccepting");
        }
        for(int i = 0; i < _servers.size(); i++) {
            _servers.get(i).stop();
        }
        try {
            _acceptingJMXSource.setValue(false);
        } catch (PropertyVetoException e) {}
    }

    /**
     * [output] Called with new connections.
     */
    @Output("onNewConnection")
    public void onNewConnection(SshServerClient sshClient) {
        if (_logger.isInfoEnabled()) {
            _logger.info("onNewConnection: " + sshClient);
        }
        // On Reactor Thread
        next("onNewConnection", null, client[0], sshClient);
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
        p = new Property<String>(HOST_KEY_FILENAME_PROPERTY, "",
                new SystemPropertySource<String>(ctx.getPropertyString(HOST_KEY_FILENAME_PROPERTY), PropertySource.PRIORITY_1, PropertySource.NULL_INVALID),
                new MapPropertySource<String>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(HOST_KEY_FILENAME_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_2, PropertySource.NULL_INVALID));
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "Host Key Filename");
        props.put(p.getId(), p);

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
