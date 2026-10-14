package com.zhaow.restful.common.spring;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives the "Go to Service" popup: the declared mapping is the pattern and whatever the
 * user typed is the path.
 */
class AntPathMatcherTest {

    private final AntPathMatcher matcher = new AntPathMatcher();

    @Test
    void matchesLiteralPath() {
        assertTrue(matcher.match("/api/users", "/api/users"));
    }

    @Test
    void rejectsDifferentLiteralPath() {
        assertFalse(matcher.match("/api/users", "/api/orders"));
    }

    @Test
    void matchesPathVariablePlaceholder() {
        assertTrue(matcher.match("/product/{productId}", "/product/42"));
    }

    /**
     * The vendored copy exposes doMatch rather than extractPathWithinPattern; the popup only
     * needs match(), but capturing the placeholder proves the pattern is parsed correctly.
     */
    @Test
    void capturesPathVariableValue() {
        Map<String, String> variables = new HashMap<>();

        assertTrue(matcher.doMatch("/product/{productId}", "/product/42", true, variables));
        assertEquals("42", variables.get("productId"));
    }

    @Test
    void pathVariablePlaceholderDoesNotSpanSegments() {
        assertFalse(matcher.match("/product/{id}", "/product/1/details"));
    }

    @Test
    void singleStarStaysWithinOneSegment() {
        assertTrue(matcher.match("/product/*", "/product/42"));
        assertFalse(matcher.match("/product/*", "/product/42/details"));
    }

    @Test
    void doubleStarSpansSegments() {
        assertTrue(matcher.match("/product/**", "/product/42/details"));
        assertTrue(matcher.match("/**", "/anything/at/all"));
    }

    /**
     * REST style mappings may constrain a placeholder, e.g. {textualPart:[a-z-]+}.{numericPart:[\\d]+}.
     */
    @Test
    void supportsRegexInsidePlaceholder() {
        assertTrue(matcher.match("/{name:[a-z-]+}.json", "/hello-world.json"));
        assertFalse(matcher.match("/{id:[0-9]+}", "/abc"));
    }

    @Test
    void trailingSlashIsTreatedAsDistinctByDefault() {
        assertFalse(matcher.match("/api/users", "/api/users/"));
    }

    @Test
    void emptyPatternMatchesOnlyEmptyPath() {
        assertTrue(matcher.match("", ""));
        assertFalse(matcher.match("", "/api"));
    }
}
