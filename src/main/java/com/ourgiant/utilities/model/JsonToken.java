package com.ourgiant.utilities.model;

/** A colored span within a JSON text, produced by {@code JsonProcessor.tokenize} for syntax highlighting. */
public record JsonToken(int start, int length, JsonTokenType type) {
}
