package com.grb.impulse;

import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.grb.util.property.JMXUtils;
import com.grb.util.property.Property;
import com.grb.util.property.PropertyConversionException;
import com.grb.util.property.PropertySource;
import com.grb.util.property.PropertyVetoException;
import com.grb.util.property.impl.MapPropertySource;
import com.grb.util.property.impl.SystemPropertySource;
import com.grb.util.property.impl.ValueOfConverter;


public class TransformContext implements DynamicMBean {
    /**
     * Whether to start the transform on startup.
     */
    static public final String TRANSFORM_AUTO_START_PROPERTY = "autoStart";
    
    /**
     * Default value for auto starting transforms.
     */
    static public final boolean DEFAULT_TRANSFORM_AUTO_START = false;

    /**
     * Transform Factory class name.
     */
    static public final String TRANSFORM_FACTORY_CLASS_PROPERTY = "factoryClassname";

    /**
     * A read only property containing the number of times the transform has been called.
     */
    static public final String CALLED_PROPERTY = "called";

    /**
     * A read only property containing the Log4J effective log level.
     */
    static public final String LOG4J_EFFECTIVE_LOG_LEVEL_PROPERTY = "log4jEffectiveLogLevel";

    /**
     * The Log4J log level for this transform. If this is not set, the 
     * log level is the Log4J effective log level.
     */
    static public final String LOG4J_LOG_LEVEL_PROPERTY = "log4jLogLevel";

    /**
     * The client side IP address. 
     */
    static public final String CLIENT_ADDRESS = "clientAddress";

    /**
     * The server side IP address.
     */
    static public final String SERVER_ADDRESS = "serverAddress";
    
    private String _name;
    private TransformDefinition _transformDef;
    private ArrayList<ConnectionDefinition> _connectionDefs;
    private Property<Boolean> _autoStart;
    private Property<String> _transformFactoryClassname;
    private TransformFactory _transformFactory;
    private Map<String, Property<?>> _properties;
    private MBeanInfo _mBeanInfo;
    private ArrayList<String> _connectionsStringArray;   // for JMX
    private Property<String[]> _connectionsStrings;
    private String _loggerClass;
    private Log _logger;

    /**
     * Javascript Extension
     */
    private HashMap<String, JavascriptDefinition> _javascriptDefs;

    @SuppressWarnings("unchecked")
    public TransformContext(String name) throws PropertyVetoException, PropertyConversionException, ClassNotFoundException, IllegalArgumentException, SecurityException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        _name = name;
        _transformDef = null;
        _connectionDefs = null;
        _properties = new TreeMap<String, Property<?>>();

        _autoStart = new Property<Boolean>(TRANSFORM_AUTO_START_PROPERTY, DEFAULT_TRANSFORM_AUTO_START, true,
                new SystemPropertySource<Boolean>(getPropertyString(TRANSFORM_AUTO_START_PROPERTY), PropertySource.PRIORITY_1, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Boolean.class)),
                new MapPropertySource<Boolean>(Impulse.CONFIG_FILE_SOURCE, getPropertyString(TRANSFORM_AUTO_START_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_2, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Boolean.class)));
        _autoStart.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "Whether to start the transform on startup");
        _properties.put(_autoStart.getId(), _autoStart);
        
        _transformFactoryClassname = new Property<String>(TRANSFORM_FACTORY_CLASS_PROPERTY, DefaultTransformFactory.class.getName(), true,
                new SystemPropertySource<String>(getPropertyString(TRANSFORM_FACTORY_CLASS_PROPERTY), PropertySource.PRIORITY_1, PropertySource.NULL_INVALID),
                new MapPropertySource<String>(Impulse.CONFIG_FILE_SOURCE, getPropertyString(TRANSFORM_FACTORY_CLASS_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_2, PropertySource.NULL_INVALID));
        _transformFactoryClassname.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "Transform factory classname");
        _properties.put(_transformFactoryClassname.getId(), _transformFactoryClassname);
        
        Property<Integer> calledProp = new Property<Integer>(CALLED_PROPERTY, 0);
        calledProp.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "A read only property containing the number of times the transform has been called");
        _properties.put(calledProp.getId(), calledProp);
        
        _connectionsStrings = new Property<String[]>("connections");
        _connectionsStrings.setType(String[].class);
        _connectionsStrings.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "Downstream connections from this transform");
        _properties.put(_connectionsStrings.getId(), _connectionsStrings);
        
        Property<String> effectiveLogLevel = new Property<String>(LOG4J_EFFECTIVE_LOG_LEVEL_PROPERTY);
        effectiveLogLevel.setType(String.class);
        effectiveLogLevel.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "A read only property containing the Log4J effective log level");
        _properties.put(effectiveLogLevel.getId(), effectiveLogLevel);
        
        Property<String> logLevel = new Property<String>(LOG4J_LOG_LEVEL_PROPERTY, "",
                new PropertySource<String>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1, PropertySource.NULL_INVALID),
                new SystemPropertySource<String>(getPropertyString(LOG4J_LOG_LEVEL_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID),
                new MapPropertySource<String>(Impulse.CONFIG_FILE_SOURCE, getPropertyString(LOG4J_LOG_LEVEL_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID),
                new SystemPropertySource<String>(SystemPropertySource.PROPERTY_NAME + " (wildcard)", getWildcardPropertyString(LOG4J_LOG_LEVEL_PROPERTY), PropertySource.PRIORITY_4, PropertySource.NULL_INVALID),
                new MapPropertySource<String>(Impulse.CONFIG_FILE_SOURCE + " (wildcard)", getWildcardPropertyString(LOG4J_LOG_LEVEL_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_5, PropertySource.NULL_INVALID));
        logLevel.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "The Log4J log level for this transform. If this is not set, the log level is the Log4J effective log level");
        _properties.put(logLevel.getId(), logLevel);

        Property<String> clientIPProp = new Property<String>(CLIENT_ADDRESS, "");
        clientIPProp.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "A read only property containing the client's IP address");
        _properties.put(clientIPProp.getId(), clientIPProp);

        Property<String> serverIPProp = new Property<String>(SERVER_ADDRESS, "");
        serverIPProp.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "A read only property containing the server's IP address");
        _properties.put(serverIPProp.getId(), serverIPProp);

        Class<?> transformFactoryClass = Class.forName(_transformFactoryClassname.getValue().trim());
        _transformFactory = (TransformFactory)transformFactoryClass.getConstructor(TransformContext.class).newInstance(this);
        _connectionsStringArray = new ArrayList<String>();
        
        _loggerClass = null;
        _logger = null;
        _javascriptDefs = null;
    }
    
    public String getName() {
        return _name;
    }
    
    public Map<String, Property<?>> getProperties() {
        return _properties;
    }
    
    public String getPropertyString(String propName) {
        return "transform." + _name + "." + propName;
    }

    public String getWildcardPropertyString(String propName) {
        return "transform.*." + propName;
    }

    @SuppressWarnings("unchecked")
    public void setTransformDefinition(TransformDefinition transformDef) {
        _transformDef = transformDef;
        try {
            Property<String> effectiveLogLevel = (Property<String>)_properties.get(LOG4J_EFFECTIVE_LOG_LEVEL_PROPERTY);
            Level level = LogManager.getLogger(_transformDef.getTransformClass()).getEffectiveLevel();
            effectiveLogLevel.setValue(level.toString());

            Property<String> logLevel = (Property<String>)_properties.get(LOG4J_LOG_LEVEL_PROPERTY);
            PropertySource<String> logLevelJMXSource = logLevel.getSource(JMXUtils.JMX_SOURCE);
            level = LogManager.getLogger(_transformDef.getTransformClass()).getLevel();
            if (level == null) {
                logLevelJMXSource.setValue(null);
            } else {                
                logLevelJMXSource.setValue(level.toString());
            }
        } catch (Exception e) {
            // should never happen
            e.printStackTrace();
        }
    }
    
    public TransformDefinition getTransformDefinition() {
        return _transformDef;
    }
    
    public void addConnectionDefinition(ConnectionDefinition connectionDef) {
        if (_connectionDefs == null) {
            _connectionDefs = new ArrayList<ConnectionDefinition>();
        }
        _connectionDefs.add(connectionDef);
        _connectionsStringArray.add(connectionDef.toSimpleString());
        JavascriptDefinition jsDef = connectionDef.getJavascriptDefinition();
        if (jsDef != null) {
            if (_javascriptDefs == null) {
                _javascriptDefs = new HashMap<String, JavascriptDefinition>();
            }
            _javascriptDefs.put(jsDef.file.getAbsolutePath(), jsDef);
        }
    }
    
    public List<ConnectionDefinition> getConnectionDefinitions() {
        return _connectionDefs;
    }
    
    public boolean autoStart() {
        return _autoStart.getValue();
    }
    
    public TransformFactory getTransformFactory() {
        return _transformFactory;
    }

    public JavascriptDefinition[] getJavascriptDefinitions() {
        if (_javascriptDefs != null) {
            JavascriptDefinition[] defs = new JavascriptDefinition[_javascriptDefs.size()];
            defs = _javascriptDefs.values().toArray(defs);
            return defs;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public void initProperties() throws PropertyVetoException, PropertyConversionException {
        try {
            Method propMethod = getTransformDefinition().getTransformClass().getMethod("getProperties", TransformContext.class);
            propMethod.invoke(null, this);
        } catch (NoSuchMethodException e) {
            if (_transformDef.getLogger().isDebugEnabled()) {
                _transformDef.getLogger().debug("Transform: " + e.getClass().getSimpleName() + " does not implement the getProperties() method");
            }            
        } catch (Exception e) {
            if (_transformDef.getLogger().isErrorEnabled()) {
                _transformDef.getLogger().error("Could not get properties from transform: " + e.getClass().getSimpleName());
            }            
        }
        // initialze and make sure all mandatory properties set
        String[] connectionStrings = new String[_connectionsStringArray.size()];
        connectionStrings = _connectionsStringArray.toArray(connectionStrings);
        _connectionsStrings.setValue(connectionStrings);
        _loggerClass = _transformDef.getTransformClass().getName() + "." + _name;
        _logger = LogFactory.getLog(_loggerClass);

        // initialize the log4jlevel first
        Property<String> logLevel = (Property<String>)_properties.get(LOG4J_LOG_LEVEL_PROPERTY);
        try {
            logLevel.initialize();
            if (logLevel.isSet()) {
                LogManager.getLogger(_loggerClass).setLevel(Level.toLevel(logLevel.getValue()));
                // set the effective log level
                Property<String> effectiveLogLevel = (Property<String>)_properties.get(LOG4J_EFFECTIVE_LOG_LEVEL_PROPERTY);
                Level level = LogManager.getLogger(_loggerClass).getEffectiveLevel();
                effectiveLogLevel.setValue(level.toString());
            } else {
                Level level = LogManager.getLogger(_loggerClass).getLevel();
                if (level != null) {
                    logLevel.setDefaultValue(level.toString());
                }
            }    
        } catch(PropertyVetoException e) {
            if (_logger.isErrorEnabled()) {
                _logger.error(String.format("Property: Error initializing property %s for transform %s (%s)", 
                        getPropertyString(logLevel.getId()), getTransformDefinition().getTransformClass().getSimpleName(), getName()));
            }            
            throw e;
        } catch(PropertyConversionException e) {
            if (_logger.isErrorEnabled()) {
                _logger.error(String.format("Property: Error converting property %s for transform %s (%s)", 
                        getPropertyString(logLevel.getId()), getTransformDefinition().getTransformClass().getSimpleName(), getName()));
            }            
            throw e;
        }
        
        Iterator<Property<?>> it = _properties.values().iterator();
        while(it.hasNext()) {
            Property<?> prop = it.next();
            PropertyChangedLogger pcl = new PropertyChangedLogger(_logger);
            if (!prop.getId().equals(CALLED_PROPERTY)) {
                ((Property<Object>)prop).addListener(pcl);
            }
            try {
                prop.initialize();
            } catch(PropertyVetoException e) {
                if (_logger.isErrorEnabled()) {
                    _logger.error(String.format("Property: Error initializing property %s for transform %s (%s)", 
                            getPropertyString(prop.getId()), getTransformDefinition().getTransformClass().getSimpleName(), getName()));
                }            
                throw e;
            } catch(PropertyConversionException e) {
                if (_logger.isErrorEnabled()) {
                    _logger.error(String.format("Property: Error converting property %s for transform %s (%s)", 
                            getPropertyString(prop.getId()), getTransformDefinition().getTransformClass().getSimpleName(), getName()));
                }            
                throw e;
            }
                        
            if (_logger.isDebugEnabled()) {
                String description = (prop.getUserDataMap().get(Impulse.PROPERTY_DESCRIPTION_KEY) == null) ? "" : (String)prop.getUserDataMap().get(Impulse.PROPERTY_DESCRIPTION_KEY);
                _logger.debug(String.format("Property: %s (%s)", prop.toKeyValueString(), description));
                _logger.debug(prop);
            } else if (_logger.isInfoEnabled()) {
                _logger.info("Property: " + prop.toKeyValueString());
            }            
            if ((!prop.hasDefaultValue()) && (!prop.isSet())) {
                String errorText = String.format("Property: Missing mandatory property %s for transform %s (%s)", 
                        getPropertyString(prop.getId()), getTransformDefinition().getTransformClass().getSimpleName(), getName());
                if (_logger.isErrorEnabled()) {
                    _logger.error(errorText);
                }            
                throw new IllegalArgumentException(errorText);
            }
        }
    }
    
    public void registerMBean() throws InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException, MalformedObjectNameException, NullPointerException {
        ManagementFactory.getPlatformMBeanServer().registerMBean(this, new ObjectName(getTransformDefinition().getTransformClass().getName() + ":name=" + getName())); 
    }

    @Override
    public Object getAttribute(String attribute)
            throws AttributeNotFoundException, MBeanException,
            ReflectionException {
        if (attribute.equals(TransformContext.LOG4J_EFFECTIVE_LOG_LEVEL_PROPERTY)) {
            Level level = LogManager.getLogger(_loggerClass).getEffectiveLevel();
            if (level == null) {
                return null;
            } else {
                return level.toString();
            }
        } else if (attribute.equals(TransformContext.LOG4J_LOG_LEVEL_PROPERTY)) {
            Level level = LogManager.getLogger(_loggerClass).getLevel();
            if (level == null) {
                return null;
            } else {
                return level.toString();
            }
        }
        return JMXUtils.getAttribute(_properties.values(), attribute);
    }

    @Override
    public AttributeList getAttributes(String[] attributes) {
        return null;
    }

    @Override
    public MBeanInfo getMBeanInfo() {
        if (_mBeanInfo == null) {
            _mBeanInfo = new MBeanInfo(getClass().getName(), getClass().getSimpleName(), JMXUtils.createAttrInfo(_properties.values()), null, null, null);        
        }
        return _mBeanInfo;
    }

    @Override
    public Object invoke(String actionName, Object[] params, String[] signature)
            throws MBeanException, ReflectionException {
        return null;
    }

    @Override
    public void setAttribute(Attribute attribute)
            throws AttributeNotFoundException, InvalidAttributeValueException,
            MBeanException, ReflectionException {
        if (attribute.getName().equals(TransformContext.LOG4J_LOG_LEVEL_PROPERTY)) {
            if (Impulse.validateLogLevel((String)attribute.getValue())) {
                Logger logger = LogManager.getLogger(_loggerClass);
                logger.setLevel(Level.toLevel((String)attribute.getValue()));
            } else {
                throw new InvalidAttributeValueException(String.format("Illegal log level \"%s\"", (String)attribute.getValue()));
            }
        } else {
            JMXUtils.setAttribute(_properties.values(), attribute);
        }
    }

    @Override
    public AttributeList setAttributes(AttributeList attributes) {
        return null;
    }

    @Override
    public String toString() {
        StringBuilder bldr = new StringBuilder();
        bldr.append("name=");
        bldr.append(_name);
        bldr.append(", autoStart=");
        bldr.append(_autoStart);
        bldr.append(", definition=");
        bldr.append(_transformDef);
        return bldr.toString();
    }    
}
