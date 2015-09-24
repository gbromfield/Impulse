package com.grb.impulse.plugins;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Map;

import com.grb.impulse.Argument;
import com.grb.impulse.BaseTransform;
import com.grb.impulse.Impulse;
import com.grb.impulse.Input;
import com.grb.impulse.Output;
import com.grb.impulse.TransformCreationContext;
import com.grb.impulse.parsers.http.HTTPMessage;
import com.grb.impulse.parsers.ws.WSMessage;
import com.grb.impulse.parsers.ws.WSMessageType;
import com.grb.impulse.utils.NetworkByteOrderNumberUtil;

/**
 * This transform frames HTTP and WebSockets messages and outputs the bytes, String,
 * {@link HTTPMessage}, or {@link WSMessage}
 *
 * In order to work with WebSockets this framer needs to be upgraded when a upgrade
 * response is received from the server. So you should have a link like the following
 * in the config file:<p>
 * <code>
 * link.out.serverHTTPFramer.onWSUpgradeResponse=clientHTTPFramer.onWebSocketUpgrade()
 * </code>
 */
public class HTTPFramer extends BaseTransform {
    /**
     * This variable is a place holder for a transform argument.
     * Argument syntax must follow <transform simple class name>.<type simple class name>.<field name>
     * This transforms outputs data into the argMap using this as a key.
     */
    @Argument
    final public static Object[] bytesHTTPMessage = {"HTTPFramer.ByteBuffer.bytesHTTPMessage", ByteBuffer.class};

    /**
     * This variable is a place holder for a transform argument.
     * Argument syntax must follow <transform simple class name>.<type simple class name>.<field name>
     * This transforms outputs data into the argMap using this as a key.
     */
    @Argument
    final public static Object[] stringHTTPMessage = {"HTTPFramer.String.stringHTTPMessage", String.class};

    /**
     * This variable is a place holder for a transform argument.
     * Argument syntax must follow <transform simple class name>.<type simple class name>.<field name>
     * This transforms outputs data into the argMap using this as a key.
     */
    @Argument
    final public static Object[] httpMessage = {"HTTPFramer.HTTPMessage.httpMessage", HTTPMessage.class};

    /**
     * This variable is a place holder for a transform argument.
     * Argument syntax must follow <transform simple class name>.<type simple class name>.<field name>
     * This transforms outputs data into the argMap using this as a key.
     */
    @Argument
    final public static Object[] bytesWSMessage = {"HTTPFramer.ByteBuffer.bytesWSMessage", ByteBuffer.class};

    /**
     * This variable is a place holder for a transform argument.
     * Argument syntax must follow <transform simple class name>.<type simple class name>.<field name>
     * This transforms outputs data into the argMap using this as a key.
     */
    @Argument
    final public static Object[] wsMessage = {"HTTPFramer.WSMessage.wsMessage", WSMessage.class};

    /**
     * This variable is a place holder for a transform argument.
     * Argument syntax must follow <transform simple class name>.<type simple class name>.<field name>
     * This transforms outputs data into the argMap using this as a key.
     */
    @Argument
    final public static Object[] bytesWSPayloadMessage = {"HTTPFramer.ByteBuffer.bytesWSPayloadMessage", ByteBuffer.class};

    public static final int MAX_SIMPLE_PAYLOAD_LENGTH               = 125;
    public static final int SIXTEEN_BIT_EXTENDED_PAYLOAD_LENGTH     = 126;
    public static final int SIXTY_FOUR_BIT_EXTENDED_PAYLOAD_LENGTH  = 127;

    private enum ReadState {
        READ_HEADER,
        READ_EXTENDED_HEADER,
        READ_PAYLOAD
    }

    /**
     * Whether in HTTP or WebSocket mode.
     */
    volatile protected boolean _webSocketMode;

    /**
     * HTTP variables
     */
    protected int _stage;
    protected com.grb.util.ByteBuffer _buffer;
    protected com.grb.util.ByteBuffer _header;
    protected ArrayList<String> _headers;
    protected byte[] _payload;
    protected int _payloadIndex;
    protected int _remaining;

    /**
     * WebSocket variables
     */
    private ReadState _readState;
    private int _bytesLeftToRead;
    private int _index;
    private ArrayList<WSMessage> _msgs;
    private ArrayList<ByteBuffer> _buffers;
    private WSMessage _currentMsg;

    public HTTPFramer(String transformName, String instanceName,
            TransformCreationContext transformCreationContext, Object... args) {
        super(transformName, instanceName, transformCreationContext);
        _stage = 0;
        _buffer = new com.grb.util.ByteBuffer(10000);
        _header = new com.grb.util.ByteBuffer(100);
        _headers = new ArrayList<String>();
        _payload = null;
        _payloadIndex = 0;
        _remaining = 0;
        _webSocketMode = false;
        
        _msgs = new ArrayList<WSMessage>();
        _buffers = new ArrayList<ByteBuffer>();
        resetWSMessage();
    }

    /**
     * [input] Data from the socket.
     * 
     * @param argMap
     */
    @Input("onData")
    public void onData(Map<String, Object> argMap, ByteBuffer readBuffer) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("onData: \r\n" + Impulse.GlobalByteArrayFormatter.format(readBuffer));
        }
        if (_webSocketMode) {
            parseWSMessage(argMap, readBuffer);
        } else {
            parseHTTPMessage(argMap, readBuffer);
        }
    }

    private void parseWSMessage(Map<String, Object> argMap, ByteBuffer readBuffer) {
        while(readBuffer.hasRemaining()) {
            byte b = readBuffer.get();
            _buffer.writeByte(b);
            _bytesLeftToRead--;
            if (_bytesLeftToRead == 0) {
                switch(_readState) {
                case READ_HEADER:
                    byte b1 = _buffer.getBackingArray()[0];
                    _index++;
                    byte b2 = _buffer.getBackingArray()[1];
                    _index++;
                    _currentMsg.fin = (b1 & 0x80) >> 7;
                    _currentMsg.rsv1 = (b1 & 0x40) >> 6;
                    _currentMsg.rsv2 = (b1 & 0x20) >> 5;
                    _currentMsg.rsv3 = (b1 & 0x10) >> 4;
                    _currentMsg.opcode = b1 & 0x0F;
                    _currentMsg.maskPresent = (b2 & 0x80) >> 7;
                    _currentMsg.payloadLengthField = b2 & 0x7F;
                    if (_currentMsg.payloadLengthField <= MAX_SIMPLE_PAYLOAD_LENGTH) {
                        _currentMsg.payloadLength = _currentMsg.payloadLengthField;
                    } else if (_currentMsg.payloadLengthField == SIXTEEN_BIT_EXTENDED_PAYLOAD_LENGTH) {
                        _bytesLeftToRead = 2;
                    } else if (_currentMsg.payloadLengthField == SIXTY_FOUR_BIT_EXTENDED_PAYLOAD_LENGTH) {
                        _bytesLeftToRead = 8;
                    }
                    if (_currentMsg.maskPresent == 1) {
                        _bytesLeftToRead += 4;
                    }
                    if (_bytesLeftToRead == 0) {
                        _readState = ReadState.READ_PAYLOAD;
                        _bytesLeftToRead = (int)_currentMsg.payloadLength;
                        _currentMsg.payload = new byte[(int)_currentMsg.payloadLength];
                    } else {
                        _readState = ReadState.READ_EXTENDED_HEADER;
                    }
                    break;
                    
                case READ_EXTENDED_HEADER:
                    if (_currentMsg.payloadLengthField == SIXTEEN_BIT_EXTENDED_PAYLOAD_LENGTH) {
                        byte[] payloadByteArray = new byte[2];
                        System.arraycopy(_buffer.getBackingArray(), _index, payloadByteArray, 0, 2);
                        _currentMsg.payloadLength = NetworkByteOrderNumberUtil.oneTwoThreeFourByteToInt(payloadByteArray);
                        _index += 2;
                    } else if (_currentMsg.payloadLengthField == SIXTY_FOUR_BIT_EXTENDED_PAYLOAD_LENGTH) {
                        byte[] payloadByteArray = new byte[8];
                        System.arraycopy(_buffer.getBackingArray(), _index, payloadByteArray, 0, 8);
                        _currentMsg.payloadLength = NetworkByteOrderNumberUtil.eightByteToUInt(payloadByteArray);
                        _index += 8;
                    }
                    if (_currentMsg.maskPresent == 1) {
                        _currentMsg.mask = new byte[4];
                        System.arraycopy(_buffer.getBackingArray(), _index, _currentMsg.mask, 0, 4);
                        _index += 4;
                    }
                    _currentMsg.payload = new byte[(int)_currentMsg.payloadLength];
                    _bytesLeftToRead = (int)_currentMsg.payloadLength;
                    _readState = ReadState.READ_PAYLOAD;
                    if (_bytesLeftToRead == 0) {
                        processWSMessageAndReset(argMap);
                    }
                    break;
                    
                case READ_PAYLOAD:
                    if (_currentMsg.payloadLength > 0) {
                        System.arraycopy(_buffer.getBackingArray(), _index, _currentMsg.payload, 0, (int)_currentMsg.payloadLength);
                        if (_currentMsg.maskPresent == 1) {
                            // need to XOR
                            for(int i = 0; i < _currentMsg.payloadLength; i++) {
                                _currentMsg.payload[i] = (byte)(_currentMsg.payload[i] ^ _currentMsg.mask[i % 4]);
                            }
                        }
                    }
                    if (_currentMsg.getMessageType().equals(WSMessageType.CONTINUATION_MESSAGE)) {
                        // TODO: This needs to be fixed up
                        _msgs.get(_msgs.size()-1).setNext(_currentMsg);
                        _currentMsg = new WSMessage();
                        _msgs.add(_currentMsg);
                    }
                    if (_currentMsg.fin == 1) {
                        processWSMessageAndReset(argMap);
                    }
                    break;
                }
            }
        }
    }

    private void parseHTTPMessage(Map<String, Object> argMap, ByteBuffer readBuffer) {
        while(readBuffer.hasRemaining()) {
            byte b = readBuffer.get();
            _buffer.writeByte(b);
            switch(_stage) {
            case 0:
                if (b == 13) {
                    _stage = 1;
                } else {
                    _header.writeByte(b);
                    _stage = 0;
                }
                break;
                
            case 1:
                if (b == 10) {
                    // end of a single header
                    if (_header.getLength() > 0) {
                        byte[] header = new byte[_header.getLength()];
                        System.arraycopy(_header.getBackingArray(), 0, header, 0, _header.getLength());
                        _headers.add(new String(header));
                        _header.clear();
                    }
                    _stage = 2;
                } else {
                    _header.writeByte(b);
                    _stage = 0;
                }
                break;
                
            case 2:
                if (b == 13) {
                    _stage = 3;
                } else {
                    _header.writeByte(b);
                    _stage = 0;
                }
                break;
                
            case 3:
                if (b == 10) {
                    // end of all headers
                    for(int i = 0; i < _headers.size(); i++) {
                        String header = _headers.get(i).trim().toLowerCase();
                        if (header.startsWith("content-length:")) {
                            _remaining = Integer.parseInt(header.substring("content-length:".length()).trim());
                            if (_remaining > 0) {
                                _payload = new byte[_remaining];
                            }
                        }
                    }
                    if (_remaining == 0) {
                        _stage = 0;
                        processHTTPMessageAndReset(argMap);
                    } else {
                        _stage = 4;
                    }
                } else {
                    _stage = 0;
                }
                break;
                
            case 4:
                _remaining--;
                _payload[_payloadIndex++] = b;
                if (_remaining <= 0) {
                    _stage = 0;
                    processHTTPMessageAndReset(argMap);
                }
                break;
            }
        }
    }
    
    /**
     * [Input] Called from a server side HTTPFramer on receiving a WebSocket 
     * upgrade response from the server. So you should have a link like the 
     * following in the config file:<p>
     * <code>
     * link.out.serverHTTPFramer.onWSUpgradeResponse=clientHTTPFramer.onWebSocketUpgrade()
     * </code>
     */
    @Input("onWebSocketUpgrade")
    public void onWebSocketUpgrade() {
        _webSocketMode = true;
    }

    /**
     * [output] Called on a framed http message.
     * 
     * @param msg
     */
    @Output("onHTTPMessage")
    public void onHTTPMessage(Map<String, Object> argMap, ByteBuffer buffer, String msg, HTTPMessage httpMsg) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("onHTTPMessage: \r\n" + msg);
        }
        next("onHTTPMessage", argMap, bytesHTTPMessage[0], buffer, stringHTTPMessage[0], msg, httpMessage[0], httpMsg);
    }

    /**
     * [output] Called on a framed ws message.
     * 
     * @param msg
     */
    @Output("onWSMessage")
    public void onWSMessage(Map<String, Object> argMap, ByteBuffer buffer, WSMessage wsMsg, ByteBuffer payload) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("onWSMessage: \r\n" + wsMsg);
        }
        next("onWSMessage", argMap, bytesWSMessage[0], buffer, wsMessage[0], wsMsg, bytesWSPayloadMessage[0], payload);
    }

    /**
     * [output] Called on receiving a WebSocket upgrade response from a
     * server.
     * 
     * @param argMap
     * @param msg
     */
    @Output("onWSUpgradeResponse")
    public void onWSUpgradeResponse(Map<String, Object> argMap, String msg) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("onWSUpgradeResponse: \r\n" + msg);
        }
        onWebSocketUpgrade();
        next("onWSUpgradeResponse", argMap);
    }

    private void processHTTPMessageAndReset(Map<String, Object> argMap) {
        byte[] data = new byte[_buffer.getLength()];
        System.arraycopy(_buffer.getBackingArray(), 0, data, 0, _buffer.getLength());
        String msg = new String(data);
        ByteBuffer output = ByteBuffer.allocate(_buffer.getLength());
        output.put(data);
        output.flip();
        HTTPMessage httpMsg = new HTTPMessage();
        if (_headers.size() > 0) {
            httpMsg.headers = new ArrayList<String>(_headers);
        }
        httpMsg.payload = _payload;
        _buffer.clear();
        _headers.clear();
        _remaining = 0;
        _payload = null;
        _payloadIndex = 0;
        if (isAWebSocketUpgradeResponse(httpMsg)) {
            onWSUpgradeResponse(argMap, msg);
        }
        onHTTPMessage(argMap, output, msg, httpMsg);
    }

    private void processWSMessageAndReset(Map<String, Object> argMap) {
        if (_msgs.size() == 0) {
            ByteBuffer buffer = ByteBuffer.allocate(_buffer.getLength());
            buffer.put(_buffer.getBackingArray(), 0, _buffer.getLength());
            buffer.flip();
            ByteBuffer payload = ByteBuffer.allocate(_currentMsg.payload.length);
            payload.put(_currentMsg.payload);
            payload.flip();
            onWSMessage(argMap, buffer, _currentMsg, payload);
        } else {
            for(int i = 0; i < _msgs.size(); i++) {
                ByteBuffer buffer = _buffers.get(i);
                buffer.flip();
                ByteBuffer payload = ByteBuffer.allocate(_currentMsg.payload.length);
                payload.put(_currentMsg.payload);
                payload.flip();
                WSMessage wsMsg = _msgs.get(i);
                onWSMessage(argMap, buffer, wsMsg, payload);
            }   
        }
        resetWSMessage();
    }

    private boolean isAWebSocketUpgradeResponse(HTTPMessage httpMsg) {
        boolean hasSwitchingProtocols = false;
        boolean upgradeWebsocket = false;
        boolean connectionUpgrade = false;
        boolean websocketAccept = false;
        if (httpMsg.headers != null) {
            for(int i = 0; i < httpMsg.headers.size(); i++) {
                String header = httpMsg.headers.get(i).trim().toLowerCase();
                if (header.startsWith("http/1.1 101 switching protocols")) {
                    hasSwitchingProtocols = true;
                } else if (header.startsWith("upgrade: websocket")) {
                    upgradeWebsocket = true;
                } else if (header.startsWith("connection: upgrade")) {
                    connectionUpgrade = true;
                } else if (header.startsWith("sec-websocket-accept:")) {
                    websocketAccept = true;
                }
            }
        }
        return hasSwitchingProtocols && upgradeWebsocket && connectionUpgrade && websocketAccept;
    }
    
    private void resetWSMessage() {
        _msgs.clear();
        _buffers.clear();
        _readState = ReadState.READ_HEADER;
        _bytesLeftToRead = 2;
        _index = 0;
        _currentMsg = new WSMessage();
        _buffer.clear();
    }
}
