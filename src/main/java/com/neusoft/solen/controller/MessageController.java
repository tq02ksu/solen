package com.neusoft.solen.controller;

import com.neusoft.solen.slotmachine.ConnectionManager;
import com.neusoft.solen.slotmachine.SlotMachineInBoundHandler;
import com.neusoft.solen.slotmachine.SoltMachineMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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

    @RequestMapping("/detail/{deviceId}")
    public ResponseEntity<Object> detail(@PathVariable ("deviceId") String deviceId) {
        if (!connectionManager.getStore().containsKey(deviceId)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(connectionManager.getStore().get(deviceId));
    }

    @RequestMapping("/list")
    public Collection<String> list() {
        return connectionManager.getStore().keySet();
    }

    @PostMapping("send")
    public ResponseEntity<Object> send( String deviceId,  String data) throws ExecutionException, InterruptedException {
        if (!connectionManager.getStore().containsKey(deviceId)) {
            return ResponseEntity.notFound().build();
        }

        Channel ch = connectionManager.getStore().get(deviceId).getChannel();
        synchronized (ch) {
            ByteBuf buf = Unpooled.wrappedBuffer(data.getBytes());
            ch.writeAndFlush(buf).get();
            return ResponseEntity.ok("Message sent: " + data);
        }
    }

    @PostMapping("sendControl")
    public ResponseEntity<Object> sendControl(String deviceId,  int ctrl) throws ExecutionException, InterruptedException {
        if (!connectionManager.getStore().containsKey(deviceId)) {
            return ResponseEntity.notFound().build();
        }

        Channel ch = connectionManager.getStore().get(deviceId).getChannel();
        ConnectionManager.Connection conn = connectionManager.getStore().get(deviceId);
        synchronized (ch) {
            byte[] buffer = new byte[16];
            buffer[0] = ctrl == 1 ? (byte) 0x31 : (byte) 0x0;
            SoltMachineMessage message = SoltMachineMessage.builder()
                    .header(conn.getHeader())
                    .index(conn.getIndex() + 1)
                    .idCode(conn.getIdCode())
                    .deviceId(deviceId)
                    .cmd((short) 3)
                    .data(buffer)
                    .build();
            ByteBuf buf = Unpooled.wrappedBuffer(encode(message));
            ch.writeAndFlush(buf).get();
            return ResponseEntity.ok("Message sent: " + message);
        }
    }

    private ByteBuf encode(SoltMachineMessage message) {
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeShortLE(message.getHeader());
        byteBuf.writeShortLE(message.getData().length + 26);
        byteBuf.writeByte(message.getIndex());
        byteBuf.writeLongLE(message.getIdCode());
        byteBuf.writeBytes(message.getDeviceId().getBytes());
        byteBuf.writeBytes(message.getDeviceId().getBytes());
        byteBuf.writeByte(reverse((byte)message.getCmd()));
        byteBuf.writeBytes(message.getData());

        byte checksum = 0;
        for (int i = 0; i < message.getData().length + 26 - 1; i ++) {
            checksum ^= byteBuf.readByte();
        }
        byteBuf.resetReaderIndex();
        byteBuf.writeByte(checksum);

        return byteBuf;
    }

    byte reverse(byte b) {
        return (byte)  Integer.reverse(((int) b) <<24);
    }
}
