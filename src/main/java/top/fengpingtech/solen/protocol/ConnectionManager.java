package top.fengpingtech.solen.protocol;

import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import top.fengpingtech.solen.model.Connection;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ConnectionManager {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);

    private final Map<String, Connection> store = new ConcurrentHashMap<>();

    public Map<String, Connection> getStore() {
        return store;
    }

    public void close(Connection conn) {
        try {
            Attribute<Object> skipping = conn.getCtx().channel().attr(AttributeKey.valueOf("Event-Skipped"));
            skipping.set(true);
            conn.getCtx().channel().close().sync();
        } catch (InterruptedException e) {
            logger.warn("error close conn: {}", conn, e);
        }
    }
}
