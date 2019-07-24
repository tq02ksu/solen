package top.tengpingtech.solen.server;

import top.tengpingtech.solen.slotmachine.ConnectionManager;
import top.tengpingtech.solen.slotmachine.SlotMachineInBoundHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.MultithreadEventLoopGroup;
import io.netty.channel.epoll.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class NettyServer {
    private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);

    private final ServerProperties serverProperties;

    private final ConnectionManager connectionManager;

    private ChannelFuture channelFuture;

    public NettyServer(ServerProperties serverProperties, ConnectionManager connectionManager) {
        this.serverProperties = serverProperties;
        this.connectionManager = connectionManager;
    }

    @PostConstruct
    public void init() {
        ServerBootstrap bootstrap = new ServerBootstrap();
        MultithreadEventLoopGroup bossGroup;
        MultithreadEventLoopGroup workGroup;
        if (Epoll.isAvailable()) {
            bossGroup = new EpollEventLoopGroup(serverProperties.getIoThreads(),
                    new DefaultThreadFactory("netty-boss", false));

            workGroup = new EpollEventLoopGroup(serverProperties.getWorkerThreads(),
                    new DefaultThreadFactory("netty-worker", false));
            // ((EpollEventLoopGroup) bossGroup).setIoRatio(100);
            // ((EpollEventLoopGroup) workGroup).setIoRatio(100);
            bootstrap.channel(EpollServerSocketChannel.class);
            bootstrap.option(EpollChannelOption.EPOLL_MODE, EpollMode.EDGE_TRIGGERED);
            bootstrap.childOption(EpollChannelOption.EPOLL_MODE, EpollMode.EDGE_TRIGGERED);
            logger.info("Use epoll edge trigger mode.");
        } else {
            bossGroup = new NioEventLoopGroup(serverProperties.getIoThreads(),
                    new DefaultThreadFactory("RpcServerBossGroup", false));
            workGroup = new NioEventLoopGroup(serverProperties.getWorkerThreads(),
                    new DefaultThreadFactory("RpcServerWorkerGroup", false));

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

        bootstrap.group(bossGroup, workGroup).childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline()
                        .addLast(new SlotMachineInBoundHandler(connectionManager))
                        .addLast(new IdleStateHandler(0,0,300));
//                        .addLast("ideStateAwareHandler", new IdleStateHandler(
//                                serverProperties.getReaderIdleTime(),
//                                serverProperties.getWriterIdleTime(),
//                                serverProperties.getKeepAliveTime()))
//                        .addLast("ide", new RpcServerChannelIdleHandler())
//                        .addLast(new RpcServerHandler());
            }
        });

        try {

             channelFuture = bootstrap.bind(serverProperties.getPort()).sync();
        } catch (InterruptedException e) {
            logger.error("netty Server failed to start, {}", e.getMessage());
        }

        if (logger.isInfoEnabled()) {
            logger.info("netty server started on port = {} success", serverProperties.getPort());
        }
    }
}
