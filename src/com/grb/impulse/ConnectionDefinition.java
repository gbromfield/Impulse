package com.grb.impulse;

public class ConnectionDefinition {
    private String _outputTransformName;
    private TransformDefinition _outputTransformDef;
    private PortDefinition _outputPortDef;
    private JavascriptDefinition _javascript;
    private String _inputTransformName;
    private TransformDefinition _inputTransformDef;
    private PortDefinition _inputPortDef;
    private String[] _inputArguments;
    
    public ConnectionDefinition(String outputTransformName, TransformDefinition outputTransformDef,
                                PortDefinition outputPortDef, JavascriptDefinition javascript,
                                String inputTransformName, TransformDefinition inputTransformDef,
                                PortDefinition inputPortDef, String[] inputArguments) {
        _outputTransformName = outputTransformName;
        _outputTransformDef = outputTransformDef;
        _outputPortDef = outputPortDef;
        _javascript = javascript;
        _inputTransformName = inputTransformName;
        _inputTransformDef = inputTransformDef;
        _inputPortDef = inputPortDef;
        _inputArguments = inputArguments;
    }
    
    public String getOutputTransformName() {
        return _outputTransformName;
    }
    
    public TransformDefinition getOutputTransformDefinition() {
        return _outputTransformDef;
    }

    public PortDefinition getOutputPortDefinition() {
        return _outputPortDef;
    }

    public JavascriptDefinition getJavascriptDefinition() { return _javascript; }

    public boolean usesJavascript() { return(_javascript != null); }

    public TransformDefinition getInputTransformDefinition() {
        return _inputTransformDef;
    }

    public String getInputTransformName() {
        return _inputTransformName;
    }

    public PortDefinition getInputPortDefinition() {
        return _inputPortDef;
    }
    
    public String[] getInputArguments() {
        return _inputArguments;
    }
    
    public String toSimpleString() {
        StringBuilder bldr = new StringBuilder();
        bldr.append(_outputPortDef.getName());
        bldr.append(" -> ");
        bldr.append(_inputTransformName);
        bldr.append(".");
        bldr.append(_inputPortDef.getName());
        return bldr.toString();
    }
    
    @Override
    public String toString() {
        StringBuilder bldr = new StringBuilder();
        bldr.append("outputTransformName=");
        bldr.append(_outputTransformName);
        bldr.append(", outputTransformDef=");
        bldr.append(_outputTransformDef.getTransformClass().getSimpleName());
        bldr.append(", outputPortDef=");
        bldr.append(_outputPortDef.getName());
        bldr.append(", inputTransformName=");
        bldr.append(_inputTransformName);
        bldr.append(", inputTransformDef=");
        bldr.append(_inputTransformDef.getTransformClass().getSimpleName());
        bldr.append(", inputPortDef=");
        bldr.append(_inputPortDef.getName());
        return bldr.toString();
    }
}
