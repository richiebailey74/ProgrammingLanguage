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
                Arguments.of("Leading With @", "@gmailMF", true),
                Arguments.of("Non-Leading Dash", "Bum-yuh", true),
                Arguments.of("Non-Leading Underscore", "Hey_Ma", true),
                Arguments.of("Single @", "@", false),
                Arguments.of("Single Letter", "J", true),
                Arguments.of("Leading Dash", "-trshsdh", false),
                Arguments.of("Leading Underscore", "_helloooo", false),
                Arguments.of("Leading Hyphen", "-five", false),
                Arguments.of("Leading Digit", "1fish2fish3fishbluefish", false),
                Arguments.of("Double @ signs", "@@", false),
                Arguments.of("Using a Decimal", "Gmail.com", false)

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
                Arguments.of("Trailing Zeros", "100000000000", true),
                Arguments.of("Negative Double Digits", "-45", true),
                Arguments.of("Large Number With Zeros", "36784500760377658", true),
                Arguments.of("Leading Zero", "01", false),
                Arguments.of("Multiple Zeros", "0000", false),
                Arguments.of("Negative Leading Zeros", "-005", false)
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
                Arguments.of("Precision Zero", "0.00", true),
                Arguments.of("Trailing Zero", "23452.30", true),
                Arguments.of("Negative Multiple Digits", "-12354.6543", true),
                Arguments.of("Positive Decimal", "1.22", true),
                Arguments.of("Trailing Decimal", "1.", false),
                Arguments.of("Leading Decimal", ".5", false),
                Arguments.of("Negative leading decimal", "-.4", false),
                Arguments.of("Double Decimal", "32..", false),
                Arguments.of("Negative Trailing Decimal", "-5.", false),
                Arguments.of("Leading Zero", "04.12", false),
                Arguments.of("Simple Decimal", ".", false)
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
                Arguments.of("Return Escape", "\'\\r\'", true),
                Arguments.of("Box Escape", "\'\\b\'", true),
                Arguments.of("Tab Escape", "\'\\t\'", true),
                Arguments.of("Quotation Escape", "\'\\\"\'", true),
                Arguments.of("Single quote", "\'\\\'\'", true),
                Arguments.of("Backslash", "\'\\\\\'", true),
                Arguments.of("Empty", "\'\'", false),
                Arguments.of("Multiple", "\'abc\'", false),
                Arguments.of("Empty Character", "\'\'", false),
                Arguments.of("Character Before Single Quote", "a\'a\'", false),
                Arguments.of("Character After Single Quote", "\'a\'a", false),
                Arguments.of("Missing First Single Quote", "a\'", false),
                Arguments.of("Missing Second Single Quote", "\'a", false)
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
                Arguments.of("Many White Space Escapes", "\"\\nHA\\tHA\\bHA HA\\rHA\"", true),
                Arguments.of("Backslash", "\"\\\\\"", true),
                Arguments.of("Unterminated", "\"unterminated", false),
                Arguments.of("Invalid Escape", "\"invalid\\escape\"", false),
                Arguments.of("Characters After Quotation", "\"HI THERE\"j", false),
                Arguments.of("Characters Before Quotation", "a\"HI THERE\"", false),
                Arguments.of("Missing First Quote", "yoyoyo\"", false),
                Arguments.of("Missing Second Quote", "\"yoyoyo", false)
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
                Arguments.of("Addition", "+", true),
                Arguments.of("Space", " ", false),
                Arguments.of("Tab", "\t", false),
                Arguments.of("Number", "123", false),
                Arguments.of("Identifier", "absd42", false),
                Arguments.of("String", "\"Hello\"", false),
                Arguments.of("Character", "\'a\'", false)
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
                Arguments.of("Example 3", "x = 123 - 43;", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "x", 0),
                        new Token(Token.Type.OPERATOR, "=", 2),
                        new Token(Token.Type.INTEGER, "123", 4),
                        new Token(Token.Type.OPERATOR, "-", 8),
                        new Token(Token.Type.INTEGER, "43", 10),
                        new Token(Token.Type.OPERATOR, ";", 12)
                )),
                Arguments.of("Example 4", "Car.Engine(4.3)", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "Car", 0),
                        new Token(Token.Type.OPERATOR, ".", 3),
                        new Token(Token.Type.IDENTIFIER, "Engine", 4),
                        new Token(Token.Type.OPERATOR, "(", 10),
                        new Token(Token.Type.DECIMAL, "4.3", 11),
                        new Token(Token.Type.OPERATOR, ")", 14)
                ))
        );
    }

    @Test
    void testException() {
        ParseException exception = Assertions.assertThrows(ParseException.class,
                () -> new Lexer("\"unterminated").lex());
        Assertions.assertEquals(13, exception.getIndex());
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
