package com.neusoft.solen.slotmachine;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

public class SlotMachineInBoundHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private static final Logger logger = LoggerFactory.getLogger(SlotMachineInBoundHandler.class);

    private final ConnectionManager connectionManager;

    public SlotMachineInBoundHandler(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        SoltMachineMessage message = decode(msg);

        if (message.getType() == 0) {
            byte[] data = message.getData();
            Assert.isTrue(data.length == 19,
                    "register packet length expect to 19, but is " + data.length);

            String id = new String(data, 0, 11);
            String location = new String(data, 11, 8);

            connectionManager.getStore().put(id, ConnectionManager.Connection.builder()
                    .channel(ctx.channel())
                    .location(location)
                    .build());

            synchronized (ctx.channel()) {
                ctx.channel().writeAndFlush(encode(SoltMachineMessage.builder()
                        .header(message.getHeader())
                        .index(message.getIndex() + 1)
                        .idCode(message.getIdCode())
                        .deviceId(message.getDeviceId())
                        .data(new byte[0])
                        .build())).get();
            }
        }

        System.out.println("message received: " + message);
    }

    private SoltMachineMessage decode(ByteBuf msg) {
        short header = msg.readShortLE();
        short length = msg.readShortLE();

        byte index = msg.readByte();

        long idCode = msg.readLongLE();

        byte[] buffer = new byte[11];
        msg.readBytes(buffer);

        String deviceId = new String(buffer);

        byte[] data = new byte[length - 26];
        msg.readBytes(data);

        byte checksum = msg.readByte();

        msg.resetReaderIndex();

        byte calc = 0;

        for (int i = 0; i < length - 1; i ++) {
            calc ^= msg.readByte();
        }

        if (calc != checksum) {
            logger.warn("checksum failed, left is {}, right is {}", calc, checksum);
        }

        return SoltMachineMessage.builder()
                .header(header)
                .index(index)
                .idCode(idCode)
                .deviceId(deviceId)
                .data(data)
                .build();
    }

    private ByteBuf encode(SoltMachineMessage message) {
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeShortLE(message.getHeader());
        byteBuf.writeShortLE(message.getData().length + 26);
        byteBuf.writeByte(message.getIndex());
        byteBuf.writeLongLE(message.getIdCode());
        byteBuf.writeBytes(message.getDeviceId().getBytes());
        byteBuf.writeBytes(message.getDeviceId().getBytes());
        byteBuf.writeByte(message.getType());
        byteBuf.writeBytes(message.getData());

        byte checksum = 0;
        for (int i = 0; i < message.getData().length + 26 - 1; i ++) {
            checksum ^= byteBuf.readByte();
        }
        byteBuf.resetReaderIndex();
        byteBuf.writeByte(checksum);

        return byteBuf;
    }
}
