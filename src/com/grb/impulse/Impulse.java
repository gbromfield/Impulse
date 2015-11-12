package com.grb.impulse;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Level;

import com.grb.reactor.ReactorThread;
import com.grb.util.ByteArrayFormatter;
import com.grb.util.property.JMXUtils;
import com.grb.util.property.Property;
import com.grb.util.property.PropertyChangeEvent;
import com.grb.util.property.PropertyChangeListener;
import com.grb.util.property.PropertyConversionException;
import com.grb.util.property.PropertySource;
import com.grb.util.property.PropertySourceChangeEvent;
import com.grb.util.property.PropertyVetoException;
import com.grb.util.property.VetoablePropertySourceChangeListener;
import com.grb.util.property.impl.MapPropertySource;
import com.grb.util.property.impl.ValueOfConverter;

/**
 * Impulse is a utility for testing network traffic interactions with clients.
 * This class contains the main program.
 */
public class Impulse implements DynamicMBean {
	final static private String Version = "@VERSION@"; 
	final static private String BuildDate = "@BUILDDATE@"; 
	static private Log Logger = LogFactory.getLog(Impulse.class);

    final public static String CONFIG_FILE_SOURCE = "config file";

    /**
     * Transform classname property
     */
    final public static String TRANSFORM_CLASS_PROPERTY = "classname";

    final public static String PROPERTY_DESCRIPTION_KEY = "description";
    
	/**
	 * Configuration File Properties
	 */
    final static public Properties ConfigProperties = new Properties();

    /**
     * Global map of transforms that were auto started so they don't 
     * get recreated when refered to.
     */
    final static public TreeMap<String, Transform> GlobalTransformMap = new TreeMap<String, Transform>();

    final static public TreeMap<String, String> GlobalClientServerAddressMap = new TreeMap<String, String>();

    final static public ByteArrayFormatter GlobalByteArrayFormatter = new ByteArrayFormatter(ByteArrayFormatter.WIRESHARK_FORMAT);
    
    final static public String IMPULSE_PROPERTY_ROOT = "impulse.";    
    final static public String BYTE_ARRAY_FORMATTER_FORMAT_PROPERTY = "byteArrayFormatterFormat";
    final static public String BYTE_ARRAY_FORMATTER_UNPRINTABLE_CHAR_PROPERTY = "byteArrayFormatterUnprintableChar";
    final static public String BYTE_ARRAY_FORMATTER_MORE_DATA_PROPERTY = "byteArrayFormatterMoreData";
    final static public String BYTE_ARRAY_FORMATTER_LIMIT_SIZE_PROPERTY = "byteArrayFormatterLimitSize";
    final static public String BYTE_ARRAY_FORMATTER_LIMIT_TYPE_PROPERTY = "byteArrayFormatterLimitType";
    final static public String CONFIG_FILE_PROPERTY = "configFile";
    final static public String LOG_STACK_TRACE_PROPERTY = "logStackTrace";
    final static public boolean LOG_STACK_TRACE_PROPERTY_DEFAULT = false;

    /**
     * Exit Impulse Operation
     */
    public static final String EXIT_OPERATION = "exit";

    final static public Map<String, Property<?>> Properties = new HashMap<String, Property<?>>();
                
    final static private Impulse OnlyInstance = new Impulse();
        
    @SuppressWarnings("unchecked")
    private Impulse() {
        Property<String> strProp = new Property<String>(CONFIG_FILE_PROPERTY, "");
        Properties.put(strProp.getId(), strProp);

        strProp = new Property<String>(BYTE_ARRAY_FORMATTER_FORMAT_PROPERTY, ByteArrayFormatter.DEFAULT_FORMAT,
        		new PropertySource<String>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new MapPropertySource<String>(Impulse.CONFIG_FILE_SOURCE, IMPULSE_PROPERTY_ROOT + BYTE_ARRAY_FORMATTER_FORMAT_PROPERTY, Impulse.ConfigProperties, PropertySource.PRIORITY_2, PropertySource.NULL_INVALID));
        strProp.addListener(new PropertyChangeListener<String>() {
            @Override
            public void propertyChanged(PropertyChangeEvent<String> event) {
                GlobalByteArrayFormatter.setFormat(event.getNewValue());
            }
        });
        strProp.getUserDataMap().put(PROPERTY_DESCRIPTION_KEY, "Format used to display byte arrays and byte buffers in debugging transforms");
        Properties.put(strProp.getId(), strProp);

        Property<Character> charProp = new Property<Character>(BYTE_ARRAY_FORMATTER_UNPRINTABLE_CHAR_PROPERTY, ByteArrayFormatter.DEFAULT_UNPRINTABLE_CHAR,
        		new PropertySource<Character>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new MapPropertySource<Character>(Impulse.CONFIG_FILE_SOURCE, IMPULSE_PROPERTY_ROOT + BYTE_ARRAY_FORMATTER_UNPRINTABLE_CHAR_PROPERTY, Impulse.ConfigProperties, PropertySource.PRIORITY_2, PropertySource.NULL_INVALID));
        charProp.addListener(new PropertyChangeListener<Character>() {
            @Override
            public void propertyChanged(PropertyChangeEvent<Character> event) {
                GlobalByteArrayFormatter.setUnprintableChar(event.getNewValue());
            }
        });
        charProp.getUserDataMap().put(PROPERTY_DESCRIPTION_KEY, "Character to be used as a replacement when printing unprintable characters");
        Properties.put(charProp.getId(), charProp);
        
        strProp = new Property<String>(BYTE_ARRAY_FORMATTER_MORE_DATA_PROPERTY, ByteArrayFormatter.DEFAULT_MORE_DATA_STRING,
        		new PropertySource<String>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new MapPropertySource<String>(Impulse.CONFIG_FILE_SOURCE, IMPULSE_PROPERTY_ROOT + BYTE_ARRAY_FORMATTER_MORE_DATA_PROPERTY, Impulse.ConfigProperties, PropertySource.PRIORITY_2, PropertySource.NULL_INVALID));
        strProp.addListener(new PropertyChangeListener<String>() {
            @Override
            public void propertyChanged(PropertyChangeEvent<String> event) {
                GlobalByteArrayFormatter.setMoreDataString(event.getNewValue());
            }
        });
        strProp.getUserDataMap().put(PROPERTY_DESCRIPTION_KEY, "String to be used to indicate that the output was truncated for size");
        Properties.put(strProp.getId(), strProp);

        Property<Integer> intProp = new Property<Integer>(BYTE_ARRAY_FORMATTER_LIMIT_SIZE_PROPERTY, ByteArrayFormatter.DEFAULT_LIMIT_SIZE,
        		new PropertySource<Integer>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new MapPropertySource<Integer>(Impulse.CONFIG_FILE_SOURCE, IMPULSE_PROPERTY_ROOT + BYTE_ARRAY_FORMATTER_LIMIT_SIZE_PROPERTY, Impulse.ConfigProperties, PropertySource.PRIORITY_2, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Integer.class)));
        intProp.addListener(new PropertyChangeListener<Integer>() {
            @Override
            public void propertyChanged(PropertyChangeEvent<Integer> event) {
                GlobalByteArrayFormatter.setLimit(event.getNewValue(), GlobalByteArrayFormatter.getLimitType());
            }
        });
        intProp.getUserDataMap().put(PROPERTY_DESCRIPTION_KEY, "Limit size of a byte array output (used in conjuction with \"" + BYTE_ARRAY_FORMATTER_LIMIT_TYPE_PROPERTY + "\")");
        Properties.put(intProp.getId(), intProp);

        strProp = new Property<String>(BYTE_ARRAY_FORMATTER_LIMIT_TYPE_PROPERTY, ByteArrayFormatter.DEFAULT_LIMIT_TYPE.toString(),
        		new PropertySource<String>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new MapPropertySource<String>(Impulse.CONFIG_FILE_SOURCE, IMPULSE_PROPERTY_ROOT + BYTE_ARRAY_FORMATTER_LIMIT_TYPE_PROPERTY, Impulse.ConfigProperties, PropertySource.PRIORITY_2, PropertySource.NULL_INVALID));
        strProp.addListener(new PropertyChangeListener<String>() {
            @Override
            public void propertyChanged(PropertyChangeEvent<String> event) {
                GlobalByteArrayFormatter.setLimit(GlobalByteArrayFormatter.getLimit(), ByteArrayFormatter.LimitType.valueOf(event.getNewValue()));
            }
        });
        strProp.addVetoableListener(new VetoablePropertySourceChangeListener<String>() {
            @Override
            public void vetoablePropertySourceChanged(
                    PropertySourceChangeEvent<String> event)
                    throws PropertyVetoException {
                String limitTypeStr = event.getNewValue();
                try {
                    ByteArrayFormatter.LimitType.valueOf(limitTypeStr);
                } catch(IllegalArgumentException e) {
                    StringBuilder bldr = new StringBuilder();
                    for(int i = 0; i < ByteArrayFormatter.LimitType.values().length; i++) {
                        if (i > 0) {
                            bldr.append(", ");
                        }
                        bldr.append(ByteArrayFormatter.LimitType.values()[i]);
                    }
                    throw new PropertyVetoException(event, String.format("Invalid attribute value \"%s\", must be one of [%s]", limitTypeStr, bldr.toString()), e);
                }
            }
        });
        strProp.getUserDataMap().put(PROPERTY_DESCRIPTION_KEY, "Limit size of a byte array output to number of bytes or rows (used in conjuction with \"" + BYTE_ARRAY_FORMATTER_LIMIT_SIZE_PROPERTY + "\")");
        Properties.put(strProp.getId(), strProp);

        Property<Boolean> boolProp = new Property<Boolean>(LOG_STACK_TRACE_PROPERTY, LOG_STACK_TRACE_PROPERTY_DEFAULT,
                new PropertySource<Boolean>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1, ValueOfConverter.getConverter(Boolean.class)),
                new MapPropertySource<Boolean>(Impulse.CONFIG_FILE_SOURCE, IMPULSE_PROPERTY_ROOT + LOG_STACK_TRACE_PROPERTY, Impulse.ConfigProperties, PropertySource.PRIORITY_2, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Boolean.class)));
        boolProp.getUserDataMap().put(PROPERTY_DESCRIPTION_KEY, "Allows logging of the stack trace when properties are changed");
        Properties.put(boolProp.getId(), boolProp);

    }
    
    @SuppressWarnings("unchecked")
    private void initProperties() throws PropertyVetoException, PropertyConversionException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException, MalformedObjectNameException, NullPointerException {
        Iterator<Property<?>> it = Properties.values().iterator();
        while(it.hasNext()) {
            Property<?> prop = it.next();
            prop.initialize();
            if (Logger.isDebugEnabled()) {
                String description = (prop.getUserDataMap().get(Impulse.PROPERTY_DESCRIPTION_KEY) == null) ? "" : (String)prop.getUserDataMap().get(Impulse.PROPERTY_DESCRIPTION_KEY);
                Logger.debug(String.format("Property: %s (%s)", prop.toKeyValueString(), description));
            } else if (Logger.isInfoEnabled()) {
                Logger.info("Property: " + prop.toKeyValueString());
            }            
            if ((!prop.hasDefaultValue()) && (!prop.isSet())) {
                throw new IllegalArgumentException(String.format("Missing mandatory impulse property %s", prop.getId()));
            }
        }    
        Property<String> strProp = (Property<String>)Properties.get(BYTE_ARRAY_FORMATTER_FORMAT_PROPERTY);
        GlobalByteArrayFormatter.setFormat(strProp.getValue());

        Property<Character> charProp = (Property<Character>)Properties.get(BYTE_ARRAY_FORMATTER_UNPRINTABLE_CHAR_PROPERTY);
        GlobalByteArrayFormatter.setUnprintableChar(charProp.getValue());

        strProp = (Property<String>)Properties.get(BYTE_ARRAY_FORMATTER_MORE_DATA_PROPERTY);
        GlobalByteArrayFormatter.setMoreDataString(strProp.getValue());

        Property<Integer> intProp = (Property<Integer>)Properties.get(BYTE_ARRAY_FORMATTER_LIMIT_SIZE_PROPERTY);
        strProp = (Property<String>)Properties.get(BYTE_ARRAY_FORMATTER_LIMIT_TYPE_PROPERTY);
        GlobalByteArrayFormatter.setLimit(intProp.getValue(), ByteArrayFormatter.LimitType.valueOf(strProp.getValue()));
        
        ManagementFactory.getPlatformMBeanServer().registerMBean(this, new ObjectName(getClass().getName() + ":name=impulse")); 
    }
        
    @Override
    public Object getAttribute(String attribute)
            throws AttributeNotFoundException, MBeanException,
            ReflectionException {
        return JMXUtils.getAttribute(Properties.values(), attribute);
    }

    @Override
    public AttributeList getAttributes(String[] attributes) {
        return null;
    }

    @Override
    public MBeanInfo getMBeanInfo() {
        return new MBeanInfo(getClass().getName(), getClass().getSimpleName(), JMXUtils.createAttrInfo(Properties.values()), null, createOperInfo(), null);        
    }

    @Override
    public Object invoke(String actionName, Object[] params, String[] signature)
            throws MBeanException, ReflectionException {
        if (actionName.equals(EXIT_OPERATION)) {
            exit();
        }
        return null;
    }

    public MBeanOperationInfo[] createOperInfo() {
        MBeanOperationInfo[] opers = {
                new MBeanOperationInfo(EXIT_OPERATION, "exits Impulse", null, null, MBeanOperationInfo.ACTION)
        };
        return opers;
    }

    @Override
    public void setAttribute(Attribute attribute)
            throws AttributeNotFoundException, InvalidAttributeValueException,
            MBeanException, ReflectionException {
        JMXUtils.setAttribute(Properties.values(), attribute);
    }

    @Override
    public AttributeList setAttributes(AttributeList attributes) {
        return null;
    }

    static public String format(Object obj) {
        if (obj == null) {
            return "null";
        }
        if (obj instanceof ByteBuffer) {
            return GlobalByteArrayFormatter.format(((ByteBuffer)obj));
        } else if (obj instanceof byte[]) {
            return GlobalByteArrayFormatter.format(((byte[])obj));
        } else if (obj instanceof Map<?, ?>) {
            Map<?, ?> data = (Map<?, ?>)obj;
            StringBuilder bldr = new StringBuilder();
            bldr.append('{');
            Iterator<?> it = data.keySet().iterator();
            while(it.hasNext()) {
                Object key = it.next();
                Object value = data.get(key);
                bldr.append(format(key));
                bldr.append('=');
                bldr.append(format(value));
                if (it.hasNext()) {
                    bldr.append(", ");
                }
            }
            bldr.append('}');
            return bldr.toString();
        }
        return String.valueOf(obj);
    }
    
    /**
     * 
     */
    static private ReactorThread Reactor = null;
 
    static public ReactorThread getReactorThread() throws IOException {
    	if (Reactor == null) {
    		Reactor = new ReactorThread();
			Reactor.start();
    	}
    	return Reactor;
    }
    
    static private ExecutorService TranformExecutor = null;

    static public ExecutorService getTransformExecutorService() {
        if (TranformExecutor == null) {
            TranformExecutor = Executors.newSingleThreadExecutor();
        }
        return TranformExecutor;
    }

    /**
     * 
     */
    static private ExecutorService EventExecutor = null;

    static public ExecutorService getEventExecutorService() {
    	if (EventExecutor == null) {
    		EventExecutor = Executors.newSingleThreadExecutor();
    	}
    	return EventExecutor;
    }

    static public boolean validateLogLevel(String logLevel) {
        return ((logLevel.equalsIgnoreCase(Level.ALL.toString()))  || 
            (logLevel.equalsIgnoreCase(Level.DEBUG.toString())) || 
            (logLevel.equalsIgnoreCase(Level.ERROR.toString())) ||
            (logLevel.equalsIgnoreCase(Level.FATAL.toString())) ||
            (logLevel.equalsIgnoreCase(Level.INFO.toString())) ||
            (logLevel.equalsIgnoreCase(Level.OFF.toString())) ||
            (logLevel.equalsIgnoreCase(Level.TRACE.toString())));
    }

    /**
     * Maps Transform name to Transform Context
     */
    final static private TreeMap<String, TransformContext> TransformContexts = new TreeMap<String, TransformContext>();
        
    static public TransformContext getTransformContext(String transformName) {
        return TransformContexts.get(transformName);
    }
    
    /**
     * Maps Transform.getClass().getName() to a TransformDefinition
     */
    final static private HashMap<String, TransformDefinition> TransformDefinitionMap = new HashMap<String, TransformDefinition>();

    static public TransformDefinition getTransformDefinition(String transformClassname) {
        return TransformDefinitionMap.get(transformClassname);
    }

    final static public CountDownLatch ExitLatch = new CountDownLatch(1);
    
    static private void readTransforms() throws Exception {
        Iterator<Object> it = ConfigProperties.keySet().iterator();
        while (it.hasNext()) {
            String key = (String)it.next();
            String value = (String)ConfigProperties.get(key); 
            if (value != null) {
                if (key.startsWith("transform.")) {
                    String[] keyParts = key.split("\\.");
                    if ((keyParts.length == 3) && (keyParts[2].equals(TRANSFORM_CLASS_PROPERTY))) {
                        try {
                            String transformName = keyParts[1];
                            String classname = value.trim();
                            TransformContext transformCtx = TransformContexts.get(transformName);
                            if (transformCtx == null) {
                                transformCtx = new TransformContext(transformName);
                                TransformContexts.put(transformName, transformCtx);
                            } else if (transformCtx.getTransformDefinition() == null) {
                                // ok
                            } else {
                                throw new IllegalArgumentException(String.format("Found duplicate transform %s for classes %s and %s", 
                                        transformName, transformCtx.getTransformDefinition().getTransformClass().getName(), classname));
                            }
                            TransformDefinition transformDef = TransformDefinitionMap.get(classname);
                            if (transformDef == null) {
                                transformDef = new TransformDefinition(value.trim());
                                Log logger = LogFactory.getLog(transformDef.getTransformClass());
                                if (logger.isInfoEnabled()) {
                                    logger.info("Adding Transform Definition: " + transformDef);
                                }
                                TransformDefinitionMap.put(classname, transformDef);
                            }
                            transformCtx.setTransformDefinition(transformDef);                            
                        } catch (Exception e) {
                            if (Logger.isErrorEnabled()) {
                                Logger.error("Error reading line - " + key + "=" + value, e);
                            }
                            throw e;
                        }
                    }
                }
            }
        }
    }
    
    static private void readConnections() throws Exception {
        Iterator<Object> it = ConfigProperties.keySet().iterator();
        while (it.hasNext()) {
            String key = (String)it.next();
            String value = (String)ConfigProperties.get(key); 
            if ((value != null) && (value.trim().length() > 0)) {
                if (key.startsWith("link.out.")) {
                    try {
                        String[] keyParts = key.substring(9).split("\\.");
                        if (keyParts.length == 2) {
                            String outTransformName = keyParts[0];
                            String outPortName = keyParts[1];
                            TransformContext outTransformCtx = TransformContexts.get(outTransformName);
                            if (outTransformCtx == null) {
                                throw new IllegalArgumentException(String.format("Unknown transform definition %s", outTransformName));
                            }

                            String[] paramList = value.split(";");
                            for(int j = 0; j < paramList.length; j++) {
                                // parse javascript
                                String[] jsArgs = paramList[j].split(":");
                                if (jsArgs[0].trim().toLowerCase().equalsIgnoreCase("javascript")) {
                                    String jsFilename = null;
                                    String jsFunction = null;
                                    if (jsArgs.length != 3) {
                                        throw new IllegalArgumentException(String.format("Input Port Definition %s has invalid javascript definition {javascript:filename:function}", paramList[j]));
                                    } else {
                                        jsFilename = jsArgs[1].trim();
                                        jsFunction = jsArgs[2].trim();
                                    }
                                    File jsFile = new File(jsFilename);
                                    if (!jsFile.exists()) {
                                        throw new IllegalArgumentException(String.format("Input Port Definition %s references a javascript file that doesn't exist - %s", paramList[j], jsFilename));
                                    }
                                    JavascriptDefinition jsCtx = new JavascriptDefinition();
                                    jsCtx.file = jsFile;
                                    jsCtx.function = jsFunction;
                                    outTransformCtx.addJavascriptDefinition(outPortName, jsCtx);
                                } else {
                                    // parse the arguments
                                    int startBracket = paramList[j].indexOf('(');
                                    int endBracket = paramList[j].indexOf(')', startBracket);
                                    if ((startBracket == -1) || (endBracket == -1)) {
                                        throw new IllegalArgumentException(String.format("Input Port Definition %s has no or incomplete argument brackets", paramList[j]));
                                    }
                                    String inputPortStr = paramList[j].substring(0, startBracket);
                                    String[] inputPortArgsStr = paramList[j].substring(startBracket+1, endBracket).split(",");
                                    ArrayList<String> argList = new ArrayList<String>();
                                    for(int i = 0; i < inputPortArgsStr.length; i++) {
                                        String arg = inputPortArgsStr[i].trim();
                                        if (arg.length() > 0) {
                                            argList.add(arg);
                                        }
                                    }
                                    String[] inputArgs = new String[argList.size()];
                                    for(int i = 0; i < argList.size(); i++) {
                                        inputArgs[i] = argList.get(i);
                                    }

                                    String[] valueParts = inputPortStr.split("\\.");
                                    if (valueParts.length == 2) {
                                        String inTransformName = valueParts[0].trim();
                                        String inPortName = valueParts[1].trim();
                                        TransformContext inTransformCtx = TransformContexts.get(inTransformName);
                                        if (inTransformCtx == null) {
                                            throw new IllegalArgumentException(String.format("Unknown transform definition %s", inTransformName));
                                        }
                                        TransformDefinition outTransformDef = outTransformCtx.getTransformDefinition();
                                        PortDefinition outPortDef = outTransformDef.getOutputPortDefinition(outPortName);
                                        if (outPortDef == null) {
                                            throw new IllegalArgumentException(String.format("Output Port Definition %s not found", outPortName));
                                        }
                                        TransformDefinition inTransformDef = inTransformCtx.getTransformDefinition();
                                        PortDefinition inPortDef = (PortDefinition)inTransformDef.getInputPortDefinition(inPortName);
                                        if (inPortDef == null) {
                                            throw new IllegalArgumentException(String.format("Input Port Definition %s not found", inPortName));
                                        }
                                        validateArguments(inputArgs, inPortDef);
                                        ConnectionDefinition connectionDef = new ConnectionDefinition(
                                                outTransformName, outTransformDef, outPortDef,
                                                inTransformName, inTransformDef, inPortDef, inputArgs);
                                        Log outLogger = LogFactory.getLog(outTransformDef.getTransformClass());
                                        Log inLogger = LogFactory.getLog(inTransformDef.getTransformClass());
                                        if (outLogger.isInfoEnabled()) {
                                            outLogger.info("Adding Connection Definition: " + connectionDef);
                                        }
                                        if (inLogger.isInfoEnabled()) {
                                            inLogger.info("Adding Connection Definition: " + connectionDef);
                                        }
                                        outTransformCtx.addConnectionDefinition(connectionDef);
                                    } else {
                                        throw new IllegalArgumentException(String.format("link.out property must have 2 part value, %d parts found", valueParts.length));
                                    }
                                }
                            }
                        } else {
                            throw new IllegalArgumentException(String.format("Expected 4 part link.out property, %d parts found", keyParts.length + 2));
                        }
                    } catch(Exception e) {
                        if (Logger.isErrorEnabled()) {
                            Logger.error("Error reading line - " + key + "=" + value, e);
                        }
                        throw e;
                    }
                }
            }
        }
    }
    
    static private void validateTransformContexts() {
        Iterator<String> it = TransformContexts.keySet().iterator();
        while(it.hasNext()) {
            String transformName = it.next();
            TransformContext transformCtx = TransformContexts.get(transformName);
            if (transformCtx.getTransformDefinition() == null) {
                throw new IllegalArgumentException("Incomplete transform specification \"" + transformName + "\"");
            }
        }
    }

    static private void initTransformContextProperties() throws PropertyVetoException, PropertyConversionException {
        Iterator<String> it = TransformContexts.keySet().iterator();
        while(it.hasNext()) {
            String transformName = it.next();
            TransformContext transformCtx = TransformContexts.get(transformName);
            transformCtx.initProperties();
        }
    }

    static private void registerTransformContexts() throws InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException, MalformedObjectNameException, NullPointerException, PropertyVetoException, PropertyConversionException {
        Iterator<String> it = TransformContexts.keySet().iterator();
        while(it.hasNext()) {
            String transformName = it.next();
            TransformContext transformCtx = TransformContexts.get(transformName);
            transformCtx.registerMBean();
        }
    }

    static private void startTransforms() throws Exception {
        Iterator<String> it = TransformContexts.keySet().iterator();
        while(it.hasNext()) {
            String transformName = it.next();
            TransformContext transformCtx = TransformContexts.get(transformName);
            if (transformCtx.autoStart()) {
                TransformCreationContext createCtx = new TransformCreationContext();
                Transform transform = createCtx.getInstance(transformCtx);
                GlobalTransformMap.put(transform.getTransformName(), transform);
            }
        }
    }
    
    static private void validateArguments(String[] inputArgs, PortDefinition inPortDef) {
        Class<?>[] argTypes = inPortDef.getMethod().getParameterTypes();
        int inputArgIndex = 0;
        for(int i = 0; i < inputArgs.length; i++) {
            String arg = inputArgs[i];
            if (arg.indexOf('"') != -1) {
                throw new IllegalArgumentException(String.format("Illegal character in argument \"%s\", arguments must not have quotes", arg));
            }
            String[] argParts = arg.split("\\.");
            if (argParts.length != 3) {
                throw new IllegalArgumentException(String.format("Illegal argument \"%s\", arguments must have 3 period seperated parts", arg));
            }
            String transformName = argParts[0];
            String name = argParts[2];
            Iterator<TransformDefinition> it = TransformDefinitionMap.values().iterator();
            boolean found = false;
            Object[] defArgName = null;
            while(it.hasNext()) {
                TransformDefinition def = it.next();
                if (transformName.equals(def.getTransformClass().getSimpleName())) {
                    defArgName = def.getArgumentName(name);
                    if (defArgName == null) {
                        throw new IllegalArgumentException(String.format("Argument %s doesn't exist in transform %s", arg, def.getTransformClass().getSimpleName()));
                    } else {
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                throw new IllegalArgumentException(String.format("Transform %s not declared for argument %s", transformName, arg));
            }
            Class<?> defArgClass = (Class<?>)defArgName[1];
            while((argTypes != null) && (inputArgIndex < argTypes.length)) {
                if (((argTypes[inputArgIndex].isAssignableFrom(Connection.class)) || (argTypes[inputArgIndex].isAssignableFrom(Map.class)))) {
                    inputArgIndex++;
                } else {
                    if (argTypes[inputArgIndex].isAssignableFrom(defArgClass)) {
                        inputArgIndex++;                     break;
                    } else {
                        throw new IllegalArgumentException(String.format("Expecting input argument of type %s", argTypes[inputArgIndex].getName()));
                    }
                }
            }
        }
    }

    static public String getTCPTransportClientAddressString(Transport transport) {
        if (transport != null) {
            InetSocketAddress localAddr = transport.getClientLocalAddress();
            InetSocketAddress remoteAddr = transport.getClientRemoteAddress();
            if ((localAddr != null) && (remoteAddr != null)) {
                return "[" + remoteAddr.toString() + "] -> [" + localAddr.toString() + "]";
            }
        }
        return "";
    }

    static public String getTCPTransportServerAddressString(Transport transport) {
        if (transport != null) {
            InetSocketAddress localAddr = transport.getServerLocalAddress();
            InetSocketAddress remoteAddr = transport.getServerRemoteAddress();
            if ((localAddr != null) && (remoteAddr != null)) {
                return "[" + localAddr.toString() + "] -> [" + remoteAddr.toString() + "]";
            }
        }
        return "";
    }
    
    @SuppressWarnings("unchecked")
    static public boolean logStackTrace() {
        return ((Property<Boolean>)Properties.get(LOG_STACK_TRACE_PROPERTY)).getValue();
    }
    
    static public void exit() {
        if (Logger.isInfoEnabled()) {
            Logger.info("Exiting");
        }
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {}
        ExitLatch.countDown();
    }
    
    /**
     * @param args
     */
    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
    	System.out.println("Impulse Version " + Version + ", " + BuildDate);
        final String syntax = "Syntax: Impulse [-Dkey=value ...] configFile";
        try {
            String configFileName = null;
            if (args.length == 0) {
                configFileName = System.getProperty("configFile");
                if (configFileName == null) {
                    System.out.println(syntax);
                    System.exit(0);
                }
            } else if (args.length == 1) {
            	configFileName = args[0];
            } else {
                for(int i = 0; i < args.length; i++) {
                    if (args[i].startsWith("-D")) {
                        String[] prop = args[i].substring(2).split("=");
                        if (prop.length == 2) {
                            if (prop[0].length() == 0) {
                                System.out.println(String.format("Error: Invalid property format \"%s\", must be in the format \"-Dkey=value\"", args[i]));
                                System.exit(0);
                            } else {
                                System.setProperty(prop[0], prop[1]);
                            }
                        } else {
                            System.out.println(String.format("Error: Invalid property format \"%s\", must be in the format \"-Dkey=value\"", args[i]));
                            System.exit(0);
                        }
                    } else {
                        if (configFileName == null) {
                            configFileName = args[i];
                        } else {
                            System.out.println(String.format("Error: Multiple config files specified \"%s\" and \"%s\"", configFileName, args[i]));
                            System.exit(0);
                        }
                    }
                }
                if (configFileName == null) {
                    System.out.println(syntax);
                    System.out.println("Error: No config file specified");
                    System.exit(0);
                }
            }
            File configFile = new File(configFileName);
            if (Logger.isInfoEnabled()) {
                Logger.info("Impulse Version " + Version);
                Logger.info("Loading config file \"" + configFile.getAbsolutePath() + "\"");
            }
            try {
                ((Property<String>)Properties.get(CONFIG_FILE_PROPERTY)).setValue(configFile.getAbsolutePath());
            } catch (Exception e) {}
            
            FileReader configReader = new FileReader(configFile);
            ConfigProperties.load(configReader);
            readTransforms();
            validateTransformContexts();
            readConnections();
            initTransformContextProperties();
            OnlyInstance.initProperties();
            registerTransformContexts();
            if (Logger.isInfoEnabled()) {
                Logger.info("Starting transforms");
            }
            startTransforms();            
            if (Logger.isInfoEnabled()) {
                Logger.info("Running......");
            }
            ExitLatch.await();
            System.exit(0);
        } catch(Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }
}
