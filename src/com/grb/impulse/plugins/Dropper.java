package com.grb.impulse.plugins;

import java.util.Map;

import com.grb.impulse.BaseTransform;
import com.grb.impulse.Impulse;
import com.grb.impulse.Input;
import com.grb.impulse.TransformCreationContext;

/**
 * This tranform is similar to having no connection on an output port at all,
 * but is needed for transforms that activate based on being connected.
 * For Example:
 *  if (connection A is connected) {
 *      send to Connection A;
 *  } else {
 *      send to connection B;
 *  } 
 * In this case if A wasn't connected it would go to B. Use this transform
 * to have things dropped rather send to B.
 */
public class Dropper extends BaseTransform {

    public Dropper(String transformName, String instanceName,
            TransformCreationContext transformCreationContext, Object... args) {
        super(transformName, instanceName, transformCreationContext);
    }

    /**
     * [input] Data to be dropped
     * @param argMap Data to be dropped
     */
    @Input("onData")
    public void onData(Map<String, Object> argMap) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("Dropping: \r\n" + Impulse.format(argMap));
        }
    }
}
