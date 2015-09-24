package com.grb.impulse;

import java.lang.reflect.Constructor;


public class DefaultTransformFactory implements TransformFactory {

    private TransformContext _transformContext;
    private long _transformIndex;
    
    public DefaultTransformFactory(TransformContext transformContext) {     // change to TransformName?
        _transformContext = transformContext;
        _transformIndex = 0;
    }

    @Override
    synchronized public Transform newInstance(TransformCreationContext createCtx, Object... args) throws Exception {
        Transform transform;
        String transformName = _transformContext.getName();
        String instanceName;
        if (_transformIndex == 0) {
            instanceName = transformName;
        } else {
            instanceName = transformName + "_" + _transformIndex;
        }

        @SuppressWarnings("unchecked")
        Constructor<Transform> cstor = (Constructor<Transform>)_transformContext.getTransformDefinition().getTransformClass().getConstructor(
                String.class, String.class, TransformCreationContext.class, Object[].class);
        transform = cstor.newInstance(transformName, instanceName, createCtx, args);
        
        _transformIndex++;
        return transform;
    }
}
