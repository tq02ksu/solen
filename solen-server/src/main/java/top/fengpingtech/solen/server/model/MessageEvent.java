package top.fengpingtech.solen.server.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MessageEvent extends Event {
    private String message;
}
