package top.fengpingtech.solen.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.MultithreadEventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollMode;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import top.fengpingtech.solen.service.EventRepository;
import top.fengpingtech.solen.slotmachine.ConnectionAttributeHolder;
import top.fengpingtech.solen.slotmachine.ConnectionKeeperHandler;
import top.fengpingtech.solen.slotmachine.ConnectionManager;
import top.fengpingtech.solen.slotmachine.EventProcessor;
import top.fengpingtech.solen.slotmachine.MessageDebugger;
import top.fengpingtech.solen.slotmachine.MessageDecoder;
import top.fengpingtech.solen.slotmachine.MessageEncoder;
import top.fengpingtech.solen.slotmachine.MessageProcessor;
import top.fengpingtech.solen.slotmachine.PacketPreprocessor;
import top.fengpingtech.solen.slotmachine.TracingLogHandler;

import javax.annotation.PostConstruct;

@Component
public class NettyServer {
    private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);

    private final ServerProperties serverProperties;

    private final ConnectionManager connectionManager;

    private final EventRepository eventRepository;

    public NettyServer(ServerProperties serverProperties, ConnectionManager connectionManager,
                       EventRepository eventRepository) {
        this.serverProperties = serverProperties;
        this.connectionManager = connectionManager;
        this.eventRepository = eventRepository;
    }

    @PostConstruct
    public void init() {
        ServerBootstrap bootstrap = new ServerBootstrap();
        MultithreadEventLoopGroup bossGroup;
        MultithreadEventLoopGroup workGroup;
        if (Epoll.isAvailable()) {
            bossGroup = new EpollEventLoopGroup(serverProperties.getIoThreads(),
                    new DefaultThreadFactory("netty-boss", true));

            workGroup = new EpollEventLoopGroup(serverProperties.getWorkerThreads(),
                    new DefaultThreadFactory("netty-worker", true));
            bootstrap.channel(EpollServerSocketChannel.class);
            bootstrap.option(EpollChannelOption.EPOLL_MODE, EpollMode.EDGE_TRIGGERED);
            bootstrap.childOption(EpollChannelOption.EPOLL_MODE, EpollMode.EDGE_TRIGGERED);
            logger.info("Use epoll edge trigger mode.");
        } else if (KQueue.isAvailable()) {
            bossGroup = new KQueueEventLoopGroup(serverProperties.getIoThreads(),
                    new DefaultThreadFactory("netty-boss", true));
            workGroup = new KQueueEventLoopGroup(serverProperties.getWorkerThreads(),
                    new DefaultThreadFactory("netty-worker", true));
            bootstrap.channel(KQueueServerSocketChannel.class);

        } else {
            bossGroup = new NioEventLoopGroup(serverProperties.getIoThreads(),
                    new DefaultThreadFactory("netty-boss", true));
            workGroup = new NioEventLoopGroup(serverProperties.getWorkerThreads(),
                    new DefaultThreadFactory("netty-worker", true));

            // ((NioEventLoopGroup) bossGroup).setIoRatio(100);
            // ((NioEventLoopGroup) workGroup).setIoRatio(100);
            bootstrap.channel(NioServerSocketChannel.class);
            // logger.info("Use normal mode.");
        }

        // config
//        bootstrap.option(ChannelOption.SO_BACKLOG, serverProperties.getBacklog());
//        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, serverProperties.isKeepAlive());
//        bootstrap.childOption(ChannelOption.TCP_NODELAY, serverProperties.isTcpNoDelay());
        bootstrap.childOption(ChannelOption.SO_REUSEADDR, true);
        bootstrap.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
//        bootstrap.childOption(ChannelOption.SO_LINGER, serverProperties.getSoLinger());
//        bootstrap.childOption(ChannelOption.SO_SNDBUF, serverProperties.getSendBufferSize());
//        bootstrap.childOption(ChannelOption.SO_RCVBUF, serverProperties.getReceiveBufferSize());

        bootstrap.group(bossGroup, workGroup).childHandler(
                new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                ch.pipeline()
                        .addLast(new TracingLogHandler())
                        .addLast(new LoggingHandler())
                        .addLast(new MessageDebugger())
                        .addLast(new ReadTimeoutHandler(600))
                        .addLast(new PacketPreprocessor())
                        .addLast(new ReadTimeoutHandler(600))
                        .addLast(new ConnectionKeeperHandler())
                        .addLast(new MessageEncoder())
                        .addLast(new MessageDecoder())
                        .addLast(new ConnectionAttributeHolder(connectionManager))
                        .addLast(new MessageProcessor(connectionManager))
                        .addLast(new EventProcessor(connectionManager, eventRepository));
            }
        });

        try {
             bootstrap.bind(serverProperties.getPort()).sync();
        } catch (InterruptedException e) {
            logger.error("netty Server failed to start, {}", e.getMessage());
        }

        if (logger.isInfoEnabled()) {
            logger.info("netty server started on port = {} success", serverProperties.getPort());
        }
    }
}
