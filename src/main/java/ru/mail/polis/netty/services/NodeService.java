package ru.mail.polis.netty.services;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.netty.utils.UriDecoder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("FieldCanBeLocal")
public class NodeService implements INodeService {
    private Logger logger = LogManager.getLogger(NodeService.class);
    private ArrayList<FullHttpResponse> preparedResult = new ArrayList<>();
    private Connector connector;

    public NodeService() {
        connector = new Connector(this);
        //System.out.println("instanced");
    }

    @Override
    public ArrayList<FullHttpResponse> delete(@NotNull String key, @NotNull final Set<String> nodes) {
        preparedResult.clear();
        for (String node : nodes) {
            try {
                URI uri = new URI(node);
                String host = uri.getHost();
                int port = uri.getPort();

                // prepare request
                URI requestUri = new URI(node + "/v0/node?id=" + key);
                HttpRequest request = buildBasicRequest(HttpMethod.DELETE, requestUri);

                connector.connect(host, port, request);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
        return preparedResult;
    }

    @Override
    public ArrayList<FullHttpResponse> upsert(@NotNull String key,
                                              @NotNull byte[] value, @NotNull final Set<String> nodes) {
        preparedResult.clear();
        for (String node : nodes) {
            try {
                URI uri = new URI(node);
                String host = uri.getHost();
                int port = uri.getPort();

                // prepare request
                URI requestUri = new URI(node + "/v0/node?id=" + key);
                HttpRequest request = buildBasicRequest(HttpMethod.PUT, requestUri);

                connector.connect(host, port, request);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
        return preparedResult;
    }

    @Override
    public ArrayList<FullHttpResponse> upsert(@NotNull FullHttpRequest redirectedRequest,
                                              @NotNull final Set<String> nodes) {
        preparedResult.clear();
        for (String node : nodes) {
            try {
                URI uri = new URI(node);
                String host = uri.getHost();
                int port = uri.getPort();

                redirectedRequest.setUri(redirectedRequest.uri()
                                        .replace("entity", "node"));

                System.out.println("#1"); //тестил
                connector.connect(host, port, redirectedRequest.retain());
                System.out.println("#2");
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
        return preparedResult;
    }

    @Override
    public ArrayList<FullHttpResponse> get(@NotNull String key, @NotNull final Set<String> nodes) {
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
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
        return preparedResult;
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

    public void closeOperation() {
        connector.forceShutdown();
    }

    class Connector {
        private Bootstrap b;
        private EventLoopGroup group;
        private final NodeService instance;
        private ChannelFuture cf;

        Connector(NodeService instance) {
            System.out.println("Connector has been created.");
            this.instance = instance;
        }

        void forceShutdown() {
            group.shutdownGracefully();
        }

        void connect(@NotNull final String host,
                     final int port,
                     @NotNull final HttpRequest request) {
            if (b != null & group != null) {
                b = null;
                group.shutdownGracefully();
                group = null;
            }
            try {
                b = new Bootstrap();
                b.channel(NioSocketChannel.class)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) throws Exception {
                                ChannelPipeline pipeline = ch.pipeline();

                                pipeline.addLast(new HttpClientCodec());
                                pipeline.addLast(new HttpObjectAggregator(1024 * 512));
                                pipeline.addLast(new NodeServiceHandler(instance));
                            }
                        })
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 500);
                group = new NioEventLoopGroup();
                b.group(group);

                cf = b.connect(host, port).sync();

                cf.channel().writeAndFlush(request);
                cf.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                group.shutdownGracefully();
            }
        }
    }
}
