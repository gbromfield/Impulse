package com.grb.impulse.plugins;

import java.nio.ByteBuffer;
import java.util.Map;

import com.grb.impulse.Argument;
import com.grb.impulse.BaseTransform;
import com.grb.impulse.Impulse;
import com.grb.impulse.Input;
import com.grb.impulse.Output;
import com.grb.impulse.TransformContext;
import com.grb.impulse.TransformCreationContext;

/**
 * This transform takes an input buffer, clones it, and
 * sends it to the ouput.
 * 
 * @see TransformContext
 */
public class Cloner extends BaseTransform {
    /**
     * This variable is a place holder for a transform argument.
     * Argument syntax must follow <transform simple class name>.<type simple class name>.<field name>
     * This transforms outputs data into the argMap using this as a key.
     */
    @Argument
    final public static Object[] clone = {"Cloner.ByteBuffer.clone", ByteBuffer.class};

    public Cloner(String transformName, String instanceName,
            TransformCreationContext transformCreationContext, Object ... args) {
        super(transformName, instanceName, transformCreationContext);
    }

    /**
     * [input] The buffer that is passed will be cloned and passed to {@link Cloner#onOutput(Map, ByteBuffer)}
     * 
     * @param argMap The buffer to clone.
     */
    @Input("onData")
    public void onData(Map<String, Object> argMap, ByteBuffer buffer) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("onData: \r\n" + Impulse.GlobalByteArrayFormatter.format(buffer));
        }
        ByteBuffer clone = ByteBuffer.allocate(buffer.limit() * 2);
        clone.put(buffer);
        buffer.flip();
        clone.put(buffer);
        buffer.flip();
        clone.flip();
        onOutput(argMap, clone);
    }
    
    /**
     * [output] The cloned buffer.
     * 
     * @param buffer The cloned buffer.
     */
    @Output("onOutput")
    public void onOutput(Map<String, Object> argMap, ByteBuffer buffer) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("onOutput: \r\n" + Impulse.GlobalByteArrayFormatter.format(buffer));
        }
        next("onOutput", argMap, clone[0], buffer);
    }
}
