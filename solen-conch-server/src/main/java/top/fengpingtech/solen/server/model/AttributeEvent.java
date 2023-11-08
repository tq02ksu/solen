package top.fengpingtech.solen.server.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AttributeEvent extends Event {
    private Integer inputStat;

    private Integer outputStat;

    private Integer rssi;

    private Double voltage;

    private Double temperature;

    private Integer gravity;

    private Integer uptime;
}
