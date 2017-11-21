package ru.mail.polis.netty.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.netty.dao.EntityDao;
import ru.mail.polis.netty.services.INodeService;
import ru.mail.polis.netty.services.NettyNodeService;
import ru.mail.polis.netty.services.Scheduler;
import ru.mail.polis.netty.utils.SetHelper;
import ru.mail.polis.netty.utils.UriDecoder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

@ChannelHandler.Sharable
public class HttpHandler extends SimpleChannelInboundHandler<Object> {
    private FullHttpRequest request;
    private EntityDao dao;
    private INodeService nodeService;
    private Set<String> topology;
    private int currentPort;
    private Scheduler scheduler;
    private Logger logger = LogManager.getLogger(HttpHandler.class);

    HttpHandler(EntityDao dao, Set<String> topology, int port, Scheduler scheduler) {
        this.currentPort = port;
        this.dao = dao;
        this.topology = topology;
        this.scheduler = scheduler;
        nodeService = new NettyNodeService(scheduler);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            request = (FullHttpRequest) msg;
            String uri = request.uri();

            // handle /v0/awake
            if (uri.equals(NettyHttpServer.AWAKE_PATH)) {
                writeResponse(HttpResponseStatus.OK, "I am awakened!".getBytes(), ctx);
                scheduler.check();
            }

            // handle /v0/status
            if (uri.equals(NettyHttpServer.STATUS_PATH)) {
                final String RESPONSE_MESSAGE = "200";
                writeResponse(HttpResponseStatus.OK, RESPONSE_MESSAGE.getBytes(), ctx);
            }

            // handle /v0/entity - HTTP API between client and server
            if (uri.contains(NettyHttpServer.ENTITY_PATH)) {
                String httpMethodName = request.method().asciiName().toString();

                switch (httpMethodName) {
                    case "GET" :
                        entityProduce(uri, ctx, (key, ctx0, ack0, from0) -> {
                            int serverPoint = findPosInitialNode(key);
                            boolean isContained = false;
                            Set<String> subSet = SetHelper.subSet(topology, serverPoint, from0);

                            Iterator<String> it = subSet.iterator();
                            while (it.hasNext()) {
                                String sub = it.next();
                                if (sub.contains(String.valueOf(currentPort))) {
                                    isContained = true;
                                    it.remove();
                                }
                            }

                            ArrayList<FullHttpResponse> responses = nodeService.get(key, subSet);

                            /* Достаем данные из текущей ноды, если она содержится в выборке*/
                            if (isContained) {
                                nodeProduce(uri, ctx, (key1, ctx1, ack, from) -> {
                                    byte[] bytes = dao.get(key1);
                                    if (bytes == null) {
                                        responses.add(buildResponse(HttpResponseStatus.NOT_FOUND, "Not Found".getBytes()));
                                    } else {
                                        responses.add(buildResponse(HttpResponseStatus.OK, bytes));
                                    }
                                });
                            }

                            checkAndRespond(responses, ack0, ctx0);
                        });
                        break;
                    case "PUT" :
                        entityProduce(uri, ctx, (key, ctx0, ack0, from0) -> {
                            int serverPoint = findPosInitialNode(key);
                            boolean isContained = false;
                            Set<String> subSet = SetHelper.subSet(topology, serverPoint, from0);

                            Iterator<String> it = subSet.iterator();
                            while (it.hasNext()) {
                                String sub = it.next();
                                if (sub.contains(String.valueOf(currentPort))) {
                                    isContained = true;
                                    it.remove();
                                }
                            }

                            /* Оставлю закомментированным, чтобы не забывать, что такой подход не очень эффективен
                            * Здесь был редирект
                            * ArrayList<FullHttpResponse> responses = nodeService.upsert(request, subSet);
                            */
                            ByteBuf buffer = (request).content().retain();
                            byte[] bytes0 = new byte[buffer.readableBytes()];
                            while (buffer.isReadable()) {
                                bytes0[buffer.readerIndex()] = buffer.readByte();
                            }
                            ArrayList<FullHttpResponse> responses = nodeService.upsert(key, bytes0, subSet);

                            /* Кладем данные на текущую ноду, если она содержится в выборке*/
                            if (isContained) {
                                nodeProduce(uri, ctx, (key1, ctx1, ack, from) -> {
                                    ByteBuf buf = request.content().resetReaderIndex();
                                    byte[] bytes = new byte[buf.readableBytes()];
                                    while (buf.isReadable()) {
                                        bytes[buf.readerIndex()] = buf.readByte();
                                    }
                                    dao.upsert(key1, bytes);
                                    responses.add(buildResponse(HttpResponseStatus.CREATED, "Created".getBytes()));
                                });
                            }

                            checkAndRespond(responses, ack0, ctx0);
                        });
                        break;
                    case "DELETE" :
                        entityProduce(uri, ctx, (key, ctx0, ack0, from0) -> {
                            int serverPoint = findPosInitialNode(key);
                            boolean isContained = false;
                            Set<String> subSet = SetHelper.subSet(topology, serverPoint, from0);

                            Iterator<String> it = subSet.iterator();
                            while (it.hasNext()) {
                                String sub = it.next();
                                if (sub.contains(String.valueOf(currentPort))) {
                                    isContained = true;
                                    it.remove();
                                }
                            }

                            ArrayList<FullHttpResponse> responses = nodeService.delete(key, subSet);

                            /* Удаляем данные из текущей ноды, если она содержится в выборке*/
                            if (isContained) {
                                nodeProduce(uri, ctx, (key1, ctx1, ack, from) -> {
                                    dao.delete(key1);
                                    responses.add(buildResponse(HttpResponseStatus.ACCEPTED, "Deleted".getBytes()));
                                });
                            }

                            checkAndRespond(responses, ack0, ctx0);
                        });
                        break;
                }
            }

            // handle /v0/node - local HTTP API between nodes
            if (uri.contains(NettyHttpServer.NODE_PATH)) {
                String httpMethodName = request.method().asciiName().toString();
                switch (httpMethodName) {
                    case "GET":
                        nodeProduce(uri, ctx, (key, ctx0, ack ,from) -> {
                            byte[] bytes = dao.get(key);
                            if (bytes == null) {
                                writeResponse(HttpResponseStatus.NOT_FOUND, "404".getBytes(), ctx0);
                            } else {
                                writeResponse(HttpResponseStatus.OK, bytes, ctx0);
                            }
                        });
                        break;
                    case "PUT":
                        nodeProduce(uri, ctx, (key, ctx0, ack, from) -> {
                            ByteBuf buf = request.content().resetReaderIndex();
                            byte[] bytes = new byte[buf.readableBytes()];
                            while (buf.isReadable()) {
                                bytes[buf.readerIndex()] = buf.readByte();
                            }
                            dao.upsert(key, bytes);
                            writeResponse(HttpResponseStatus.CREATED, "Created".getBytes(), ctx0);
                        });
                        break;
                    case "DELETE":
                        nodeProduce(uri, ctx, (key, ctx0, ack, from) -> {
                            dao.delete(key);
                            writeResponse(HttpResponseStatus.ACCEPTED, "Accepted".getBytes(), ctx0);
                        });
                        break;
                }
            }
        }
    }

    private void entityProduce(final @NotNull String uri, final ChannelHandlerContext ctx,
                               Producer producer) {
        String key = UriDecoder.getParameter(uri, "id");
        String replicas = UriDecoder.getParameter(uri, "replicas");
        int ack, from;

        if (replicas != null && !replicas.equals("")) {
            from = Integer.parseInt(String.valueOf(replicas.charAt(2)));
            ack  = Integer.parseInt(String.valueOf(replicas.charAt(0)));
        } else {
            from = topology.size();
            ack  = from / 2 + 1;
        }

        if (ack == 0 || ack > from) {
            writeResponse(HttpResponseStatus.BAD_REQUEST, "400".getBytes(), ctx);
            return;
        }

        if (key != null && key.equals("")) {
            writeResponse(HttpResponseStatus.BAD_REQUEST, "400".getBytes(), ctx);
        } else if (key != null) {
            producer.produce(key, ctx, ack, from);
        }
    }

    private void nodeProduce(final @NotNull String uri, final ChannelHandlerContext ctx,
                             Producer producer) {
        String key = UriDecoder.getParameter(uri, "id");
        if (key != null && key.equals("")) {
            writeResponse(HttpResponseStatus.BAD_REQUEST, "400 Bad request".getBytes(), ctx);
        } else if (key != null) {
            producer.produce(key, ctx, null, null);
        }
    }

    @FunctionalInterface
    interface Producer {
        void produce(String key, ChannelHandlerContext ctx, Integer ack, Integer from);
    }


    private void writeResponse(final @NotNull HttpResponseStatus status,
                               final @NotNull byte[] bytes,
                               final @NotNull ChannelHandlerContext ctx) {
        boolean isKeepAlive = HttpUtil.isKeepAlive(request);

        FullHttpResponse response = new DefaultFullHttpResponse(
                HTTP_1_1, status,
                Unpooled.copiedBuffer(bytes)
        );
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, bytes.length);

        if (isKeepAlive) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }
        ctx.writeAndFlush(response).addListener(future -> {
            if (!future.isSuccess()) {
                logger.error("Something wrong with written some data.");
            }
        });
    }

    private FullHttpResponse buildResponse(final @NotNull HttpResponseStatus status,
                                           final @NotNull byte[] bytes) {

        FullHttpResponse response = new DefaultFullHttpResponse(
                HTTP_1_1, status,
                Unpooled.copiedBuffer(bytes)
        );
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, bytes.length);

        return response;
    }

    private int findPosInitialNode(String key) {
        return Math.abs(key.hashCode()) % topology.size();
    }

    private void checkAndRespond(final @NotNull ArrayList<FullHttpResponse> responses,
                                 final int ack, final @NotNull ChannelHandlerContext ctx) {
        int code404 = 0;
        int code201 = 0;
        int code202 = 0;
        int code200 = 0;
        byte[] bytes = new byte[0];
        for (FullHttpResponse r : responses) {
            if (r.status().code() == 404) code404++;
            if (r.status().code() == 201) code201++;
            if (r.status().code() == 202) code202++;
            if (r.status().code() == 200) {
                code200++;
                ByteBuf buf = r.content();
                bytes = new byte[buf.readableBytes()];
                while (buf.isReadable()) {
                    bytes[buf.readerIndex()] = buf.readByte();
                }
            }
        }

        if (code200 >= ack) {
            writeResponse(HttpResponseStatus.OK, bytes, ctx);
            return;
        }

        if (code201 >= ack) {
            writeResponse(HttpResponseStatus.CREATED, "Created".getBytes(), ctx);
            return;
        }

        if (code202 >= ack) {
            writeResponse(HttpResponseStatus.ACCEPTED, "Accepted".getBytes(), ctx);
            return;
        }

        if (code404 >= ack) {
            writeResponse(HttpResponseStatus.NOT_FOUND, "Not Found".getBytes(), ctx);
            return;
        }

        writeResponse(HttpResponseStatus.GATEWAY_TIMEOUT, "Gateway Timeout".getBytes(), ctx);
    }
}
