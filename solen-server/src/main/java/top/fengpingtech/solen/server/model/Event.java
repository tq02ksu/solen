package top.fengpingtech.solen.server.model;

import lombok.Data;

import java.util.Date;

@Data
public class Event {
    private Long eventId;
    private String deviceId;
    private EventType type;
    private Date time;

    private Integer index;
}
