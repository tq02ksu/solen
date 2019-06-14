package com.neusoft.solen.slotmachine;

import io.netty.channel.Channel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ConnectionManager {
    private final Map<String, Connection> store = new ConcurrentHashMap<>();

    public Map<String, Connection> getStore() {
        return store;
    }

    @Builder
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Connection {
        private String serverHost;
        private String serverPort;
        private String location;
        private Channel channel;
    }
}
