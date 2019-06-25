package com.neusoft.solen.controller;

import com.neusoft.solen.SolenApplicationTests;
import com.neusoft.solen.slotmachine.SoltMachineMessage;
import io.netty.buffer.ByteBuf;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.*;

public class MessageControllerTest  extends SolenApplicationTests {
    @Autowired
    private MessageController controller;
    @Test
    public void testEncode() {
        ByteBuf byteBuf = controller.encode(SoltMachineMessage.builder()
                .header(13175)
                .index(18)
                .idCode(8389750987502775627L)
                .deviceId("10619030006")
                .cmd((short) 3)
                .data("test".getBytes())
                .build());

        MessageController.logBytebuf(byteBuf, "test");
    }
}
