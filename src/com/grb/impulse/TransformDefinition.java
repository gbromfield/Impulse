package com.grb.impulse;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TransformDefinition {

    private Class<?> _clazz;
    private TreeMap<String, PortDefinition> _inputPortMap = new TreeMap<String, PortDefinition>();
    private TreeMap<String, PortDefinition> _outputPortMap = new TreeMap<String, PortDefinition>();
    private TreeMap<String, Object[]> _argumentMap = null;
    private Log _logger;
    
    public TransformDefinition(String classname) throws ClassNotFoundException {
        _clazz = Class.forName(classname);
        _inputPortMap = new TreeMap<String, PortDefinition>();
        _outputPortMap = new TreeMap<String, PortDefinition>();
        _argumentMap = null;
        _logger = LogFactory.getLog(_clazz);
        addPortDefinitions();
        addArgumentDefinitions();
    }

    public Class<?> getTransformClass() {
        return _clazz;
    }
    
    public PortDefinition getInputPortDefinition(String name) {
        return _inputPortMap.get(name);        
    }

    public PortDefinition getOutputPortDefinition(String name) {
        return _outputPortMap.get(name);        
    }

    public Object[] getArgumentName(String name) {
        if (_argumentMap != null) {
            return _argumentMap.get(name);        
        }
        return null;
    }

    public Log getLogger() {
        return _logger;
    }
    
    @Override
    public int hashCode() {
        return getTransformClass().getName().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TransformDefinition) {
            TransformDefinition other = (TransformDefinition)obj;
            if (getTransformClass().getName().equals(other.getTransformClass().getName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder bldr = new StringBuilder();
        bldr.append("class=");
        bldr.append(_clazz.getSimpleName());
        bldr.append(", inputs={");
        Iterator<String> it = _inputPortMap.keySet().iterator();
        boolean first = true;
        while(it.hasNext()) {
            if (first) {
                first = false;
            } else {
                bldr.append(", ");
            }
            bldr.append(it.next());
        }
        bldr.append("}");
        bldr.append(", outputs={");
        it = _outputPortMap.keySet().iterator();
        first = true;
        while(it.hasNext()) {
            if (first) {
                first = false;
            } else {
                bldr.append(", ");
            }
            bldr.append(it.next());
        }
        bldr.append("}");
        return bldr.toString();
    }

    private void addPortdefinition(PortDefinition portDef, TreeMap<String, PortDefinition> portMap) {
        if (portMap.get(portDef.getName()) != null) {
            throw new IllegalArgumentException(String.format("found duplicate port definition %s in class %s", portDef.getName(), portDef.getPortClass().getSimpleName()));
        }
        portMap.put(portDef.getName(), portDef);
    }

    private void addPortDefinitions() {
        Method[] methods = _clazz.getMethods();
        for (int i = 0; i < methods.length; i++) {
            Input input = methods[i].getAnnotation(Input.class);
            if (input != null) {
                PortDefinition portDef = new PortDefinition(_clazz, input.value(), methods[i]);
                if (_logger.isInfoEnabled()) {   
                    _logger.info("Adding Input Port Definition: " + portDef);
                }
                addPortdefinition(portDef, _inputPortMap);
            }
            Output output = methods[i].getAnnotation(Output.class);
            if (output != null) {
                PortDefinition portDef = new PortDefinition(_clazz, output.value(), methods[i]);
                if (_logger.isInfoEnabled()) {   
                    _logger.info("Adding Output Port Definition: " + portDef);
                }
                addPortdefinition(portDef, _outputPortMap);
            }
        }
    }

    private void addArgumentDefinitions() {
        Field[] fields = _clazz.getFields();
        for (int i = 0; i < fields.length; i++) {
            Argument argument = fields[i].getAnnotation(Argument.class);
            if (argument != null) {
                try {
                    Object[] argValue = (Object[])fields[i].get(null);
                    if (argValue.length != 2) {
                        throw new IllegalArgumentException(String.format("Argument \"%s\" object array must have 2 parts", fields[i].getName()));
                    }
                    if (argValue[0].getClass() != String.class) {
                        throw new IllegalArgumentException(String.format("Argument \"%s\" object array's first part must be of type %s instead of %s", fields[i].getName(), String.class.getSimpleName(), argValue[0].getClass().getName()));
                    }
                    if (argValue[1].getClass() != Class.class) {
                        throw new IllegalArgumentException(String.format("Argument \"%s\" object array's second part must be of type %s instead of %s", fields[i].getName(), Class.class.getSimpleName(), argValue[1].getClass().getName()));
                    }
                    String[] argStr = ((String)argValue[0]).split("\\.");
                    Class<?> argClass = (Class<?>)argValue[1];
                    if (argStr.length != 3) {
                        throw new IllegalArgumentException(String.format("Argument \"%s\" name must have three parts", fields[i].getName()));
                    }
                    if (!argStr[0].equals(_clazz.getSimpleName())) {
                        throw new IllegalArgumentException(String.format("Argument \"%s\" name must have first part equal to \"%s\"", fields[i].getName(), _clazz.getSimpleName()));
                    }
                    if (!argStr[1].equals(argClass.getSimpleName())) {
                        throw new IllegalArgumentException(String.format("Argument \"%s\" name must have second part equal to \"%s\"", fields[i].getName(), argClass.getSimpleName()));
                    }
                    if (!argStr[2].equals(fields[i].getName())) {
                        throw new IllegalArgumentException(String.format("Argument \"%s\" name must have third part equal to \"%s\"", fields[i].getName(), fields[i].getName()));
                    }
                    if (_logger.isInfoEnabled()) {   
                        _logger.info("Adding Argument Definition: " + argValue[0]);
                    }
                    if (_argumentMap == null) {
                        _argumentMap = new TreeMap<String, Object[]>();
                    }
                    _argumentMap.put(fields[i].getName(), argValue);
                } catch (IllegalArgumentException e) {
                    throw e;
                } catch (Exception e) {
                    throw new IllegalArgumentException("Error adding Argument definition " + fields[i].getName(), e);
                }
            }
        }
    }
}
