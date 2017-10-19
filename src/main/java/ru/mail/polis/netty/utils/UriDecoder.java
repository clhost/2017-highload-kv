package ru.mail.polis.netty.utils;

import io.netty.handler.codec.http.QueryStringDecoder;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class UriDecoder {
    private static QueryStringDecoder decoder;
    private static String cachedUri;

    public static String getParameter(final @NotNull String uri,
                                      final @NotNull String key) {
        if (cachedUri != null && cachedUri.equals(uri)) {
            Map<String, List<String>> params = decoder.parameters();
            if (!params.isEmpty()) {
                for (Map.Entry<String, List<String>> param : params.entrySet()) {
                    if (param.getKey().equals(key)) {
                        return param.getValue().get(0);
                    }
                }
            }
            return null;
        } else {
            cachedUri = uri;
            decoder = new QueryStringDecoder(uri);
            Map<String, List<String>> params = decoder.parameters();
            if (!params.isEmpty()) {
                for (Map.Entry<String, List<String>> param : params.entrySet()) {
                    if (param.getKey().equals(key)) {
                        return param.getValue().get(0);
                    }
                }
            }
            return null;
        }
    }
}
