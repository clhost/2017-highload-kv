package ru.mail.polis.netty.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ConnectTimeoutException;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.netty.dao.EntityDao;
import ru.mail.polis.netty.services.INodeService;
import ru.mail.polis.netty.services.NodeService;
import ru.mail.polis.netty.utils.SetHelper;
import ru.mail.polis.netty.utils.UriDecoder;

import java.util.ArrayList;
import java.util.Set;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

//@ChannelHandler.Sharable
public class HttpHandler extends SimpleChannelInboundHandler<Object> {
    private FullHttpRequest request;
    private EntityDao dao;
    private NodeService nodeService;
    private Set<String> topology;
    private Logger logger = LogManager.getLogger(HttpHandler.class);

    HttpHandler(EntityDao dao, Set<String> topology) {
        this.dao = dao;
        this.topology = topology;
        nodeService = new NodeService(); //fixme: тестовый вариант
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

            // handle /v0/status
            if (uri.equals(NettyHttpServer.STATUS_RAWPATH)) {
                final String RESPONSE_MESSAGE = "200";
                writeResponse(HttpResponseStatus.OK, RESPONSE_MESSAGE.getBytes(), ctx);
            }

            // handle /v0/entity - HTTP API between client and server
            if (uri.contains(NettyHttpServer.ENTITY_RAWPATH)) {
                String httpMethodName = request.method().asciiName().toString();
                String replicas;
                int ack, from;

                switch (httpMethodName) {
                    case "GET" :
                        System.out.println("E-GET: " + request.uri());

                        String getKey = UriDecoder.getParameter(uri, "id");
                        replicas = UriDecoder.getParameter(uri, "replicas");

                        if (replicas != null && !replicas.equals("")) {
                            from = Integer.parseInt(String.valueOf(replicas.charAt(2)));
                            ack  = Integer.parseInt(String.valueOf(replicas.charAt(0)));
                        } else {
                            from = topology.size();
                            ack  = from / 2 + 1;
                        }

                        if (ack == 0 || ack > from) {
                            writeResponse(HttpResponseStatus.BAD_REQUEST, "400".getBytes(), ctx);
                            break;
                        }

                        if (getKey != null && getKey.equals("")) {
                            writeResponse(HttpResponseStatus.BAD_REQUEST, "400".getBytes(), ctx);
                        } else if (getKey != null) {
                            int serverPoint = findPosInitialNode(getKey);
                            Set<String> subSet = SetHelper.subSet(topology, serverPoint, from);
                            // это какие узлы надо опросить, из них хотя бы ack должны ответить
                            ArrayList<FullHttpResponse> responses = nodeService.get(getKey, subSet);

                            checkAndRespond(responses, ack, ctx);
                        }
                        break;
                    case "PUT" :
                        System.out.println("E-PUT: " + request.uri());

                        String putKey = UriDecoder.getParameter(uri, "id");
                        replicas = UriDecoder.getParameter(uri, "replicas");
                        if (replicas != null && !replicas.equals("")) {
                            from = Integer.parseInt(String.valueOf(replicas.charAt(2)));
                            ack  = Integer.parseInt(String.valueOf(replicas.charAt(0)));
                        } else {
                            from = topology.size();
                            ack  = from / 2 + 1;
                        }

                        if (ack == 0 || ack > from) {
                            writeResponse(HttpResponseStatus.BAD_REQUEST, "400".getBytes(), ctx);
                            break;
                        }

                        if (putKey != null && putKey.equals("")) {
                            writeResponse(HttpResponseStatus.BAD_REQUEST, "400".getBytes(), ctx);
                        } else if (putKey != null) {
                            int serverPoint = findPosInitialNode(putKey);
                            Set<String> subSet = SetHelper.subSet(topology, serverPoint, from);
                            // редирект PUT реквеста, чтобы не писать свой в NodeService :)
                            ArrayList<FullHttpResponse> responses = nodeService.upsert(request, subSet);
                            checkAndRespond(responses, ack, ctx);
                        }
                        break;
                    case "DELETE" :
                        System.out.println("E-DELETE: " + request.uri());

                        String delKey = UriDecoder.getParameter(uri, "id");
                        replicas = UriDecoder.getParameter(uri, "replicas");

                        if (replicas != null && !replicas.equals("")) {
                            from = Integer.parseInt(String.valueOf(replicas.charAt(2)));
                            ack  = Integer.parseInt(String.valueOf(replicas.charAt(0)));
                        } else {
                            from = topology.size();
                            ack  = from / 2 + 1;
                        }

                        if (ack == 0 || ack > from) {
                            writeResponse(HttpResponseStatus.BAD_REQUEST, "400".getBytes(), ctx);
                            break;
                        }

                        if (delKey != null && delKey.equals("")) {
                            writeResponse(HttpResponseStatus.BAD_REQUEST, "400".getBytes(), ctx);
                        } else if (delKey != null) {
                            int serverPoint = findPosInitialNode(delKey);
                            Set<String> subSet = SetHelper.subSet(topology, serverPoint, from);
                            ArrayList<FullHttpResponse> responses = nodeService.delete(delKey, subSet);
                            checkAndRespond(responses, ack, ctx);
                        }
                        break;
                }
            }

            // handle /v0/node - local HTTP API between nodes
            if (uri.contains(NettyHttpServer.NODE_RAWPATH)) {
                String httpMethodName = request.method().asciiName().toString();
                switch (httpMethodName) {
                    case "GET":
                        System.out.println("N-GET: " + request.uri());

                        String getKey = UriDecoder.getParameter(uri, "id");
                        if (getKey != null && getKey.equals("")) {
                            writeResponse(HttpResponseStatus.BAD_REQUEST, "400".getBytes(), ctx);
                        } else if (getKey != null) {
                            byte[] bytes = dao.get(getKey);
                            if (bytes == null) {
                                writeResponse(HttpResponseStatus.NOT_FOUND, "404".getBytes(), ctx);
                            } else {
                                writeResponse(HttpResponseStatus.OK, bytes, ctx);
                            }
                        }
                        break;
                    case "PUT":
                        System.out.println("N-PUT: " + request.uri());

                        String putKey = UriDecoder.getParameter(uri, "id");
                        if (putKey != null && putKey.equals("")) {
                            writeResponse(HttpResponseStatus.BAD_REQUEST, "400".getBytes(), ctx);
                        } else if (putKey != null){
                            ByteBuf buf = request.content();
                            byte[] bytes = new byte[buf.readableBytes()];
                            while (buf.isReadable()) {
                                bytes[buf.readerIndex()] = buf.readByte();
                            }
                            dao.upsert(putKey, bytes);
                            writeResponse(HttpResponseStatus.CREATED, "201".getBytes(), ctx);
                        }
                        break;
                    case "DELETE":
                        System.out.println("N-DELETE: " + request.uri());

                        String delKey = UriDecoder.getParameter(uri, "id");
                        if (delKey != null && delKey.equals("")) {
                            writeResponse(HttpResponseStatus.BAD_REQUEST, "400".getBytes(), ctx);
                        } else if (delKey != null) {
                            dao.delete(delKey);
                            writeResponse(HttpResponseStatus.ACCEPTED, "200".getBytes(), ctx);
                        }
                        break;
                }
            }
        }
    }

    private boolean writeResponse(final @NotNull HttpResponseStatus status,
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
                //future.cause().printStackTrace();
            }
        });
        return isKeepAlive;
    }

    private int findPosInitialNode(String key) {
        return Math.abs(key.hashCode()) % topology.size();
    }

    private void checkAndRespond(final @NotNull ArrayList<FullHttpResponse> responses,
                                 final int ack, final @NotNull ChannelHandlerContext ctx) {
        int code404 = 0;
        int code504 = 0;
        int code400 = 0;
        int code201 = 0;
        int code202 = 0;
        int code200 = 0;
        byte[] bytes = new byte[0];
        for (FullHttpResponse r : responses) {
            if (r.status().code() == 404) code404++;
            if (r.status().code() == 501) code504++;
            if (r.status().code() == 400) code400++;
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
            writeResponse(HttpResponseStatus.CREATED, "201".getBytes(), ctx);
            return;
        }
        if (code202 >= ack) {
            writeResponse(HttpResponseStatus.ACCEPTED, "202".getBytes(), ctx);
            return;
        }
        if (code400 >= ack) {
            writeResponse(HttpResponseStatus.BAD_REQUEST, "400".getBytes(), ctx);
            return;
        }
        if (code404 >= ack) {
            writeResponse(HttpResponseStatus.NOT_FOUND, "404".getBytes(), ctx);
            return;
        }
        if (code504 >= ack) {
            writeResponse(HttpResponseStatus.GATEWAY_TIMEOUT, "504".getBytes(), ctx);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        //super.exceptionCaught(ctx, cause);
        if (cause instanceof ConnectTimeoutException) {
            //System.out.println("Exception");
            //ctx.close();
            //nodeService.closeOperation();
        }
    }
}
