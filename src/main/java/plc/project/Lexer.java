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
            // Skip whitespace
            while (chars.has(0) && Character.isWhitespace(chars.get(0))) {
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
        char current = chars.get(0);
        if (isLetter(current) || current == '_') {
            return lexIdentifier();
        } else if (current == '\'' ) {
            return lexCharacter();
        } else if (current == '"') {
            return lexString();
        } else if (isDigit(current) || current == '+' || current == '-') {
            return lexNumber();
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
        if (isDecimal) {
            return new Token(Token.Type.DECIMAL, lexeme, start);
        } else {
            return new Token(Token.Type.INTEGER, lexeme, start);
        }
    }

    public Token lexCharacter() {
        int start = chars.index;
        chars.advance(); // Skip opening '
        if (!chars.has(0)) {
            throw new ParseException("Unterminated character literal", chars.index);
        }
        char c = chars.get(0);
        StringBuilder sb = new StringBuilder();
        if (c == '\\') {
            sb.append(c);
            chars.advance();
            if (!chars.has(0)) {
                throw new ParseException("Invalid escape in character literal", chars.index);
            }
            char escape = chars.get(0);
            if ("bnrt'\"\\".indexOf(escape) == -1) {
                throw new ParseException("Invalid escape character", chars.index);
            }
            sb.append(escape);
            chars.advance();
        } else {
            if (c == '\'') {
                throw new ParseException("Empty character literal", chars.index);
            }
            sb.append(c);
            chars.advance();
        }
        if (!chars.has(0) || chars.get(0) != '\'') {
            throw new ParseException("Unterminated character literal", chars.index);
        }
        chars.advance(); // Skip closing '
        return new Token(Token.Type.CHARACTER, "'" + sb.toString() + "'", start);
    }

    public Token lexString() {
        int start = chars.index;
        StringBuilder sb = new StringBuilder();
        sb.append(chars.get(0)); // Append opening "
        chars.advance();
        while (chars.has(0)) {
            char c = chars.get(0);
            if (c == '"') {
                sb.append(c);
                chars.advance();
                return new Token(Token.Type.STRING, sb.toString(), start);
            } else if (c == '\\') {
                sb.append(c);
                chars.advance();
                if (!chars.has(0)) {
                    throw new ParseException("Invalid escape in string literal", chars.index);
                }
                char escape = chars.get(0);
                if ("bnrt'\"\\ ".indexOf(escape) == -1) {
                    throw new ParseException("Invalid escape character", chars.index);
                }
                sb.append(escape);
                chars.advance();
            } else if (c == '\n' || c == '\r') {
                throw new ParseException("Unterminated string literal", chars.index);
            } else {
                sb.append(c);
                chars.advance();
            }
        }
        throw new ParseException("Unterminated string literal", chars.index);
    }

    public void lexEscape() {
        if (!match("[bnrt'\"\\\\]")) {
            throw new ParseException("Invalid escape sequence", chars.index);
        }
    }

    public Token lexOperator() {
        int start = chars.index;
        StringBuilder sb = new StringBuilder();
        char first = chars.get(0);
        sb.append(first);
        chars.advance();
        // Handle multi-character operators
        if (first == '!' || first == '=' || first == '<' || first == '>') {
            if (chars.has(0) && chars.get(0) == '=') {
                sb.append('=');
                chars.advance();
            }
        } else if (first == '&') {
            if (chars.has(0) && chars.get(0) == '&') {
                sb.append('&');
                chars.advance();
            }
        } else if (first == '|') {
            if (chars.has(0) && chars.get(0) == '|') {
                sb.append('|');
                chars.advance();
            }
        }
        return new Token(Token.Type.OPERATOR, sb.toString(), start);
    }
    private boolean isWhitespace(char c) {
        return c == ' ' || c == '\b' || c == '\n' || c == '\r' || c == '\t';
    }

    private boolean isLetter(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }

    private boolean isLetterOrDigit(char c) {
        return isLetter(c) || isDigit(c);
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
