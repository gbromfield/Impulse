package com.grb.impulse.plugins;

import java.nio.ByteBuffer;
import java.util.Map;

import com.grb.impulse.Argument;
import com.grb.impulse.BaseTransform;
import com.grb.impulse.Input;
import com.grb.impulse.Output;
import com.grb.impulse.TransformCreationContext;
import com.grb.impulse.parsers.ws.WSMessage;

/**
 * This transform takes a WebSockets message as input and outputs the message
 * based on it's message type.
 * If a message is received for which there is no connection, the onDefault
 * output will be called.
 */
public class WSProtocolDemux extends BaseTransform {
    /**
     * This variable is a place holder for a transform argument.
     * Argument syntax must follow <transform simple class name>.<type simple class name>.<field name>
     * This transforms outputs data into the argMap using this as a key.
     */
    @Argument
    final public static Object[] bytesWSPayload = {"WSProtocolDemux.ByteBuffer.bytesWSPayload", ByteBuffer.class};

    public WSProtocolDemux(String transformName, String instanceName,
            TransformCreationContext transformCreationContext, Object... args) {
        super(transformName, instanceName, transformCreationContext);    
    }

    @Input("onWSMessage")
    public void onWSMessage(Map<String, Object> argMap, WSMessage webMsg) {
        if (webMsg == null) {
            if (_logger.isDebugEnabled()) {
                _logger.debug("WebMessage: " + null);
            }
            return;
        }
        switch(webMsg.getMessageType()) {
        case BINARY_MESSAGE:
            onBinaryMessage(argMap, webMsg);
            break;
            
        case CONNECTION_CLOSE:
            onConnectionClose(argMap, webMsg);
            break;
            
        case CONTINUATION_MESSAGE:
            onMessageContinuation(argMap, webMsg);
            break;
            
        case PING:
            onPing(argMap, webMsg);
            break;
            
        case PONG:
            onPong(argMap, webMsg);
            break;
            
        case TEXT_MESSAGE:
            onTextMessage(argMap, webMsg);
            break;
        }
    }
    
    /**
     * [output] Called on a websocket message continuation message.
     * 
     * @param webMsg
     */
    @Output("onMessageContinuation")
    public void onMessageContinuation(Map<String, Object> argMap, WSMessage webMsg) {
        if (hasOutputConnection("onMessageContinuation")) {
            if (_logger.isDebugEnabled()) {
                _logger.debug("onMessageContinuation: \r\n" + webMsg);
            }
            next("onMessageContinuation", argMap);
        } else if (hasOutputConnection("onDataMessage")) {
            onDataMessage(argMap, webMsg);
        } else {
            onDefault(argMap, webMsg);
        }
    }

    /**
     * [output] Called on a websocket text message.
     * 
     * @param webMsg
     */
    @Output("onTextMessage")
    public void onTextMessage(Map<String, Object> argMap, WSMessage webMsg) {
        if (hasOutputConnection("onTextMessage")) {
            if (_logger.isDebugEnabled()) {
                _logger.debug("onTextMessage: \r\n" + webMsg);
            }
            next("onTextMessage", argMap, webMsg);
        } else if (hasOutputConnection("onDataMessage")) {
            onDataMessage(argMap, webMsg);
        } else {
            onDefault(argMap, webMsg);
        }
    }

    /**
     * [output] Called on a websocket binary message.
     * 
     * @param webMsg
     */
    @Output("onBinaryMessage")
    public void onBinaryMessage(Map<String, Object> argMap, WSMessage webMsg) {
        if (hasOutputConnection("onBinaryMessage")) {
            if (_logger.isDebugEnabled()) {
                _logger.debug("onBinaryMessage: \r\n" + webMsg);
            }
            next("onBinaryMessage", argMap, webMsg);
        } else if (hasOutputConnection("onDataMessage")) {
            onDataMessage(argMap, webMsg);
        } else {
            onDefault(argMap, webMsg);
        }
    }

    /**
     * [output] Called on a websocket connection close message.
     * 
     * @param webMsg
     */
    @Output("onConnectionClose")
    public void onConnectionClose(Map<String, Object> argMap, WSMessage webMsg) {
        if (hasOutputConnection("onConnectionClose")) {
            if (_logger.isDebugEnabled()) {
                _logger.debug("onConnectionClose: \r\n" + webMsg);
            }
            next("onConnectionClose", argMap);
        } else if (hasOutputConnection("onCtrlMessage")) {
            onCtrlMessage(argMap, webMsg);
        } else {
            onDefault(argMap, webMsg);
        }
    }

    /**
     * [output] Called on a websocket ping message.
     * 
     * @param webMsg
     */
    @Output("onPing")
    public void onPing(Map<String, Object> argMap, WSMessage webMsg) {
        if (hasOutputConnection("onPing")) {
            if (_logger.isDebugEnabled()) {
                _logger.debug("onPing: \r\n" + webMsg);
            }
            next("onPing", argMap);
        } else if (hasOutputConnection("onCtrlMessage")) {
            onCtrlMessage(argMap, webMsg);
        } else {
            onDefault(argMap, webMsg);
        }
    }

    /**
     * [output] Called on a websocket pong message.
     * 
     * @param webMsg
     */
    @Output("onPong")
    public void onPong(Map<String, Object> argMap, WSMessage webMsg) {
        if (hasOutputConnection("onPong")) {
            if (_logger.isDebugEnabled()) {
                _logger.debug("onPong: \r\n" + webMsg);
            }
            next("onPong", argMap);
        } else if (hasOutputConnection("onCtrlMessage")) {
            onCtrlMessage(argMap, webMsg);
        } else {
            onDefault(argMap, webMsg);
        }
    }

    /**
     * [output] Called on a websocket data message.
     * 
     * @param webMsg
     */
    @Output("onDataMessage")
    public void onDataMessage(Map<String, Object> argMap, WSMessage webMsg) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("onDataMessage: \r\n" + webMsg);
        }
        next("onDataMessage", argMap, webMsg);
    }

    /**
     * [output] Called on a websocket control message.
     * 
     * @param webMsg
     */
    @Output("onCtrlMessage")
    public void onCtrlMessage(Map<String, Object> argMap, WSMessage webMsg) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("onCtrlMessage: \r\n" + webMsg);
        }
        next("onCtrlMessage", argMap, webMsg);
    }

    /**
     * [output] Called on a websocket message.
     * 
     * @param webMsg
     */
    @Output("onDefault")
    public void onDefault(Map<String, Object> argMap, WSMessage webMsg) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("onDefault: \r\n" + webMsg);
        }
        next("onDefault", argMap, webMsg);
    }
    
    private void next(String outputPortName, Map<String, Object> argMap, WSMessage webMsg) {
        if ((webMsg.payload == null) || (webMsg.payload.length == 0)) {
            next(outputPortName, argMap);
        } else {
            next(outputPortName, argMap, bytesWSPayload[0], ByteBuffer.wrap(webMsg.payload));
        }        
    }
}
