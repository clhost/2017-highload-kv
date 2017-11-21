package ru.mail.polis.netty.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.*;
import ru.mail.polis.netty.dao.EntityDao;
import ru.mail.polis.netty.services.schedule.Scheduler;
import ru.mail.polis.netty.services.ttl.TTLSaver;

import java.util.Set;

public class NettyHttpServerInitializer extends ChannelInitializer<SocketChannel> {
    private EntityDao dao;
    private Set<String> topology;
    private int port;
    private Scheduler scheduler;
    private TTLSaver ttlSaver;

    NettyHttpServerInitializer(EntityDao dao, Set<String> topology, int port, Scheduler scheduler, TTLSaver ttlSaver) {
        this.port = port;
        this.topology = topology;
        this.dao = dao;
        this.scheduler = scheduler;
        this.ttlSaver = ttlSaver;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        pipeline.addLast(new HttpRequestDecoder());
        //pipeline.addLast(new HttpResponseEncoder());

        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(1024*512));
        pipeline.addLast(new HttpHandler(dao, topology, port, scheduler, ttlSaver));
    }
}
