package top.fengpingtech.solen.server.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import top.fengpingtech.solen.server.netty.EventModel;

import java.nio.ByteBuffer;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@EventModel(value = 0x0001, desc = "心跳信息")
public class HeartbeatEvent extends Event {
    private Short intervalSeconds;

    private Integer uptimeSeconds;

    @Override
    public void fromData(byte[] data) {
        setType(EventType.HEARTBEAT);

        ByteBuffer buffer = ByteBuffer.allocate(data.length);

        intervalSeconds = (short) Math.abs(buffer.get());

        uptimeSeconds = buffer.getInt();
    }
}
