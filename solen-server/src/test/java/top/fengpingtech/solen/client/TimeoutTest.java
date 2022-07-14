package top.fengpingtech.solen.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import top.fengpingtech.solen.server.model.SoltMachineMessage;
import top.fengpingtech.solen.server.protocol.MessageDebugger;
import top.fengpingtech.solen.server.protocol.MessageDecoder;
import top.fengpingtech.solen.server.protocol.MessageEncoder;

public class TimeoutTest {
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
                            p.addLast(new ClientRegisterHandler("42320041627"));
                        }
                    });

            // Start the client.
            ChannelFuture f = b.connect("fengping-tech.top", 31978).sync();

            Thread.sleep(100000000);
            // Wait until the connection is closed.
//            f.channel().close().sync();
        } finally {
            // Shut down the event loop to terminate all threads.
            group.shutdownGracefully();
        }
    }

    private static class ClientRegisterHandler extends ChannelInboundHandlerAdapter {
        private final String deviceId;
        public ClientRegisterHandler(String deviceId) {
            this.deviceId = deviceId;
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            Channel channel = ctx.channel();
            ByteBuf buf = ctx.alloc().buffer();
            MessageEncoder encoder = new MessageEncoder();
            encoder.encode(SoltMachineMessage.builder()
                    .header(13175)
                    .index(0)
                    .idCode(12345L)
                    .deviceId(deviceId)
                    .cmd((short) 0)
                    .data(new byte[]{10, 65, 0, 0, 76, 71, 0, 0})
                    .build(), buf);

            ByteBuf slice = buf.slice(0, 10);
            buf.retain();
            channel.writeAndFlush(slice);

            Thread.sleep(1000);

            channel.writeAndFlush(buf.slice(10, buf.readableBytes() - 10));
        }
    }
}
