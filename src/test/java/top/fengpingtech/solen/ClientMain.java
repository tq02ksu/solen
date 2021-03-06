package top.fengpingtech.solen;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import top.fengpingtech.solen.protocol.MessageDebugger;
import top.fengpingtech.solen.protocol.MessageDecoder;
import top.fengpingtech.solen.protocol.MessageEncoder;

public class ClientMain {
    public static void main(String[] args) throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    //.option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ChannelPipeline p = ch.pipeline();
                            p.addLast("logging", new LoggingHandler());
                            p.addLast("debugger", new MessageDebugger());
                            p.addLast(new MessageEncoder());
                            p.addLast(new MessageDecoder());
                            p.addLast(new ClientTest.ClientFragmentedRegisterHandler("55520041112"));
                        }
                    });

            // Start the client.
            ChannelFuture f = b.connect("127.0.0.1", 7889).sync();

            Thread.sleep(100000000);
            // Wait until the connection is closed.
//            f.channel().close().sync();
        } finally {
            // Shut down the event loop to terminate all threads.
            group.shutdownGracefully();
        }
    }
}
