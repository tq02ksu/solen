package com.neusoft.solen.controller;

import com.neusoft.solen.slotmachine.ConnectionManager;
import com.neusoft.solen.slotmachine.SlotMachineInBoundHandler;
import com.neusoft.solen.slotmachine.SoltMachineMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.neusoft.solen.slotmachine.SlotMachineInBoundHandler.*;

@RestController
public class MessageController {
    private static final Logger logger = LoggerFactory.getLogger(MessageController.class);

    private final ConnectionManager connectionManager;

    public MessageController(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
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

    @RequestMapping("/listAll")
    public Object listAll() {
        TreeSet<ConnectionManager.Connection> set = new TreeSet<>(
                Comparator.comparing(ConnectionManager.Connection::getDeviceId));
        set.addAll(connectionManager.getStore().values());
        return set;
    }

    @RequestMapping("/statByField")
    public Object statByField(@RequestParam String field) {
        Collection<ConnectionManager.Connection> values = connectionManager.getStore().values();
        Function<ConnectionManager.Connection, Object> getter = c -> {
            try {
                return Objects.requireNonNull(BeanUtils.getPropertyDescriptor(
                        ConnectionManager.Connection.class, field))
                        .getReadMethod().invoke(c);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        return values.stream().collect(Collectors.groupingBy(getter, Collectors.counting()));
    }

    @PostMapping("/sendControl")
    public ResponseEntity<Object> sendControl(@RequestBody SendRequest request) throws ExecutionException, InterruptedException {
        String deviceId = request.getDeviceId();
        if (!connectionManager.getStore().containsKey(deviceId)) {
            return ResponseEntity.notFound().build();
        }

        Channel ch = connectionManager.getStore().get(deviceId).getChannel();
        ConnectionManager.Connection conn = connectionManager.getStore().get(deviceId);
        synchronized (ch) {
            byte[] buffer = new byte[] {(byte) (request.getCtrl()), (byte) (0x01 - request.getCtrl()),
                    0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F};

            buffer[0] = (byte) request.getCtrl();
            SoltMachineMessage message = SoltMachineMessage.builder()
                    .header(conn.getHeader())
                    .index(conn.getIndex() + 1)
                    .idCode(conn.getIdCode())
                    .deviceId(deviceId)
                    .cmd((short) 3)
                    .data(buffer)
                    .build();
            ByteBuf buf = Unpooled.wrappedBuffer(encode(message));
            SlotMachineInBoundHandler.logBytebuf(buf, "sending control");
            ch.writeAndFlush(buf).get();
            return ResponseEntity.ok("Message sent: " + message);
        }
    }

    @PostMapping("/sendAscii")
    public ResponseEntity<Object> sendAscii(@RequestBody SendRequest request) throws Exception {
        String deviceId = request.getDeviceId();
        String data = request.getData();
        if (!connectionManager.getStore().containsKey(deviceId)) {
            return ResponseEntity.notFound().build();
        }

        Channel ch = connectionManager.getStore().get(deviceId).getChannel();
        ConnectionManager.Connection conn = connectionManager.getStore().get(deviceId);
        synchronized (ch) {
            SoltMachineMessage message = SoltMachineMessage.builder()
                    .header(conn.getHeader())
                    .index(conn.getIndex() + 1)
                    .idCode(conn.getIdCode())
                    .deviceId(deviceId)
                    .cmd((short) 129)
                    .data(data.getBytes())
                    .build();
            ByteBuf buf = Unpooled.wrappedBuffer(encode(message));
            logBytebuf(buf, "sending ascii");
            ch.writeAndFlush(buf).get();
            return ResponseEntity.ok("Message sent: " + message);
        }
    }
}
