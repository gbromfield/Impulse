package com.grb.impulse.parsers.tl1;

import java.nio.ByteBuffer;

abstract public class TL1Decoder {

	protected TL1Message _message;

    public TL1Decoder() {
        _message = null;
    }

    abstract public TL1Message decodeTL1Message(ByteBuffer readBuffer) throws TL1MessageMaxSizeExceededException;

    protected boolean arrayContains(byte[] source, byte[] comparison, int offset, int length) {
        if ((source.length >= length) && (comparison.length >= length)) {
            for(int i = 0; i < length; i++) {
                if (source[offset+i] != comparison[i]) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public class Results {
        public int total;
        public int passed;
        public int failed;

        public void add(Results result) {
            total += result.total;
            passed += result.passed;
            failed += result.failed;
        }

        public String toString() {
            return String.format("Total: %d, Success: %d, Failed:%d\n");
        }
    }

    public static void test(TL1Decoder decoder, int[] results, String ...cmds) {
        int cmdIdx = 0;
        while(cmdIdx < cmds.length) {
            ByteBuffer buf = ByteBuffer.allocate(cmds[cmdIdx].length());
            buf.put(cmds[cmdIdx].getBytes());
            buf.flip();
            while(buf.hasRemaining()) {
                try {
                    TL1Message msg = decoder.decodeTL1Message(buf);
                    if (msg != null) {
                        System.out.print(String.format("%s: %s", msg.getClass().getSimpleName(), msg));
                        cmdIdx++;
                        String simpleName = msg.getClass().getSimpleName();
                        String expectedSimpleName = cmds[cmdIdx];
                        cmdIdx++;
                        String tl1Str = msg.toString();
                        String expectedTL1Str = cmds[cmdIdx];
                        boolean error = false;
                        if (!simpleName.equals(expectedSimpleName)) {
                            System.out.println("---FAILED: expected class name of " + expectedSimpleName);
                            error = true;
                        }
                        if (!tl1Str.equals(expectedTL1Str)) {
                            System.out.println("---FAILED: expected TL1 string of " + expectedTL1Str);
                            error = true;
                        }
                        if (error) {
                            results[1] = results[1] + 1;
                        } else {
                            results[0] = results[0] + 1;
                            System.out.println("---SUCCESS");
                        }
                    }
                } catch(TL1MessageMaxSizeExceededException e) {
                    cmdIdx++;
                    String expectedSimpleName = cmds[cmdIdx];
                    cmdIdx++;
                    String expectedExceptionStr = cmds[cmdIdx];
                    boolean error = false;
                    if (!e.getClass().getSimpleName().equals(expectedSimpleName)) {
                        System.out.println("---FAILED: expected class name of " + expectedSimpleName);
                        error = true;
                    }
                    if (!e.getMessage().equals(expectedExceptionStr)) {
                        System.out.println("---FAILED: expected exception string of " + expectedExceptionStr);
                        error = true;
                    }
                    if (error) {
                        results[1] = results[1] + 1;
                    } else {
                        results[0] = results[0] + 1;
                        System.out.println("---SUCCESS");
                    }
                } catch(Throwable t) {
                    results[1] = results[1] + 1;
                }
            }
            cmdIdx++;
        }
    }
}
