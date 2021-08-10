package top.fengpingtech.solen.app.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import solen.model.EventType;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class EventIndex {
    private Long eventId;
    private String deviceId;
    private byte[] dataKey;
    private EventType type;
}
