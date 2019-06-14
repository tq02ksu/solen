package com.neusoft.solen.slotmachine;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SoltMachineMessage {

    /**
     * new byte[] {0x77, 0x33}
     */
    @Builder.Default
    private int header = 0x3377;

    /**
     * 包序号
     */
    private int index;

    /**
     * 识别码，小端表示为0x79 0x75 0x77 0x65 0x6E 0x36 0x30 0x32
     */
    @Builder.Default
    private long idCode = 0x230363E656775797L;

    private int type;

    private byte[] data;
}
