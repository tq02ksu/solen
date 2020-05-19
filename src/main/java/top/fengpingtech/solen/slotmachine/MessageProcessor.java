package top.fengpingtech.solen.slotmachine;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import top.fengpingtech.solen.model.Connection;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * process terminal message:
 *  cmd=0 register message
 *  cmd=1 heart beat
 *  cmd=2 reply
 *  cmd=128 text report message terminated by invisible characters like '\0', '\n' or timeout with 3 seconds
 */
public class MessageProcessor extends MessageToMessageDecoder<SoltMachineMessage> {
    private static final Logger logger = LoggerFactory.getLogger(MessageProcessor.class);

    private static final int TEXT_REPORT_TIMEOUT_SECONDS = 3;

    private static final List<Byte> TEXT_TERMINATORS = Collections.unmodifiableList(
            Arrays.asList((byte)0x00, (byte)0x0a));

    private static final String ATTRIBUTE_KEY_MESSAGE_BUFFER = "MESSAGE_BUFFER";

    private final ConnectionManager connectionManager;

    public MessageProcessor(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    private void processMessage(ChannelHandlerContext ctx, SoltMachineMessage msg, List<Object> out) {
        if (msg.getCmd() == 0) {
            byte[] data = msg.getData();
            Assert.isTrue(data.length == 8,
                    "register packet length expect to 8, but is " + data.length);

            int lac = (data[0] & 0xFF) + ((data[1] & 0xFF) << 8);
            int ci = (data[4] & 0xFF) + ((data[5] & 0xFF) << 8);

            if (connectionManager.getStore().containsKey(msg.getDeviceId())
                    && connectionManager.getStore().get(msg.getDeviceId()).getChannel().isActive()) {
                Connection conn = connectionManager.getStore().get(msg.getDeviceId());
                logger.warn("detected active device: {}", conn);
                connectionManager.close(conn);
            }

            Connection connection = Optional
                    .ofNullable(connectionManager.getStore().get(msg.getDeviceId()))
                    .orElseGet(Connection::new);

            connection.setChannel(ctx.channel());
            connection.setDeviceId(msg.getDeviceId());
            connection.setLac(lac);
            connection.setCi(ci);
            connection.setHeader(msg.getHeader());
            connection.setIdCode(msg.getIdCode());
            connectionManager.getStore().putIfAbsent(msg.getDeviceId(), connection);
        } else if (msg.getCmd() == 1) {
            int outputStat = (msg.getData()[0] & 0x02 ) >> 1;
            int inputStat = msg.getData()[0] & 0x01;
            Connection conn = connectionManager.getStore().get(msg.getDeviceId());
            if (conn != null) {
                conn.setInputStat(inputStat);
                conn.setOutputStat(outputStat);
                conn.setLastHeartBeatTime(new Date());
                conn.setRssi((msg.getData()[1] & 0xFF) + ((msg.getData()[2] & 0xFF) << 8));
                conn.setDebugData1((msg.getData()[3] & 0xFF) + ((msg.getData()[4] & 0xFF) << 8));
                conn.setDebugData2((msg.getData()[4] & 0xFF) + ((msg.getData()[6] & 0xFF) << 8));
                conn.setDebugData3((msg.getData()[7] & 0xFF) + ((msg.getData()[8] & 0xFF) << 8));
                conn.setDebugData4((msg.getData()[9] & 0xFF) + ((msg.getData()[10] & 0xFF) << 8)
                        + ((msg.getData()[11] & 0xFF) << 16) + ((msg.getData()[12] & 0xFF) << 24));
                conn.setDebugData5((msg.getData()[13] & 0xFF) + ((msg.getData()[14] & 0xFF) << 8)
                        + ((msg.getData()[15] & 0xFF) << 16) + ((msg.getData()[16] & 0xFF) << 24));

                for (CountDownLatch sync : conn.getOutputStatSyncs()) {
                    sync.countDown();
                }
            }
        } else if (msg.getCmd() == 128) {
            Connection conn = connectionManager.getStore().get(msg.getDeviceId());
            if (conn == null) {
                logger.warn("skipped message : {}, device not registered", msg);
            } else {
                AttributeKey<byte[]> key = AttributeKey.valueOf(ATTRIBUTE_KEY_MESSAGE_BUFFER);
                Attribute<byte[]> val = ctx.channel().attr(key);
                SplitResult result;
                synchronized (val) {
                    result = split(ctx, msg.getData(), val.getAndSet(null));
                    val.getAndSet(result.buffer);
                }

                if (!result.segments.isEmpty()) {
                    result.segments.forEach(s -> processMessage(ctx, conn, s));
                }

                // schedule process
                if (val.get() != null) {
                    ctx.executor().schedule(() -> {
                        for (byte[] message = val.get(); message != null; message = val.get()) {
                            if (val.compareAndSet(message, null)) {
                                processMessage(ctx, conn, message);
                            }
                        }

                    }, TEXT_REPORT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                }
            }
        }
    }

    private SplitResult split(ChannelHandlerContext ctx, byte[] data, byte[] buffer) {
        SplitResult result = new SplitResult();
        ByteBuf text = ctx.alloc().buffer();
        if (buffer != null) {
            text.writeBytes(buffer);
        }
        text.writeBytes(data);

        ByteBuf buf = ctx.alloc().buffer();

        while (text.isReadable()) {
            byte b = text.readByte();
            if (TEXT_TERMINATORS.contains(b) && buf.isReadable()) {
                result.segments.add(array(buf));
                buf.clear();
            } else if (!TEXT_TERMINATORS.contains(b)) {
                buf.writeByte(b);
            }
        }

        if (buf.isReadable()) {
            result.buffer = array(buf);
        }

        buf.release();
        text.release();
        return result;
    }

    private byte[] array(ByteBuf buf) {
        byte[] req = new byte[buf.readableBytes()];
        buf.readBytes(req);
        return req;
    }

    private static class SplitResult {
        List<byte[]> segments = new ArrayList<>();
        byte[] buffer;
    }


    private void processMessage(ChannelHandlerContext ctx, Connection conn, byte[] data) {
        ByteBuf buf = ctx.channel().alloc().buffer();
        synchronized (conn) {
            String content = new String(data, StandardCharsets.UTF_8);
            Date now = new Date();
            conn.setLastHeartBeatTime(now);
            conn.getReports().add(0, new Connection.Report(now, content));
            if (conn.getReports().size() > 10) {
                conn.getReports().remove(10);
            }
        }
        buf.release();
    }

    private void sendReply(SoltMachineMessage message, List<Object> out) {
        if (message.getCmd() == 2) {
            // skip for reply message
            return;
        }

        out.add(SoltMachineMessage.builder()
                .header(message.getHeader())
                .index(message.getIndex())
                .idCode(message.getIdCode())
                .cmd((short) 2)
                .deviceId(message.getDeviceId())
                .data(new byte[]{(byte) message.getCmd()})  // arg==0
                .build());
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, SoltMachineMessage msg, List<Object> out) throws Exception {
        sendReply(msg, out);

        processMessage(ctx, msg, out);

        for (Object o : out) {
            ctx.pipeline().writeAndFlush(o);
        }
    }
}
