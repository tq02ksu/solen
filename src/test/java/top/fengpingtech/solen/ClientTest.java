package top.fengpingtech.solen;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
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
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import top.fengpingtech.solen.slotmachine.MessageEncoder;
import top.fengpingtech.solen.slotmachine.SoltMachineMessage;

public class ClientTest extends SolenApplicationTests {
    @Autowired
    MockMvc mockMvc;

    @Test
    public void testClient() throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    //.option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();
                            //p.addLast(new LoggingHandler(LogLevel.INFO));
//                            p.addLast(new MessageDecoder());
//                            p.addLast(new MessageEncoder());
                            //p.addLast("encoder", new MessageEncoder());
                            //p.addLast("decoder", new MessageDecoder());
                            //p.addFirst(new LineBasedFrameDecoder(65535));
                            p.addLast(new ClientRegisterHandler());
                        }
                    });

            // Start the client.
            ChannelFuture f = b.connect("127.0.0.1", 7889).sync();
            System.out.println("EchoClient.main ServerBootstrap配置启动完成");

            // Wait until the connection is closed.
            f.channel().close().sync();
            System.out.println("EchoClient.end");
        } finally {
            // Shut down the event loop to terminate all threads.
            group.shutdownGracefully();
        }

        String json = mockMvc.perform(MockMvcRequestBuilders.get("/listAll"))
              .andReturn().getResponse().getContentAsString();
        System.out.println(json);
    }

    @Test
    public void testFragmentedClient() throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    //.option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();
                            //p.addLast(new LoggingHandler(LogLevel.INFO));
//                            p.addLast(new MessageDecoder());
//                            p.addLast(new MessageEncoder());
                            //p.addLast("encoder", new MessageEncoder());
                            //p.addLast("decoder", new MessageDecoder());
                            //p.addFirst(new LineBasedFrameDecoder(65535));
                            p.addLast(new ClientFragmentedRegisterHandler());
                        }
                    });

            // Start the client.
            ChannelFuture f = b.connect("127.0.0.1", 7889).sync();
            System.out.println("EchoClient.main ServerBootstrap配置启动完成");

            // Wait until the connection is closed.
            f.channel().close().sync();
            System.out.println("EchoClient.end");
        } finally {
            // Shut down the event loop to terminate all threads.
            group.shutdownGracefully();
        }

        String json = mockMvc.perform(MockMvcRequestBuilders.get("/listAll"))
                .andReturn().getResponse().getContentAsString();
        System.out.println(json);
    }

    private static class ClientRegisterHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            Channel channel = ctx.channel();
            ByteBuf buf = ctx.alloc().buffer();
            MessageEncoder encoder = new MessageEncoder();
            encoder.encode(SoltMachineMessage.builder()
                    .header(13175)
                    .index(0)
                    .idCode(12345L)
                    .deviceId("10619030001")
                    .cmd((short) 0)
                    .data(new byte[] {10, 65, 0, 0, 76, 71, 0, 0})
                    .build(), buf);
            channel.writeAndFlush(buf);
        }
    }

    private static class ClientFragmentedRegisterHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            Channel channel = ctx.channel();
            ByteBuf buf = ctx.alloc().buffer();
            MessageEncoder encoder = new MessageEncoder();
            encoder.encode(SoltMachineMessage.builder()
                    .header(13175)
                    .index(0)
                    .idCode(12345L)
                    .deviceId("10619030001")
                    .cmd((short) 0)
                    .data(new byte[] {10, 65, 0, 0, 76, 71, 0, 0})
                    .build(), buf);

            ByteBuf slice = buf.slice(0, 10);
            buf.retain();
            channel.writeAndFlush(slice);

            Thread.sleep(1000);

            channel.writeAndFlush(buf.slice(10, buf.readableBytes() - 10));
        }
    }
}