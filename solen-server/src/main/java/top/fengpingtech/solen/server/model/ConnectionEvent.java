package top.fengpingtech.solen.server.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ConnectionEvent extends Event {
    private Long lac;

    private Long ci;
}
