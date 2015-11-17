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
                String output = parseElem.parse(new ParseContext(input.getBytes(), 0));
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
                .includeDelimiter(false);

        TextParser wsParser = new TextParser()
                .setAllowedChars(CharacterList.SPACE_TAB_CHARS)
                .setDelimiterChars(CharacterList.ALPHABETIC_MINUS_WHITESPACE_CHARS)
                .includeDelimiter(false);

        TextParser ctagParser = new TextParser()
                .setAllowedChars(CharacterList.ALPHABETIC_MINUS_WHITESPACE_CHARS)
                .setDelimiterChars(CharacterList.WHITESPACE_CHARS)
                .includeDelimiter(false);

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
                .includeDelimiter(false);

        TextParser qtidParser = new TextParser()
                .setAllowedChars(CharacterList.ALPHABETIC_CHARS)
                .setDelimiterStrings("\"", "\"")
                .includeDelimiter(false);

        TextParser dateParser = new TextParser()
                .setAllowedChars(CharacterList.NUMBER_CHARS)
                .setDelimiterChars(CharacterList.WHITESPACE_CHARS)
                .includeDelimiter(false);
        dateParser.getAllowedChars().add('-');

        TextParser timeParser = new TextParser()
                .setAllowedChars(CharacterList.NUMBER_CHARS)
                .setDelimiterChars(CharacterList.WHITESPACE_CHARS)
                .includeDelimiter(false);
        timeParser.getAllowedChars().add(':');

        TextParser codeParser = new TextParser()
                .setAllowedChars(CharacterList.ALPHABETIC_CHARS);

        TextParser compldParser = new TextParser()
                .setAllowedChars(CharacterList.ALPHABETIC_MINUS_WHITESPACE_CHARS)
                .setDelimiterChars(CharacterList.WHITESPACE_CHARS)
                .includeDelimiter(false);

        ParseContext tidpc = new ParseContext(respMsg.getBytes());

        try {
            System.out.println("Preamble = " + preParser.parse(tidpc));
            System.out.println("TID = " + tidParser.parse(tidpc));
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
        } catch (ParseException e) {
            e.printStackTrace();
        }

    }
}
