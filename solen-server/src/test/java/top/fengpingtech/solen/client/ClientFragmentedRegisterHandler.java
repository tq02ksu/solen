package top.fengpingtech.solen.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.EventExecutorGroup;
import top.fengpingtech.solen.server.model.SoltMachineMessage;
import top.fengpingtech.solen.server.protocol.MessageEncoder;

public class ClientFragmentedRegisterHandler extends ChannelInboundHandlerAdapter {
    private final String deviceId;

    public ClientFragmentedRegisterHandler(String deviceId) {
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
                .data(new byte[] {10, 65, 0, 0, 76, 71, 0, 0})
                .build(), buf);

        ByteBuf slice = buf.slice(0, 10);
        buf.retain();
        channel.writeAndFlush(slice);

        Thread.sleep(1000);

        channel.writeAndFlush(buf.slice(10, buf.readableBytes() - 10));

        Thread.sleep(1000);

        buf = ctx.alloc().buffer();

        SoltMachineMessage msg = SoltMachineMessage.builder()
                .header(13175)
                .index(104)
                .idCode(8389750321502775627L)
                .deviceId(deviceId)
                .cmd((short) 1)
                .data(new byte[] {15, 16, 0, 120, 0, 39, 1, 54, 1, -1, 46, 0, 0, 67, 0, 0, 0})
                .build();
        encoder.encode(msg, buf);
        channel.pipeline().writeAndFlush(buf);

        // test cmd=128
        buf = ctx.alloc().buffer();
        msg = SoltMachineMessage.builder()
                .header(13175)
                .index(104)
                .idCode(8389750321502775627L)
                .deviceId(deviceId)
                .cmd((short) 128)
                .data("12345".getBytes())
                .build();
        encoder.encode(msg, buf);
        channel.pipeline().writeAndFlush(buf);

        Thread.sleep(15000);
        buf = ctx.alloc().buffer();
        msg = SoltMachineMessage.builder()
                .header(13175)
                .index(104)
                .idCode(8389750321502775627L)
                .deviceId("10619030001")
                .cmd((short) 128)
                .data("12345".getBytes())
                .build();
        encoder.encode(msg, buf);
        channel.pipeline().writeAndFlush(buf);

        Thread.sleep(5000);
        buf = ctx.alloc().buffer();
        msg = SoltMachineMessage.builder()
                .header(13175)
                .index(104)
                .idCode(8389750321502775627L)
                .deviceId(deviceId)
                .cmd((short) 128)
                .data("abcdf".getBytes())
                .build();
        encoder.encode(msg, buf);
        channel.pipeline().writeAndFlush(buf);
        Thread.sleep(2000);
        buf = ctx.alloc().buffer();
        msg = SoltMachineMessage.builder()
                .header(13175)
                .index(104)
                .idCode(8389750321502775627L)
                .deviceId(deviceId)
                .cmd((short) 128)
                .data(new byte[] {(byte)0x67, (byte) 0x68, (byte)0x69,(byte) 0})
                .build();
        encoder.encode(msg, buf);
        channel.pipeline().writeAndFlush(buf);
    }
}
