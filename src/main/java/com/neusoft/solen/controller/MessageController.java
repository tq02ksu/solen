package com.neusoft.solen.controller;

import com.neusoft.solen.slotmachine.ConnectionManager;
import com.neusoft.solen.slotmachine.SlotMachineInBoundHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
public class MessageController {
    private final ConnectionManager connectionManager;

    public MessageController(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @RequestMapping("/sendAll")
    public Object sendAll(@RequestParam String data) throws ExecutionException, InterruptedException {

        Map<String, String> result = new HashMap<>();

        for (Map.Entry<String, ConnectionManager.Connection> entry : connectionManager.getStore().entrySet()) {
            Channel ch = entry.getValue().getChannel();
            ByteBuf buf = Unpooled.wrappedBuffer("test send message".getBytes());
            ch.writeAndFlush(buf).get();
            result.put(entry.getKey(), "test send message");
        }
        return result;
    }
}
