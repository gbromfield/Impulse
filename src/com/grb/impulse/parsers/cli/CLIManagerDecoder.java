package com.grb.impulse.parsers.cli;

import com.grb.impulse.utils.ArrayMatcher;

import java.nio.ByteBuffer;

/**
 * Created by gbromfie on 4/18/16.
 */
public class CLIManagerDecoder {

    protected com.grb.util.ByteBuffer _buffer;
    protected int _maxBufferSize;
    protected String _commandCompletionString;
    protected ArrayMatcher _matcher;

    public CLIManagerDecoder(int maxBufferSize, String commandCompletionString) {
        _maxBufferSize = maxBufferSize;
        _buffer = new com.grb.util.ByteBuffer(_maxBufferSize);
        _commandCompletionString = commandCompletionString;
        byte[] commandCompletionBytes = _commandCompletionString.getBytes();
        _matcher = new ArrayMatcher(commandCompletionBytes);
    }

    public CLIMessage decodeCLIMessage(ByteBuffer readBuffer) throws CLIMessageMaxSizeExceededException {
        try {
            while (readBuffer.hasRemaining()) {
                byte b = readBuffer.get();
                _buffer.writeByte(b);
                if (_buffer.getLength() > _maxBufferSize) {
                    throw new CLIMessageMaxSizeExceededException(String.format("Error: maximum %d character size of input message reached", _maxBufferSize));
                }
                if (_matcher.match(b)) {
                    CLIMessage retMsg = new CLIMessage(_buffer);
                    _buffer.clear();
                    return retMsg;
                }
            }
        } catch (Exception e) {
            _buffer.clear();
            throw e;
        }
        return null;
    }

    public static void test(CLIManagerDecoder decoder, int[] results, String ...cmds) {
        int cmdIdx = 0;
        while(cmdIdx < cmds.length) {
            ByteBuffer buf = ByteBuffer.allocate(cmds[cmdIdx].length());
            buf.put(cmds[cmdIdx].getBytes());
            buf.flip();
            while(buf.hasRemaining()) {
                try {
                    CLIMessage msg = decoder.decodeCLIMessage(buf);
                    if (msg != null) {
                        cmdIdx++;
                        String tl1Str = msg.toString();
                        String expectedTL1Str = cmds[cmdIdx];
                        cmdIdx++;
                    }
                } catch(CLIMessageMaxSizeExceededException e) {
                    cmdIdx++;
                } catch(Throwable t) {
                    results[1] = results[1] + 1;
                }
            }
            cmdIdx++;
        }
    }

    public static void main(String[] args) {
        CLIManagerDecoder decoder = new CLIManagerDecoder(1000, "\n");
        String[][] strs = {
                {"YYY\ngaga: ", "YYY"},
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