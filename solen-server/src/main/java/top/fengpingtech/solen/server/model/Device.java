package top.fengpingtech.solen.server.model;

import io.netty.channel.Channel;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@Data
public class Device {
    private String deviceId;

    private AtomicInteger index;

    private List<Connection> connections;

    @Data
    @AllArgsConstructor
    public static class Connection {

        private Channel channel;

        private Long idCode;
        public int hashCode() {
            return channel.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Connection that = (Connection) o;
            return Objects.equals(channel, that.channel);
        }
    }
}
