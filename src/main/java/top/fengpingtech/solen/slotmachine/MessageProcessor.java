package top.fengpingtech.solen.slotmachine;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import top.fengpingtech.solen.model.Connection;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

public class MessageProcessor extends MessageToMessageDecoder<SoltMachineMessage> {
    private static final Logger logger = LoggerFactory.getLogger(MessageProcessor.class);

    private final ConnectionManager connectionManager;

    public MessageProcessor(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    private void processMessage(Channel channel, SoltMachineMessage msg, List<Object> out) {
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

            connection.setChannel(channel);
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
                conn.setRssi((int) msg.getData()[1]);

                for (CountDownLatch sync : conn.getOutputStatSyncs()) {
                    sync.countDown();
                }
            }
        } else if (msg.getCmd() == 128) {
            Connection conn = connectionManager.getStore().get(msg.getDeviceId());
            if (conn == null) {
                logger.warn("skipped message : {}, device not registered", msg);
            } else {
                String content = new String(msg.getData());
                Date now = new Date();
                conn.setLastHeartBeatTime(now);
                conn.getReports().add(0, new Connection.Report(now, content));
                if (conn.getReports().size() > 10) {
                    conn.getReports().remove(10);
                }
            }
        }
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

        processMessage(ctx.channel(), msg, out);

        for (Object o : out) {
            ctx.pipeline().writeAndFlush(o);
        }
    }
}
