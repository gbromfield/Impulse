package com.grb.impulse;


public interface TransformFactory {    
    public Transform newInstance(TransformCreationContext createCtx, Object... args) throws Exception;
}
