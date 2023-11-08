package top.fengpingtech.solen.server.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SoltMachineMessage {
    public static final int MSG_HEADER = 0x7572;

    private String connectionId;

    /**
     * new byte[] {0x77, 0x33}
     */
    private Integer header;

    private Short source;

    private Short gun;

    private Short txType;

    private Byte end;

    private String deviceId;

    private Integer cmd;

    private byte[] data;
}
