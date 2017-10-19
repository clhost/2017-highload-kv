package ru.mail.polis.netty.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.netty.dao.EntityDao;
import ru.mail.polis.netty.utils.UriDecoder;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;


public class HttpHandler extends SimpleChannelInboundHandler<Object> {
    private FullHttpRequest request;
    private EntityDao dao;
    private Logger logger = LogManager.getLogger(HttpHandler.class);

    HttpHandler(EntityDao dao) {
        this.dao = dao;
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
            if (uri.equals("/v0/status")) {
                final String RESPONSE_MESSAGE = "200";
                writeResponse(HttpResponseStatus.OK, RESPONSE_MESSAGE.getBytes(), ctx);
            }

            // handle /v0/entity
            if (uri.contains("/v0/entity")) {
                String httpMethodName = request.method().asciiName().toString();

                switch (httpMethodName) {
                    case "GET" :
                        String value = UriDecoder.getParameter(uri, "id");
                        if (value != null && value.equals("")) {
                            writeResponse(HttpResponseStatus.BAD_REQUEST, "400".getBytes(), ctx);
                        } else if (value != null) {
                            byte[] bytes = dao.get(value);
                            if (bytes == null) {
                                writeResponse(HttpResponseStatus.NOT_FOUND, "404".getBytes(), ctx);
                            } else {
                                writeResponse(HttpResponseStatus.OK, bytes, ctx);
                            }
                        }
                        break;
                    case "PUT" :
                        String key = UriDecoder.getParameter(uri, "id");
                        if (key != null && key.equals("")) {
                            writeResponse(HttpResponseStatus.BAD_REQUEST, "400".getBytes(), ctx);
                        } else if (key != null){
                            ByteBuf buf = request.content();
                            byte[] bytes = new byte[buf.readableBytes()];

                            while (buf.isReadable()) {
                                bytes[buf.readerIndex()] = buf.readByte();
                            }
                            dao.upsert(key, bytes);
                            writeResponse(HttpResponseStatus.CREATED, "201".getBytes(), ctx);
                        }
                        break;
                    case "DELETE" :
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
        if (msg instanceof HttpContent) {

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
}