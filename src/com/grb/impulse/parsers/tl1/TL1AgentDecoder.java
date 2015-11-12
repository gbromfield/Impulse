package com.grb.impulse.parsers.tl1;

import org.omg.CORBA.UNKNOWN;

import java.nio.ByteBuffer;

/**
 * Created by gbromfie on 11/9/15.
 *
 * This parser handles non TL1 text between messages but with one caveat:
 * It is going to fail if
 * 1. there is an "IP ", "RL ", etc... followed later by a "/r/n<" in the non-TL1 text, or
 * 2. there is an "\r\n\n   " in the non-TL1 text
 */
public class TL1AgentDecoder extends TL1Decoder {

    private enum ParseState {
        UNKNOWN,
        POSSIBLE_ACK,
        OUTPUT_MESSAGE,
        KNOWN
    }

    private com.grb.util.ByteBuffer _buffer;
    private int _startIdx;
    private ParseState _state;

    public TL1AgentDecoder() {
        _buffer = new com.grb.util.ByteBuffer(100);
        reset();
    }

    public TL1Message decodeTL1Message(ByteBuffer readBuffer) throws TL1MessageMaxSizeExceededException {
        while(readBuffer.hasRemaining()) {
            if (!_state.equals(ParseState.KNOWN)) {
                byte b = readBuffer.get();
                _buffer.writeByte(b);
                byte[] backingArray = _buffer.getBackingArray();
                int idx = _buffer.getLength();
                if (idx > TL1OutputMessage.MAX_SIZE) {
                    reset();
                    throw new TL1MessageMaxSizeExceededException(String.format("Error: maximum %d character size of output message reached", TL1OutputMessage.MAX_SIZE));
                }
                if ((!_state.equals(ParseState.OUTPUT_MESSAGE)) && (idx >= TL1AckMessage.PREAMBLE_FINGERPRINT_SIZE)) {
                    if (arrayContains(backingArray, TL1IPAckMessage.PREAMBLE, idx - TL1AckMessage.PREAMBLE_FINGERPRINT_SIZE, TL1AckMessage.PREAMBLE_FINGERPRINT_SIZE)) {
                        _message = new TL1IPAckMessage();
                        _startIdx = idx - TL1AckMessage.PREAMBLE_FINGERPRINT_SIZE;
                        _state = ParseState.POSSIBLE_ACK;
                    } else if (arrayContains(backingArray, TL1NAAckMessage.PREAMBLE, idx - TL1AckMessage.PREAMBLE_FINGERPRINT_SIZE, TL1AckMessage.PREAMBLE_FINGERPRINT_SIZE)) {
                        _message = new TL1NAAckMessage();
                        _startIdx = idx - TL1AckMessage.PREAMBLE_FINGERPRINT_SIZE;
                        _state = ParseState.POSSIBLE_ACK;
                    } else if (arrayContains(backingArray, TL1NGAckMessage.PREAMBLE, idx - TL1AckMessage.PREAMBLE_FINGERPRINT_SIZE, TL1AckMessage.PREAMBLE_FINGERPRINT_SIZE)) {
                        _message = new TL1NGAckMessage();
                        _startIdx = idx - TL1AckMessage.PREAMBLE_FINGERPRINT_SIZE;
                        _state = ParseState.POSSIBLE_ACK;
                    } else if (arrayContains(backingArray, TL1OKAckMessage.PREAMBLE, idx - TL1AckMessage.PREAMBLE_FINGERPRINT_SIZE, TL1AckMessage.PREAMBLE_FINGERPRINT_SIZE)) {
                        _message = new TL1OKAckMessage();
                        _startIdx = idx - TL1AckMessage.PREAMBLE_FINGERPRINT_SIZE;
                        _state = ParseState.POSSIBLE_ACK;
                    } else if (arrayContains(backingArray, TL1PFAckMessage.PREAMBLE, idx - TL1AckMessage.PREAMBLE_FINGERPRINT_SIZE, TL1AckMessage.PREAMBLE_FINGERPRINT_SIZE)) {
                        _message = new TL1PFAckMessage();
                        _startIdx = idx - TL1AckMessage.PREAMBLE_FINGERPRINT_SIZE;
                        _state = ParseState.POSSIBLE_ACK;
                    } else if (arrayContains(backingArray, TL1RLAckMessage.PREAMBLE, idx - TL1AckMessage.PREAMBLE_FINGERPRINT_SIZE, TL1AckMessage.PREAMBLE_FINGERPRINT_SIZE)) {
                        _message = new TL1RLAckMessage();
                        _startIdx = idx - TL1AckMessage.PREAMBLE_FINGERPRINT_SIZE;
                        _state = ParseState.POSSIBLE_ACK;
                    }
                }
                if (_state.equals(ParseState.POSSIBLE_ACK)) {
                    if (arrayContains(backingArray, TL1IPAckMessage.PROLOGUE, idx - TL1IPAckMessage.PROLOGUE.length, TL1IPAckMessage.PROLOGUE.length)) {
                        ((TL1AckMessage)_message).parse(backingArray, 0, _startIdx, idx);
                        return reset();
                    }
                }
                if ((!_state.equals(ParseState.OUTPUT_MESSAGE)) && (idx >= TL1OutputMessage.PREAMBLE.length)) {
                    if (arrayContains(backingArray, TL1OutputMessage.PREAMBLE, idx - TL1OutputMessage.PREAMBLE.length, TL1OutputMessage.PREAMBLE.length)) {
                        _startIdx = idx - TL1OutputMessage.PREAMBLE.length;
                        _state = ParseState.OUTPUT_MESSAGE;
                    }
                }
                if ((_state.equals(ParseState.OUTPUT_MESSAGE)) && (idx >= TL1OutputMessage.FINGERPRINT_SIZE)) {
                    if (arrayContains(backingArray, TL1OutputMessage.RESPONSE_CODE, idx - TL1OutputMessage.RESPONSE_CODE.length, TL1OutputMessage.RESPONSE_CODE.length)) {
                        _message = new TL1ResponseMessage(backingArray, 0, _startIdx, idx);
                        _state = ParseState.KNOWN;
                    } else if ((arrayContains(backingArray, TL1OutputMessage.CRITICAL_ALARM_CODE, idx - TL1OutputMessage.CRITICAL_ALARM_CODE.length, TL1OutputMessage.CRITICAL_ALARM_CODE.length)) ||
                            (arrayContains(backingArray, TL1OutputMessage.MAJOR_ALARM_CODE, idx - TL1OutputMessage.MAJOR_ALARM_CODE.length, TL1OutputMessage.MAJOR_ALARM_CODE.length)) ||
                            (arrayContains(backingArray, TL1OutputMessage.MINOR_ALARM_CODE, idx - TL1OutputMessage.MINOR_ALARM_CODE.length, TL1OutputMessage.MINOR_ALARM_CODE.length)) ||
                            (arrayContains(backingArray, TL1OutputMessage.NON_ALARM_CODE, idx - TL1OutputMessage.NON_ALARM_CODE.length, TL1OutputMessage.NON_ALARM_CODE.length))) {
                        _message = new TL1AOMessage(backingArray, 0, _startIdx, idx);
                        _state = ParseState.KNOWN;
                    }
                }
            } else {
                try {
                    if (_message.parse(readBuffer)) {
                        return reset();
                    }
                } catch (TL1MessageMaxSizeExceededException e) {
                    reset();
                    throw e;
                }
            }
        }
        return null;
    }

    private TL1Message reset() {
        _buffer.clear();
        TL1Message tmp = _message;
        _message = null;
        _startIdx = -1;
        _state = ParseState.UNKNOWN;
        return tmp;
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
                {"PF 0001\r\n<IP GAGABLUB YYY \r\n <;   OK 0002\r\n<", "TL1PFAckMessage", "PF 0001\r\n<", "0", "TL1OKAckMessage", "IP GAGABLUB YYY \r\n <;   OK 0002\r\n<", "24"},
                {"IP IP 0001\r\n<", "TL1IPAckMessage", "IP IP 0001\r\n<", "3"},
                {"          WELCOME TO PARSING.... !;<IP 0001\r\n<", "TL1IPAckMessage", "          WELCOME TO PARSING.... !;<IP 0001\r\n<", "36"},
                {"          WELCOME TO PARSING.... !;<\r", "\n", "\n", "   HOLMNJCRK01", " 85-10-09 22:05:12\r", "\nM  0001 COMPLD\r\n;", "TL1ResponseMessage", "          WELCOME TO PARSING.... !;<\r\n\n   HOLMNJCRK01 85-10-09 22:05:12\r\nM  0001 COMPLD\r\n;", "36"},
                {"          WELCOME TO PARSING.... !;<\r\n\n   BOCAFLMA015 93-06-02 12:00:00\r\n*C 789 REPT PM T1\r\n   \"AID-T1-1:CVL,50\"\r\n   \"AID-T1-2:CVL,10\"\r\n   \"AID-T1-n:CVL,22\"\r\n;", "TL1AOMessage", "          WELCOME TO PARSING.... !;<\r\n\n   BOCAFLMA015 93-06-02 12:00:00\r\n*C 789 REPT PM T1\r\n   \"AID-T1-1:CVL,50\"\r\n   \"AID-T1-2:CVL,10\"\r\n   \"AID-T1-n:CVL,22\"\r\n;", "36"},
                {outputMessageOverflow, "TL1MessageMaxSizeExceededException", "Error: maximum 8192 character size of output message reached"},
                {responseMessageOverflow, "TL1MessageMaxSizeExceededException", "Error: maximum 8192 character size of output message reached"},
                {aoMessageOverflow, "TL1MessageMaxSizeExceededException", "Error: maximum 8192 character size of output message reached"},
                {"IP 0001\r\n<", "TL1IPAckMessage", "IP 0001\r\n<", "0"},
                {"PF 0001\r\n<", "TL1PFAckMessage", "PF 0001\r\n<", "0"},
                {"OK 0001\r\n<", "TL1OKAckMessage", "OK 0001\r\n<", "0"},
                {"NA 0001\r\n<", "TL1NAAckMessage", "NA 0001\r\n<", "0"},
                {"NG 0001\r\n<", "TL1NGAckMessage", "NG 0001\r\n<", "0"},
                {"RL 0001\r\n<", "TL1RLAckMessage", "RL 0001\r\n<", "0"},
                {"IP \r", "\n", "\n", "   HOLMNJCRK01", " 85-10-09 22:05:12\r", "\nM  0001 COMPLD\r\n;", "TL1ResponseMessage", "IP \r\n\n   HOLMNJCRK01 85-10-09 22:05:12\r\nM  0001 COMPLD\r\n;", "3"},
                {"IP 0001\r", "\n<", "TL1IPAckMessage", "IP 0001\r\n<", "0"},
                {"IP 0001\r", "\n<IP 0002\r\n<", "TL1IPAckMessage", "IP 0001\r\n<", "0", "TL1IPAckMessage", "IP 0002\r\n<", "0"},
                {"\r\n\n   HOLMNJCRK01 85-10-09 22:05:12\r\nM  0001 COMPLD\r\n;", "TL1ResponseMessage", "\r\n\n   HOLMNJCRK01 85-10-09 22:05:12\r\nM  0001 COMPLD\r\n;", "0"},
                {"\r\n\n   HOLMNJCRK01 85-10-09 22:05:12\r\nM  0001 COMPLD\r\n;IP 0001\r\n<", "TL1ResponseMessage", "\r\n\n   HOLMNJCRK01 85-10-09 22:05:12\r\nM  0001 COMPLD\r\n;", "0", "TL1IPAckMessage", "IP 0001\r\n<", "0"},
                {"\r\n\n   HOLMNJCRK01 85-10-09 22:05:12\r\nM  0001 COMPLD\r\n;IP 0001\r\n<\r\n\n   HOLMNJCRK01 85-10-09 22:05:12\r\nM  0001 COMPLD\r\n;", "TL1ResponseMessage", "\r\n\n   HOLMNJCRK01 85-10-09 22:05:12\r\nM  0001 COMPLD\r\n;", "0", "TL1IPAckMessage", "IP 0001\r\n<", "0", "TL1ResponseMessage", "\r\n\n   HOLMNJCRK01 85-10-09 22:05:12\r\nM  0001 COMPLD\r\n;", "0"},
                {"IP \r\n\n   BOCAFLMA015 93-06-02 12:00:00\r\n*C 789 REPT PM T1\r\n   \"AID-T1-1:CVL,50\"\r\n   \"AID-T1-2:CVL,10\"\r\n   \"AID-T1-n:CVL,22\"\r\n;", "TL1AOMessage", "IP \r\n\n   BOCAFLMA015 93-06-02 12:00:00\r\n*C 789 REPT PM T1\r\n   \"AID-T1-1:CVL,50\"\r\n   \"AID-T1-2:CVL,10\"\r\n   \"AID-T1-n:CVL,22\"\r\n;", "3"},
                {"\r\n\n   BOCAFLMA015 93-06-02 12:00:00\r\n** 789 REPT PM T1\r\n   \"AID-T1-1:CVL,50\"\r\n   \"AID-T1-2:CVL,10\"\r\n   \"AID-T1-n:CVL,22\"\r\n;", "TL1AOMessage", "\r\n\n   BOCAFLMA015 93-06-02 12:00:00\r\n** 789 REPT PM T1\r\n   \"AID-T1-1:CVL,50\"\r\n   \"AID-T1-2:CVL,10\"\r\n   \"AID-T1-n:CVL,22\"\r\n;", "0"},
                {"\r\n\n   BOCAFLMA015 93-06-02 12:00:00\r\n*  789 REPT PM T1\r\n   \"AID-T1-1:CVL,50\"\r\n   \"AID-T1-2:CVL,10\"\r\n   \"AID-T1-n:CVL,22\"\r\n;", "TL1AOMessage", "\r\n\n   BOCAFLMA015 93-06-02 12:00:00\r\n*  789 REPT PM T1\r\n   \"AID-T1-1:CVL,50\"\r\n   \"AID-T1-2:CVL,10\"\r\n   \"AID-T1-n:CVL,22\"\r\n;", "0"},
                {"\r\n\n   BOCAFLMA015 93-06-02 12:00:00\r\nA  789 REPT PM T1\r\n   \"AID-T1-1:CVL,50\"\r\n   \"AID-T1-2:CVL,10\"\r\n   \"AID-T1-n:CVL,22\"\r\n;", "TL1AOMessage", "\r\n\n   BOCAFLMA015 93-06-02 12:00:00\r\nA  789 REPT PM T1\r\n   \"AID-T1-1:CVL,50\"\r\n   \"AID-T1-2:CVL,10\"\r\n   \"AID-T1-n:CVL,22\"\r\n;", "0"},
                {"\r\n\n   BOCAFLMA015 93-06-02 12:00:00\r\nA  789 REPT PM T1\r\n   \"AID-T1-1:CVL,50\"\r\n   \"AID-T1-2:CVL,10\"\r\n   \"AID-T1-n:CVL,22\"\r\n;\r\n\n   BOCAFLMA015 93-06-02 12:00:00\r\nA  789 REPT PM T1\r\n   \"AID-T1-1:CVL,50\"\r\n   \"AID-T1-2:CVL,10\"\r\n   \"AID-T1-n:CVL,22\"\r\n;", "TL1AOMessage", "\r\n\n   BOCAFLMA015 93-06-02 12:00:00\r\nA  789 REPT PM T1\r\n   \"AID-T1-1:CVL,50\"\r\n   \"AID-T1-2:CVL,10\"\r\n   \"AID-T1-n:CVL,22\"\r\n;", "0", "TL1AOMessage", "\r\n\n   BOCAFLMA015 93-06-02 12:00:00\r\nA  789 REPT PM T1\r\n   \"AID-T1-1:CVL,50\"\r\n   \"AID-T1-2:CVL,10\"\r\n   \"AID-T1-n:CVL,22\"\r\n;", "0"},
                {"\r\n", "\n   BOCAFLMA015", " 93-06-02 12:00:00\r\n", "A  789 REPT PM T1\r\n   \"AID-T1-1:CVL,50\"\r\n   \"AID-T1-2:CVL,10\"\r\n   \"AID-T1-n:CVL,22\"\r\n;", "TL1AOMessage", "\r\n\n   BOCAFLMA015 93-06-02 12:00:00\r\nA  789 REPT PM T1\r\n   \"AID-T1-1:CVL,50\"\r\n   \"AID-T1-2:CVL,10\"\r\n   \"AID-T1-n:CVL,22\"\r\n;", "0"},
                {"\r\n\n   BOCAFLMA015 93-06-02 12:00:00\r\nA  789 REPT PM T1\r\n   \"AID-T1-1:CVL,50\"\r\n   \"AID-T1-2:CVL,10\"\r\n   \"AID-T1-n:CVL,22\"\r\n;IP 0001\r\n<\r\n\n   HOLMNJCRK01 85-10-09 22:05:12\r\nM  0001 COMPLD\r\n;", "TL1AOMessage", "\r\n\n   BOCAFLMA015 93-06-02 12:00:00\r\nA  789 REPT PM T1\r\n   \"AID-T1-1:CVL,50\"\r\n   \"AID-T1-2:CVL,10\"\r\n   \"AID-T1-n:CVL,22\"\r\n;", "0", "TL1IPAckMessage", "IP 0001\r\n<", "0", "TL1ResponseMessage", "\r\n\n   HOLMNJCRK01 85-10-09 22:05:12\r\nM  0001 COMPLD\r\n;", "0"},
                {"IP 0001\r\n<\r\n\n   HOLMNJCRK01 85-10-09 22:05:12\r\nM  0001 COMPLD\r\n;\r\n\n   BOCAFLMA015 93-06-02 12:00:00\r\nA  789 REPT PM T1\r\n   \"AID-T1-1:CVL,50\"\r\n   \"AID-T1-2:CVL,10\"\r\n   \"AID-T1-n:CVL,22\"\r\n;IP 0001\r\n<\r\n\n   HOLMNJCRK01 85-10-09 22:05:12\r\nM  0001 COMPLD\r\n;", "TL1IPAckMessage", "IP 0001\r\n<", "0", "TL1ResponseMessage", "\r\n\n   HOLMNJCRK01 85-10-09 22:05:12\r\nM  0001 COMPLD\r\n;", "0", "TL1AOMessage", "\r\n\n   BOCAFLMA015 93-06-02 12:00:00\r\nA  789 REPT PM T1\r\n   \"AID-T1-1:CVL,50\"\r\n   \"AID-T1-2:CVL,10\"\r\n   \"AID-T1-n:CVL,22\"\r\n;", "0",  "TL1IPAckMessage", "IP 0001\r\n<", "0", "TL1ResponseMessage", "\r\n\n   HOLMNJCRK01 85-10-09 22:05:12\r\nM  0001 COMPLD\r\n;", "0"},
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
