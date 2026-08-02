package com.ourgiant.utilities.core;

import com.ourgiant.utilities.model.JsonToken;
import com.ourgiant.utilities.model.JsonTokenType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonProcessorTest {

    @Test
    void formatsNestedObjectWithTwoSpaceIndent() throws JsonProcessingException {
        String input = "{\"b\":2,\"a\":[1,2,3],\"nested\":{\"k\":true,\"n\":null}}";
        String expected = """
                {
                  "b": 2,
                  "a": [
                    1,
                    2,
                    3
                  ],
                  "nested": {
                    "k": true,
                    "n": null
                  }
                }""";
        assertEquals(expected, JsonProcessor.format(input));
    }

    @Test
    void formatUnwrapsAStringifiedJsonLiteral() throws JsonProcessingException {
        String stringified = "\"{\\\"a\\\":1}\"";
        assertEquals("{\n  \"a\": 1\n}", JsonProcessor.format(stringified));
    }

    @Test
    void formatIgnoresWhitespaceInsideStringValues() throws JsonProcessingException {
        String input = "{\"text\":\"a  b\\nc\"}";
        assertEquals("{\n  \"text\": \"a  b\\nc\"\n}", JsonProcessor.format(input));
    }

    @Test
    void compactRemovesWhitespaceOutsideStrings() throws JsonProcessingException {
        String input = "{\n  \"a\" : 1,\n  \"b\": [1, 2, 3]\n}";
        assertEquals("{\"a\":1,\"b\":[1,2,3]}", JsonProcessor.compact(input));
    }

    @Test
    void compactPreservesWhitespaceInsideStrings() throws JsonProcessingException {
        String input = "{ \"text\" : \"has  spaces\" }";
        assertEquals("{\"text\":\"has  spaces\"}", JsonProcessor.compact(input));
    }

    @Test
    void stringifyWrapsAndEscapes() throws JsonProcessingException {
        assertEquals("\"{\\\"a\\\":1}\"", JsonProcessor.stringify("{\"a\":1}"));
    }

    @Test
    void escapeAndUnescapeRoundTripPlainText() throws JsonProcessingException {
        String original = "line one\nline two\ttabbed \"quoted\"";
        String escaped = JsonProcessor.escape(original);
        assertEquals(original, JsonProcessor.unescape(escaped));
    }

    @Test
    void validateAcceptsWellFormedJsonWithNoWarnings() throws JsonProcessingException {
        List<String> warnings = new ArrayList<>();
        JsonProcessor.validate("{\"a\":[1,2,3]}", warnings);
        assertTrue(warnings.isEmpty());
    }

    @Test
    void validateRejectsInputNotStartingWithBraceOrBracket() {
        List<String> warnings = new ArrayList<>();
        JsonProcessingException ex = assertThrows(JsonProcessingException.class,
                () -> JsonProcessor.validate("\"just a string\"", warnings));
        assertEquals("JSON must start with { or [", ex.getMessage());
    }

    @Test
    void validateRejectsUnmatchedClosingBracket() {
        List<String> warnings = new ArrayList<>();
        JsonProcessingException ex = assertThrows(JsonProcessingException.class,
                () -> JsonProcessor.validate("{\"a\":1}}", warnings));
        assertTrue(ex.getMessage().startsWith("Unmatched closing bracket"));
    }

    @Test
    void validateRejectsUnclosedBrackets() {
        List<String> warnings = new ArrayList<>();
        JsonProcessingException ex = assertThrows(JsonProcessingException.class,
                () -> JsonProcessor.validate("{\"a\":[1,2,3]", warnings));
        assertEquals("Unclosed brackets", ex.getMessage());
    }

    @Test
    void validateWarnsOnMismatchedBracketTypes() throws JsonProcessingException {
        List<String> warnings = new ArrayList<>();
        JsonProcessor.validate("{\"a\":1]", warnings);
        assertTrue(warnings.stream().anyMatch(w -> w.contains("Mismatched brackets")));
    }

    @Test
    void validateWarnsOnTrailingComma() throws JsonProcessingException {
        List<String> warnings = new ArrayList<>();
        JsonProcessor.validate("{\"a\":1,}", warnings);
        assertTrue(warnings.stream().anyMatch(w -> w.contains("Trailing comma")));
    }

    @Test
    void validateIgnoresBracketsInsideStrings() throws JsonProcessingException {
        List<String> warnings = new ArrayList<>();
        JsonProcessor.validate("{\"a\":\"[{}]\"}", warnings);
        assertTrue(warnings.isEmpty());
    }

    @Test
    void formatRejectsInputOverTheSizeCap() {
        String oversized = "{" + "a".repeat(JsonProcessor.MAX_INPUT_LENGTH + 1) + "}";
        JsonProcessingException ex = assertThrows(JsonProcessingException.class, () -> JsonProcessor.format(oversized));
        assertTrue(ex.getMessage().contains("too large"));
    }

    @Test
    void validateRejectsInputOverTheSizeCap() {
        String oversized = "{" + "a".repeat(JsonProcessor.MAX_INPUT_LENGTH + 1) + "}";
        List<String> warnings = new ArrayList<>();
        JsonProcessingException ex = assertThrows(JsonProcessingException.class,
                () -> JsonProcessor.validate(oversized, warnings));
        assertTrue(ex.getMessage().contains("too large"));
    }

    @Test
    void validateRejectsNestingOverTheDepthCap() {
        String tooDeep = "[".repeat(JsonProcessor.MAX_NESTING_DEPTH + 1) + "]".repeat(JsonProcessor.MAX_NESTING_DEPTH + 1);
        List<String> warnings = new ArrayList<>();
        JsonProcessingException ex = assertThrows(JsonProcessingException.class,
                () -> JsonProcessor.validate(tooDeep, warnings));
        assertTrue(ex.getMessage().contains("Nesting too deep"));
    }

    @Test
    void formatRejectsNestingOverTheDepthCap() {
        String tooDeep = "[".repeat(JsonProcessor.MAX_NESTING_DEPTH + 1) + "]".repeat(JsonProcessor.MAX_NESTING_DEPTH + 1);
        JsonProcessingException ex = assertThrows(JsonProcessingException.class, () -> JsonProcessor.format(tooDeep));
        assertTrue(ex.getMessage().contains("Nesting too deep"));
    }

    @Test
    void formatDoesNotCrashOnUnbalancedClosingBrackets() throws JsonProcessingException {
        // More closing brackets than opening ones is malformed, but format() is a
        // best-effort pretty-printer used ahead of validation - it must degrade
        // gracefully (no IllegalArgumentException from a negative indent) rather
        // than crash on adversarial/malformed input.
        String malformed = "{\"a\":1}}}}";
        String result = JsonProcessor.format(malformed);
        assertTrue(result.contains("\"a\": 1"));
    }

    @Test
    void tokenizeDistinguishesKeysFromStringValues() throws JsonProcessingException {
        String text = "{\"key\":\"value\"}";
        List<JsonToken> tokens = JsonProcessor.tokenize(text);
        assertEquals(JsonTokenType.KEY, tokenTypeOf(tokens, text, "\"key\""));
        assertEquals(JsonTokenType.STRING, tokenTypeOf(tokens, text, "\"value\""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "false"})
    void tokenizeClassifiesBooleans(String literal) throws JsonProcessingException {
        String text = "{\"a\":" + literal + "}";
        List<JsonToken> tokens = JsonProcessor.tokenize(text);
        assertEquals(JsonTokenType.BOOLEAN, tokenTypeOf(tokens, text, literal));
    }

    @Test
    void tokenizeClassifiesNull() throws JsonProcessingException {
        String text = "{\"a\":null}";
        List<JsonToken> tokens = JsonProcessor.tokenize(text);
        assertEquals(JsonTokenType.NULL, tokenTypeOf(tokens, text, "null"));
    }

    @Test
    void tokenizeClassifiesNumbersIncludingExponents() throws JsonProcessingException {
        String text = "{\"a\":-1.5e10}";
        List<JsonToken> tokens = JsonProcessor.tokenize(text);
        assertEquals(JsonTokenType.NUMBER, tokenTypeOf(tokens, text, "-1.5e10"));
    }

    @Test
    void tokenizeMarksBracesAndPunctuationSeparately() throws JsonProcessingException {
        List<JsonToken> tokens = JsonProcessor.tokenize("{}");
        assertEquals(2, tokens.size());
        assertEquals(JsonTokenType.PUNCTUATION, tokens.get(0).type());
        assertEquals(0, tokens.get(0).start());
        assertEquals(1, tokens.get(0).length());
    }

    private static JsonTokenType tokenTypeOf(List<JsonToken> tokens, String sourceText, String expectedSpan) {
        for (JsonToken token : tokens) {
            String span = sourceText.substring(token.start(), token.start() + token.length());
            if (span.equals(expectedSpan)) {
                return token.type();
            }
        }
        throw new AssertionError("No token found matching span: " + expectedSpan);
    }
}
