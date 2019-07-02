package com.neusoft.solen.slotmachine;

import io.netty.channel.Channel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
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
        private String deviceId;
        private String serverHost;
        private String serverPort;
        private int lac;
        private int ci;
        private Channel channel;

        private int header;
        private int index;
        private long idCode;
        private int inputStat;
        private int outputStat;

        @Builder.Default
        private List<Report> reports = new LinkedList<>();
    }

    @Builder
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Report {
        private Date time;
        private String content;
    }
}
