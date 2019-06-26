package com.neusoft.solen.slotmachine;

import com.neusoft.solen.controller.MessageController;
import io.netty.buffer.ByteBuf;
import org.junit.Test;

import static org.junit.Assert.*;

public class SlotMachineInBoundHandlerTest {

    @Test
    public void encode() {
        ByteBuf byteBuf = SlotMachineInBoundHandler.encode(SoltMachineMessage.builder()
                .header(13175)
                .index(18)
                .idCode(8389750987502775627L)
                .deviceId("10619030006")
                .cmd((short) 3)
                .data("test".getBytes())
                .build());

        SlotMachineInBoundHandler.logBytebuf(byteBuf, "test");
    }

    @Test
    public void reverse() {
    }

    @Test
    public void logBytebuf() {
    }
}
