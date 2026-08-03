package com.ourgiant.utilities.util;

import com.ourgiant.utilities.core.JsonProcessingException;
import com.ourgiant.utilities.core.JsonProcessor;
import com.ourgiant.utilities.model.JsonToken;
import com.ourgiant.utilities.model.JsonTokenType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLHandshakeException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Checks GitHub's releases API for a newer JSON Viewer version. This is the app's only outbound
 * network call at runtime — everything else (formatting, linting, tokenizing) is fully offline.
 * Response parsing reuses {@link JsonProcessor#tokenize}, the same hand-rolled tokenizer the app
 * already uses for syntax highlighting, rather than pulling in a JSON library for two fields
 * (see SPEC.md for why this project keeps JSON parsing hand-rolled).
 */
public final class UpdateChecker {
    private static final Logger logger = LoggerFactory.getLogger(UpdateChecker.class);
    private static final String RELEASES_URL = "https://api.github.com/repos/OurGiant/json-reader/releases/latest";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    public record ReleaseInfo(String version, String htmlUrl) {
    }

    private UpdateChecker() {
    }

    /**
     * Does a real network call — run this off the EDT (e.g. from a SwingWorker).
     * @return empty on a generic failure (offline, rate-limited, no releases yet, malformed response, ...)
     * @throws NetworkFetchException on a TLS handshake failure specifically, with a user-facing message.
     */
    public static Optional<ReleaseInfo> fetchLatestRelease() {
        try {
            HttpClient client = HttpClientFactory.create(CONNECT_TIMEOUT);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(RELEASES_URL))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "json-reader")
                .timeout(REQUEST_TIMEOUT)
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return Optional.empty();
            }
            return parseReleaseInfo(response.body());
        } catch (SSLHandshakeException e) {
            logger.warn("TLS handshake failed fetching latest release from GitHub (possible TLS-inspecting proxy)", e);
            throw new NetworkFetchException("Couldn't verify the secure connection (possible corporate network proxy)", e);
        } catch (Exception e) {
            logger.warn("Failed to fetch latest release from GitHub", e);
            return Optional.empty();
        }
    }

    /**
     * Extracts {@code tag_name}/{@code html_url} from a GitHub release API response, tracking
     * bracket depth so only the top-level fields match — the response also nests an
     * {@code author} object with its own {@code html_url} (the author's profile page, not the
     * release page), which a depth-blind scan would wrongly prefer or overwrite depending on
     * field order.
     */
    static Optional<ReleaseInfo> parseReleaseInfo(String json) {
        try {
            List<JsonToken> tokens = JsonProcessor.tokenize(json);
            int depth = 0;
            String tagName = null;
            String htmlUrl = null;
            for (int i = 0; i < tokens.size(); i++) {
                JsonToken token = tokens.get(i);
                if (token.type() == JsonTokenType.PUNCTUATION) {
                    char c = json.charAt(token.start());
                    if (c == '{' || c == '[') {
                        depth++;
                    } else if (c == '}' || c == ']') {
                        depth--;
                    }
                    continue;
                }
                if (depth != 1 || token.type() != JsonTokenType.KEY) {
                    continue;
                }
                String key = json.substring(token.start() + 1, token.start() + token.length() - 1);
                if (!key.equals("tag_name") && !key.equals("html_url")) {
                    continue;
                }
                int j = i + 1;
                while (j < tokens.size() && tokens.get(j).type() == JsonTokenType.PUNCTUATION) {
                    j++;
                }
                if (j >= tokens.size() || tokens.get(j).type() != JsonTokenType.STRING) {
                    continue;
                }
                JsonToken valueToken = tokens.get(j);
                String value = JsonProcessor.unescape(
                    json.substring(valueToken.start() + 1, valueToken.start() + valueToken.length() - 1));
                if (key.equals("tag_name")) {
                    tagName = value;
                } else {
                    htmlUrl = value;
                }
            }
            if (tagName == null || htmlUrl == null) {
                return Optional.empty();
            }
            String version = tagName.startsWith("v") ? tagName.substring(1) : tagName;
            return Optional.of(new ReleaseInfo(version, htmlUrl));
        } catch (JsonProcessingException e) {
            logger.warn("Malformed release response from GitHub", e);
            return Optional.empty();
        }
    }

    /** @return true if {@code latest} is a strictly newer dotted-numeric version than {@code current}; false (not an exception) on any non-numeric segment. */
    public static boolean isNewerVersion(String latest, String current) {
        try {
            String[] latestParts = latest.split("\\.");
            String[] currentParts = current.split("\\.");
            int len = Math.max(latestParts.length, currentParts.length);
            for (int i = 0; i < len; i++) {
                int l = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;
                int c = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
                if (l > c) {
                    return true;
                }
                if (l < c) {
                    return false;
                }
            }
        } catch (NumberFormatException e) {
            logger.debug("Could not compare versions: {} vs {}", latest, current);
        }
        return false;
    }
}
