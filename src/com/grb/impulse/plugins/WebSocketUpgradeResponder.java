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
import com.grb.impulse.parsers.http.HTTPMessage;
import com.grb.impulse.parsers.ws.WSDecoder;
import com.grb.impulse.parsers.ws.WSEncoder;
import com.grb.util.property.JMXUtils;
import com.grb.util.property.Property;
import com.grb.util.property.PropertyConversionException;
import com.grb.util.property.PropertySource;
import com.grb.util.property.PropertyVetoException;
import com.grb.util.property.impl.MapPropertySource;
import com.grb.util.property.impl.SystemPropertySource;

/**
 * Used with http://www.websocket.org/echo.html
 * and config file websocketsEcho.properties.
 * This transform simulates an HTTP server responding
 * to a WebSocket upgrade request.
 */
public class WebSocketUpgradeResponder extends BaseTransform {
    /**
     * This variable is a place holder for a transform argument.
     * Argument syntax must follow <transform simple class name>.<type simple class name>.<field name>
     * This transforms outputs data into the argMap using this as a key.
     */
    @Argument
    final public static Object[] upgradeResponse = {"WebSocketUpgradeResponder.ByteBuffer.upgradeResponse", ByteBuffer.class};

    /**
     * Websocket server key.
     */
    public static final String SERVER_KEY = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    
    /**
     * Websocket key parameter name.
     */
    public static final String WEBSOCKET_KEY = "Sec-WebSocket-Key: ";
    
    
    /**
     * Server Handshake property.
     */
    public static final String SERVER_HANDSHAKE_PROPERTY = "serverHandshake";
    
    /**
     * Default server handshake response
     */
    public static final String SERVER_HANDSHAKE_PROPERTY_DEFAULT =  "HTTP/1.1 101 Switching Protocols\r\n" +
                                                                    "Upgrade: websocket\r\n" +
                                                                    "Connection: Upgrade\r\n" +
                                                                    "Sec-WebSocket-Accept: %s\r\n\r\n";

    private WSEncoder _encoder;
    private WSDecoder _decoder;

    public WebSocketUpgradeResponder(String transformName, String instanceName,
            TransformCreationContext transformCreationContext, Object... args) {
        super(transformName, instanceName, transformCreationContext);
        _encoder = new WSEncoder();
        _decoder = new WSDecoder(0);
    }

    /**
     * [input] Data from the socket.
     * 
     * @param argMap
     */
    @Input("onData")
    public void onData(Map<String, Object> argMap, HTTPMessage httpMessage) {
        String key = _decoder.decodeHandshakeKey(httpMessage);
        if (key == null) {
            if (_logger.isDebugEnabled()) {
                _logger.debug("onData  (Not WebSocket Upgrade): \r\n" + httpMessage);
            }
            
        } else {
            if (_logger.isDebugEnabled()) {
                _logger.debug("onData (WebSocket Upgrade): \r\n" + httpMessage);
            }
            String acceptToken;
            try {
                acceptToken = _encoder.encodeAcceptToken(key);
                String acceptResp = String.format(getStringProperty(SERVER_HANDSHAKE_PROPERTY), acceptToken);
                ByteBuffer buffer = ByteBuffer.allocate(1000);
                buffer.put(acceptResp.getBytes("UTF-8"));
                buffer.flip();
                onWebSocketUpgrade(argMap, buffer);
            } catch (Exception e) {
                if (_logger.isErrorEnabled()) {
                    _logger.error("Error encoding accept", e);
                }
            }
        }
    }

    /**
     * [output] Called on a websocket server handshake message.
     * 
     * @param buffer
     */
    @Output("onWebSocketUpgrade")
    public void onWebSocketUpgrade(Map<String, Object> argMap, ByteBuffer buffer) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("onWebSocketUpgrade: \r\n" + Impulse.GlobalByteArrayFormatter.format(buffer));
        }
        next("onWebSocketUpgrade", argMap, upgradeResponse[0], buffer);
    }
    
    /**
     * This method is for declaring this transform's properties. This method is called
     * using reflection at application startup by the transform's context. These properties
     * will be initialized after this call to validate their values. The TransformContext
     * for this Transform owns the properties and then clones the properties
     * for the transform when the transform is created so that each Transform has its
     * own copy. 
     * 
     * @param ctx the transform context.
     * @throws PropertyVetoException
     * @throws PropertyConversionException
     */
    @SuppressWarnings("unchecked")
    public static void getProperties(TransformContext ctx) throws PropertyVetoException, PropertyConversionException { 
        Map<String, Property<?>> props = ctx.getProperties();
        
        Property<?> p;        
        p = new Property<String>(SERVER_HANDSHAKE_PROPERTY, SERVER_HANDSHAKE_PROPERTY_DEFAULT, true,
                new PropertySource<String>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new MapPropertySource<String>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(SERVER_HANDSHAKE_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_2, PropertySource.NULL_INVALID),
                new SystemPropertySource<String>(ctx.getPropertyString(SERVER_HANDSHAKE_PROPERTY), PropertySource.PRIORITY_3, PropertySource.NULL_INVALID));
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "Server Handshake");
        props.put(p.getId(), p);
    }
}
