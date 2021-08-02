package top.fengpingtech.solen.server.netty;

import io.netty.channel.Channel;
import top.fengpingtech.solen.server.DeviceService;
import top.fengpingtech.solen.server.model.Device;
import top.fengpingtech.solen.server.model.SoltMachineMessage;
import top.fengpingtech.solen.server.protocol.ConnectionKeeperHandler;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
                    .header(conn.getHeader())
                    .index(index)
                    .idCode(conn.getIdCode())
                    .deviceId(deviceId)
                    .cmd((short) 129)
                    .data(message.getBytes(StandardCharsets.UTF_8))
                    .build();
            ch.pipeline().writeAndFlush(msg);
        });
    }

    @Override
    public void sendControl(String deviceId, int stat) {
        Device device = connectionHolder.getDevice(deviceId);

        if (device == null) {
            throw new IllegalArgumentException("device with id = " + deviceId + " have no live device connection!");
        }

        CountDownLatch latch = new CountDownLatch(1);
        try {
            int index = device.getIndex().getAndIncrement();
            device.getControlSyncs().add(latch);
            device.getConnections().forEach(conn -> {
                Channel ch = conn.getChannel();
                byte[] buffer = new byte[]{(byte) (stat), (byte) (0x01 - stat),
                        0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F};

                SoltMachineMessage message = SoltMachineMessage.builder()
                        .header(conn.getHeader())
                        .index(index)
                        .idCode(conn.getIdCode())
                        .deviceId(deviceId)
                        .cmd((short) 3)
                        .data(buffer)
                        .build();
                ch.pipeline().writeAndFlush(message);
            });

            boolean success = latch.await(20, TimeUnit.SECONDS);
            if (!success) {
                throw new IllegalStateException("while control result fail!");
            }
        } catch (Exception e) {
            throw new IllegalStateException("error while await control status", e);
        } finally {
            device.getControlSyncs().remove(latch);
        }
    }

}
