package top.fengpingtech.solen;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import top.fengpingtech.solen.server.model.SoltMachineMessage;
import top.fengpingtech.solen.server.protocol.MessageDebugger;
import top.fengpingtech.solen.server.protocol.MessageDecoder;
import top.fengpingtech.solen.server.protocol.MessageEncoder;

import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientPerformanceMain {
    public static void main(String[] args) throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            for (int i = 0; i < 2; i ++) {
                long deviceId = 55520000000L + new Random().nextInt(10000000);
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
                                p.addLast(new PerformanceHandler(String.valueOf(deviceId)));
                            }
                        });

                // Start the client.
                ChannelFuture f = b.connect("localhost", 7889).sync();
            }
            Thread.sleep(100000000);
            // Wait until the connection is closed.
//            f.channel().close().sync();
        } finally {
            // Shut down the event loop to terminate all threads.
            group.shutdownGracefully();
        }
    }

    static class PerformanceHandler extends ChannelInboundHandlerAdapter {
        private final String deviceId;

        private final AtomicInteger index = new AtomicInteger(0);

        PerformanceHandler(String deviceId) {
            this.deviceId = deviceId;
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            Channel channel = ctx.channel();
            ByteBuf buf = ctx.alloc().buffer();
            MessageEncoder encoder = new MessageEncoder();
            encoder.encode(SoltMachineMessage.builder()
                    .header(13175)
                    .index(index.getAndIncrement() % 256)
                    .idCode(12345L)
                    .deviceId(deviceId)
                    .cmd((short) 0)
                    .data(new byte[] {10, 65, 0, 0, 76, 71, 0, 0})
                    .build(), buf);
            ByteBuf slice = buf.slice(0, 10);
            buf.retain();
            channel.writeAndFlush(slice);
            Thread.sleep(1000);

            channel.writeAndFlush(buf.slice(10, buf.readableBytes() - 10));

            sendHeartBeat(ctx);

            sendDeviceInfo(ctx);
        }

        private void sendDeviceInfo(ChannelHandlerContext ctx) {
            ByteBuf buf = ctx.alloc().buffer();
            MessageEncoder encoder = new MessageEncoder();
            String message = "{\"body\":{\"devicelist\":[{\"DEVICE_ID\":\"1015\",\"STATUS_CODE\":\"0\",\"STATUS_REASON_CODE\":\"\"},{\"DEVICE_ID\":\"1016\",\"STATUS_CODE\":\"0\",\"STATUS_REASON_CODE\":\"\"},{\"DEVICE_ID\":\"1007\",\"STATUS_CODE\":\"0\",\"STATUS_REASON_CODE\":\"\"}],\"CPU_USE_INFO\":\"0%\",\"MEM_AVAI_INFO\":\"2.1G\",\"STORAGE_AVAI_INFO\":\"458G\",\"STATUS_CODE\":\"00\"},\"IP_ADDRESS\":\"192.168.133.88\",\"MAC_ADDRESS\":\"C4-09-38-97-B2-F7\",\"SESSION\":\"a5ffed75-28ec-4b61-acd3-6e7889bc79dc\",\"reserve\":null,\"DEVICE_ID\":null,\"info_type\":\"2\"}\n";

            SoltMachineMessage msg = SoltMachineMessage.builder()
                    .header(13175)
                    .index(index.getAndIncrement())
                    .idCode(8389750321502775627L)
                    .deviceId(deviceId)
                    .cmd((short) 128)
                    .data(message.getBytes(StandardCharsets.UTF_8))
                    .build();
            encoder.encode(msg, buf);
            ctx.channel().pipeline().writeAndFlush(buf);

            ctx.executor().schedule(() -> sendDeviceInfo(ctx), 10, TimeUnit.SECONDS);
        }

        private void sendHeartBeat(ChannelHandlerContext ctx) {
            ByteBuf buf = ctx.alloc().buffer();
            MessageEncoder encoder = new MessageEncoder();
            SoltMachineMessage msg = SoltMachineMessage.builder()
                    .header(13175)
                    .index(index.getAndIncrement())
                    .idCode(8389750321502775627L)
                    .deviceId(deviceId)
                    .cmd((short) 1)
                    .data(new byte[] {15, 16, 0, 120, 0, 39, 1, 54, 1, -1, 46, 0, 0, 67, 0, 0, 0})
                    .build();
            encoder.encode(msg, buf);
            ctx.channel().pipeline().writeAndFlush(buf);

            ctx.executor().schedule(() -> sendHeartBeat(ctx), 10, TimeUnit.SECONDS);
        }
    }
}

