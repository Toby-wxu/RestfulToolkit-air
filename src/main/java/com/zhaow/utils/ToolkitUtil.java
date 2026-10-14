package com.zhaow.utils;


import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Pure URL / query-param text helpers. Deliberately free of IntelliJ APIs so it can be
 * unit tested without a platform fixture.
 */
public class ToolkitUtil {

    private static final Pattern LOCALHOST_PATTERN =
            Pattern.compile("(http(s?)://)?localhost(:\\d+)?");

    // The domain and IP alternatives are separated by '|' with no surrounding space, and the
    // optional port must be consumed, otherwise ":8080" would survive the strip.
    private static final Pattern HOST_AND_PORT_PATTERN = Pattern.compile(
            "(http(s?)://)?(" +
            "([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,6}" + "|" +  // domain
            "((2[0-4]\\d|25[0-5]|[01]?\\d\\d?)\\.){3}(2[0-4]\\d|25[0-5]|[01]?\\d\\d?)" + "|" + // ip address
            "localhost" +
            ")(:\\d+)?");

    private ToolkitUtil() {
    }

    @NotNull
    public static String removeRedundancyMarkup(@NotNull String pattern) {
        if (pattern.contains("localhost")) {
            pattern = LOCALHOST_PATTERN.matcher(pattern).replaceFirst("");
        }
        // quick test if reg exp should be used
        if (pattern.contains("http:") || pattern.contains("https:")) {
            pattern = HOST_AND_PORT_PATTERN.matcher(pattern).replaceFirst("");
        }

        //TODO : resolve RequestMapping(params="method=someMethod")
        int queryStart = pattern.indexOf('?');
        if (queryStart >= 0) {
            pattern = pattern.substring(0, queryStart);
        }
        return pattern;
    }


    @NotNull
    public static String textToRequestParam(String text) {
        StringBuilder param = new StringBuilder();

        Map<String, String> paramMap = textToParamMap(text);

        paramMap.forEach((s, o) -> param.append(s).append('=').append(o).append('&'));

        return param.length() == 0 ? "" : param.deleteCharAt(param.length() - 1).toString();
    }


    /**
     * Parses the "key: value" lines of the Request Params pane. Lines starting with "//"
     * are treated as comments. A value may itself contain ':', so only the first one splits.
     */
    @NotNull
    public static Map<String, String> textToParamMap(String text) {
        // Insertion-ordered: the generated query string must be stable between copies.
        Map<String, String> paramMap = new LinkedHashMap<>();

        for (String line : text.split("\n")) {
            if (line.startsWith("//") || !line.contains(":")) {
                continue;
            }

            String[] prop = line.split(":", 2);

            if (prop.length > 1) {
                paramMap.put(prop[0].trim(), prop[1].trim());
            }
        }
        return paramMap;
    }


}
