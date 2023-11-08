package top.fengpingtech.solen.server.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import top.fengpingtech.solen.server.netty.EventModel;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@EventModel(value = 0x0007, desc = "告警信息")
public class AlarmEvent extends DataEvent {

    @Override
    public void fromData(byte[] data) {
        super.fromData(data);

        setType(EventType.ALARM_DATA);
    }
}
