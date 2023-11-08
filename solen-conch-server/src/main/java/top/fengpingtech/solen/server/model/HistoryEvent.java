package top.fengpingtech.solen.server.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import top.fengpingtech.solen.server.netty.EventModel;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@EventModel(value = 0x0006, desc = "历史信息")
public class HistoryEvent extends DataEvent {
    @Override
    public void fromData(byte[] data) {
        super.fromData(data);

        setType(EventType.HISTORY_DATA);
    }
}
