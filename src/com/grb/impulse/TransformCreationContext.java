package com.grb.impulse;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.Map;
import java.util.TreeMap;

public class TransformCreationContext {

    /**
     * Maps Transform Name to transform 
     */
    private TreeMap<String, Transform> _transformMap;

    /**
     * Javascript Engine
     */
    private ScriptEngineManager _scriptEngineManager;
    private ScriptEngine _scriptEngine;

    public TransformCreationContext() {
        _transformMap = new TreeMap<String, Transform>();
        _scriptEngineManager = null;
        _scriptEngine = null;
    }
        
    public Transform getInstance(TransformContext transformCtx, Object... args) throws Exception {
        JavascriptDefinition[] jsCtxs = transformCtx.getJavascriptDefinitions();
        if (jsCtxs != null) {
            if (_scriptEngineManager == null) {
                _scriptEngineManager = new ScriptEngineManager();
                _scriptEngine = _scriptEngineManager.getEngineByName("JavaScript");
            }
            for(int i = 0; i < jsCtxs.length; i++) {
                JavascriptDefinition ctx = jsCtxs[i];
                _scriptEngine.eval(new java.io.FileReader(ctx.file));
            }
        }
        Transform transform = _transformMap.get(transformCtx.getName());
        if (transform == null) {
            // check the auto started globals
            transform = Impulse.GlobalTransformMap.get(transformCtx.getName());
            if (transform == null) {
                transform = transformCtx.getTransformFactory().newInstance(this, args);
                _transformMap.put(transformCtx.getName(), transform);
                transform.init();
                transform.start();
            }
        }
        return transform;
    }
    
    public Map<String, Transform> getTransformMap() {
        return _transformMap;
    }

    public ScriptEngine getJavascriptEngine() { return _scriptEngine; }
}
