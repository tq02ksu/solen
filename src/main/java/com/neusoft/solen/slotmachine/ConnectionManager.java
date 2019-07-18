package com.neusoft.solen.slotmachine;

import io.netty.channel.Channel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ConnectionManager {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);

    private Thread thread;

    private boolean shutdown = false;
    private final Map<String, Connection> store = new ConcurrentHashMap<>();

    public Map<String, Connection> getStore() {
        return store;
    }

    @PostConstruct
    public void init() {
        thread = new Thread(() -> {
            while (true) {
                store.forEach((key, val) -> {
                    try {
                        if (val.getChannel().isActive()
                                && System.currentTimeMillis() - val.getLastHeartBeatTime().getTime()
                                > 5L * 60 * 60 * 1000) {
                            logger.info("deviceId={} last heartbeatTime is {}, seem to lost connection, ticking",
                                    key, val.getLastHeartBeatTime());
                            val.getChannel().close().get();
                        }
                    } catch (Exception e) {
                        logger.error("error while close channel, deviceId={}", key);
                    }
                });

                if (shutdown) {
                    return;
                }

                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    logger.info("interrupted", e);
                    break;
                }
            }
        });
        thread.setName("Connection-Guard");
        thread.setDaemon(true);
        thread.start();
    }

    @PreDestroy
    public void destroy() {
        shutdown = true;
        thread.interrupt();
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

        private long idCode;
        private int inputStat;
        private int outputStat;
        private Date lastHeartBeatTime = new Date();

        @Builder.Default
        private List<Report> reports = new LinkedList<>();

        private transient AtomicInteger index = new AtomicInteger(0);

        private transient List<CountDownLatch> outputStatSyncs = new CopyOnWriteArrayList<>();
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
