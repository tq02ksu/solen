package top.fengpingtech.solen.server.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import top.fengpingtech.solen.server.netty.EventModel;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@EventModel(value = 0x0014, desc = "实时信息")
public class RealtimeEvent extends DataEvent {
    @Override
    public void fromData(byte[] data) {
        super.fromData(data);

        setType(EventType.REALTIME_DATA);
    }
}
