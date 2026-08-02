package com.ourgiant.utilities.core;

import com.ourgiant.utilities.model.JsonToken;
import com.ourgiant.utilities.model.JsonTokenType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.regex.Pattern;

/** Pure JSON formatting/validation/tokenizing logic, with no dependency on Swing. */
public final class JsonProcessor {

    /** Caps how much text a single call will process, so a pasted/opened file can't hang the UI thread or exhaust memory. */
    public static final int MAX_INPUT_LENGTH = 10_000_000;

    /** Caps bracket nesting depth, so pathologically nested input can't build an unbounded stack/indent buffer. */
    public static final int MAX_NESTING_DEPTH = 1_000;

    private static final Pattern NUMBER_PATTERN = Pattern.compile("-?\\d+(\\.\\d+)?([eE][+-]?\\d+)?");
    private static final Pattern TRAILING_COMMA_PATTERN = Pattern.compile(".*,\\s*[}\\]].*", Pattern.DOTALL);

    private JsonProcessor() {
    }

    public static String format(String input) throws JsonProcessingException {
        checkSize(input);
        input = unwrapStringLiteral(input);
        StringBuilder result = new StringBuilder();
        int indentLevel = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (escaped) {
                result.append(c);
                escaped = false;
            } else if (c == '\\') {
                result.append(c);
                escaped = true;
            } else if (c == '"') {
                inString = !inString;
                result.append(c);
            } else if (inString) {
                result.append(c);
            } else {
                switch (c) {
                    case '\t', '\n', '\r', ' ' -> {
                    }
                    case ',' -> {
                        result.append(c).append('\n');
                        result.append("  ".repeat(indentLevel));
                    }
                    case ':' -> result.append(c).append(' ');
                    case '[', '{' -> {
                        indentLevel++;
                        checkDepth(indentLevel);
                        result.append(c).append('\n');
                        result.append("  ".repeat(indentLevel));
                    }
                    case ']', '}' -> {
                        result.append('\n');
                        indentLevel--;
                        result.append("  ".repeat(Math.max(indentLevel, 0)));
                        result.append(c);
                    }
                    default -> result.append(c);
                }
            }
        }

        return result.toString();
    }

    public static String compact(String input) throws JsonProcessingException {
        checkSize(input);
        input = unwrapStringLiteral(input);
        StringBuilder result = new StringBuilder();
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (escaped) {
                result.append(c);
                escaped = false;
            } else if (c == '\\') {
                result.append(c);
                escaped = true;
            } else if (c == '"') {
                inString = !inString;
                result.append(c);
            } else if (inString) {
                result.append(c);
            } else if (c != ' ' && c != '\n' && c != '\r' && c != '\t') {
                result.append(c);
            }
        }

        return result.toString();
    }

    public static String stringify(String input) throws JsonProcessingException {
        checkSize(input);
        return "\"" + escape(input) + "\"";
    }

    public static String escape(String input) throws JsonProcessingException {
        checkSize(input);
        return input.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    public static String unescape(String input) throws JsonProcessingException {
        checkSize(input);
        return input.replace("\\\"", "\"").replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t").replace("\\\\", "\\");
    }

    /** Bracket-matching + trailing-comma lint. Throws on structural errors, appends non-fatal issues to {@code warnings}. */
    public static void validate(String input, List<String> warnings) throws JsonProcessingException {
        checkSize(input);
        input = unwrapStringLiteral(input);
        if (!input.startsWith("{") && !input.startsWith("[")) {
            throw new JsonProcessingException("JSON must start with { or [");
        }

        Deque<Character> brackets = new ArrayDeque<>();
        boolean inString = false;
        boolean escaped = false;
        int line = 1;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '\n') {
                line++;
            }

            if (escaped) {
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                inString = !inString;
            } else if (!inString) {
                if (c == '{' || c == '[') {
                    brackets.push(c);
                    checkDepth(brackets.size());
                } else if (c == '}' || c == ']') {
                    if (brackets.isEmpty()) {
                        throw new JsonProcessingException("Unmatched closing bracket at line " + line);
                    }
                    char open = brackets.pop();
                    if ((c == '}' && open != '{') || (c == ']' && open != '[')) {
                        warnings.add("Mismatched brackets at line " + line);
                    }
                }
            }
        }

        if (!brackets.isEmpty()) {
            throw new JsonProcessingException("Unclosed brackets");
        }

        if (TRAILING_COMMA_PATTERN.matcher(input).matches()) {
            warnings.add("Trailing comma detected (not allowed in strict JSON)");
        }
    }

    /** Classifies each string/number/boolean/null/punctuation span in {@code text} for syntax highlighting. */
    public static List<JsonToken> tokenize(String text) throws JsonProcessingException {
        checkSize(text);
        List<JsonToken> tokens = new ArrayList<>();
        boolean inString = false;
        boolean escaped = false;
        int stringStart = -1;
        StringBuilder tokenBuf = new StringBuilder();
        int tokenStart = -1;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (escaped) {
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                if (inString) {
                    boolean isKey = false;
                    for (int j = i + 1; j < text.length(); j++) {
                        char lookahead = text.charAt(j);
                        if (lookahead == ':') {
                            isKey = true;
                            break;
                        }
                        if (!Character.isWhitespace(lookahead)) {
                            break;
                        }
                    }
                    tokens.add(new JsonToken(stringStart, i + 1 - stringStart, isKey ? JsonTokenType.KEY : JsonTokenType.STRING));
                    inString = false;
                } else {
                    stringStart = i;
                    inString = true;
                }
            } else if (!inString) {
                if (isPunctuation(c)) {
                    if (tokenBuf.length() > 0) {
                        tokens.add(classify(tokenBuf.toString(), tokenStart));
                        tokenBuf = new StringBuilder();
                    }
                    tokens.add(new JsonToken(i, 1, JsonTokenType.PUNCTUATION));
                    tokenStart = -1;
                } else if (!Character.isWhitespace(c)) {
                    if (tokenStart == -1) {
                        tokenStart = i;
                    }
                    tokenBuf.append(c);
                } else if (tokenBuf.length() > 0) {
                    tokens.add(classify(tokenBuf.toString(), tokenStart));
                    tokenBuf = new StringBuilder();
                    tokenStart = -1;
                }
            }
        }

        if (tokenBuf.length() > 0) {
            tokens.add(classify(tokenBuf.toString(), tokenStart));
        }

        return tokens;
    }

    private static boolean isPunctuation(char c) {
        return c == '{' || c == '}' || c == '[' || c == ']' || c == ',' || c == ':';
    }

    private static JsonToken classify(String token, int start) {
        JsonTokenType type;
        if (token.equals("true") || token.equals("false")) {
            type = JsonTokenType.BOOLEAN;
        } else if (token.equals("null")) {
            type = JsonTokenType.NULL;
        } else if (NUMBER_PATTERN.matcher(token).matches()) {
            type = JsonTokenType.NUMBER;
        } else {
            type = JsonTokenType.PUNCTUATION;
        }
        return new JsonToken(start, token.length(), type);
    }

    private static String unwrapStringLiteral(String input) throws JsonProcessingException {
        input = input.trim();
        if (input.startsWith("\"") && input.endsWith("\"")) {
            return unescape(input.substring(1, input.length() - 1));
        }
        return input;
    }

    private static void checkSize(String input) throws JsonProcessingException {
        if (input.length() > MAX_INPUT_LENGTH) {
            throw new JsonProcessingException(
                "Input too large: " + input.length() + " characters exceeds the " + MAX_INPUT_LENGTH + "-character limit");
        }
    }

    private static void checkDepth(int depth) throws JsonProcessingException {
        if (depth > MAX_NESTING_DEPTH) {
            throw new JsonProcessingException(
                "Nesting too deep: exceeds the " + MAX_NESTING_DEPTH + "-level limit");
        }
    }
}
