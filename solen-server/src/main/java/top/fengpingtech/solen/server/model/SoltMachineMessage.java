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

    private String connectionId;

    /**
     * new byte[] {0x77, 0x33}
     */
    @Builder.Default
    private Integer header = 0x3377;

    /**
     * 包序号
     */
    private Integer index;

    /**
     * 识别码，小端表示为0x79 0x75 0x77 0x65 0x6E 0x36 0x30 0x32
     */
    @Builder.Default
    private Long idCode = 0x230363E656775797L;

    private String deviceId;

    private Short cmd;

    private byte[] data;
}
