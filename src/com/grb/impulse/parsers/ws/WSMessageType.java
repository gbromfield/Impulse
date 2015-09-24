package com.grb.impulse.parsers.ws;

public enum WSMessageType {
    CONTINUATION_MESSAGE(0x0),
    TEXT_MESSAGE(0x1),
    BINARY_MESSAGE(0x2), 
    CONNECTION_CLOSE(0x8),
    PING(0x9),
    PONG(0xA);
    
    private int _opcode;
    
    private WSMessageType(int opcode) {
        _opcode = opcode;
    }
    
    public int getOpcode() {
        return _opcode;
    }
    
    static WSMessageType valueOf(int opcode) {
        WSMessageType[] types = values();
        for(int i = 0; i < types.length; i++) {
            if (types[i].getOpcode() == opcode) {
                return types[i];
            }
        }
        return null;
    }
}
