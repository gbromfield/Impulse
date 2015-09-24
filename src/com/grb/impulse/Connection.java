package com.grb.impulse;


/**
 * Represents one unidirectional connection in a many to many
 * cardinality between transforms.
 *
 */
public class Connection {
    private ConnectionDefinition _connectionDef;
    private Transform _inputTransform;
    
    public Connection(ConnectionDefinition connectionDef, Transform inputTransform) {
    	_connectionDef = connectionDef;
    	_inputTransform = inputTransform;
    }
    
    public ConnectionDefinition getConnectionDefinition() {
        return _connectionDef;
    }
    
    public Transform getInputTransform() {
        return _inputTransform;
    }
    
    @Override
    public String toString() {
    	StringBuilder bldr = new StringBuilder();
    	bldr.append("connectionDef=");
    	bldr.append(_connectionDef);
    	bldr.append(", inputTransform=");
    	bldr.append(_inputTransform);
    	return bldr.toString();
    }
}
