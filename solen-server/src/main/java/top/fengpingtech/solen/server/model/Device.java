package top.fengpingtech.solen.server.model;

import io.netty.channel.Channel;
import lombok.Data;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Data
public class Device {
    private String deviceId;

    private AtomicInteger index;

    private List<Channel> connections;
}
