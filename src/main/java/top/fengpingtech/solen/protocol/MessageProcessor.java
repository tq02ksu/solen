package top.fengpingtech.solen.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import top.fengpingtech.solen.bean.Coordinate;
import top.fengpingtech.solen.bean.CoordinateSystem;
import top.fengpingtech.solen.model.BaseStation;
import top.fengpingtech.solen.model.Connection;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

/**
 * process terminal message:
 * <pre>
 *  cmd=0 register message
 *  cmd=1 heart beat
 *  cmd=2 reply
 *  cmd=128 text report message terminated by invisible characters like '\0', '\n' or timeout with 3 seconds
 * </pre>
 */
public class MessageProcessor extends MessageToMessageDecoder<SoltMachineMessage> {
    private static final Logger logger = LoggerFactory.getLogger(MessageProcessor.class);

    private final ConnectionManager connectionManager;

    public MessageProcessor(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    private void processMessage(ChannelHandlerContext ctx, SoltMachineMessage msg) {
        if (msg.getCmd() == 0) {
            ByteBuf data = ctx.alloc().heapBuffer(msg.getData().length).writeBytes(msg.getData());
            Assert.isTrue(data.readableBytes() == 8,
                    "register packet length expect to 8, but is " + data.readableBytes());

            long lac = data.readUnsignedIntLE();
            long ci = data.readUnsignedIntLE();
            data.release();

            if (connectionManager.getStore().containsKey(msg.getDeviceId())
                    && connectionManager.getStore().get(msg.getDeviceId()).getCtx().channel().isActive()) {
                Connection conn = connectionManager.getStore().get(msg.getDeviceId());
                logger.warn("detected active device: {}", conn);
                connectionManager.close(conn);
            }

            Connection connection = Optional
                    .ofNullable(connectionManager.getStore().get(msg.getDeviceId()))
                    .orElseGet(Connection::new);

            connection.setCtx(ctx);
            connection.setDeviceId(msg.getDeviceId());
            connection.setLac(lac);
            connection.setCi(ci);
            connection.setHeader(msg.getHeader());
            connection.setIdCode(msg.getIdCode());
            connectionManager.getStore().putIfAbsent(msg.getDeviceId(), connection);
            ctx.channel().attr(AttributeKey.valueOf("DeviceId")).set(msg.getDeviceId());
        } else if (msg.getCmd() == 1) {
            ByteBuf data = ctx.alloc().heapBuffer(msg.getData().length).writeBytes(msg.getData());
            byte stat = data.readByte();

            int outputStat = (stat & 0x02) >> 1;
            int inputStat = stat & 0x01;
            Connection conn = connectionManager.getStore().get(msg.getDeviceId());
            if (conn != null) {
                conn.setInputStat(inputStat);
                conn.setOutputStat(outputStat);
                conn.setLastHeartBeatTime(new Date());
                // data[1], data[2]
                conn.setRssi((int) data.readShortLE());
                // data[3], data[4]
                conn.setVoltage((double) data.readShortLE() / 10);
                // data[5], data[6]
                conn.setTemperature((double) data.readShortLE() / 10);
                // data[7], data[8]
                conn.setGravity((int) data.readShortLE());
                conn.setUptime(data.readIntLE());

                for (CountDownLatch sync : conn.getOutputStatSyncs()) {
                    sync.countDown();
                }
            }
            data.release();
        } else if (msg.getCmd() == 128) {
            Connection conn = connectionManager.getStore().get(msg.getDeviceId());
            if (conn == null) {
                logger.warn("skipped message : {}, device not registered", msg);
            } else {
                conn.setLastHeartBeatTime(new Date());
                synchronized (conn) {
                    String content = new String(msg.getData(), StandardCharsets.UTF_8);
                    Date now = new Date();
                    conn.setLastHeartBeatTime(now);
                    conn.getReports().add(0, new Connection.Report(now, content));
                    if (conn.getReports().size() > 10) {
                        conn.getReports().remove(10);
                    }
                }
            }
        } else if (msg.getCmd() == 5) {
            Connection conn = connectionManager.getStore().get(msg.getDeviceId());
            int contentLength = 1 + 15 + 1 + 1 + 84 + 16 + 16 + 20;
            if (conn == null) {
                logger.warn("skipped message : {}, device not registered", msg);
            } else if (msg.getData().length != contentLength) {
                logger.warn("skipped message: {}, content length unexpected, expect={}, actual={}",
                        msg, contentLength, msg.getData().length);
            } else {
                ByteBuf data = ctx.alloc().heapBuffer(msg.getData().length).writeBytes(msg.getData());
                int accessType = data.readByte();
                byte[] imeiBuffer = new byte[15];
                data.readBytes(imeiBuffer);
                String imei = new String(imeiBuffer, StandardCharsets.UTF_8);
                boolean cdma = data.readByte() != 0;
                int networkType = data.readByte();
                List<BaseStation> stations = parseStations(data);
                byte[] doubleBuf = new byte[16];
                data.readBytes(doubleBuf);
                double lat = Double.parseDouble(new String(doubleBuf));
                data.readBytes(doubleBuf);
                double lng = Double.parseDouble(new String(doubleBuf));
                byte[] iccIdBuf = new byte[20];
                data.readBytes(iccIdBuf);
                data.release();
                String iccId = new String(iccIdBuf);
                Coordinate c =  Coordinate.builder()
                        .system(CoordinateSystem.WGS84)
                        .lat(lat)
                        .lng(lng)
                        .build();

                logger.info("receiving gprs data: accessType={}, imei={}, cdma={}, "
                                + "networkType={}, stations={}, iccId={}, coordinate={}",
                        accessType, imei, cdma, networkType, stations, iccId, c);
                conn.setCoordinate(c);
                conn.setIccId(iccId);
                conn.setLastHeartBeatTime(new Date());
            }
        }
    }

    private List<BaseStation> parseStations(ByteBuf data) {
        List<BaseStation> stations = new ArrayList<>();

        for (int i = 0; i < 6; i++) {
            stations .add(
                    BaseStation.builder()
                            .valid(data.readByte())
                            .mcc(data.readShortLE())
                            .mnc(data.readByte())
                            .lac(data.readIntLE())
                            .cellId(data.readIntLE())
                            .signal(data.readShortLE())
                            .build());
        }
        return stations;
    }

    private void processMessage(ChannelHandlerContext ctx, Connection conn, byte[] data) {
        synchronized (conn) {
            String content = new String(data, StandardCharsets.UTF_8);
            Date now = new Date();
            conn.setLastHeartBeatTime(now);
            conn.getReports().add(0, new Connection.Report(now, content));
            if (conn.getReports().size() > 10) {
                conn.getReports().remove(10);
            }
        }
    }

    private void sendReply(SoltMachineMessage message, List<Object> out) {
        if (message.getCmd() == 2 || message.getCmd() == 128) {
            // skip for reply message for reply and message
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
    protected void decode(ChannelHandlerContext ctx, SoltMachineMessage msg, List<Object> out) {
        List<Object> replies = new ArrayList<>();
        sendReply(msg, replies);

        synchronized (ctx.channel()) {
            for (Object o : replies) {
                ctx.pipeline().write(o);
            }
            ctx.pipeline().flush();
        }

        processMessage(ctx, msg);
        out.add(msg);
    }
}
