package top.fengpingtech.solen.server.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import top.fengpingtech.solen.server.netty.EventModel;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class DisconnectEvent extends Event {
    private Date loginTime;

    private Integer stationType;

    private Integer preserve;

    private Integer monitorBoardVersion;

    private Integer masterBoardVersion;


}
