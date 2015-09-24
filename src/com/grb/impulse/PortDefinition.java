package com.grb.impulse;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class PortDefinition {
    static private Log Logger = LogFactory.getLog(PortDefinition.class);

    protected Class<?> _clazz;
    protected String _name;
    protected Method _method;
    
    public PortDefinition(Class<?> clazz, String name, Method method) {
        _clazz = clazz;
        _name = name;
        _method = method;
        int modifiers = _method.getModifiers();
        if (!Modifier.isPublic(modifiers)) {
            throw new IllegalArgumentException(String.format("Error adding port definition class=%s, name=%s, methodName=%s - not public",
                    clazz.getSimpleName(), name, _method.getName()));
        }
    }
    
    public Class<?> getPortClass() {
        return _clazz;
    }
    
    public String getName() {
        return _name;
    }

    public Method getMethod() {
        return _method;
    }
    
    public boolean isStatic() {
        return Modifier.isStatic(_method.getModifiers());
    }
    
    @Override
	public boolean equals(Object arg0) {
    	if (arg0 instanceof PortDefinition) {
    		PortDefinition portDef = (PortDefinition)arg0;
    		return (_clazz.equals(portDef._clazz) && _name.equals(portDef._name));
    	}
		return false;
	}

    public String toStringSimple() {
        StringBuilder bldr = new StringBuilder();
        bldr.append("name=");
        bldr.append(_name);
        return bldr.toString();
    }
    
    public String toStringDetail() {
        StringBuilder bldr = new StringBuilder();
        bldr.append("class=");
        bldr.append(_clazz.getName());
        bldr.append(", name=");
        bldr.append(_name);
        bldr.append(", method=");
        bldr.append(_method.getName());
        return bldr.toString();
    }
    
    @Override
    public String toString() {
        if (Logger.isDebugEnabled()) {
            return toStringDetail();
        }
        return toStringSimple();
    }
}
