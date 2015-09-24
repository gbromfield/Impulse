package com.grb.impulse.parsers.ws;



public class WSMessage {
        
    public int fin = -1;
    public int rsv1 = -1;
    public int rsv2 = -1;
    public int rsv3 = -1;
    public int opcode = -1;
    public int maskPresent = -1;
    public int payloadLengthField = -1;
    public long payloadLength = -1;
    public byte[] mask = null;
    public byte[] payload = null;
    private WSMessage _next = null;
    
    public WSMessage() {
    }
    
    public WSMessage(String str) {
        fin = 1;
        rsv1 = 0;
        rsv2 = 0;
        rsv3 = 0;
        opcode = WSMessageType.TEXT_MESSAGE.getOpcode();
        maskPresent = 0;
        payload = str.getBytes();
        payloadLength = payload.length;
        mask = null;
    }
        
    public WSMessageType getMessageType() {
        return WSMessageType.valueOf(opcode);
    }

    public String getText() {
        return new String(payload);
    }
    
    public void setNext(WSMessage next) {
    	_next = next;
    }

    public WSMessage getNext() {
    	return _next;
    }
 
    @Override
    public String toString() {
        StringBuilder bldr = new StringBuilder();
        bldr.append("fin=");
        bldr.append(fin);
        bldr.append(", rsv1=");
        bldr.append(rsv1);
        bldr.append(", rsv2=");
        bldr.append(rsv2);
        bldr.append(", rsv3=");
        bldr.append(rsv3);
        bldr.append(", opcode=");
        bldr.append(getMessageType());
        bldr.append(", maskPresent=");
        bldr.append(maskPresent);
        bldr.append(", payloadLength=");
        bldr.append(payloadLength);
        if ((getMessageType().equals(WSMessageType.TEXT_MESSAGE)) && (payload != null)) {
            bldr.append(", payload=\"");
            String txt = new String(payload);
            bldr.append(txt);
            bldr.append("\"");
        }
        return bldr.toString();
    }
}
