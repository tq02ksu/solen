package top.fengpingtech.solen.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.Map;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Event {
    private Long id;
    private String deviceId;
    private EventType type;
    private Date time;
    private Map<String, String> details;
}
