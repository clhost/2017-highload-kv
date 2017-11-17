package ru.mail.polis.netty.services;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Set;

public interface INodeService {
    ArrayList<FullHttpResponse> delete(@NotNull final String key, @NotNull final Set<String> nodes);
    ArrayList<FullHttpResponse> upsert(@NotNull final String key,
                                       @NotNull final byte[] value, @NotNull final Set<String> nodes);
    ArrayList<FullHttpResponse> upsert(@NotNull final FullHttpRequest redirectedRequest,
                                       @NotNull final Set<String> nodes);
    ArrayList<FullHttpResponse> get(@NotNull final String key, @NotNull final Set<String> nodes);
}
