package ru.mail.polis.netty.services.node_service;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.netty.services.schedule.Scheduler;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class ApacheNodeService implements INodeService {
    private Logger logger = LogManager.getLogger(ApacheNodeService.class);
    private ArrayList<FullHttpResponse> preparedResult = new ArrayList<>();
    private Scheduler scheduler;

    public ApacheNodeService(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public ArrayList<FullHttpResponse> delete(@NotNull String key,
                                              @NotNull Set<String> nodes,
                                              @NotNull final HttpHeaders headers) {
        URI requestUri = null;
        preparedResult.clear();
        for (String node : nodes) {
            try {
                // prepare request
                requestUri = new URI(node + "/v0/node?id=" + key);

                Request request = Request.Delete(requestUri);

                for (Map.Entry<String, String> entry : headers) {
                    if (!entry.getKey().equals(org.apache.http.HttpHeaders.CONTENT_LENGTH)) {
                        request.addHeader(new BasicHeader(entry.getKey(), entry.getValue()));
                    }
                }

                Response apacheResponse = request.execute();
                int code = apacheResponse.returnResponse().getStatusLine().getStatusCode();

                FullHttpResponse nettyResponse;
                if (code == 202) {
                    nettyResponse = buildResponse(HttpResponseStatus.ACCEPTED, "accepted".getBytes());
                    preparedResult.add(nettyResponse);
                }

            } catch (URISyntaxException | IOException e) {
                if (!e.getClass().equals(URISyntaxException.class)) {
                    logger.warn("Probably, happened connect refused.");
                    preparedResult.add(buildResponse(HttpResponseStatus.GATEWAY_TIMEOUT, "gateway".getBytes()));
                    if (requestUri != null) {
                        scheduler.save(
                                buildRequest(
                                        requestUri.toString(),
                                        HttpVersion.HTTP_1_1,
                                        HttpMethod.DELETE,
                                        null,
                                        headers));
                    }
                } else {
                    logger.warn("Uri syntax error.");
                }
            }
        }
        return preparedResult;
    }

    @Override
    public ArrayList<FullHttpResponse> upsert(@NotNull String key,
                                              @NotNull byte[] value,
                                              @NotNull Set<String> nodes,
                                              @NotNull HttpHeaders headers) {
        URI requestUri = null;
        preparedResult.clear();
        for (String node : nodes) {
            try {
                // prepare request
                requestUri = new URI(node + "/v0/node?id=" + key);
                Request request = Request.Put(requestUri).bodyByteArray(value);

                for (Map.Entry<String, String> entry : headers) {
                    if (!entry.getKey().equals(org.apache.http.HttpHeaders.CONTENT_LENGTH)) {
                        request.addHeader(new BasicHeader(entry.getKey(), entry.getValue()));
                    }
                }

                Response apacheResponse = request.execute();
                int code = apacheResponse.returnResponse().getStatusLine().getStatusCode();

                FullHttpResponse nettyResponse;
                if (code == 201) {
                    nettyResponse = buildResponse(HttpResponseStatus.CREATED, "created".getBytes());
                    preparedResult.add(nettyResponse);
                }

            } catch (URISyntaxException | IOException e) {
                if (!e.getClass().equals(URISyntaxException.class)) {
                    logger.warn("Probably, happened connect refused.");
                    preparedResult.add(buildResponse(HttpResponseStatus.GATEWAY_TIMEOUT, "gateway".getBytes()));
                    if (requestUri != null) {
                        scheduler.save(
                                buildRequest(
                                        requestUri.toString(),
                                        HttpVersion.HTTP_1_1,
                                        HttpMethod.PUT,
                                        value,
                                        headers));
                    }
                } else {
                    logger.warn("Uri syntax error.");
                }
            }
        }
        return preparedResult;
    }

    @Override
    public ArrayList<FullHttpResponse> get(@NotNull String key,
                                           @NotNull Set<String> nodes,
                                           @NotNull final HttpHeaders headers) {
        preparedResult.clear();
        for (String node : nodes) {
            try {
                // prepare request
                URI requestUri = new URI(node + "/v0/node?id=" + key);
                Request request = Request.Get(requestUri);

                for (Map.Entry<String, String> entry : headers) {
                    if (!entry.getKey().equals(org.apache.http.HttpHeaders.CONTENT_LENGTH)) {
                        request.addHeader(new BasicHeader(entry.getKey(), entry.getValue()));
                    }
                }

                Response apacheResponse = request.execute();
                org.apache.http.HttpResponse response = apacheResponse.returnResponse();

                int code = response.getStatusLine().getStatusCode();
                FullHttpResponse nettyResponse;
                if (code == 200) {
                    nettyResponse = buildResponse(HttpResponseStatus.OK, EntityUtils.toByteArray(response.getEntity()));
                    preparedResult.add(nettyResponse);
                }

                if (code == 404) {
                    nettyResponse = buildResponse(HttpResponseStatus.NOT_FOUND, "not found".getBytes());
                    preparedResult.add(nettyResponse);
                }

            } catch (URISyntaxException | IOException e) {
                if (!e.getClass().equals(URISyntaxException.class)) {
                    logger.warn("Probably, happened connect refused.");
                    preparedResult.add(buildResponse(HttpResponseStatus.GATEWAY_TIMEOUT, "gateway".getBytes()));
                } else {
                    logger.warn("Uri syntax error.");
                }
            }
        }
        return preparedResult;
    }

    private FullHttpResponse buildResponse(HttpResponseStatus status, byte[] bytes) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HTTP_1_1, status,
                Unpooled.copiedBuffer(bytes)
        );
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, bytes.length);

        return response;
    }

    private static FullHttpRequest buildRequest(@NotNull final String uri,
                                         @NotNull HttpVersion version,
                                         @NotNull HttpMethod method,
                                         byte[] data,
                                         @NotNull HttpHeaders headers) {

        if (method == HttpMethod.PUT & data == null) {
            throw new NullPointerException("The data for the PUT method turned out to be null.");
        }

        if (method == HttpMethod.DELETE & data != null) {
            throw new RuntimeException("The DELETE method doesn't require any data.");
        }

        FullHttpRequest httpRequest = new DefaultFullHttpRequest(version, method, uri);
        httpRequest.headers().set(headers);

        if (data != null) {
            httpRequest.content().writeBytes(data);
        }

        return httpRequest;
    }
}
