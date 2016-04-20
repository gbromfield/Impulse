package com.grb.impulse.parsers.cli;

import com.grb.util.ByteBuffer;

/**
 * Created by gbromfie on 4/18/16.
 */
public class CLIMessage {
    protected String _messageStr;
    protected ByteBuffer _message;
    protected String _promptStr;
    protected ByteBuffer _prompt;

    public CLIMessage(ByteBuffer buffer) {
        this(buffer, 0, buffer.getLength(), 0);
    }

    public CLIMessage(ByteBuffer buffer, int offset, int length, int promptLength) {
        byte[] backing = buffer.getBackingArray();
        StringBuilder bldr = new StringBuilder();
        for(int i = offset; i < (offset + length); i++) {
            bldr.append((char)backing[i]);
        }
        _messageStr = bldr.toString();
        _message = new ByteBuffer(length);
        _message.writeBytes(backing, offset, length);
        if (promptLength > 0) {
            bldr = new StringBuilder();
            for(int i = 0; i < promptLength; i++) {
                bldr.append((char)backing[length+i]);
            }
            _promptStr = bldr.toString();
            _prompt = new ByteBuffer(promptLength);
            _prompt.writeBytes(backing, length, promptLength);
        }
    }

    public ByteBuffer getMessage() {
        return _message;
    }

    public String getMessageStr() {
        return _messageStr;
    }

    public ByteBuffer getPrompt() {
        return _prompt;
    }

    public String getPromptStr() {
        return _promptStr;
    }

    public String toString() {
        return getMessageStr();
    }
}
