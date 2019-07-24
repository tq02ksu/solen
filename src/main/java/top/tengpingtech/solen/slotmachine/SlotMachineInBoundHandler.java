package top.tengpingtech.solen.slotmachine;

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
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

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
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
        SoltMachineMessage message = decode(msg);
        sendReply(ctx.channel(), message);

        if (message.getCmd() == 0) {
            byte[] data = message.getData();
            Assert.isTrue(data.length == 8,
                    "register packet length expect to 8, but is " + data.length);

            ByteBuf location = Unpooled.wrappedBuffer(data);
            int lac = (location.readByte() & 0xFF) + ((location.readByte() & 0xFF) << 8);
            location.readBytes(2);

            int ci = (location.readByte() & 0xFF) + ((location.readByte() & 0xFF) << 8);

            if (connectionManager.getStore().containsKey(message.getDeviceId())
                    && connectionManager.getStore().get(message.getDeviceId()).getChannel().isActive()) {
                logger.warn("detected active device: {}", connectionManager.getStore().get(message.getDeviceId()));
            }

            ConnectionManager.Connection connection = Optional
                    .ofNullable(connectionManager.getStore().get(message.getDeviceId()))
                    .orElseGet(ConnectionManager.Connection::new);

            connection.setChannel(ctx.channel());
            connection.setDeviceId(message.getDeviceId());
            connection.setLac(lac);
            connection.setCi(ci);
            connection.setHeader(message.getHeader());
            connection.setIdCode(message.getIdCode());
            connectionManager.getStore().putIfAbsent(message.getDeviceId(), connection);
        } else if (message.getCmd() == 1) {
            int outputStat = (message.getData()[0] & 0x02 ) >> 1;
            int inputStat = message.getData()[0] & 0x01;
            ConnectionManager.Connection conn = connectionManager.getStore().get(message.getDeviceId());
            conn.setInputStat(inputStat);
            conn.setOutputStat(outputStat);
            conn.setLastHeartBeatTime(new Date());

            for (CountDownLatch sync : conn.getOutputStatSyncs()) {
                sync.countDown();
            }
        } else if (message.getCmd() == 128) {
            String content = new String(message.getData());
            Date time = new Date();
            List<ConnectionManager.Report> reports = connectionManager.getStore().get(message.getDeviceId()).getReports();
            reports.add(0, new ConnectionManager.Report(time, content));
            if (reports.size() > 10) {
                reports.remove(10 );
            }
        }

        logger.info("message received: " + message);
    }

    private static void sendReply(Channel channel, SoltMachineMessage message) {
        if (message.getCmd() == 2) {
            // skip for reply message
            return;
        }
        synchronized (channel) {
            ByteBuf reply = encode(SoltMachineMessage.builder()
                    .header(message.getHeader())
                    .index(message.getIndex())
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

        short cmd = (short) (msg.readByte() & 0xFF); // cmd 是大端

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

    private static byte reverse(byte b) {
       return (byte)  Integer.reverse(((int) b) <<24);
    }

    public static void logBytebuf(ByteBuf byteBuf, String comment) {
        if (logger.isDebugEnabled()) {
            StringBuilder tmp = new StringBuilder("0x");
            while (byteBuf.isReadable()) {
                tmp.append(String.format("%02x ", byteBuf.readByte() & 0xFF));
            }
            logger.debug(comment + "(le): {}", tmp.toString());
            byteBuf.resetReaderIndex();

            tmp = new StringBuilder("0x");

            while (byteBuf.isReadable()) {
                tmp.append(String.format("%02x ", reverse(byteBuf.readByte()) & 0xFF));
            }
            logger.trace(comment + "(be): {}", tmp.toString());
            byteBuf.resetReaderIndex();
        }
    }
}
