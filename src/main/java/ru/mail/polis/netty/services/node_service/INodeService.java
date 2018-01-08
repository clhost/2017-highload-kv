package ru.mail.polis.netty.services.node_service;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Set;

public interface INodeService {
    ArrayList<FullHttpResponse> delete(@NotNull final String key,
                                       @NotNull final Set<String> nodes,
                                       @NotNull final HttpHeaders headers);
    ArrayList<FullHttpResponse> upsert(@NotNull final String key,
                                       @NotNull final byte[] value, @NotNull final Set<String> nodes,
                                       @NotNull final HttpHeaders headers);
    ArrayList<FullHttpResponse> get(@NotNull final String key,
                                    @NotNull final Set<String> nodes,
                                    @NotNull final HttpHeaders headers);
}
