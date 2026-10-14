package com.zhaow.restful.common;

import com.intellij.openapi.diagnostic.Logger;
import com.zhaow.utils.JsonUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class RequestHelper {

    private static final Logger LOG = Logger.getInstance(RequestHelper.class);

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private static final List<String> SUPPORTED_METHODS =
            List.of("GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS", "TRACE", "PATCH");

    private static final Pattern CHARSET_PATTERN =
            Pattern.compile("[Cc]hars[eet]\\s*=\\s*\"?([\\w\\-]+)\"?");

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            // A dev tool probing local services must not lean on system proxy settings.
            .proxy(HttpClient.Builder.NO_PROXY)
            .build();

    private RequestHelper() {
    }

    public static String request(String url, String method) {
        if (method == null) {
            return "method is null";
        }
        String upper = method.toUpperCase(Locale.ROOT);
        if (!SUPPORTED_METHODS.contains(upper)) {
            return "not supported method : " + method + ".";
        }
        return execute(completed(url), upper, null);
    }

    public static String postRequestBodyWithJson(String url, String json) {
        return execute(completed(url), "POST", json);
    }

    @NotNull
    private static String execute(String url, String method, String body) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(REQUEST_TIMEOUT);

        if (body == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            builder.header("Content-Type", "application/json; charset=utf-8")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        }

        try {
            HttpResponse<byte[]> response =
                    CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
            return decode(response);
        } catch (IOException e) {
            LOG.warn("Request failed: " + url, e);
            return "There was an error accessing to URL: " + url + "\n\n" + e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warn("Request interrupted: " + url, e);
            return "Request interrupted: " + url;
        } catch (IllegalArgumentException e) {
            LOG.warn("Malformed URL: " + url, e);
            return "Invalid URL: " + url + "\n\n" + e;
        }
    }

    @NotNull
    private static String decode(HttpResponse<byte[]> response) {
        byte[] payload = response.body();
        if (payload == null || payload.length == 0) {
            return response.statusCode() + " " + response.request().method() + " (empty response)";
        }
        String text = new String(payload, charsetOf(response));
        return JsonUtils.isValidJson(text) ? JsonUtils.format(text) : text;
    }

    private static Charset charsetOf(HttpResponse<byte[]> response) {
        return response.headers().firstValue("Content-Type")
                .map(RequestHelper::parseCharset)
                .orElse(StandardCharsets.UTF_8);
    }

    private static Charset parseCharset(String contentType) {
        Matcher matcher = CHARSET_PATTERN.matcher(contentType);
        if (matcher.find()) {
            try {
                return Charset.forName(matcher.group(1));
            } catch (Exception ignored) {
                // Unusable charset declaration: fall back to UTF-8.
            }
        }
        return StandardCharsets.UTF_8;
    }

    private static String completed(String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return "http://" + url;
        }
        return url;
    }
}
