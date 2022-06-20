package top.fengpingtech.solen.server.protocol;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.fengpingtech.solen.server.model.Device;
import top.fengpingtech.solen.server.model.SoltMachineMessage;
import top.fengpingtech.solen.server.netty.ConnectionHolder;

import java.util.List;
import java.util.concurrent.CountDownLatch;

public class ConnectionKeeperHandler extends ChannelDuplexHandler {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionKeeperHandler.class);

    private static final AttributeKey<String> DEVICE_ID_ATTRIBUTE_KEY = AttributeKey.valueOf("DeviceId");

    private final ConnectionHolder connectionHolder;

    public ConnectionKeeperHandler(ConnectionHolder connectionHolder) {
        this.connectionHolder = connectionHolder;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof SoltMachineMessage) {
            sendReply(ctx, (SoltMachineMessage) msg);
            syncControlSender(ctx, (SoltMachineMessage) msg);
            String deviceId = ((SoltMachineMessage) msg).getDeviceId();
            long idCode = ((SoltMachineMessage) msg).getIdCode();
            Integer header = ((SoltMachineMessage) msg).getHeader();
            ctx.channel().attr(DEVICE_ID_ATTRIBUTE_KEY).setIfAbsent(deviceId);

            connectionHolder.add(deviceId, idCode, ctx.channel(), header);
        }
        super.channelRead(ctx, msg);
    }

    private void syncControlSender(ChannelHandlerContext ctx, SoltMachineMessage msg) {
        Device device = connectionHolder.getDevice(msg.getDeviceId());
        if (device != null) {
            device.getControlSyncs().forEach(CountDownLatch::countDown);
        }
    }

    void sendReply(ChannelHandlerContext ctx, SoltMachineMessage msg) {
        if (msg.getCmd() == 2 || msg.getCmd() == 128) {
            // skip for reply message for reply and message
            return;
        }

        ctx.pipeline().writeAndFlush(SoltMachineMessage.builder()
                .header(msg.getHeader())
                .index(msg.getIndex())
                .idCode(msg.getIdCode())
                .cmd((short) 2)
                .deviceId(msg.getDeviceId())
                .data(new byte[]{msg.getCmd().byteValue()})  // arg==0
                .build());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        String deviceId = ctx.channel().attr(DEVICE_ID_ATTRIBUTE_KEY).get();
        if (cause instanceof ReadTimeoutException && deviceId != null) {
            logger.info("{}({}) read timeout, closing channel", deviceId, ctx.channel().id().asLongText());

            connectionHolder.remove(deviceId, ctx.channel());
        }
        super.exceptionCaught(ctx, cause);
        ctx.pipeline().close();
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        String deviceId = ctx.channel().attr(DEVICE_ID_ATTRIBUTE_KEY).get();
        if (deviceId != null) {
            logger.info("{}({}) unregistered, closing channel", deviceId, ctx.channel().id().asLongText());
            connectionHolder.remove(deviceId, ctx.channel());
        }
        super.channelUnregistered(ctx);
    }
}
