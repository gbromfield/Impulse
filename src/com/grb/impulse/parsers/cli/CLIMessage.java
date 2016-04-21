package com.grb.impulse.parsers.cli;

import com.grb.util.ByteBuffer;

/**
 * Created by gbromfie on 4/18/16.
 */
public class CLIMessage {
    protected int _terminalLength;
    protected ByteBuffer _message;

    public CLIMessage(ByteBuffer buffer, int terminalLength) {
        this(buffer, 0, buffer.getLength(), terminalLength);
    }

    public CLIMessage(ByteBuffer buffer, int offset, int length, int terminalLength) {
        _message = new ByteBuffer(length);
        _message.write(buffer.getBackingArray(), offset, length);
        _terminalLength = terminalLength;
    }

    public ByteBuffer getData() {
        ByteBuffer b = new ByteBuffer(_message.getLength()-_terminalLength);
        b.writeBytes(_message.getBackingArray(), 0, _message.getLength()-_terminalLength);
        return b;
    }

    public String getDataStr() {
        StringBuilder bldr = new StringBuilder();
        for(int i = 0; i < (_message.getLength()-_terminalLength); i++) {
            bldr.append((char)_message.getBackingArray()[i]);
        }
        return bldr.toString();
    }

    public ByteBuffer getTerminal() {
        ByteBuffer b = new ByteBuffer(_terminalLength);
        b.writeBytes(_message.getBackingArray(), _message.getLength() - _terminalLength, _terminalLength);
        return b;
    }

    public String getTerminalStr() {
        StringBuilder bldr = new StringBuilder();
        for(int i = _message.getLength()-_terminalLength; i < _message.getLength(); i++) {
            bldr.append((char)_message.getBackingArray()[i]);
        }
        return bldr.toString();
    }

    public ByteBuffer getMessage() {
        return _message;
    }

    public String getMessageStr() {
        StringBuilder bldr = new StringBuilder();
        for(int i = 0; i < _message.getLength(); i++) {
            bldr.append((char)_message.getBackingArray()[i]);
        }
        return bldr.toString();
    }

    public String toString() {
        return getMessageStr();
    }
}
