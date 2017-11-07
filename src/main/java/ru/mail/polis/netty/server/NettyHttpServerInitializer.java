package ru.mail.polis.netty.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.*;
import ru.mail.polis.netty.dao.EntityDao;

import java.util.Set;

public class NettyHttpServerInitializer extends ChannelInitializer<SocketChannel> {
    private EntityDao dao;
    private Set<String> topology;

    NettyHttpServerInitializer(EntityDao dao, Set<String> topology) {
        this.topology = topology;
        this.dao = dao;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        pipeline.addLast(new HttpRequestDecoder());
        //pipeline.addLast(new HttpResponseEncoder());

        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(1024*512));
        pipeline.addLast(new HttpHandler(dao, topology));
    }
}
