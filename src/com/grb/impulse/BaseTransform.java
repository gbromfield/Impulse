package com.grb.impulse;

import java.lang.management.ManagementFactory;
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
import javax.management.MBeanOperationInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.script.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.grb.util.logging.LoggingContext;
import com.grb.util.property.JMXUtils;
import com.grb.util.property.Property;
import com.grb.util.property.PropertyConversionException;
import com.grb.util.property.PropertyVetoException;


/**
 * Base transform class with methods helpful for transform development.
 */
public class BaseTransform implements Transform, LoggingContext, DynamicMBean {
	protected String _loggerName;
    protected String _transformName;
	protected String _instanceName;
	protected TransformCreationContext _transformCreationContext;
	protected Log _logger;
	protected TreeMap<String, ArrayList<Connection>> _connections;
	protected Map<String, Property<?>> _properties;
	protected MBeanInfo _mBeanInfo;
	protected ObjectName _objectName;
	protected boolean _disposed;
	protected Transport _transport;
	
	/**
	 * BaseTransform constructor.
	 * 
	 * @param transformName Name of the transform.
	 * @param instanceName Name of the transform instance.
	 * @param transformCreationContext Context to create the transform in.
	 */
    protected BaseTransform(String transformName, String instanceName, TransformCreationContext transformCreationContext) {
    	_loggerName = null;
        _transformName = transformName;
        _instanceName = instanceName;
        _transformCreationContext = transformCreationContext;
        _logger = null;
        _connections = new TreeMap<String, ArrayList<Connection>>();
        _properties = null;
        _mBeanInfo = null;
        _objectName = null;
        _disposed = false;
        _transport = null;
        
        Map<String, Transform> transformMap = transformCreationContext.getTransformMap();
        if (transformMap != null) {
            Iterator<Transform> it = transformMap.values().iterator();
            while(it.hasNext()) {
                Transform transform = it.next();
                if (transform instanceof Transport) {
                    _transport = (Transport)transform;
                    break;
                }
            }
        }
    }

	public String getTransformName() {
		return _transformName;
	}

	public String getInstanceName() {
	    return _instanceName;
	}
	
	/**
	 * Gets the transform's creation context.
	 * 
	 * @return the transform's creation context.
	 */
	public TransformCreationContext getTransformCreationContext() {
	    return _transformCreationContext;
	}
	
	/**
	 * Gets the transform's context.
	 * 
	 * @return the transform's context.
	 */
	public TransformContext getTransformContext() {
		return Impulse.getTransformContext(_transformName);
	}

	/**
	 * Gets a transform's context given its name.
	 * 
	 * @param transformName Name of the transform to lookup.
	 * @return a transform's context given its name.
	 */
	static public TransformContext getTransformContext(String transformName) {
		return Impulse.getTransformContext(transformName);
	}

	@SuppressWarnings("unchecked")
    @Override
    public void init() throws Exception {
		_loggerName = getClass().getName() + "." + getTransformName() + "." + getInstanceName();
		_logger = LogFactory.getLog(_loggerName);
		if (_logger.isInfoEnabled()) {
			_logger.info("Initializing Transform");
		}
				
		// get connections
		TransformContext transformCtx = getTransformContext();
		if (transformCtx != null) {   // could be null in testing
	        List<ConnectionDefinition> connectionDefs = transformCtx.getConnectionDefinitions();
	        if (connectionDefs != null) {
	            Iterator<ConnectionDefinition> it = connectionDefs.iterator();
	            while(it.hasNext()) {
	                ConnectionDefinition connectionDef = it.next();
	                addConnection(connectionDef);
	            }
	        }
	        _properties = new TreeMap<String, Property<?>>();
	        Iterator<Property<?>> it = transformCtx.getProperties().values().iterator();
            PropertyChangedLogger pcl = new PropertyChangedLogger(_logger);
	        while(it.hasNext()) {
	            Property<?> prop = it.next();
	            Property<?> clone = (Property<?>)prop.clone();
	            if (!clone.getId().equals(TransformContext.CALLED_PROPERTY)) {
	                clone.clearListeners();
	                ((Property<Object>)clone).addListener(pcl);
	            }
	            _properties.put(clone.getId(), clone);
	            if (clone.getId().equals(TransformContext.LOG4J_LOG_LEVEL_PROPERTY)) {
	                Level level = LogManager.getLogger(_loggerName).getLevel();
	                if (level != null) {
	                    ((Property<String>)clone).setDefaultValue(level.toString());
	                }
	            } else if (clone.getId().equals(TransformContext.CALLED_PROPERTY)) {
	                ((Property<Integer>)clone).setValue(0);
	            }
	        }
	        registerMBean();
		}
	}

	@Override
	public void start() throws Exception {
		if (_logger.isInfoEnabled()) {
			_logger.info("Starting Transform");
		}
	}

	@Override
	public Log getLog() {
		return _logger;
	}

    @Override
    public String formatLog(String msg) {
        return msg;
    }

	public Property<?> getProperty(String propName) {
		return _properties.get(propName);
	}

	@SuppressWarnings("unchecked")
	public boolean getBooleanProperty(String propName) {
		return ((Property<Boolean>)getProperty(propName)).getValue();
	}

	@SuppressWarnings("unchecked")
	public int getIntegerProperty(String propName) {
		return ((Property<Integer>)getProperty(propName)).getValue();
	}

	@SuppressWarnings("unchecked")
	public long getLongProperty(String propName) {
		return ((Property<Long>)getProperty(propName)).getValue();
	}

	@SuppressWarnings("unchecked")
	public String getStringProperty(String propName) {
		return ((Property<String>)getProperty(propName)).getValue();
	}

	public String getPropertyString(String propName) {
	    return "transform." + getTransformName() + "." + propName;
	}
	
	public boolean hasOutputConnection(String outputPortName) {
        ArrayList<Connection> connections = _connections.get(outputPortName);
        if ((connections != null) && (connections.size() > 0)) {
            return true;
        }
        return false;
	}
	
	public List<Connection> getConnections(String outputPortName) {
	    return _connections.get(outputPortName);
	}
	
	/**
	 * Calls the next transform(s) that is connected to the given output port
	 * name. 
	 * 
	 * @param outputPortName Output port to the next transform.
	 * @param argMap Arguments from the transform input. Null if no input.
	 * @param args String/Object pairs for the argument map that are parameters for the next transform.
	 */
    public void next(String outputPortName, Map<String, Object> argMap, Object... args) {
        ConnectionContext inConnCtx = new ConnectionContext();
        ConnectionContext outConnCtx = inConnCtx;
        inConnCtx.log = _logger;
        inConnCtx.outputPortName = outputPortName;
        inConnCtx.argMap = argMap;
        inConnCtx.args = args;
        inConnCtx.connectionNames = null;
        ArrayList<Connection> connections = _connections.get(outputPortName);
        if ((connections != null) && (connections.size() > 0)) {
            inConnCtx.connectionNames = new String[connections.size()];
            for(int i = 0; i < connections.size(); i++) {
                Connection c = connections.get(i);
                inConnCtx.connectionNames[i] = c.getName();
            }
        }

        JavascriptDefinition jsDef = getTransformContext().getJavascriptDefinition(outputPortName);
        if (jsDef != null) {
            ScriptEngine engine = _transformCreationContext.getJavascriptEngine();
            Invocable inv = (Invocable) engine;
            try {
                if ((_logger != null) && (_logger.isDebugEnabled())) {
                    _logger.debug(String.format("About to execute javascript file %s, function %s", jsDef.file.getName(), jsDef.function));
                }
                Object tmpObj = inv.invokeFunction(jsDef.function, inConnCtx);
                if (!(tmpObj instanceof ConnectionContext)) {
                    if ((_logger != null) && (_logger.isErrorEnabled())) {
                        _logger.error(String.format("Exception executing javascript file %s, function %s, expecting a String[] return value got %s",
                                jsDef.file.getName(), jsDef.function, tmpObj.getClass().getName()));
                    }
                } else {
                    outConnCtx = (ConnectionContext)tmpObj;
                }
                if ((_logger != null) && (_logger.isDebugEnabled())) {
                    _logger.debug(String.format("Executed javascript file %s, function %s, returned %s",
                            jsDef.file.getName(), jsDef.function, outConnCtx));
                }
            } catch (Exception e) {
                if ((_logger != null) && (_logger.isErrorEnabled())) {
                    _logger.error(String.format("Exception executing javascript file %s, function %s",
                            jsDef.file.getName(), jsDef.function), e);
                }
            }
        }

        if ((outConnCtx.connectionNames != null) && (outConnCtx.connectionNames.length > 0)) {
			for(int i = 0; i < connections.size(); i++) {
				Connection c = connections.get(i);
                for(int j = 0; j < outConnCtx.connectionNames.length; j++) {
                    if (c.getName().equals(outConnCtx.connectionNames[j])) {
                        next(c, argMap, args);
                        break;
                    }
                }
            }
        }
    }

    /**
	 * Calls the next transform(s) using the given connection.
     * 
     * @param connection Connection to the next transform.
     * @param argMap Arguments from the transform input. Null if no input.
     * @param args String/Object pairs for the argument map that are parameters for the next transform.
     */
	public void next(Connection connection, Map<String, Object> argMap, Object... args) {
	    Transform inputTransform = null;
        try {
        	if (ConnectionLogger.isDebugEnabled()) {
        		ConnectionLogger.debug("About to follow connection " + connection);
        	}
        	// is it dynamic?
        	inputTransform = connection.getInputTransform();
        	if ((inputTransform == null) && (!connection.getConnectionDefinition().getInputPortDefinition().isStatic())) {
        		inputTransform = createTransform(connection.getConnectionDefinition());
        	}
        	// calculate args
            Object[] argsToUse = null;
            Method method = connection.getConnectionDefinition().getInputPortDefinition().getMethod();
            Class<?>[] argTypes = method.getParameterTypes();
            if ((argTypes != null) && (argTypes.length > 0)) {
                argsToUse = new Object[argTypes.length];
                Map<String, Object> newArgMap = null;
                int argIndex = 0;
                String[] inputArgs = connection.getConnectionDefinition().getInputArguments();
                boolean argsOverlap = overlaps(argMap, args);
                for(int i = 0; i < argTypes.length; i++) {
                    if (argTypes[i].isAssignableFrom(Connection.class)) {
                        argsToUse[i] = connection;
                    } else if (argTypes[i].isAssignableFrom(Map.class)) {
                        if (newArgMap == null) {
                            newArgMap = createNewArgMap(argMap, args);
                        }
                        argsToUse[i] = newArgMap;
                    } else {
                        if (inputArgs.length < (argIndex + 1)) {
                            throw new IndexOutOfBoundsException(String.format("The config file is missing an argument, cannot index %d (from the @Input method) in an input argument array size of %d", argIndex, inputArgs.length));
                        }
                        String key = inputArgs[argIndex++];
                        if ((argMap != null) && (argMap.containsKey(key)) && (!argsOverlap)) {
                            argsToUse[i] = argMap.get(key);
                        } else {
                            if (newArgMap == null) {
                                newArgMap = createNewArgMap(argMap, args);
                            }
                            if (!newArgMap.containsKey(key)) {
                                String argNames = newArgMap.keySet().toString();
                                throw new IllegalArgumentException(String.format("Cannot find argument %s (from the config file) in transform input arguments %s", key, argNames));
                            }
                            argsToUse[i] = newArgMap.get(key);
                        }
                    }
                }
                if (inputArgs.length != argIndex) {
                    throw new IndexOutOfBoundsException(String.format("The config file has too many arguments %d, expecting %d",  inputArgs.length, argIndex));
                }
            }
            if (inputTransform != null) {
                @SuppressWarnings("unchecked")
                Property<Integer> calledProp = (Property<Integer>)inputTransform.getProperty(TransformContext.CALLED_PROPERTY);
                if (calledProp != null) {
                    calledProp.setValue(calledProp.getValue() + 1);
                }
            }
            TransformContext inTransformCtx = getTransformContext(connection.getConnectionDefinition().getInputTransformName());               
            if (inTransformCtx != null) {
                @SuppressWarnings("unchecked")
                Property<Integer> calledProp = (Property<Integer>)inTransformCtx.getProperties().get(TransformContext.CALLED_PROPERTY);
                if (calledProp != null) {
                    calledProp.setValue(calledProp.getValue() + 1);
                }
            }
        	connection.getConnectionDefinition().getInputPortDefinition().getMethod().invoke(inputTransform, argsToUse);
        	if (ConnectionLogger.isDebugEnabled()) {
        		ConnectionLogger.debug("Returned from connection " + connection);
        	}
        } catch (Throwable t) {
        	if ((_logger != null) && (_logger.isErrorEnabled())) {
        		_logger.error("Exception following connection: " + connection, t);
        	}
        	if (inputTransform != null) {
                Log inLogger = inputTransform.getLog();
                if ((inLogger != null) && (inLogger.isErrorEnabled())) {
                    inLogger.error("Exception following connection: " + connection, t);
                }
        	}
        }
	}
	
	public void dispose() {
	    // don't dispose of auto started transforms
		if ((!_disposed) && (!getTransformContext().autoStart())) {
			_disposed = true;
		    unregisterMBean();
		    // call nexts
		    HashSet<String> visited = new HashSet<String>();
		    Iterator<ArrayList<Connection>> it = _connections.values().iterator();
		    while(it.hasNext()) {
		        ArrayList<Connection> connections = it.next();
                for (int i = 0; i < connections.size(); i++) {
                    Connection connection = connections.get(i);
                    Transform transform = connection.getInputTransform();
                    if (!visited.contains(transform.getInstanceName())) {
                        visited.add(transform.getInstanceName());
                        try {
                            transform.dispose();
                        } catch(Throwable t) {
                            if ((_logger != null) && (_logger.isErrorEnabled())) {
                                _logger.error("Exception following connection: " + connection, t);
                            }
                            Log inLogger = transform.getLog();
                            if ((inLogger != null) && (inLogger.isErrorEnabled())) {
                                inLogger.error("Exception following connection: " + connection, t);
                            }
                        }
                    }
                }
		    }
		    // dispose of connections to help garbage collection
		    _connections.clear();
		}
	}
	
	@Override
	public String toString() {
		StringBuilder bldr = new StringBuilder();
		bldr.append("transformName=");
		bldr.append(_transformName);
		bldr.append(", instanceName=");
		bldr.append(_instanceName);
		return bldr.toString();
	}
	
	protected Transform createTransform(ConnectionDefinition connectionDef) throws Exception {
        TransformContext inTransformCtx = getTransformContext(connectionDef.getInputTransformName());				
		return _transformCreationContext.getInstance(inTransformCtx);						
	}
	
	protected void addConnection(ConnectionDefinition connectionDef) throws Exception {
		Connection connection;
		if (connectionDef.getInputPortDefinition().isStatic()) {
			connection = new Connection(connectionDef, null);
		} else {
			connection = new Connection(connectionDef, createTransform(connectionDef));
		}
		addConnection(connectionDef.getOutputPortDefinition().getName(), connection);
	}
	
	protected void addConnection(String outPortName, Connection connection) {
	    ArrayList<Connection> connections = _connections.get(outPortName);
		if (connections == null) {
			connections = new ArrayList<Connection>();
			_connections.put(outPortName, connections);
		}
		connections.add(connection);
	}

    public void registerMBean() throws InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException, MalformedObjectNameException, NullPointerException, PropertyVetoException, PropertyConversionException {
        _objectName = new ObjectName(getClass().getName() + "." + getTransformName() + ":name=" + getInstanceName());
        ManagementFactory.getPlatformMBeanServer().registerMBean(this, _objectName); 
    }

    public void unregisterMBean() {
        if (_objectName != null) {
            try {
                ManagementFactory.getPlatformMBeanServer().unregisterMBean(_objectName);
            } catch (Exception e) {
                if (_logger.isWarnEnabled()) {
                    _logger.warn("Error unregistering MBean: " + _objectName, e);
                }
            } 
        }
    }

    @Override
    public Object getAttribute(String attribute) throws AttributeNotFoundException,
            MBeanException, ReflectionException {
        if (attribute.equals(TransformContext.LOG4J_EFFECTIVE_LOG_LEVEL_PROPERTY)) {
            Level level = LogManager.getLogger(_loggerName).getEffectiveLevel();
            if (level == null) {
                return null;
            } else {
                return level.toString();
            }
        } else if (attribute.equals(TransformContext.LOG4J_LOG_LEVEL_PROPERTY)) {
            Level level = LogManager.getLogger(_loggerName).getLevel();
            if (level == null) {
                return null;
            } else {
                return level.toString();
            }
        } else if (attribute.equals(TransformContext.CLIENT_ADDRESS)) {
            return Impulse.getTCPTransportClientAddressString(_transport);
        } else if (attribute.equals(TransformContext.SERVER_ADDRESS)) {
            return Impulse.getTCPTransportServerAddressString(_transport);
        }
        return JMXUtils.getAttribute(_properties.values(), attribute);
    }

    @Override
    public AttributeList getAttributes(String[] arg0) {
        return null;
    }

    @Override
    public MBeanInfo getMBeanInfo() {
        if (_mBeanInfo == null) {
            _mBeanInfo = new MBeanInfo(getClass().getName(), getClass().getSimpleName(), JMXUtils.createAttrInfo(_properties.values()), null, createOperInfo(), null);        
        }
        return _mBeanInfo;
    }

    /**
     * Overridden by subclasses to invoke operations from JMX.
     * 
     * @param actionName The name of the action to be invoked.
     * @param params An array containing the parameters to be set when the action is invoked.
     * @param signature An array containing the signature of the action. The class objects will
     * be loaded through the same class loader as the one used for loading the
     * MBean on which the action is invoked.
     * @return The object returned by the action, which represents the result of
     * invoking the action on the MBean specified.
     * @throws MBeanException
     * @throws ReflectionException
     */
    @Override
    public Object invoke(String actionName, Object[] params, String[] signature)
            throws MBeanException, ReflectionException {
        return null;
    }

    @Override
    public void setAttribute(Attribute attribute) throws AttributeNotFoundException,
            InvalidAttributeValueException, MBeanException, ReflectionException {
        if (attribute.getName().equals(TransformContext.LOG4J_LOG_LEVEL_PROPERTY)) {
            Logger logger = LogManager.getLogger(_loggerName);
            logger.setLevel(Level.toLevel((String)attribute.getValue()));
        } else {
            JMXUtils.setAttribute(_properties.values(), attribute);
        }
    }

    @Override
    public AttributeList setAttributes(AttributeList arg0) {
        return null;
    }
    
    protected MBeanOperationInfo[] createOperInfo() {
        return null;
    }

    private HashMap<String, Object> createNewArgMap(Map<String, Object> argMap, Object[] args) {
        HashMap<String, Object> newArgMap = new HashMap<String, Object>();
        if (argMap != null) {
            newArgMap.putAll(argMap);
        }
        if (args != null) {
            if (((args.length / 2) * 2) != args.length) {
                throw new IllegalArgumentException("error calling BaseTransform::next(), must have pairs of arguments");
            }
            int idx = 0;
            while (idx < args.length) {
                newArgMap.put((String)args[idx++], args[idx++]);
            }
        }
        return newArgMap;
    }    
    
    /**
     * Check to see if any of the transform's argument are contained
     * within the argMap passed to it. This may happen if there is
     * more than one transform of the same type in the chain.
     * 
     * @param argMap Args passed into the transform
     * @param args Args defined by the transform
     * @return True if any of the transform defined args is in the passed in argMap. 
     */
    private boolean overlaps(Map<String, Object> argMap, Object[] args) {
        if ((argMap != null) && (args != null)) {
            if (((args.length / 2) * 2) != args.length) {
                throw new IllegalArgumentException("error calling BaseTransform::next(), must have pairs of arguments");
            }
            int idx = 0;
            while (idx < args.length) {
                if (argMap.containsKey(args[idx])) {
                    return true;
                }
                idx += 2;
            }
        }
        return false;
    }
}
