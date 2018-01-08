package ru.mail.polis.netty.services.node_service;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.netty.services.schedule.Scheduler;

import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Set;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

@SuppressWarnings("FieldCanBeLocal")
public class NettyNodeService implements INodeService {
    private Logger logger = LogManager.getLogger(NettyNodeService.class);
    private ArrayList<FullHttpResponse> preparedResult = new ArrayList<>();
    private Connector connector;
    private Scheduler scheduler;

    public NettyNodeService(Scheduler scheduler) {
        connector = new Connector(this);
        this.scheduler = scheduler;
    }

    @Override
    public ArrayList<FullHttpResponse> delete(@NotNull String key,
                                              @NotNull final Set<String> nodes,
                                              @NotNull final HttpHeaders headers) {
        preparedResult.clear();
        for (String node : nodes) {
            HttpRequest request = null;
            try {
                URI uri = new URI(node);
                String host = uri.getHost();
                int port = uri.getPort();

                // prepare request
                URI requestUri = new URI(node + "/v0/node?id=" + key);
                request = buildBasicRequest(HttpMethod.DELETE, requestUri);

                connector.connect(host, port, request);
            } catch (URISyntaxException | ConnectException e) {
                scheduler.save(request);
                preparedResult.add(buildResponse504());
                connector.shutdownGracefully();
            }
        }
        return preparedResult;
    }

    @Override
    public ArrayList<FullHttpResponse> upsert(@NotNull String key,
                                              @NotNull byte[] value,
                                              @NotNull final Set<String> nodes,
                                              @NotNull HttpHeaders headers) {
        preparedResult.clear();
        for (String node : nodes) {
            FullHttpRequest request = null;
            try {
                URI uri = new URI(node);
                String host = uri.getHost();
                int port = uri.getPort();

                // prepare request
                URI requestUri = new URI(node + "/v0/node?id=" + key);
                request = (FullHttpRequest) buildBasicRequest(HttpMethod.PUT, requestUri);
                request.content().writeBytes(value);

                request.headers().set(headers);
                request.headers().set(HttpHeaderNames.CONTENT_LENGTH, value.length);

                connector.connect(host, port, request);
            } catch (URISyntaxException | ConnectException e) {
                logger.warn("Probably, happened connect refused to : " + request.uri());
                scheduler.save(request);
                preparedResult.add(buildResponse504());
                connector.shutdownGracefully();
            }
        }
        return preparedResult;
    }

    @Override
    public ArrayList<FullHttpResponse> get(@NotNull String key,
                                           @NotNull final Set<String> nodes,
                                           @NotNull final HttpHeaders headers) {
        preparedResult.clear();
        for (String node : nodes) {
            try {
                URI uri = new URI(node);
                String host = uri.getHost();
                int port = uri.getPort();

                // prepare request
                URI requestUri = new URI(node + "/v0/node?id=" + key);
                HttpRequest request = buildBasicRequest(HttpMethod.GET, requestUri);


                connector.connect(host, port, request);
            } catch (URISyntaxException | ConnectException e) {
                preparedResult.add(buildResponse504());
                connector.shutdownGracefully();
            }
        }
        return preparedResult;
    }

    private FullHttpResponse buildResponse504() {
        byte[] bytes = "Gateway timeout.".getBytes();

        FullHttpResponse response = new DefaultFullHttpResponse(
                HTTP_1_1, HttpResponseStatus.GATEWAY_TIMEOUT,
                Unpooled.copiedBuffer(bytes)
        );
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, bytes.length);

        return response;
    }

    private HttpRequest buildBasicRequest(HttpMethod method, URI uri) {
        HttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, method, uri.toASCIIString()
        );
        request.headers().set(HttpHeaderNames.HOST, uri.getHost());
        request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        request.headers().set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);

        return request;
    }

    ArrayList<FullHttpResponse> getPreparedResult() {
        return preparedResult;
    }

    class Connector {
        private Bootstrap b;
        private EventLoopGroup group;
        private final NettyNodeService instance;
        private ChannelFuture cf;

        Connector(NettyNodeService instance) {
            this.instance = instance;
        }

        void shutdownGracefully() {
            b = null;
            group.shutdownGracefully();
        }

        void connect(@NotNull final String host,
                     final int port,
                     @NotNull final HttpRequest request) throws ConnectTimeoutException {
            try {
                b = new Bootstrap();
                b.channel(NioSocketChannel.class)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) throws Exception {
                                ChannelPipeline pipeline = ch.pipeline();

                                pipeline.addLast(new HttpClientCodec());
                                pipeline.addLast(new HttpObjectAggregator(1024 * 512));
                                pipeline.addLast(new NettyNodeServiceHandler(instance));
                            }
                        })
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 500);
                group = new NioEventLoopGroup();
                b.group(group);

                cf = b.connect(host, port).sync();

                cf.channel().writeAndFlush(request);
                cf.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                b = null;
                group.shutdownGracefully();
            } finally {
                b = null;
                group.shutdownGracefully();
            }
        }
    }
}
