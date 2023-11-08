package top.fengpingtech.solen.server.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class LocationEvent extends Event {
    private String imei;

    private Double lat;

    private Double lng;

    private String iccId;
}
