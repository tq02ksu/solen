package top.fengpingtech.solen.slotmachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import top.fengpingtech.solen.model.Connection;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ConnectionManager {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);

    private Thread thread;

    private boolean shutdown = false;
    private final Map<String, Connection> store = new ConcurrentHashMap<>();

    public Map<String, Connection> getStore() {
        return store;
    }

    public void close(Connection conn ) {
        try {
            conn.getChannel().close().sync();
        } catch (InterruptedException e) {
            logger.warn("error close conn: {}", conn, e);
        }
    }

    @PostConstruct
    public void init() {
        thread = new Thread(() -> {
            while (true) {
                store.forEach((key, val) -> {
                    try {
                        if (val.getChannel().isOpen()
                                && System.currentTimeMillis() - val.getLastHeartBeatTime().getTime() > Connection.HEARTBEAT_TIMEOUT_MS * 2) {
                            logger.info("deviceId={} last heartbeatTime is {}, seem to lost connection, ticking",
                                    key, val.getLastHeartBeatTime());
                            close(val);
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
}