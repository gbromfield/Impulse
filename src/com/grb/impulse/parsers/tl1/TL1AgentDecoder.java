package com.grb.impulse.parsers.tl1;

import java.nio.ByteBuffer;

/**
 * Created by gbromfie on 11/9/15.
 */
public class TL1AgentDecoder extends TL1Decoder {
    private com.grb.util.ByteBuffer _buffer;

    public TL1AgentDecoder() {
        _buffer = new com.grb.util.ByteBuffer(100);
    }

    public TL1Message decodeTL1Message(ByteBuffer readBuffer) throws TL1MessageMaxSizeExceededException {
        while(readBuffer.hasRemaining()) {
            if (_message == null) {
                byte b = readBuffer.get();
                _buffer.writeByte(b);
                byte[] backingArray = _buffer.getBackingArray();
                int idx = _buffer.getLength();
                if (idx > TL1OutputMessage.MAX_SIZE) {
                    _buffer.clear();
                    _message = null;
                    throw new TL1MessageMaxSizeExceededException(String.format("Error: maximum %d character size of output message reached", TL1OutputMessage.MAX_SIZE));
                }
                if (idx >= 3) {
                    if (arrayContains(backingArray, TL1IPAckMessage.preamble, 0, TL1IPAckMessage.preamble.length)) {
                        _message = new TL1IPAckMessage();
                    } else if (arrayContains(backingArray, TL1NAAckMessage.preamble, 0, TL1NAAckMessage.preamble.length)) {
                        _message = new TL1NAAckMessage();
                    } else if (arrayContains(backingArray, TL1NGAckMessage.preamble, 0, TL1NGAckMessage.preamble.length)) {
                        _message = new TL1NGAckMessage();
                    } else if (arrayContains(backingArray, TL1OKAckMessage.preamble, 0, TL1OKAckMessage.preamble.length)) {
                        _message = new TL1OKAckMessage();
                    } else if (arrayContains(backingArray, TL1PFAckMessage.preamble, 0, TL1PFAckMessage.preamble.length)) {
                        _message = new TL1PFAckMessage();
                    } else if (arrayContains(backingArray, TL1RLAckMessage.preamble, 0, TL1RLAckMessage.preamble.length)) {
                        _message = new TL1RLAckMessage();
                    }
                }
                if ((_message == null) && (idx >= 4)) {
                    if (arrayContains(backingArray, TL1OutputMessage.responseCode, idx - TL1OutputMessage.responseCode.length, TL1OutputMessage.responseCode.length)) {
                        _message = new TL1ResponseMessage(backingArray, 0, idx);
                    } else if ((arrayContains(backingArray, TL1OutputMessage.criticalAlarmCode, idx - TL1OutputMessage.criticalAlarmCode.length, TL1OutputMessage.criticalAlarmCode.length)) ||
                            (arrayContains(backingArray, TL1OutputMessage.majorAlarmCode, idx - TL1OutputMessage.majorAlarmCode.length, TL1OutputMessage.majorAlarmCode.length)) ||
                            (arrayContains(backingArray, TL1OutputMessage.minorAlarmCode, idx - TL1OutputMessage.minorAlarmCode.length, TL1OutputMessage.minorAlarmCode.length)) ||
                            (arrayContains(backingArray, TL1OutputMessage.nonAlarmCode, idx - TL1OutputMessage.nonAlarmCode.length, TL1OutputMessage.nonAlarmCode.length))) {
                        _message = new TL1AOMessage(backingArray, 0, idx);
                    }
                }
            } else {
                try {
                    if (_message.parse(readBuffer)) {
                        _buffer.clear();
                        TL1Message tmp = _message;
                        _message = null;
                        return tmp;
                    }
                } catch (TL1MessageMaxSizeExceededException e) {
                    _buffer.clear();
                    _message = null;
                    throw e;
                }
            }
        }
        return null;
    }

    public static void main(String[] args) {
        TL1Decoder decoder = new TL1AgentDecoder();
        StringBuilder bldr = new StringBuilder(TL1OutputMessage.MAX_SIZE + 1);
        bldr.append("\r\n\n ");
        for(int i = 0; i < TL1OutputMessage.MAX_SIZE - 3; i++) {
            bldr.append('X');
        }
        String outputMessageOverflow = bldr.toString();
        bldr = new StringBuilder(TL1OutputMessage.MAX_SIZE + 1);
        bldr.append("\r\n\n HOLMNJCRK01 85-10-09 22:05:12\r\nM ");
        int len = bldr.length();
        for(int i = 0; i < TL1OutputMessage.MAX_SIZE - len + 1; i++) {
            bldr.append('X');
        }
        String responseMessageOverflow = bldr.toString();
        bldr = new StringBuilder(TL1OutputMessage.MAX_SIZE + 1);
        bldr.append("\r\n\n   BOCAFLMA015 93-06-02 12:00:00\r\n*C");
        len = bldr.length();
        for(int i = 0; i < TL1OutputMessage.MAX_SIZE - len + 1; i++) {
            bldr.append('X');
        }
        String aoMessageOverflow = bldr.toString();
        String[][] strs = {
                {"          WELCOME TO PARSING.... !\r", "\n", "\n", "   HOLMNJCRK01", " 85-10-09 22:05:12\r", "\nM  0001 COMPLD\r\n;", "TL1ResponseMessage", "          WELCOME TO PARSING.... !\r\n\n   HOLMNJCRK01 85-10-09 22:05:12\r\nM  0001 COMPLD\r\n;"},
                {outputMessageOverflow, "TL1MessageMaxSizeExceededException", "Error: maximum 8192 character size of output message reached"},
                {responseMessageOverflow, "TL1MessageMaxSizeExceededException", "Error: maximum 8192 character size of output message reached"},
                {aoMessageOverflow, "TL1MessageMaxSizeExceededException", "Error: maximum 8192 character size of output message reached"},
                {"IP 0001\r\n<", "TL1IPAckMessage", "IP 0001\r\n<"},
                {"PF 0001\r\n<", "TL1PFAckMessage", "PF 0001\r\n<"},
                {"OK 0001\r\n<", "TL1OKAckMessage", "OK 0001\r\n<"},
                {"NA 0001\r\n<", "TL1NAAckMessage", "NA 0001\r\n<"},
                {"NG 0001\r\n<", "TL1NGAckMessage", "NG 0001\r\n<"},
                {"RL 0001\r\n<", "TL1RLAckMessage", "RL 0001\r\n<"},
                {"\r", "\n", "\n", "   HOLMNJCRK01", " 85-10-09 22:05:12\r", "\nM  0001 COMPLD\r\n;", "TL1ResponseMessage", "\r\n\n   HOLMNJCRK01 85-10-09 22:05:12\r\nM  0001 COMPLD\r\n;"},
                {"IP 0001\r", "\n<", "TL1IPAckMessage", "IP 0001\r\n<"},
                {"IP 0001\r", "\n<IP 0002\r\n<", "TL1IPAckMessage", "IP 0001\r\n<", "TL1IPAckMessage", "IP 0002\r\n<"},
                {"\r\n\n   HOLMNJCRK01 85-10-09 22:05:12\r\nM  0001 COMPLD\r\n;", "TL1ResponseMessage", "\r\n\n   HOLMNJCRK01 85-10-09 22:05:12\r\nM  0001 COMPLD\r\n;"},
                {"\r\n\n   HOLMNJCRK01 85-10-09 22:05:12\r\nM  0001 COMPLD\r\n;IP 0001\r\n<", "TL1ResponseMessage", "\r\n\n   HOLMNJCRK01 85-10-09 22:05:12\r\nM  0001 COMPLD\r\n;", "TL1IPAckMessage", "IP 0001\r\n<"},
                {"\r\n\n   HOLMNJCRK01 85-10-09 22:05:12\r\nM  0001 COMPLD\r\n;IP 0001\r\n<\r\n\n   HOLMNJCRK01 85-10-09 22:05:12\r\nM  0001 COMPLD\r\n;", "TL1ResponseMessage", "\r\n\n   HOLMNJCRK01 85-10-09 22:05:12\r\nM  0001 COMPLD\r\n;", "TL1IPAckMessage", "IP 0001\r\n<", "TL1ResponseMessage", "\r\n\n   HOLMNJCRK01 85-10-09 22:05:12\r\nM  0001 COMPLD\r\n;"},
                {"\r\n\n   BOCAFLMA015 93-06-02 12:00:00\r\n*C 789 REPT PM T1\r\n   \"AID-T1-1:CVL,50\"\r\n   \"AID-T1-2:CVL,10\"\r\n   \"AID-T1-n:CVL,22\"\r\n;", "TL1AOMessage", "\r\n\n   BOCAFLMA015 93-06-02 12:00:00\r\n*C 789 REPT PM T1\r\n   \"AID-T1-1:CVL,50\"\r\n   \"AID-T1-2:CVL,10\"\r\n   \"AID-T1-n:CVL,22\"\r\n;"},
                {"\r\n\n   BOCAFLMA015 93-06-02 12:00:00\r\n** 789 REPT PM T1\r\n   \"AID-T1-1:CVL,50\"\r\n   \"AID-T1-2:CVL,10\"\r\n   \"AID-T1-n:CVL,22\"\r\n;", "TL1AOMessage", "\r\n\n   BOCAFLMA015 93-06-02 12:00:00\r\n** 789 REPT PM T1\r\n   \"AID-T1-1:CVL,50\"\r\n   \"AID-T1-2:CVL,10\"\r\n   \"AID-T1-n:CVL,22\"\r\n;"},
                {"\r\n\n   BOCAFLMA015 93-06-02 12:00:00\r\n*  789 REPT PM T1\r\n   \"AID-T1-1:CVL,50\"\r\n   \"AID-T1-2:CVL,10\"\r\n   \"AID-T1-n:CVL,22\"\r\n;", "TL1AOMessage", "\r\n\n   BOCAFLMA015 93-06-02 12:00:00\r\n*  789 REPT PM T1\r\n   \"AID-T1-1:CVL,50\"\r\n   \"AID-T1-2:CVL,10\"\r\n   \"AID-T1-n:CVL,22\"\r\n;"},
                {"\r\n\n   BOCAFLMA015 93-06-02 12:00:00\r\nA  789 REPT PM T1\r\n   \"AID-T1-1:CVL,50\"\r\n   \"AID-T1-2:CVL,10\"\r\n   \"AID-T1-n:CVL,22\"\r\n;", "TL1AOMessage", "\r\n\n   BOCAFLMA015 93-06-02 12:00:00\r\nA  789 REPT PM T1\r\n   \"AID-T1-1:CVL,50\"\r\n   \"AID-T1-2:CVL,10\"\r\n   \"AID-T1-n:CVL,22\"\r\n;"},
                {"\r\n\n   BOCAFLMA015 93-06-02 12:00:00\r\nA  789 REPT PM T1\r\n   \"AID-T1-1:CVL,50\"\r\n   \"AID-T1-2:CVL,10\"\r\n   \"AID-T1-n:CVL,22\"\r\n;\r\n\n   BOCAFLMA015 93-06-02 12:00:00\r\nA  789 REPT PM T1\r\n   \"AID-T1-1:CVL,50\"\r\n   \"AID-T1-2:CVL,10\"\r\n   \"AID-T1-n:CVL,22\"\r\n;", "TL1AOMessage", "\r\n\n   BOCAFLMA015 93-06-02 12:00:00\r\nA  789 REPT PM T1\r\n   \"AID-T1-1:CVL,50\"\r\n   \"AID-T1-2:CVL,10\"\r\n   \"AID-T1-n:CVL,22\"\r\n;", "TL1AOMessage", "\r\n\n   BOCAFLMA015 93-06-02 12:00:00\r\nA  789 REPT PM T1\r\n   \"AID-T1-1:CVL,50\"\r\n   \"AID-T1-2:CVL,10\"\r\n   \"AID-T1-n:CVL,22\"\r\n;"},
                {"\r\n", "\n   BOCAFLMA015", " 93-06-02 12:00:00\r\n", "A  789 REPT PM T1\r\n   \"AID-T1-1:CVL,50\"\r\n   \"AID-T1-2:CVL,10\"\r\n   \"AID-T1-n:CVL,22\"\r\n;", "TL1AOMessage", "\r\n\n   BOCAFLMA015 93-06-02 12:00:00\r\nA  789 REPT PM T1\r\n   \"AID-T1-1:CVL,50\"\r\n   \"AID-T1-2:CVL,10\"\r\n   \"AID-T1-n:CVL,22\"\r\n;"},
                {"\r\n\n   BOCAFLMA015 93-06-02 12:00:00\r\nA  789 REPT PM T1\r\n   \"AID-T1-1:CVL,50\"\r\n   \"AID-T1-2:CVL,10\"\r\n   \"AID-T1-n:CVL,22\"\r\n;IP 0001\r\n<\r\n\n   HOLMNJCRK01 85-10-09 22:05:12\r\nM  0001 COMPLD\r\n;", "TL1AOMessage", "\r\n\n   BOCAFLMA015 93-06-02 12:00:00\r\nA  789 REPT PM T1\r\n   \"AID-T1-1:CVL,50\"\r\n   \"AID-T1-2:CVL,10\"\r\n   \"AID-T1-n:CVL,22\"\r\n;", "TL1IPAckMessage", "IP 0001\r\n<", "TL1ResponseMessage", "\r\n\n   HOLMNJCRK01 85-10-09 22:05:12\r\nM  0001 COMPLD\r\n;"},
                {"IP 0001\r\n<\r\n\n   HOLMNJCRK01 85-10-09 22:05:12\r\nM  0001 COMPLD\r\n;\r\n\n   BOCAFLMA015 93-06-02 12:00:00\r\nA  789 REPT PM T1\r\n   \"AID-T1-1:CVL,50\"\r\n   \"AID-T1-2:CVL,10\"\r\n   \"AID-T1-n:CVL,22\"\r\n;IP 0001\r\n<\r\n\n   HOLMNJCRK01 85-10-09 22:05:12\r\nM  0001 COMPLD\r\n;", "TL1IPAckMessage", "IP 0001\r\n<", "TL1ResponseMessage", "\r\n\n   HOLMNJCRK01 85-10-09 22:05:12\r\nM  0001 COMPLD\r\n;", "TL1AOMessage", "\r\n\n   BOCAFLMA015 93-06-02 12:00:00\r\nA  789 REPT PM T1\r\n   \"AID-T1-1:CVL,50\"\r\n   \"AID-T1-2:CVL,10\"\r\n   \"AID-T1-n:CVL,22\"\r\n;", "TL1IPAckMessage", "IP 0001\r\n<", "TL1ResponseMessage", "\r\n\n   HOLMNJCRK01 85-10-09 22:05:12\r\nM  0001 COMPLD\r\n;"},
        };
        int[] results = new int[2];
        results[0] = 0;
        results[1] = 0;
        for(int i = 0; i < strs.length; i++) {
            test(decoder, results, strs[i]);
        }
        System.out.println(String.format("Total: %d, Success: %d, Failed:%d", results[0] + results[1], results[0], results[1]));
    }
}
