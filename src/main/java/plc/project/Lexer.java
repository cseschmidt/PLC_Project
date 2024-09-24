package plc.project;

import java.util.ArrayList;
import java.util.List;

/**
 * The lexer works through three main functions:
 *
 *  - {@link #lex()}, which repeatedly calls lexToken() and skips whitespace
 *  - {@link #lexToken()}, which lexes the next token
 *  - {@link CharStream}, which manages the state of the lexer and literals
 *
 * If the lexer fails to parse something (such as an unterminated string) you
 * should throw a {@link ParseException} with an index at the invalid character.
 *
 * The {@link #peek(String...)} and {@link #match(String...)} functions are
 * helpers you need to use, they will make the implementation easier.
 */
public final class Lexer {

    private final CharStream chars;

    public Lexer(String input) {
        chars = new CharStream(input);
    }

    /**
     * Repeatedly lexes the input using {@link #lexToken()}, also skipping over
     * whitespace where appropriate.
     */
    public List<Token> lex() {
        List<Token> tokens = new ArrayList<>();
        while (chars.has(0)) {
            // Skip whitespace (isWhitespace checks for [ \b\n\r\t])
            while (chars.has(0) && isWhitespace(chars.get(0))){
                chars.advance();
                chars.skip();
            }
            if (chars.has(0)) {
                tokens.add(lexToken());
            }
        }
        return tokens;
    }

    /**
     * This method determines the type of the next token, delegating to the
     * appropriate lex method. As such, it is best for this method to not change
     * the state of the char stream (thus, use peek not match).
     *
     * The next character should start a valid token since whitespace is handled
     * by {@link #lex()}
     */
    public Token lexToken() {
        if (peek("[A-Za-z_]")) {
            return lexIdentifier();
        } else if (peek("[+-]") && (peek("[+-]", "[1-9]") || peek("[+-]", "0", "."))) {
            return lexNumber(); // Handles signed integers and decimals
        } else if (peek("0") || peek("[1-9]")) {
            return lexNumber(); // Handles unsigned integers and decimals
        } else if (peek("'")) {
            return lexCharacter();
        } else if (peek("\"")) {
            return lexString();
        } else {
            return lexOperator();
        }
    }

    public Token lexIdentifier() {
        if (!match("[A-Za-z_]")) {
            throw new ParseException("Invalid identifier start", chars.index);
        }
        while (match("[A-Za-z0-9_-]")) {
            // Consume the rest of the identifier
        }
        return chars.emit(Token.Type.IDENTIFIER);
    }

    public Token lexNumber() {
        int start = chars.index;
        StringBuilder sb = new StringBuilder();
        // Optional sign
        if (chars.get(0) == '+' || chars.get(0) == '-') {
            sb.append(chars.get(0));
            chars.advance();
        }
        boolean isDecimal = false;
        // Integer part
        if (chars.get(0) == '0') {
            sb.append(chars.get(0));
            chars.advance();
        } else if (isDigit(chars.get(0))) {
            while (chars.has(0) && isDigit(chars.get(0))) {
                sb.append(chars.get(0));
                chars.advance();
            }
        } else {
            throw new ParseException("Invalid number", chars.index);
        }
        // Check for decimal part
        if (chars.has(0) && chars.get(0) == '.') {
            isDecimal = true;
            sb.append(chars.get(0));
            chars.advance();
            if (!chars.has(0) || !isDigit(chars.get(0))) {
                throw new ParseException("Invalid decimal number", chars.index);
            }
            while (chars.has(0) && isDigit(chars.get(0))) {
                sb.append(chars.get(0));
                chars.advance();
            }
        }
        String lexeme = sb.toString();
        chars.skip();
        if (isDecimal) {
            return new Token(Token.Type.DECIMAL, lexeme, start);
        } else {
            return new Token(Token.Type.INTEGER, lexeme, start);
        }
    }

    public Token lexCharacter() {
        // Checks to see character begins with '
        if (!match("'")) {
            throw new ParseException("Character literal must start with a single quote.", chars.index);
        }

        if (!chars.has(0)) {
            throw new ParseException("Unterminated character literal.", chars.index);
        }

        //Checks to see if character is an escape charcter
        if (match("\\\\")) {
            lexEscape();
        } else if (match("[^'\\\\\\n\\r]")) {
            // Checks to see if next character is valid (not new line or ')
        } else {
            throw new ParseException("Invalid character in character literal.", chars.index);
        }

        // Checks to see character ends with '
        if (!match("'")) {
            throw new ParseException("Unterminated character literal.", chars.index);
        }

        return chars.emit(Token.Type.CHARACTER);
    }

    public Token lexString() {
        // Checks to see string ends with "
        if (!match("\"")) {
            throw new ParseException("String literal must start with a double quote.", chars.index);
        }

        // Loops until " is reached
        while (chars.has(0)) {
            // If string has ", ends and creates string token
            if (peek("\"")) {
                match("\"");
                return chars.emit(Token.Type.STRING);
            } else if (match("\\\\")) {
                lexEscape();
            } else if (match("[^\"\\\\\\n\\r]")) {
                // Checks to see if next string character is valid (not new line or ")
            } else {
                throw new ParseException("Invalid character in string literal.", chars.index);
            }
        }

        throw new ParseException("Unterminated string literal.", chars.index);
    }

    public void lexEscape() {
        if (!match("[bnrt'\"\\\\]")) {
            throw new ParseException("Invalid escape sequence", chars.index);
        }
    }

    public Token lexOperator() {
        // Check for &&
        if (match("&", "&")) {
            return chars.emit(Token.Type.OPERATOR);
        }
        // Check for ||
        else if (match("\\|", "\\|")) {
            return chars.emit(Token.Type.OPERATOR);
        }
        // Check for two character comparison operators
        else if (match("[<>!=]")) {
            if (match("=")) {
                // Matched '=', length updated
            }
            return chars.emit(Token.Type.OPERATOR);
        } else if (!peek("[ \b\n\r\t]")) {
            // Any other character excluding whitespace
            chars.advance();
            return chars.emit(Token.Type.OPERATOR);
        } else {
            throw new ParseException("Invalid operator", chars.index);
        }
    }

    private boolean isWhitespace(char c) {
        return c == ' ' || c == '\b' || c == '\n' || c == '\r' || c == '\t';
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }
    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true if the next characters are {@code 'a', 'b', 'c'}.
     */
    public boolean peek(String... patterns) {
        int offset = 0;
        for (String pattern : patterns) {
            if (!chars.has(offset)) {
                return false;
            }
            String c = String.valueOf(chars.get(offset));
            if (!c.matches(pattern)) {
                return false;
            }
            offset++;
        }
        return true;
    }

    /**
     * Returns true in the same way as {@link #peek(String...)}, but also
     * advances the character stream past all matched characters if peek returns
     * true. Hint - it's easiest to have this method simply call peek.
     */
    public boolean match(String... patterns) {
        boolean result = peek(patterns);
        if (result) {
            for (int i = 0; i < patterns.length; i++) {
                chars.advance();
            }
        }
        return result;
    }

    /**
     * A helper class maintaining the input string, current index of the char
     * stream, and the current length of the token being matched.
     *
     * You should rely on peek/match for state management in nearly all cases.
     * The only field you need to access is {@link #index} for any {@link
     * ParseException} which is thrown.
     */
    public static final class CharStream {

        private final String input;
        private int index = 0;
        private int length = 0;

        public CharStream(String input) {
            this.input = input;
        }

        public boolean has(int offset) {
            return index + offset < input.length();
        }

        public char get(int offset) {
            return input.charAt(index + offset);
        }

        public void advance() {
            index++;
            length++;
        }

        public void skip() {
            length = 0;
        }

        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
            return new Token(type, input.substring(start, index), start);
        }

    }

}
