package ru.mail.polis.netty.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.http.client.fluent.Request;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.mail.polis.KVService;
import ru.mail.polis.netty.dao.EntityDao;
import ru.mail.polis.netty.services.Scheduler;

import java.io.IOException;
import java.util.Set;


public class NettyHttpServer implements KVService{
    static final String STATUS_PATH = "/v0/status";
    static final String ENTITY_PATH = "/v0/entity";
    static final String NODE_PATH = "/v0/node";
    static final String AWAKE_PATH = "/v0/awake";
    private final String WORK_DIRECTORY;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workersGroup;
    private int port;
    private ChannelFuture cf;
    private EntityDao dao; // FIXME: 19.10.2017 Single dao - may be bad
    private Logger logger = LogManager.getLogger(NettyHttpServer.class);
    private Set<String> topology;
    private Scheduler scheduler;

    public NettyHttpServer(String workDir, int port, EntityDao dao, Set<String> topology) {
        this.WORK_DIRECTORY = workDir;
        this.topology = topology;
        this.dao = dao;
        this.port = port;
        scheduler = new Scheduler(workDir + "/" +  String.valueOf(port));
        bossGroup = new NioEventLoopGroup();
        workersGroup = new NioEventLoopGroup();
    }

    private void run() {
        System.out.println("Server binds on the port: " + port);
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
        try {
            final ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap
                    .group(bossGroup, workersGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new NettyHttpServerInitializer(dao, topology, port, scheduler))
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            cf = serverBootstrap.bind(port).sync();

            awake();
            scheduler.restoreState();
            scheduler.check();
        } catch (InterruptedException e) {
            logger.warn("Interrupted.");
        }
    }

    private void awake() {
        for (String uri : topology) {
            if (!uri.contains(String.valueOf(port))) {
                try {
                    Request.Get(uri + NettyHttpServer.AWAKE_PATH).execute();
                } catch (IOException e) {
                    logger.error("The node " + uri + " is asleep.");
                }
            }
        }
        System.out.println(port + ": Awake operation has been done.");
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
