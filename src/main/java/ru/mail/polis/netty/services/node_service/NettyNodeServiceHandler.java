package ru.mail.polis.netty.services.nodeService;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;


public class NettyNodeServiceHandler extends SimpleChannelInboundHandler<HttpObject> {
    private NettyNodeService service;

    NettyNodeServiceHandler(NettyNodeService service) {
        this.service = service;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        if (msg instanceof HttpResponse) {
            FullHttpResponse response = (FullHttpResponse) msg;
            service.getPreparedResult().add(response.retain());
            ctx.close();
        }
    }
}
