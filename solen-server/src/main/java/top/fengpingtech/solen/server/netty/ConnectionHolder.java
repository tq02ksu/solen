package top.fengpingtech.solen.server.netty;

import io.netty.channel.Channel;
import top.fengpingtech.solen.server.model.Device;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConnectionHolder {
    private final ConcurrentHashMap<String, Device> deviceKeeper = new ConcurrentHashMap<>();

    public void add(String deviceId, long idCode, Channel channel, Integer header) {
        Device.Connection conn = new Device.Connection(channel, idCode, header);
        deviceKeeper.computeIfAbsent(deviceId, (k) -> {
            Device d = new Device();
            d.setIndex(new AtomicInteger(0));
            d.setDeviceId(k);
            d.setConnections(Collections.singletonList(conn));
            return d;
        });

        if (!deviceKeeper.get(deviceId).getConnections().contains(conn)) {
            deviceKeeper.computeIfPresent(deviceId, (key, oldVal) -> {
                Device d = new Device();
                d.setIndex(new AtomicInteger(0));
                d.setDeviceId(key);
                d.setConnections(
                        Collections.unmodifiableList(
                                Stream.concat(oldVal.getConnections().stream(), Stream.of(conn))
                                        .collect(Collectors.toList())));
                return d;
            });
        }
    }

    public void remove(String deviceId, Channel channel ) {
        Device.Connection conn = new Device.Connection(channel, 0L, 0);
        Device device = deviceKeeper.computeIfPresent(deviceId, (key, oldVal) -> {
            Device d = new Device();
            d.setDeviceId(key);
            d.setIndex(oldVal.getIndex());
            d.setConnections(Collections.unmodifiableList(
                    oldVal.getConnections().stream()
                            .filter(c -> !c.equals(conn)).collect(Collectors.toList())));
            return d;
        });
        if (device != null && device.getConnections().isEmpty()) {
            deviceKeeper.remove(deviceId, device);
        }
    }

    public Device getDevice(String deviceId) {
        return deviceKeeper.get(deviceId);
    }

    public List<Device> getDevices() {
        return Collections.unmodifiableList(new ArrayList<>(deviceKeeper.values()));
    }
}
