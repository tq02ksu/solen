package top.fengpingtech.solen.server.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class LocationEvent extends Event {
    /**
     * using AttributeEvent.imei
     */
    @Deprecated
    private String imei;

    private Double lat;

    private Double lng;

    /**
     * using AttributeEvent.iccId
     */
    @Deprecated
    private String iccId;
}
