package top.fengpingtech.solen;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
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
import org.springframework.core.io.ClassPathResource;
import top.fengpingtech.solen.protocol.MessageDebugger;
import top.fengpingtech.solen.protocol.MessageDecoder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 第一个注册包异常，后续是成功的
 * 2020-05-22 03:28:18.054  INFO [solen,,0x459395a4,] 19745 --- [etty-worker-3-1] t.f.solen.slotmachine.MessageDebugger    : [id: 0x459395a4, L:/172.16.202.26:7889 - R:/218.204.252.160:43712]: receiving bytes 0xaf 6c fb 8b cd f7 fe 78 53 e4 ab e1 ee 77 33 22 00 00 4b 69 64 73 77 61 6e 74 31 30 36 32 30 30 34 30
 * 2020-05-22 03:31:48.458  INFO [solen,,0x459395a4,] 19745 --- [etty-worker-3-1] t.f.solen.slotmachine.MessageDebugger    : [id: 0x459395a4, L:/172.16.202.26:7889 - R:/218.204.252.160:43712]: receiving bytes 0x77 33 22 00 01 4b 69 64 73 77 61 6e 74 31 30 36 32 30 30 34 30 30 30 32 00 0a 41 00 00 4c 47 00 00 2d
 * 2020-05-22 03:34:13.824  INFO [solen,,0x459395a4,] 19745 --- [etty-worker-3-1] t.f.solen.slotmachine.MessageDebugger    : [id: 0x459395a4, L:/172.16.202.26:7889 - R:/218.204.252.160:43712]: receiving bytes 0x77 33 22 00 02 4b 69 64 73 77 61 6e 74 31 30 36 32 30 30 34 30 30 30 32 00 0a 41 00 00 4c 47 00 00 2e
 * 2020-05-22 03:36:13.177  INFO [solen,,0x459395a4,] 19745 --- [etty-worker-3-1] t.f.solen.slotmachine.MessageDebugger    : [id: 0x459395a4, L:/172.16.202.26:7889 - R:/218.204.252.160:43712]: receiving bytes 0x77 33 22 00 03 4b 69 64 73 77 61 6e 74 31 30 36 32 30 30 34 30 30 30 32 00 0a 41 00 00 4c 47 00 00 2f
 * 2020-05-22 03:39:01.919  INFO [solen,,0x459395a4,] 19745 --- [etty-worker-3-1] t.f.solen.slotmachine.MessageDebugger    : [id: 0x459395a4, L:/172.16.202.26:7889 - R:/218.204.252.160:43712]: receiving bytes 0x77 33 22 00 04 4b 69 64 73 77 61 6e 74 31 30 36 32 30 30 34 30 30 30 32 00 0a 41 00 00 4c 47 00 00 28
 * 2020-05-22 03:41:01.460  INFO [solen,,0x459395a4,] 19745 --- [etty-worker-3-1] t.f.solen.slotmachine.MessageDebugger    : [id: 0x459395a4, L:/172.16.202.26:7889 - R:/218.204.252.160:43712]: receiving bytes 0x77 33 22 00 05 4b 69 64 73 77 61 6e 74 31 30 36 32 30 30 34 30 30 30 32 00 0a 41 00 00 4c 47 00 00 29
 */
public class ClientInvalidRegisterTest {
    public static void main(String[] args) throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();

        Bootstrap b = new Bootstrap();
        b.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast("logging", new LoggingHandler());
                        p.addLast("debugger", new MessageDebugger());
                        p.addLast(new MessageDecoder());
                        p.addLast(new DeviceRegisterHandler());
                    }
                });
        ChannelFuture f = b.connect("127.0.0.1", 7889).sync();

        Thread.sleep(1000000);
    }

    private static class DeviceRegisterHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            ClassPathResource resource = new ClassPathResource("/invalid-register.txt");
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                    if (line.matches("(//|#).*")) {
                        continue;
                    }

                    List<Integer> arr = Arrays.stream(line.replaceAll("^.*0x", "").split(" "))
                            .map(s -> Integer.parseInt(s, 16)).collect(Collectors.toList());
                    ByteBuf buf = ctx.alloc().buffer();
                    arr.forEach(buf::writeByte);
                    ctx.writeAndFlush(buf);
                    Thread.sleep(1000);
                }
            }
        }
    }
}
