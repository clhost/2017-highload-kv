package ru.mail.polis.netty.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.mail.polis.KVService;
import ru.mail.polis.netty.dao.EntityDao;


public class NettyHttpServer implements KVService{
    private EventLoopGroup bossGroup;
    private EventLoopGroup workersGroup;
    private int port;
    private ChannelFuture cf;
    private EntityDao dao; // FIXME: 19.10.2017 Single dao - may be bad
    private Logger logger = LogManager.getLogger(NettyHttpServer.class);

    public NettyHttpServer(int port, EntityDao dao) {
        this.dao = dao;
        this.port = port;
        bossGroup = new NioEventLoopGroup();
        workersGroup = new NioEventLoopGroup();
    }

    private void run() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
        try {
            final ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap
                    .group(bossGroup, workersGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new NettyHttpServerInitializer(dao))
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            cf = serverBootstrap.bind(port).sync();
        } catch (InterruptedException e) {
            logger.warn("Interrupted.");
        }
    }

    private void shutdown() {
        try {
            bossGroup.shutdownGracefully();
            workersGroup.shutdownGracefully();
            cf.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void start() {
        run();
    }

    @Override
    public void stop() {
        shutdown();
    }

}
