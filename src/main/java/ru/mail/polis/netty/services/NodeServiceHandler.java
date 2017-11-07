package ru.mail.polis.netty.services;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ConnectTimeoutException;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;


public class NodeServiceHandler extends SimpleChannelInboundHandler<HttpObject> {
    private NodeService service;

    NodeServiceHandler(NodeService service) {
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
