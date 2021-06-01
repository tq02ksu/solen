package top.fengpingtech.solen.server.protocol;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.fengpingtech.solen.server.model.Device;
import top.fengpingtech.solen.server.model.SoltMachineMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConnectionKeeperHandler extends ChannelDuplexHandler {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionKeeperHandler.class);

    private static final AttributeKey<String> DEVICE_ID_ATTRIBUTE_KEY = AttributeKey.valueOf("DeviceId");

    private final ConcurrentHashMap<String, Device> deviceKeeper;

    public ConnectionKeeperHandler() {
        deviceKeeper = new ConcurrentHashMap<>();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof SoltMachineMessage) {
            sendReply(ctx, (SoltMachineMessage) msg);
            String deviceId = ((SoltMachineMessage) msg).getDeviceId();
            long idCode = ((SoltMachineMessage) msg).getIdCode();
            ctx.channel().attr(DEVICE_ID_ATTRIBUTE_KEY).setIfAbsent(deviceId);

            deviceKeeper.computeIfAbsent(deviceId, (k) -> {
                Device d = new Device();
                d.setIndex(new AtomicInteger(0));
                d.setDeviceId(k);
                d.setConnections(Collections.singletonList(new Device.Connection(ctx.channel(), idCode)));
                return d;
            });

            Device.Connection conn = new Device.Connection(ctx.channel(), idCode);
            if (!deviceKeeper.get(deviceId).getConnections().contains(conn)) {
                deviceKeeper.computeIfPresent(deviceId, (key, oldVal) -> {
                   Device d = new Device();
                   d.setDeviceId(key);
                   d.setConnections(
                           Collections.unmodifiableList(
                                   Stream.concat(oldVal.getConnections().stream(), Stream.of(conn))
                                   .collect(Collectors.toList())));
                   return d;
                });
            }
        }
        super.channelRead(ctx, msg);
    }

    void sendReply(ChannelHandlerContext ctx, SoltMachineMessage msg) {
        if (msg.getCmd() == 2 || msg.getCmd() == 128) {
            // skip for reply message for reply and message
            return;
        }

        synchronized (ctx.channel()) {
            ctx.pipeline().writeAndFlush(SoltMachineMessage.builder()
                    .header(msg.getHeader())
                    .index(msg.getIndex())
                    .idCode(msg.getIdCode())
                    .cmd((short) 2)
                    .deviceId(msg.getDeviceId())
                    .data(new byte[]{msg.getCmd().byteValue()})  // arg==0
                    .build());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        String deviceId = ctx.channel().attr(DEVICE_ID_ATTRIBUTE_KEY).get();
        if (cause instanceof ReadTimeoutException && deviceId != null) {
            logger.info("{} read timeout, closing channel", ctx.channel().id().asLongText());

            Device device = deviceKeeper.computeIfPresent(deviceId, (key, oldVal) -> {
                Device d = new Device();
                d.setDeviceId(key);
                d.setIndex(oldVal.getIndex());
                d.setConnections(Collections.unmodifiableList(
                        oldVal.getConnections().stream()
                        .filter(c -> c != ctx.channel()).collect(Collectors.toList())));
                return d;
            });
            if (device != null && device.getConnections().isEmpty()) {
                deviceKeeper.remove(deviceId, device);
            }
        }
        super.exceptionCaught(ctx, cause);
    }

    public Device getDevice(String deviceId) {
        return deviceKeeper.get(deviceId);
    }

    List<Device> getDevices() {
        return Collections.unmodifiableList(new ArrayList<>(deviceKeeper.values()));
    }
}
