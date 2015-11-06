package com.grb.impulse;


/**
 * Represents one unidirectional connection in a many to many
 * cardinality between transforms.
 *
 */
public class Connection {
    private String _name;
    private ConnectionDefinition _connectionDef;
    private Transform _inputTransform;
    
    public Connection(ConnectionDefinition connectionDef, Transform inputTransform) {
    	_connectionDef = connectionDef;
    	_inputTransform = inputTransform;
        _name = String.format("%s.%s->%s.%s",
                _connectionDef.getOutputTransformDefinition().getTransformClass().getSimpleName(),
                _connectionDef.getOutputPortDefinition().getName(),
                _connectionDef.getInputTransformDefinition().getTransformClass().getSimpleName(),
                _connectionDef.getInputPortDefinition().getName());
    }

    public String getName() { return _name; }

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
