package com.grb.impulse.parsers.cli;

import com.grb.impulse.utils.ArrayMatcher;
import com.grb.util.ByteArrayFormatter;

import java.nio.ByteBuffer;

/**
 * Created by gbromfie on 4/18/16.
 */
public class CLIAgentDecoder {

    protected ArrayMatcher[] _promptMatchers;
    protected com.grb.util.ByteBuffer _buffer;
    protected int _maxBufferSize;

    public CLIAgentDecoder(int maxBufferSize) {
        _maxBufferSize = maxBufferSize;
        _promptMatchers = null;
        _buffer = new com.grb.util.ByteBuffer(_maxBufferSize);
    }

    public void setPrompts(String[] prompts) {
        _promptMatchers = new ArrayMatcher[prompts.length];
        for(int i = 0; i < prompts.length; i++) {
            _promptMatchers[i] = new ArrayMatcher(prompts[i].getBytes());
        }
    }

    public CLIMessage decodeCLIMessage(ByteBuffer readBuffer) throws CLIMessageMaxSizeExceededException {
        try {
            while (readBuffer.hasRemaining()) {
                byte b = readBuffer.get();
                _buffer.writeByte(b);
                if (_buffer.getLength() > _maxBufferSize) {
                    throw new CLIMessageMaxSizeExceededException(String.format("Error: maximum %d character size of input message reached", _maxBufferSize));
                }
                Integer match = null;
                if (_promptMatchers == null) {
                    CLIMessage retMsg = new CLIMessage(_buffer);
                    _buffer.clear();
                    return retMsg;
                } else {
                    for (int i = 0; i < _promptMatchers.length; i++) {
                        if (_promptMatchers[i].match(b)) {
                            CLIMessage retMsg = new CLIMessage(_buffer, 0, _buffer.getLength() - _promptMatchers[i].getLength(), _promptMatchers[i].getLength());
                            _buffer.clear();
                            for(int j = 0; j < _promptMatchers.length; j++) {
                                _promptMatchers[j].reset();
                            }
                            return retMsg;
                        }
                    }
                }
            }
        } catch (Exception e) {
            _buffer.clear();
            throw e;
        }
        return null;
    }

    public static void test(CLIAgentDecoder decoder, int[] results, String ...cmds) {
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
        CLIAgentDecoder decoder = new CLIAgentDecoder(1000);
        decoder.setPrompts(new String[] {"\ngaga: "});
        String[][] strs = {
                {"\ngag\ngaga: ", "YYY"},
                {"YabaDaba\ngagdoo\ngaga: ", "YYY"},
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