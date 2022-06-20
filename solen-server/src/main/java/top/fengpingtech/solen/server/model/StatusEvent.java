package top.fengpingtech.solen.server.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class StatusEvent extends Event {
    public static final String STATUS_NORMAL = "NORMAL";
    public static final String STATUS_DISCONNECTED = "DISCONNECTED";

    private String status;
}
