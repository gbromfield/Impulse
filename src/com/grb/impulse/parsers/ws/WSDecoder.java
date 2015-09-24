package com.grb.impulse.parsers.ws;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import com.grb.impulse.parsers.http.HTTPMessage;
import com.grb.impulse.utils.NetworkByteOrderNumberUtil;

public class WSDecoder {
    
    private enum ReadState {
        READ_HEADER,
        READ_EXTENDED_HEADER,
        READ_PAYLOAD
    }

    public static final String WEBSOCKET_KEY = "Sec-WebSocket-Key: ";

    public static final int MAX_SIMPLE_PAYLOAD_LENGTH               = 125;
    public static final int SIXTEEN_BIT_EXTENDED_PAYLOAD_LENGTH     = 126;
    public static final int SIXTY_FOUR_BIT_EXTENDED_PAYLOAD_LENGTH  = 127;

    private ByteBuffer _buffer;
    private ReadState _readState;
    private int _bytesLeftToRead;
    private int _index;
    private ArrayList<WSMessage> _msgs;
    private WSMessage _currentMsg;
    
    public WSDecoder(int initialBufferSize) {
    	_buffer = ByteBuffer.allocate(initialBufferSize);
    	_msgs = new ArrayList<WSMessage>();
    	reset();
    }
    
    public String decodeHandshakeKey(ByteBuffer readBuffer) {
        byte[] b = readBuffer.array();
        String s = new String(b);
        String[] rows = s.split("\r\n");
        for(int i = 0; i < rows.length; i++) {
            if (rows[i].startsWith(WEBSOCKET_KEY)) {
                return rows[i].substring(WEBSOCKET_KEY.length());
            }
        }
        return null;
    }

    public String decodeHandshakeKey(HTTPMessage httpMessage) {
        if (httpMessage.headers != null) {
            for(String hdr : httpMessage.headers) {
                if (hdr.startsWith(WEBSOCKET_KEY)) {
                    return hdr.substring(WEBSOCKET_KEY.length());
                }
            }
        }
        return null;
    }

    public WSMessage decodeWSMessage(ByteBuffer readBuffer) {
        int available = readBuffer.limit() - readBuffer.position();
        if (available == 0) {
            return null;
        }
        if (_bytesLeftToRead > 0) {
            if (_bytesLeftToRead > (_buffer.capacity() - _buffer.position())) {
                ByteBuffer newBuffer = ByteBuffer.allocate(_buffer.capacity() + (_bytesLeftToRead - (_buffer.capacity() - _buffer.position())));
                _buffer.flip();
                newBuffer.put(_buffer);
                _buffer = newBuffer;
            }
            int bytesToRead = Math.min(_bytesLeftToRead, available);
            byte[] data = new byte[bytesToRead];
            readBuffer.get(data);
            _buffer.put(data);               
            _bytesLeftToRead -= bytesToRead;
        }
        if (_bytesLeftToRead == 0) {
            switch(_readState) {
            case READ_HEADER:
                byte b1 = _buffer.array()[0];
                _index++;
                byte b2 = _buffer.array()[1];
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
                } else {
                    _readState = ReadState.READ_EXTENDED_HEADER;
                }
                return decodeWSMessage(readBuffer);
                
            case READ_EXTENDED_HEADER:
                if (_currentMsg.payloadLengthField == SIXTEEN_BIT_EXTENDED_PAYLOAD_LENGTH) {
                    byte[] payloadByteArray = new byte[2];
                    System.arraycopy(_buffer.array(), _index, payloadByteArray, 0, 2);
                    _currentMsg.payloadLength = NetworkByteOrderNumberUtil.oneTwoThreeFourByteToInt(payloadByteArray);
                    _index += 2;
                } else if (_currentMsg.payloadLengthField == SIXTY_FOUR_BIT_EXTENDED_PAYLOAD_LENGTH) {
                    byte[] payloadByteArray = new byte[8];
                    System.arraycopy(_buffer.array(), _index, payloadByteArray, 0, 8);
                    _currentMsg.payloadLength = NetworkByteOrderNumberUtil.eightByteToUInt(payloadByteArray);
                    _index += 8;
                }
                if (_currentMsg.maskPresent == 1) {
                    _currentMsg.mask = new byte[4];
                    System.arraycopy(_buffer.array(), _index, _currentMsg.mask, 0, 4);
                    _index += 4;
                }
                _currentMsg.payload = new byte[(int)_currentMsg.payloadLength];
                _bytesLeftToRead = (int)_currentMsg.payloadLength;
                _readState = ReadState.READ_PAYLOAD;
                if (_bytesLeftToRead > 0) {
                    return decodeWSMessage(readBuffer);
                }
                
            case READ_PAYLOAD:
            	if (_currentMsg.payloadLength > 0) {
                    System.arraycopy(_buffer.array(), _index, _currentMsg.payload, 0, (int)_currentMsg.payloadLength);
                    if (_currentMsg.maskPresent == 1) {
                        // need to XOR
                        for(int i = 0; i < _currentMsg.payloadLength; i++) {
                            _currentMsg.payload[i] = (byte)(_currentMsg.payload[i] ^ _currentMsg.mask[i % 4]);
                        }
                    }
            	}
                if (_currentMsg.getMessageType().equals(WSMessageType.CONTINUATION_MESSAGE)) {
             		_msgs.get(_msgs.size()-1).setNext(_currentMsg);
               		_currentMsg = new WSMessage();
               		_msgs.add(_currentMsg);
                }
                if (_currentMsg.fin == 1) {
                	WSMessage retMsg = _msgs.get(0);
                	reset();
                	return retMsg;
                }
            }
        }
        return null;
    }
    
    private void reset() {
        _msgs.clear();
        _readState = ReadState.READ_HEADER;
        _bytesLeftToRead = 2;
        _index = 0;
        _currentMsg = new WSMessage();
        _msgs.add(_currentMsg);
        _buffer.clear();
    }
}
