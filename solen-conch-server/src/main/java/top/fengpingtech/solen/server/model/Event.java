package top.fengpingtech.solen.server.model;

import lombok.Data;

import java.util.Date;

@Data
public class Event {
    private Long eventId;

    private String connectionId;

    private Integer header;

    private Short source;

    private Short gun;

    private Short txType;

    private Byte end;

    private String deviceId;

    private Integer cmd;

    private EventType type;

    public void fromData(byte[] data) {
        throw new UnsupportedOperationException();
    }
}
