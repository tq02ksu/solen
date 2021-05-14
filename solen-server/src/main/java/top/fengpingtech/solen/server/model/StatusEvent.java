package top.fengpingtech.solen.server.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class StatusEvent extends Event {
    private Integer inputStat;

    private Integer outputStat;

    private Integer rssi;

    private Double voltage;

    private Double temperature;

    private Integer gravity;

    private Integer uptime;
}
