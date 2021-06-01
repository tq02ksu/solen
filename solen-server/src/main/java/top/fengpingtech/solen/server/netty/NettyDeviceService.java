package top.fengpingtech.solen.server.netty;

import io.netty.channel.Channel;
import top.fengpingtech.solen.server.DeviceService;
import top.fengpingtech.solen.server.model.Device;
import top.fengpingtech.solen.server.model.SoltMachineMessage;
import top.fengpingtech.solen.server.protocol.ConnectionKeeperHandler;

import java.nio.charset.StandardCharsets;

public class NettyDeviceService implements DeviceService {

    private final ConnectionHolder connectionHolder;

    public NettyDeviceService(ConnectionHolder connectionHolder) {
        this.connectionHolder = connectionHolder;
    }

    @Override
    public void sendMessage(String deviceId, String message) {
        Device device = connectionHolder.getDevice(deviceId);

        if (device == null) {
            throw new IllegalArgumentException("device with id = " + deviceId + " have no live device connection!");
        }

        int index = device.getIndex().getAndIncrement();
        device.getConnections().forEach(conn -> {
            Channel ch = conn.getChannel();
            SoltMachineMessage msg = SoltMachineMessage.builder()
                    .index(index)
                    .idCode(conn.getIdCode())
                    .deviceId(deviceId)
                    .cmd((short) 129)
                    .data(message.getBytes(StandardCharsets.UTF_8))
                    .build();
            ch.pipeline().writeAndFlush(msg);
        });
    }

}
