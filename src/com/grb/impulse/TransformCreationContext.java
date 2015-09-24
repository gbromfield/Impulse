package com.grb.impulse;

import java.util.Map;
import java.util.TreeMap;

public class TransformCreationContext {

    /**
     * Maps Transform Name to transform 
     */
    private TreeMap<String, Transform> _transformMap;
    
    public TransformCreationContext() {
        _transformMap = new TreeMap<String, Transform>();
    }
        
    public Transform getInstance(TransformContext transformCtx, Object... args) throws Exception {
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
}
