package com.neusoft.solen.controller;

import com.neusoft.solen.slotmachine.ConnectionManager;
import com.neusoft.solen.slotmachine.SlotMachineInBoundHandler;
import io.netty.channel.Channel;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ExecutionException;

@RestController
public class MessageController {
    private final ConnectionManager connectionManager;

    public MessageController(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @RequestMapping("/sendAll")
    public Object sendAll(@RequestParam String data) throws ExecutionException, InterruptedException {
        for (ConnectionManager.Connection conn : connectionManager.getStore().values()) {
            Channel ch = conn.getChannel();
            ch.writeAndFlush("test send message ").get();
        }
        return "ok: " + data;
    }
}
