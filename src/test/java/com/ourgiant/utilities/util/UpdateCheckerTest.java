package com.ourgiant.utilities.util;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateCheckerTest {

    @Test
    void detectsNewerPatchVersion() {
        assertTrue(UpdateChecker.isNewerVersion("1.2.1", "1.2.0"));
    }

    @Test
    void detectsNewerMinorVersion() {
        assertTrue(UpdateChecker.isNewerVersion("1.3.0", "1.2.9"));
    }

    @Test
    void detectsNewerMajorVersion() {
        assertTrue(UpdateChecker.isNewerVersion("2.0.0", "1.9.9"));
    }

    @Test
    void equalVersionsAreNotNewer() {
        assertFalse(UpdateChecker.isNewerVersion("1.2.0", "1.2.0"));
    }

    @Test
    void olderVersionIsNotNewer() {
        assertFalse(UpdateChecker.isNewerVersion("1.1.0", "1.2.0"));
    }

    @Test
    void differingSegmentCountsAreHandled() {
        assertTrue(UpdateChecker.isNewerVersion("1.2.1.0", "1.2"));
        assertFalse(UpdateChecker.isNewerVersion("1.2", "1.2.0.1"));
    }

    @Test
    void nonNumericSegmentsAreTreatedAsNotNewerRatherThanThrowing() {
        // Every segment before the malformed one must be equal, or an earlier segment decides
        // the comparison via short-circuit before the malformed one is ever parsed -- these two
        // both require reaching the bad segment.
        assertFalse(UpdateChecker.isNewerVersion("0.1.0", "0.1.0-SNAPSHOT"));
        assertFalse(UpdateChecker.isNewerVersion("1.1.0-rc1", "1.1.0"));
    }

    @Test
    void parsesTagNameAndHtmlUrlFromAReleaseResponse() {
        String response = """
            {
              "url": "https://api.github.com/repos/OurGiant/json-reader/releases/1",
              "html_url": "https://github.com/OurGiant/json-reader/releases/tag/v1.2.0",
              "tag_name": "v1.2.0",
              "author": {
                "login": "octocat",
                "html_url": "https://github.com/octocat"
              }
            }""";
        Optional<UpdateChecker.ReleaseInfo> result = UpdateChecker.parseReleaseInfo(response);
        assertTrue(result.isPresent());
        assertEquals("1.2.0", result.get().version());
        assertEquals("https://github.com/OurGiant/json-reader/releases/tag/v1.2.0", result.get().htmlUrl());
    }

    @Test
    void stripsLeadingVFromTagName() {
        String response = """
            {"tag_name": "v2.0.0", "html_url": "https://github.com/OurGiant/json-reader/releases/tag/v2.0.0"}""";
        Optional<UpdateChecker.ReleaseInfo> result = UpdateChecker.parseReleaseInfo(response);
        assertEquals("2.0.0", result.get().version());
    }

    @Test
    void missingFieldYieldsEmpty() {
        assertTrue(UpdateChecker.parseReleaseInfo("{\"tag_name\": \"v1.0.0\"}").isEmpty());
        assertTrue(UpdateChecker.parseReleaseInfo("{\"html_url\": \"https://github.com/x\"}").isEmpty());
    }

    @Test
    void malformedJsonYieldsEmptyRatherThanThrowing() {
        assertTrue(UpdateChecker.parseReleaseInfo("not json at all {{{").isEmpty());
    }
}
