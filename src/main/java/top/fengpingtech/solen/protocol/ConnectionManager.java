package top.fengpingtech.solen.protocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import top.fengpingtech.solen.model.Connection;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ConnectionManager {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);

    private boolean shutdown = false;
    private final Map<String, Connection> store = new ConcurrentHashMap<>();

    public Map<String, Connection> getStore() {
        return store;
    }

    public void close(Connection conn) {
        try {
            conn.getCtx().channel().close().sync();
        } catch (InterruptedException e) {
            logger.warn("error close conn: {}", conn, e);
        }
    }
}
