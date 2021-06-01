package top.fengpingtech.solen.server.netty;

import top.fengpingtech.solen.server.DeviceService;
import top.fengpingtech.solen.server.model.Device;
import top.fengpingtech.solen.server.model.SoltMachineMessage;
import top.fengpingtech.solen.server.protocol.ConnectionKeeperHandler;

import java.nio.charset.StandardCharsets;

public class NettyDeviceService implements DeviceService {

    private final ConnectionKeeperHandler keeperHandler;

    public NettyDeviceService(ConnectionKeeperHandler keeperHandler) {
        this.keeperHandler = keeperHandler;
    }

    @Override
    public void sendMessage(String deviceId, String message) {
        Device device = keeperHandler.getDevice(deviceId);

        if (device == null) {
            throw new IllegalArgumentException("device with id = " + deviceId + " have no live device connection!");
        }

        int index = device.getIndex().getAndIncrement();
        SoltMachineMessage msg = SoltMachineMessage.builder()
                .index(index)
                .deviceId(deviceId)
                .cmd((short) 129)
                .data(message.getBytes(StandardCharsets.UTF_8))

                .build();

        device.getConnections().forEach(ch -> {
            synchronized (ch) {
                ch.pipeline().writeAndFlush(msg);
            }
        });
    }
}
