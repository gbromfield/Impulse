package com.grb.impulse.parsers.tl1.parser;

import com.grb.impulse.parsers.tl1.TL1AgentDecoder;
import com.grb.impulse.parsers.tl1.TL1Message;
import com.grb.impulse.parsers.tl1.TL1MessageMaxSizeExceededException;
import com.grb.impulse.parsers.tl1.TL1ResponseMessage;

import java.nio.ByteBuffer;
import java.text.ParseException;

/**
 * Created by gbromfie on 11/16/15.
 */
public class TextParserTester {
    public static boolean test(TextParser parseElem, String[][] args) {
        int success = 0;
        int failure = 0;
        for(int i = 0; i < args.length; i++) {
            String input = args[i][0];
            String expectedOutput = args[i][1];
            int position = -1;
            if (args[i].length == 3) {
                position = Integer.parseInt(args[i][2]);
            }
            try {
                String output = parseElem.parse(new ParseContext(input.getBytes(), 0, input.length()));
                if (output.equals(expectedOutput)) {
                    System.out.println(String.format("%s - SUCCESS", input));
                    success++;
                } else {
                    System.out.println(String.format("%s - FAILURE, expected \"%s\", got \"%s\"", input, expectedOutput, output));
                    failure++;
                }
            } catch (ParseException e) {
                if (expectedOutput.equals("ParseException")) {
                    if (position == e.getErrorOffset()) {
                        System.out.println(String.format("%s - SUCCESS", input));
                        success++;
                    } else {
                        System.out.println(String.format("%s - FAILURE, unexpected position expected \"%s\", got \"%s\"", input, position, e.getErrorOffset()));
                        failure++;
                    }
                } else {
                    System.out.println(String.format("%s - FAILURE, unexpected exception %s", input, e.getMessage()));
                    failure++;
                }
            }
        }
        System.out.println(String.format("\nTOTAL=%d, SUCCESS=%d, FAILED=%d\n", success+failure, success, failure));
        return (failure > 0);
    }

    protected static final TextParser optionalSpacesParser = new TextParser()
            .setAllowedChars(CharacterList.NO_CHARS)
            .addAllowedChar(' ')
            .setDelimiterChars(CharacterList.ALL_CHARS)
            .removeDelimeterChar(' ')
            .includeDelimiter(false);

    protected static final TextParser mandatorySpacesParser = new TextParser(optionalSpacesParser)
            .setLengths(1, Integer.MAX_VALUE);

    protected static final TextParser dateParser = new TextParser()
            .setAllowedChars(CharacterList.NUMBER_CHARS)
            .addAllowedChar('-')
            .setDelimiterChars(CharacterList.WHITESPACE_CHARS)
            .includeDelimiter(false)
            .setLengths(10, 10);

    protected static final TextParser timeParser = new TextParser()
            .setAllowedChars(CharacterList.NUMBER_CHARS)
            .addAllowedChar(':')
            .addAllowedChar(',')
            .setDelimiterChars(CharacterList.WHITESPACE_CHARS)
            .includeDelimiter(false)
            .setLengths(12, 12);

    protected static final TextParser textFieldParser = new TextParser()
            .setAllowedChars(CharacterList.ALPHABETIC_MINUS_WHITESPACE_CHARS)
            .setDelimiterChars(CharacterList.WHITESPACE_CHARS)
            .includeDelimiter(false);

    protected static final TextParser bracketParser = new TextParser()
            .setDelimiterStrings("[", "]")
            .setAllowedChars(CharacterList.ALPHABETIC_CHARS)
            .includeDelimiter(true);

    protected static final TextParser LessGreaterThanParser = new TextParser()
            .setAllowedChars(CharacterList.NO_CHARS)
            .addAllowedChar('<')
            .addAllowedChar('>')
            .setDelimiterChars(CharacterList.WHITESPACE_CHARS)
            .setLengths(1, 1)
            .includeDelimiter(false);

    static public void test2() {
//        String input = "2016-04-29 16:41:52,009 INFO     twisted      REVERSE";
        String input = "2016-04-29 16:41:53,886 INFO     bpprov       [cbbc1be6-8999-4780-b52f-924dabf45826] [Tl1Endpoint] <\n" +
            "\r" +
            "\n" +
            "\n" +
            "   \"PV0247D\" 16-04-29 20:40:52\r\n" +
            "M  2 COMPLD\n" +
            "   \"CIENA,\\\"6500 32-SLOT OPTICAL\\\",CNE,\\\"REL1150Z.QG\\\"\"\n" +
            ";\n";
        ParseContext pc = new ParseContext(input.getBytes(), 0, input.length());
        try {
            System.out.println(dateParser.parse(pc));
            mandatorySpacesParser.parse(pc);
            System.out.println(timeParser.parse(pc));
            mandatorySpacesParser.parse(pc);
            System.out.println(textFieldParser.parse(pc));
            mandatorySpacesParser.parse(pc);
            System.out.println(textFieldParser.parse(pc));
            mandatorySpacesParser.parse(pc);
            System.out.println(bracketParser.parse(pc));
            mandatorySpacesParser.parse(pc);
            System.out.println(bracketParser.parse(pc));
            mandatorySpacesParser.parse(pc);
            System.out.println(LessGreaterThanParser.parse(pc));
            // run the rest through a TL1 Parser
            ByteBuffer tl1Buffer = ByteBuffer.allocate(pc.length);
            tl1Buffer.put(pc.buffer, pc.mark + 1, pc.length - pc.mark - 1);
            tl1Buffer.flip();
            TL1AgentDecoder agentDecoder = new TL1AgentDecoder();
            TL1ResponseMessage tl1Msg = (TL1ResponseMessage)agentDecoder.decodeTL1Message(tl1Buffer);
            // DO WE WANT LOG MESSAGE TIME AND DATE OR TL! MESSAGE TIME AND DATE??
            System.out.println(String.format("== %s ==", tl1Msg.getDate()));
            System.out.println(String.format("%s -> %s : %s %s %s", tl1Msg.getTid(), "RA", tl1Msg.getTime(), tl1Msg.getCTAG(), tl1Msg.getComplCode()));
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (TL1MessageMaxSizeExceededException e) {
            e.printStackTrace();
        }
    }

    static public void main(String[] args) {
        test2();
        boolean failure = false;
        TextParser elem = new TextParser()
                .setAllowedChars(CharacterList.ALPHABETIC_CHARS);
        String[][] args1 = new String[][] {
                {"fsdfs", "fsdfs"},
                {"fsdfs\r", "ParseException", "5"},
                {"\"fsdfs\"", "\"fsdfs\""},
                {"\"   f s \"d f 's\"", "\"   f s \"d f 's\""},
        };
        if (test(elem, args1)) {
            failure = true;
        }

        elem = new TextParser()
                .setAllowedChars(CharacterList.CONTROL_CHARS);
        args1 = new String[][] {
                {"fsdfs", "ParseException", "0"},
                {"\r\n ", "ParseException", "2"},
                {"\r\n\t", "\r\n\t"},
        };
        if (test(elem, args1)) {
            failure = true;
        }

        CharacterList cl = new CharacterList().add(CharacterList.ALPHABETIC_CHARS).add(CharacterList.CONTROL_CHARS);
        elem = new TextParser()
                .setAllowedChars(cl);
        args1 = new String[][] {
                {"fsdfs", "fsdfs"},
                {"fsdfs\r", "fsdfs\r"},
                {"\"fsdfs\"", "\"fsdfs\""},
                {"\"   f s \"d f 's\"", "\"   f s \"d f 's\""},
                {"\r\n\t", "\r\n\t"},
        };
        if (test(elem, args1)) {
            failure = true;
        }

        cl = new CharacterList().add(CharacterList.ALPHABETIC_CHARS).add(CharacterList.CONTROL_CHARS);
        elem = new TextParser()
                .setAllowedChars(cl)
                .setDelimiterChars(new CharacterList(CharacterList.NO_CHARS).add(' '))
                .includeDelimiter(false);
        args1 = new String[][] {
                {" ", ""},
                {"\r\n\t ", "\r\n\t"},
                {"fs dfs", "fs"},
                {"fsdfs\r ", "fsdfs\r"},
                {"\"f sdfs\"", "\"f"},
        };
        if (test(elem, args1)) {
            failure = true;
        }

        if (failure) {
            System.out.println("RUN COMPLETED - FAILURES INDICATED");
        } else {
            System.out.println("RUN COMPLETED - NO FAILURES");
        }

        TextParser p = new TextParser().setDelimiterStrings("/*","*/")
                .includeDelimiter(false);

        try {
            String s = p.parse(new ParseContext("/* gaga */".getBytes()));
            if (s.equals(" gaga ")) {
                System.out.println(s + " SUCCESS");
            } else {
                System.out.println(s + " FAILURE");
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        try {
            String s = p.parse(new ParseContext("xxxxxxxxxxx /* blub */".getBytes(), 12, 10));
            if (s.equals(" blub ")) {
                System.out.println(s + " SUCCESS");
            } else {
                System.out.println(s + " FAILURE");
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        String ipMsg = "IP C0001\r\n<";
        TextParser cmdParser = new TextParser()
                .setAllowedChars(CharacterList.ALPHABETIC_MINUS_WHITESPACE_CHARS)
                .includeDelimiter(false)
                .setLengths(2, 2);

        TextParser wsParser = new TextParser()
                .setAllowedChars(CharacterList.SPACE_TAB_CHARS)
                .setDelimiterChars(CharacterList.ALPHABETIC_MINUS_WHITESPACE_CHARS)
                .includeDelimiter(false);

        TextParser ctagParser = new TextParser()
                .setAllowedChars(CharacterList.ALPHABETIC_MINUS_WHITESPACE_CHARS)
                .setDelimiterChars(CharacterList.WHITESPACE_CHARS)
                .includeDelimiter(false)
                .setLengths(1, Integer.MAX_VALUE);

        ParseContext pc = new ParseContext(ipMsg.getBytes());

        try {
            System.out.println("Command = " + cmdParser.parse(pc, 2));
            System.out.println("WS = " + wsParser.parse(pc));
            System.out.println("CTAG = " + ctagParser.parse(pc));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        String respMsg = "\r\n\n   HOLMNJCRK01 85-10-09 22:05:12\r\nM  0001 COMPLD\r\n;";
        TextParser preParser = new TextParser().setAllowedChars(CharacterList.WHITESPACE_CHARS)
                .setDelimiterChars(CharacterList.ALPHABETIC_MINUS_WHITESPACE_CHARS)
                .includeDelimiter(false);

        TextParser tidParser = new TextParser()
                .setAllowedChars(CharacterList.ALPHABETIC_MINUS_WHITESPACE_CHARS)
                .setDelimiterChars(CharacterList.WHITESPACE_CHARS)
                .includeDelimiter(false)
                .setLengths(1, Integer.MAX_VALUE);

        TextParser qtidParser = new TextParser()
                .setAllowedChars(CharacterList.ALPHABETIC_CHARS)
                .setDelimiterStrings("\"", "\"")
                .includeDelimiter(false)
                .setLengths(1, Integer.MAX_VALUE);

        TextParser dateParser = new TextParser()
                .setAllowedChars(CharacterList.NUMBER_CHARS)
                .addAllowedChar('-')
                .setDelimiterChars(CharacterList.WHITESPACE_CHARS)
                .includeDelimiter(false)
                .setLengths(8, 8);

        TextParser timeParser = new TextParser()
                .setAllowedChars(CharacterList.NUMBER_CHARS)
                .addAllowedChar(':')
                .setDelimiterChars(CharacterList.WHITESPACE_CHARS)
                .includeDelimiter(false)
                .setLengths(8, 8);

        TextParser codeParser = new TextParser()
                .setAllowedChars(CharacterList.ALPHABETIC_CHARS)
                .setLengths(1, Integer.MAX_VALUE);

        TextParser compldParser = new TextParser()
                .setAllowedChars(CharacterList.ALPHABETIC_MINUS_WHITESPACE_CHARS)
                .setDelimiterChars(CharacterList.WHITESPACE_CHARS)
                .includeDelimiter(false);

        ParseContext tidpc = new ParseContext(respMsg.getBytes());

        try {
            System.out.println("Preamble = " + preParser.parse(tidpc));
            System.out.println("TID = " + tidParser.parse(tidpc));
//            System.out.println("TID = " + qtidParser.parse(tidpc));
            System.out.println("WS = " + preParser.parse(tidpc));
            System.out.println("DATE = " + dateParser.parse(tidpc));
            System.out.println("WS = " + preParser.parse(tidpc));
            System.out.println("TIME = " + timeParser.parse(tidpc));
            System.out.println("WS = " + preParser.parse(tidpc));
            System.out.println("CODE = " + codeParser.parse(tidpc, 2));
            System.out.println("WS = " + preParser.parse(tidpc));
            System.out.println("CTAG = " + ctagParser.parse(tidpc));
            System.out.println("WS = " + preParser.parse(tidpc));
            System.out.println("COMPLD = " + compldParser.parse(tidpc));
            System.out.println("WS = " + preParser.parse(tidpc));

        } catch (ParseException e) {
            e.printStackTrace();
        }

        String autoMsg = "\r\n\n   BOCAFLMA015 93-06-02 12:00:00\r\nA  789 REPT PM T1\r\n   \"AID-T1-1:CVL,50\"\r\n;";
        tidpc = new ParseContext(autoMsg.getBytes());

        try {
            System.out.println("Preamble = " + preParser.parse(tidpc));
            System.out.println("TID = " + tidParser.parse(tidpc));
//            System.out.println("TID = " + qtidParser.parse(tidpc));
            System.out.println("WS = " + preParser.parse(tidpc));
            System.out.println("DATE = " + dateParser.parse(tidpc));
            System.out.println("WS = " + preParser.parse(tidpc));
            System.out.println("TIME = " + timeParser.parse(tidpc));
            System.out.println("WS = " + preParser.parse(tidpc));
            System.out.println("CODE = " + codeParser.parse(tidpc, 2));
            System.out.println("WS = " + preParser.parse(tidpc));
            System.out.println("ATAG = " + ctagParser.parse(tidpc));
            System.out.println("WS = " + preParser.parse(tidpc));
            System.out.println("VERB = " + compldParser.parse(tidpc));
            System.out.println("WS = " + preParser.parse(tidpc));
            System.out.println("MOD1 = " + compldParser.parse(tidpc));
            System.out.println("WS = " + preParser.parse(tidpc));
            System.out.println("MOD2 = " + compldParser.parse(tidpc));
            System.out.println("WS = " + preParser.parse(tidpc));

        } catch (ParseException e) {
            e.printStackTrace();
        }


        String inMsg = "BLUB-USER:GAGATID:USER:CTAG::PASSWORD;";
        tidpc = new ParseContext(inMsg.getBytes());

        TextParser verbParser = new TextParser()
                .setAllowedChars(CharacterList.ALPHABETIC_CHARS)
                .setDelimiterChars(CharacterList.NO_CHARS)
                .addDelimeterChar('-')
                .addDelimeterChar(':')
                .setLengths(1, Integer.MAX_VALUE)
                .includeDelimiter(false);

        TextParser cmdCodeDelParser = new TextParser()
                .setAllowedChars(CharacterList.NO_CHARS)
                .addAllowedChar('-')
                .addDelimeterChars(CharacterList.ALPHABETIC_CHARS)
                .removeDelimeterChar('-')
                .includeDelimiter(false);

        TextParser modParser = new TextParser()
                .setAllowedChars(CharacterList.ALPHABETIC_CHARS)
                .setDelimiterChars(CharacterList.NO_CHARS)
                .addDelimeterChar('-')
                .addDelimeterChar(':')
                .includeDelimiter(false);

        TextParser colonDelParser = new TextParser()
                .setAllowedChars(CharacterList.WHITESPACE_CHARS)
                .addAllowedChar(':')
                .addDelimeterChars(CharacterList.ALPHABETIC_MINUS_WHITESPACE_CHARS)
                .removeDelimeterChar(':')
                .includeDelimiter(false);

        TextParser tid2Parser = new TextParser()
                .setAllowedChars(CharacterList.ALPHABETIC_CHARS)
                .removeAllowedChar(':')
                .setDelimiterChars(CharacterList.NO_CHARS)
                .addDelimeterChar(':')
                .includeDelimiter(false);

        TextParser aidParser = new TextParser()
                .setAllowedChars(CharacterList.ALPHABETIC_CHARS)
                .removeAllowedChar(':')
                .setDelimiterChars(CharacterList.NO_CHARS)
                .addDelimeterChar(':')
                .includeDelimiter(false);

        try {
            System.out.println("VERB = " + verbParser.parse(tidpc));
            System.out.println("DASH = " + cmdCodeDelParser.parse(tidpc));
            System.out.println("MOD1 = " + modParser.parse(tidpc));
            System.out.println("DASH = " + cmdCodeDelParser.parse(tidpc));
//            System.out.println("TID = " + qtidParser.parse(tidpc));
            System.out.println("MOD2 = " + modParser.parse(tidpc));
            System.out.println("COLON = " + colonDelParser.parse(tidpc));
            System.out.println("TID = " + tid2Parser.parse(tidpc));
            System.out.println("COLON = " + colonDelParser.parse(tidpc));
            System.out.println("AID = " + aidParser.parse(tidpc));
            System.out.println("COLON = " + colonDelParser.parse(tidpc));
            System.out.println("CTAG = " + aidParser.parse(tidpc));
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
