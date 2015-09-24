package com.grb.impulse.plugins;

import java.nio.ByteBuffer;
import java.util.Map;

import com.grb.impulse.Argument;
import com.grb.impulse.BaseTransform;
import com.grb.impulse.Impulse;
import com.grb.impulse.Input;
import com.grb.impulse.Output;
import com.grb.impulse.TransformCreationContext;
import com.grb.impulse.parsers.ws.WSEncoder;
import com.grb.impulse.parsers.ws.WSMessage;

/**
 * Used with http://www.websocket.org/echo.html
 * and config file websocketsEcho.properties.
 */
public class WebSocketEcho extends BaseTransform {
    /**
     * This variable is a place holder for a transform argument.
     * Argument syntax must follow <transform simple class name>.<type simple class name>.<field name>
     * This transforms outputs data into the argMap using this as a key.
     */
    @Argument
    final public static Object[] bytesWSMessage = {"WebSocketEcho.ByteBuffer.bytesWSMessage", ByteBuffer.class};

    private WSEncoder _encoder;
    
	public WebSocketEcho(String transformName, String instanceName,
			TransformCreationContext transformCreationContext, Object... args) {
		super(transformName, instanceName, transformCreationContext);
		_encoder = new WSEncoder();
	}

	/**
	 * [input] Data to be echoed back.
	 * 
	 * @param argMap
	 */
	@Input("onData")
	public void onData(Map<String, Object> argMap, WSMessage msg) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("onData: \r\n" + msg);
        }
	    WSMessage out = new WSMessage(msg.getText());
		onOutput(argMap, _encoder.toByteBuffer(out));
	}
	
	/**
	 * [output] Echoed output.
	 * 
	 * @param buffer
	 */
	@Output("onOutput")
	public void onOutput(Map<String, Object> argMap, ByteBuffer buffer) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("onOutput: \r\n" + Impulse.GlobalByteArrayFormatter.format(buffer));
        }
		next("onOutput", argMap, bytesWSMessage[0], buffer);
	}
}
