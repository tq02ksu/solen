package com.neusoft.solen.slotmachine;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

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
        sendReply(ctx.channel(), message);

        if (message.getCmd() == 0) {
            byte[] data = message.getData();
            Assert.isTrue(data.length == 8,
                    "register packet length expect to 8, but is " + data.length);

            ByteBuf location = Unpooled.wrappedBuffer(data);
            int lac = ((int) location.readByte()) << 8 + location.readByte();
            location.readBytes(2);

            int ci = ((int) location.readByte()) << 8 + location.readByte();

            connectionManager.getStore().put(message.getDeviceId(), ConnectionManager.Connection.builder()
                    .channel(ctx.channel())
                    .deviceId(message.getDeviceId())
                    .lac(lac)
                    .ci(ci)
                    .header(message.getHeader())
                    .index(message.getIndex())
                    .idCode(message.getIdCode())
                    .build());
        } else if (message.getCmd() == 1) {
            int outputStat = (message.getData()[0] & 0x02 ) >> 1;
            int inputStat = message.getData()[0] & 0x01;
            ConnectionManager.Connection conn = connectionManager.getStore().get(message.getDeviceId());
            conn.setInputStat(inputStat);
            conn.setOutputStat(outputStat);
        } else if (message.getCmd() == 128) {
            String content = new String(message.getData());
            Date time = new Date();
            List<ConnectionManager.Report> reports = connectionManager.getStore().get(message.getDeviceId()).getReports();

            synchronized (ctx.channel()) {
                if (reports.size() >= 10) {
                    reports.remove(10);
                }

                reports.add(0, new ConnectionManager.Report(time, content));
            }
        }

        logger.info("message received: " + message);
    }

    public static void sendReply(Channel channel, SoltMachineMessage message) {
        synchronized (channel) {
            ByteBuf reply = encode(SoltMachineMessage.builder()
                    .header(message.getHeader())
                    .index(message.getIndex() + 1)
                    .idCode(message.getIdCode())
                    .cmd((short) 2)
                    .deviceId(message.getDeviceId())
                    .data(new byte[]{(byte) message.getCmd()})  // arg==0
                    .build());
            logBytebuf(reply, "sending reply ...");
            try {
                channel.writeAndFlush(reply).get();
            } catch (Exception e) {
                throw new RuntimeException("error while send reply", e);
            }
        }
    }

    private static SoltMachineMessage decode(ByteBuf msg) {
        if (logger.isDebugEnabled()) {
            logBytebuf(msg, "decode message");
        }

        short header = msg.readShortLE();
        short length = msg.readShortLE();

        byte index = msg.readByte();

        long idCode = msg.readLongLE();

        byte[] buffer = new byte[11];
        msg.readBytes(buffer);

        String deviceId = new String(buffer);

        short cmd = msg.readByte(); // cmd 是大端

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
                .cmd(cmd)
                .deviceId(deviceId)
                .data(data)
                .build();
    }

    public static ByteBuf encode(SoltMachineMessage message) {
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeShortLE(message.getHeader());
        byteBuf.writeShortLE(message.getData().length + 26);
        byteBuf.writeByte(message.getIndex());
        byteBuf.writeLongLE(message.getIdCode());
        byteBuf.writeBytes(message.getDeviceId().getBytes());
        byteBuf.writeByte((byte)message.getCmd()); // cmd 不是小端
        byteBuf.writeBytes(message.getData());

        byte checksum = 0;
        for (int i = 0; i < message.getData().length + 26 - 1; i ++) {
            checksum ^= byteBuf.readByte();
        }
        byteBuf.resetReaderIndex();
        byteBuf.writeByte(checksum);

        return byteBuf;
    }

    public static byte reverse(byte b) {
       return (byte)  Integer.reverse(((int) b) <<24);
    }

    public static void logBytebuf(ByteBuf byteBuf, String comment) {
        if (logger.isDebugEnabled()) {
            StringBuilder tmp = new StringBuilder("0x");
            while (byteBuf.isReadable()) {
                tmp.append(String.format("%02x ", byteBuf.readByte()));
            }
            logger.debug(comment + "(le): {}", tmp.toString());
            byteBuf.resetReaderIndex();

            tmp = new StringBuilder("0x");

            while (byteBuf.isReadable()) {
                tmp.append(String.format("%02x ", reverse(byteBuf.readByte())));
            }
            logger.trace(comment + "(be): {}", tmp.toString());
            byteBuf.resetReaderIndex();
        }
    }
}
