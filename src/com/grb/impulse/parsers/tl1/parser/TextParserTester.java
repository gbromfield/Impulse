package com.grb.impulse.parsers.tl1.parser;

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

    static public void main(String[] args) {
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
            System.out.println(p.parse(new ParseContext("/* gaga */".getBytes())));
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
