package com.grb.impulse.parsers.tl1;

import java.nio.ByteBuffer;
import java.text.ParseException;

/**
 * Created by gbromfie on 11/9/15.
 */
public class TL1ManagerDecoder extends TL1Decoder {

    public TL1ManagerDecoder() {
    }

    public TL1Message decodeTL1Message(ByteBuffer readBuffer) throws TL1MessageMaxSizeExceededException, ParseException {
        if (_message == null) {
            _message = new TL1InputMessage();
        }
        try {
            if (_message.parse(readBuffer)) {
                TL1Message tmp = _message;
                _message = null;
                return tmp;
            }
        } catch(TL1MessageMaxSizeExceededException e) {
            _message = null;
            throw e;
        }
        return null;
    }

    public static void main(String[] args) {
        TL1Decoder decoder = new TL1ManagerDecoder();
        StringBuilder bldr = new StringBuilder(TL1InputMessage.MAX_SIZE + 1);
        for(int i = 0; i < TL1InputMessage.MAX_SIZE + 1; i++) {
            bldr.append('X');
        }
        String inputMessageOverflow = bldr.toString();
        String[][] strs = {
                {inputMessageOverflow, "TL1MessageMaxSizeExceededException", "Error: maximum 16384 character size of input message reached"},
                {"RTRV-LI::5551234:123:RTRVIND=TI-SLHR;", "TL1InputMessage", "RTRV-LI::5551234:123:RTRVIND=TI-SLHR;"},
                {"RTRV-LI::5551234:123:RTRVIND=TI-SLHR;", "TL1InputMessage", "RTRV-LI::5551234:123:RTRVIND=TI-SLHR;"},
                {"RTRV-LI", "::5551234:123:", "RTRVIND=TI-SLHR;", "TL1InputMessage", "RTRV-LI::5551234:123:RTRVIND=TI-SLHR;"},
                {"RTRV-LI::5551234:123:RTRVIND=TI-SLHR;RTRV-LI::5551235:124:RTRVIND=TI-SLHR;", "TL1InputMessage", "RTRV-LI::5551234:123:RTRVIND=TI-SLHR;", "TL1InputMessage", "RTRV-LI::5551235:124:RTRVIND=TI-SLHR;"},
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
