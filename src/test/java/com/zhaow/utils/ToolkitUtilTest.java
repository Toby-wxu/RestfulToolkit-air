package com.zhaow.utils;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolkitUtilTest {

    @Test
    void stripsLocalhostPrefix() {
        assertEquals("/api/users", ToolkitUtil.removeRedundancyMarkup("http://localhost:8080/api/users"));
        assertEquals("/api/users", ToolkitUtil.removeRedundancyMarkup("localhost:8080/api/users"));
    }

    @Test
    void stripsHostAndPortPrefix() {
        assertEquals("/api/users", ToolkitUtil.removeRedundancyMarkup("http://example.com:9000/api/users"));
        assertEquals("/api/users", ToolkitUtil.removeRedundancyMarkup("https://10.0.0.5:8080/api/users"));
    }

    @Test
    void truncatesQueryString() {
        assertEquals("/api/users", ToolkitUtil.removeRedundancyMarkup("/api/users?page=1&size=10"));
    }

    @Test
    void leavesPlainPathAlone() {
        assertEquals("/api/users/{id}", ToolkitUtil.removeRedundancyMarkup("/api/users/{id}"));
    }

    @Test
    void parsesKeyColonValueLines() {
        Map<String, String> params = ToolkitUtil.textToParamMap("page: 1\nsize: 20\n// note: ignored\n");

        assertEquals(2, params.size());
        assertEquals("1", params.get("page"));
        assertEquals("20", params.get("size"));
    }

    /**
     * Only the first ':' separates key from value, otherwise a time-like value is truncated.
     */
    @Test
    void keepsColonsInsideTheValue() {
        Map<String, String> params = ToolkitUtil.textToParamMap("start: 12:30:00");

        assertEquals("12:30:00", params.get("start"));
    }

    @Test
    void skipsLinesWithoutASeparator() {
        Map<String, String> params = ToolkitUtil.textToParamMap("novalue\nkey: value");

        assertEquals(1, params.size());
        assertEquals("value", params.get("key"));
    }

    @Test
    void joinsParamsWithoutTrailingAmpersand() {
        String query = ToolkitUtil.textToRequestParam("page: 1");

        assertEquals("page=1", query);
    }

    @Test
    void preservesInputOrderSoCopiedUrlsAreStable() {
        String query = ToolkitUtil.textToRequestParam("a: 1\nb: 2\nc: 3");

        assertEquals("a=1&b=2&c=3", query);
        assertTrue(query.split("&").length == 3);
    }

    @Test
    void emptyTextProducesEmptyQuery() {
        assertEquals("", ToolkitUtil.textToRequestParam(""));
    }
}
