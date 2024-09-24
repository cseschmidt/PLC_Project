package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class LexerTests {

    @ParameterizedTest
    @MethodSource
    void testIdentifier(String test, String input, boolean success) {
        test(input, Token.Type.IDENTIFIER, success);
    }

    private static Stream<Arguments> testIdentifier() {
        return Stream.of(
                Arguments.of("Alphabetic", "getName", true),
                Arguments.of("Alphanumeric", "thelegend27", true),
                Arguments.of("Leading Hyphen", "-five", false),
                Arguments.of("Leading Digit", "1fish2fish3fishbluefish", false),
                Arguments.of("Single Character", "a", true),
                Arguments.of("Hyphenated", "a-b-c", true)
                );
    }

    @ParameterizedTest
    @MethodSource
    void testInteger(String test, String input, boolean success) {
        test(input, Token.Type.INTEGER, success);
    }

    private static Stream<Arguments> testInteger() {
        return Stream.of(
                Arguments.of("Single Digit", "1", true),
                Arguments.of("Multiple Digits", "12345", true),
                Arguments.of("Negative", "-1", true),
                Arguments.of("Leading Zero", "01", false),
                Arguments.of("Decimal", "123.456", false),
                Arguments.of("Comma Separated", "1,234", false),
                Arguments.of("Leading Zeros", "007", false)
                );
    }

    @ParameterizedTest
    @MethodSource
    void testDecimal(String test, String input, boolean success) {
        test(input, Token.Type.DECIMAL, success);
    }

    private static Stream<Arguments> testDecimal() {
        return Stream.of(
                Arguments.of("Multiple Digits", "123.456", true),
                Arguments.of("Negative Decimal", "-1.0", true),
                Arguments.of("Trailing Decimal", "1.", false),
                Arguments.of("Leading Decimal", ".5", false),
                Arguments.of("Single Digit", "1", false),
                Arguments.of("Trailing Zeros", "7.000", true),
                Arguments.of("Double Decimal", "1..0", false),
                Arguments.of("Multiple Negative Digits", "-123.456", true)



                );
    }

    @ParameterizedTest
    @MethodSource
    void testCharacter(String test, String input, boolean success) {
        test(input, Token.Type.CHARACTER, success);
    }

    private static Stream<Arguments> testCharacter() {
        return Stream.of(
                Arguments.of("Alphabetic", "\'c\'", true),
                Arguments.of("Newline Escape", "\'\\n\'", true),
                Arguments.of("Empty", "\'\'", false),
                Arguments.of("Multiple", "\'abc\'", false),
                Arguments.of("Unterminated", "\'", false),
                Arguments.of("Newline", "\'\n\'", false),
                Arguments.of("Return", "\'\r\'", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testString(String test, String input, boolean success) {
        test(input, Token.Type.STRING, success);
    }

    private static Stream<Arguments> testString() {
        return Stream.of(
                Arguments.of("Empty", "\"\"", true),
                Arguments.of("Alphabetic", "\"abc\"", true),
                Arguments.of("Newline Escape", "\"Hello,\\nWorld\"", true),
                Arguments.of("Unterminated", "\"unterminated", false),
                Arguments.of("Invalid Escape", "\"invalid\\escape\"", false),
                Arguments.of("Symbols", "\"!@#$%^&*()\"", true),
                Arguments.of("Newline Unterminated", "\"unterminated\n\"", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testOperator(String test, String input, boolean success) {
        //this test requires our lex() method, since that's where whitespace is handled.
        test(input, Arrays.asList(new Token(Token.Type.OPERATOR, input, 0)), success);
    }

    private static Stream<Arguments> testOperator() {
        return Stream.of(
                Arguments.of("Character", "(", true),
                Arguments.of("Comparison", "!=", true),
                Arguments.of("Space", " ", false),
                Arguments.of("Tab", "\t", false),
                Arguments.of("Comparison", "$", true),
                Arguments.of("Comparison", "+", true)
                );
    }

    @ParameterizedTest
    @MethodSource
    void testExamples(String test, String input, List<Token> expected) {
        test(input, expected, true);
    }

    private static Stream<Arguments> testExamples() {
        return Stream.of(
                Arguments.of("Example 1", "LET x = 5;", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "LET", 0),
                        new Token(Token.Type.IDENTIFIER, "x", 4),
                        new Token(Token.Type.OPERATOR, "=", 6),
                        new Token(Token.Type.INTEGER, "5", 8),
                        new Token(Token.Type.OPERATOR, ";", 9)
                )),
                Arguments.of("Example 2", "print(\"Hello, World!\");", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "print", 0),
                        new Token(Token.Type.OPERATOR, "(", 5),
                        new Token(Token.Type.STRING, "\"Hello, World!\"", 6),
                        new Token(Token.Type.OPERATOR, ")", 21),
                        new Token(Token.Type.OPERATOR, ";", 22)
                )),
                Arguments.of("Example 3", "LET   x\b\t\r\n = 5;", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "LET", 0),
                        new Token(Token.Type.IDENTIFIER, "x", 6),
                        new Token(Token.Type.OPERATOR, "=", 12),
                        new Token(Token.Type.INTEGER, "5", 14),
                        new Token(Token.Type.OPERATOR, ";", 15)
                )),
                Arguments.of("Example 4", "\'\"\'string\"\'\"", Arrays.asList(
                        new Token(Token.Type.CHARACTER, "'\"'", 0),
                        new Token(Token.Type.IDENTIFIER, "string", 3),
                        new Token(Token.Type.STRING, "\"'\"", 9)
                )),
                Arguments.of("Example 5", "one   two", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "one", 0),
                        new Token(Token.Type.IDENTIFIER, "two", 6)
                )),
                Arguments.of("Example 7", "token\n", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "token", 0)
                )),
                Arguments.of("Example 8", "one\btwo", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "one", 0),
                        new Token(Token.Type.IDENTIFIER, "two", 4)
                )),
                Arguments.of("Example 9", "1.2.3", Arrays.asList(
                        new Token(Token.Type.DECIMAL, "1.2", 0),
                        new Token(Token.Type.OPERATOR, ".", 3),
                        new Token(Token.Type.INTEGER, "3", 4)
                )),
                Arguments.of("Example 10", "!====", Arrays.asList(
                        new Token(Token.Type.OPERATOR, "!=", 0),
                        new Token(Token.Type.OPERATOR, "==", 2),
                        new Token(Token.Type.OPERATOR, "=", 4)
                )),
                Arguments.of("Example 11", "!=-0.12+02", Arrays.asList(
                        new Token(Token.Type.OPERATOR, "!=", 0),
                        new Token(Token.Type.DECIMAL, "-0.12", 2),
                        new Token(Token.Type.OPERATOR, "+", 7),
                        new Token(Token.Type.INTEGER, "0", 8),
                        new Token(Token.Type.INTEGER, "2", 9)
                ))


        );
    }

    @ParameterizedTest
    @MethodSource
    void testAdditionalIdentifiers(String test, String input, boolean success) {
        test(input, Token.Type.IDENTIFIER, success);
    }

    private static Stream<Arguments> testAdditionalIdentifiers() {
        return Stream.of(
                Arguments.of("Starts with Underscore", "_validIdentifier", true),
                Arguments.of("Contains Hyphen", "valid-identifier", true),
                Arguments.of("Starts with Hyphen", "-invalid", false),
                Arguments.of("Starts with Digit", "1invalid", false),
                Arguments.of("Only Hyphen", "-", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testAdditionalIntegers(String test, String input, boolean success) {
        test(input, Token.Type.INTEGER, success);
    }

    private static Stream<Arguments> testAdditionalIntegers() {
        return Stream.of(
                Arguments.of("Positive Sign", "+123", true),
                Arguments.of("Negative Zero", "-0", false),
                Arguments.of("Leading Zero Non-Zero Digit", "0123", false),
                Arguments.of("Zero", "0", true),
                Arguments.of("Zero with Sign", "+0", false),
                Arguments.of("Non-Digit Characters", "123abc", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testAdditionalDecimals(String test, String input, boolean success) {
        test(input, Token.Type.DECIMAL, success);
    }

    private static Stream<Arguments> testAdditionalDecimals() {
        return Stream.of(
                Arguments.of("Positive Decimal", "+123.456", true),
                Arguments.of("Negative Zero Decimal", "-0.123", true),
                Arguments.of("Leading Zero Non-Zero Digit", "0123.456", false),
                Arguments.of("Missing Integer Part", ".456", false),
                Arguments.of("Missing Fractional Part", "123.", false),
                Arguments.of("Multiple Decimal Points", "1.2.3", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testAdditionalCharacters(String test, String input, boolean success) {
        test(input, Token.Type.CHARACTER, success);
    }

    private static Stream<Arguments> testAdditionalCharacters() {
        return Stream.of(
                Arguments.of("Escaped Backslash", "'\\\\'", true),
                Arguments.of("Escaped Single Quote", "'\\''", true),
                Arguments.of("Invalid Escape Sequence", "'\\x'", false),
                Arguments.of("Unterminated Character", "'a", false),
                Arguments.of("Newline in Character", "'\n'", false),
                Arguments.of("Carriage Return in Character", "'\r'", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testAdditionalStrings(String test, String input, boolean success) {
        test(input, Token.Type.STRING, success);
    }

    private static Stream<Arguments> testAdditionalStrings() {
        return Stream.of(
                Arguments.of("String with Escaped Characters", "\"Line\\nBreak\"", true),
                Arguments.of("String with Escaped Double Quote", "\"He said, \\\"Hello\\\"\"", true),
                Arguments.of("Unterminated String", "\"This is unterminated", false),
                Arguments.of("Invalid Escape Sequence", "\"Invalid\\escape\"", false),
                Arguments.of("String with Newline", "\"This is\nInvalid\"", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testAdditionalOperators(String test, String input, boolean success) {
        test(input, Arrays.asList(new Token(Token.Type.OPERATOR, input, 0)), success);
    }

    private static Stream<Arguments> testAdditionalOperators() {
        return Stream.of(
                Arguments.of("Single Character Operator", "+", true),
                Arguments.of("Compound Operator", "&&", true),
                Arguments.of("Comparison Operator", "<=", true),
                Arguments.of("Unknown Operator", "@", true), // As per 'any character' rule
                Arguments.of("Whitespace", " ", false),
                Arguments.of("Tab Character", "\t", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testWhitespaceHandling(String test, String input, List<Token> expected) {
        test(input, expected, true);
    }

    private static Stream<Arguments> testWhitespaceHandling() {
        return Stream.of(
                Arguments.of("Whitespace Between Tokens", "123 \t\n456", Arrays.asList(
                        new Token(Token.Type.INTEGER, "123", 0),
                        new Token(Token.Type.INTEGER, "456", 6)
                )),
                Arguments.of("Leading and Trailing Whitespace", "   identifier   ", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "identifier", 3)
                )),
                Arguments.of("Whitespace Only", " \t\n\r\b", Arrays.asList())
        );
    }

    @Test
    void testComplexExpressions() {
        String input = "LET x = -123.456;\nprint(\"Hello,\\nWorld!\");";
        List<Token> expected = Arrays.asList(
                new Token(Token.Type.IDENTIFIER, "LET", 0),
                new Token(Token.Type.IDENTIFIER, "x", 4),
                new Token(Token.Type.OPERATOR, "=", 6),
                new Token(Token.Type.DECIMAL, "-123.456", 8),
                new Token(Token.Type.OPERATOR, ";", 16),
                new Token(Token.Type.IDENTIFIER, "print", 18),
                new Token(Token.Type.OPERATOR, "(", 23),
                new Token(Token.Type.STRING, "\"Hello,\\nWorld!\"", 24),
                new Token(Token.Type.OPERATOR, ")", 40),
                new Token(Token.Type.OPERATOR, ";", 41)
        );
        test(input, expected, true);
    }

    @Test
    void testErrorHandlingInvalidCharacter() {
        String input = "'\\a'";
        ParseException exception = Assertions.assertThrows(ParseException.class,
                () -> new Lexer(input).lex());
        Assertions.assertEquals(2, exception.getIndex()); // Index of invalid escape sequence
    }

    @Test
    void testErrorHandlingUnrecognizedToken() {
        String input = "'";
        ParseException exception = Assertions.assertThrows(ParseException.class,
                () -> new Lexer(input).lex());
        Assertions.assertEquals(1, exception.getIndex());
    }

    @Test
    void testStringWithEscapedQuotes() {
        String input = "\"She said, \\\"Hello!\\\"\"";
        List<Token> expected = Arrays.asList(
                new Token(Token.Type.STRING, "\"She said, \\\"Hello!\\\"\"", 0)
        );
        test(input, expected, true);
    }

    @Test
    void testCharacterWithEscapedBackslash() {
        String input = "'\\\\'";
        List<Token> expected = Arrays.asList(
                new Token(Token.Type.CHARACTER, "'\\\\'", 0)
        );
        test(input, expected, true);
    }

    @Test
    void testNumberWithEmbeddedWhitespace() {
        String input = "1 234";
        List<Token> expected = Arrays.asList(
                new Token(Token.Type.INTEGER, "1", 0),
                new Token(Token.Type.INTEGER, "234", 2)
        );
        test(input, expected, true);
    }

    @Test
    void testDecimalWithoutLeadingDigit() {
        String input = ".456";
        List<Token> expected = Arrays.asList(
                new Token(Token.Type.OPERATOR, ".", 0),
                new Token(Token.Type.INTEGER, "456", 1)
        );
        test(input, expected, true);
    }

    @Test
    void testOperatorsCombinedWithIdentifiers() {
        String input = "&&operator";
        List<Token> expected = Arrays.asList(
                new Token(Token.Type.OPERATOR, "&&", 0),
                new Token(Token.Type.IDENTIFIER, "operator", 2)
        );
        test(input, expected, true);
    }

    @Test
    void testException() {
        ParseException exception = Assertions.assertThrows(ParseException.class,
                () -> new Lexer("\"unterminated").lex());
        Assertions.assertEquals(13, exception.getIndex());
    }

    @Test
    void testUnterminatedCharacterException() {
        ParseException exception = Assertions.assertThrows(ParseException.class,
                () -> new Lexer("'c").lex());
        Assertions.assertEquals(2, exception.getIndex());  // Expected index for unterminated character
    }

    @Test
    void testInvalidEscapeException() {
        ParseException exception = Assertions.assertThrows(ParseException.class,
                () -> new Lexer("\"invalid\\escape\"").lex());
        Assertions.assertEquals(9, exception.getIndex());  // Expected index for invalid escape sequence
    }

    /**
     * Tests that lexing the input through {@link Lexer#lexToken()} produces a
     * single token with the expected type and literal matching the input.
     */
    private static void test(String input, Token.Type expected, boolean success) {
        try {
            if (success) {
                Assertions.assertEquals(new Token(expected, input, 0), new Lexer(input).lexToken());
            } else {
                Assertions.assertNotEquals(new Token(expected, input, 0), new Lexer(input).lexToken());
            }
        } catch (ParseException e) {
            Assertions.assertFalse(success, e.getMessage());
        }
    }

    /**
     * Tests that lexing the input through {@link Lexer#lex()} matches the
     * expected token list.
     */
    private static void test(String input, List<Token> expected, boolean success) {
        try {
            if (success) {
                Assertions.assertEquals(expected, new Lexer(input).lex());
            } else {
                Assertions.assertNotEquals(expected, new Lexer(input).lex());
            }
        } catch (ParseException e) {
            Assertions.assertFalse(success, e.getMessage());
        }
    }

}
