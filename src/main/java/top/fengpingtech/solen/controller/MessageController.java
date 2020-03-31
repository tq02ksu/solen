package top.fengpingtech.solen.controller;

import top.fengpingtech.solen.bean.ConnectionBean;
import top.fengpingtech.solen.slotmachine.SlotMachineInBoundHandler;
import top.fengpingtech.solen.model.Connection;
import top.fengpingtech.solen.slotmachine.ConnectionManager;
import top.fengpingtech.solen.slotmachine.SoltMachineMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.beans.PropertyDescriptor;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
public class MessageController {
    private static final Logger logger = LoggerFactory.getLogger(MessageController.class);

    private static final Map<String, Comparator<Connection>> COMPARATORS =
            new HashMap<String, Comparator<Connection>>() {
        {
            // lac, ci, inputStat, outputStat, deviceId
            put("default", Comparator.comparing(Connection::getDeviceId));
            put("deviceId", Comparator.comparing(Connection::getDeviceId));
            put("lac", Comparator.comparing(Connection::getLac));
            put("ci", Comparator.comparing(Connection::getCi));
            put("inputStat", Comparator.comparing(Connection::getInputStat));
            put("outputStat", Comparator.comparing(Connection::getOutputStat));
        }
    };

    private final ConnectionManager connectionManager;

    public MessageController(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @GetMapping("/device/{deviceId}")
    public ResponseEntity<Object> detail(@PathVariable ("deviceId") String deviceId) {
        if (!connectionManager.getStore().containsKey(deviceId)) {
            return ResponseEntity.notFound().build();
        }

        ConnectionBean bean = ConnectionBean.build(connectionManager.getStore().get(deviceId));
        return ResponseEntity.ok(bean);
    }

    @DeleteMapping("/device/{deviceId}")
    public Object delete(@PathVariable("deviceId") String deviceId,
                         @RequestParam(required = false, defaultValue = "false") boolean force) {
        if (!connectionManager.getStore().containsKey(deviceId)) {
            return  ResponseEntity.notFound().build();
        }

        Connection device = connectionManager.getStore().get(deviceId);
        if (device.getChannel().isActive() && !force) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body("can not delete connecting device");
        }

        if (device.getChannel().isActive()) {
           connectionManager.close(device);
        }
        connectionManager.getStore().remove(deviceId);

        return ConnectionBean.build(device);
    }

    @RequestMapping("/list")
    public Object list(@RequestParam(value = "sort", defaultValue = "deviceId") String sort,
                                   @RequestParam(value = "order", defaultValue = "ASC") String order,
                                   @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
                                   @RequestParam(value = "pageSize", defaultValue = "100") int pageSize) {
        Comparator<Connection> comparator = COMPARATORS.containsKey(sort) ? COMPARATORS.get(sort) : COMPARATORS.get("default");
        comparator = order.equalsIgnoreCase("DESC") ? comparator.reversed() : comparator;

        List<Connection> list = connectionManager.getStore().values().stream()
                .map(c -> Connection.builder()
                        .deviceId(c.getDeviceId())
                        .lac(c.getLac())
                        .ci(c.getCi())
                        .channel(c.getChannel())
                        .idCode(c.getIdCode())
                        .inputStat(c.getInputStat())
                        .outputStat(c.getOutputStat())
                        .build())
                .sorted(comparator)
                .collect(Collectors.toList());

        int total = list.size();
        int start = Integer.max(0, (pageNo - 1) * pageSize);
        int size = Integer.max(0, Integer.min(pageSize, total - start));
        return new HashMap<String, Object> () {
            {
                put("total", total);
                put("data", list.subList(start, size).stream().map(ConnectionBean::build).collect(Collectors.toList()));
            }
        };
    }

    @RequestMapping("/statByField")
    public Map<String, Long> statByField(@RequestParam String field) {
        Collection<Connection> values = connectionManager.getStore().values();
        Function<Connection, String> getter = c -> {
            try {
                Object target = c;
                for (String f : field.split("[.]")) {
                    PropertyDescriptor pd = Objects.requireNonNull(BeanUtils.getPropertyDescriptor(
                            c.getClass(), f));
                    target = pd.getReadMethod().invoke(target);
                }

                return target.toString();
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

        if (connectionManager.getStore().get(deviceId).getOutputStatSyncs().size() > 3) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(
                    "too many request for this device: " + deviceId);
        }
        Connection conn = connectionManager.getStore().get(deviceId);
        if (!conn.getChannel().isActive()) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(
                    "Terminal is disconnected: " + deviceId);
        }

        SoltMachineMessage message;
        CountDownLatch  latch = new CountDownLatch(1);

        try {
            synchronized (conn.getChannel()) {
                conn.getOutputStatSyncs().add(latch);
                byte[] buffer = new byte[]{(byte) (request.getCtrl()), (byte) (0x01 - request.getCtrl()),
                        0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F};

                message = SoltMachineMessage.builder()
                        .header(conn.getHeader())
                        .index(conn.getIndex().getAndIncrement())
                        .idCode(conn.getIdCode())
                        .deviceId(deviceId)
                        .cmd((short) 3)
                        .data(buffer)
                        .build();
                ByteBuf buf = Unpooled.wrappedBuffer(SlotMachineInBoundHandler.encode(message));
                SlotMachineInBoundHandler.logBytebuf(buf, "sending control");
                conn.getChannel().writeAndFlush(buf).get();
            }
            boolean success = latch.await(20, TimeUnit.SECONDS);

            if (success) {
                return ResponseEntity.ok("Message sent: " + message);
            } else {
                return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body("request timeout 20 seconds");
            }
        } finally {
            conn.getOutputStatSyncs().remove(latch);
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
        if (!ch.isActive()) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(
                    "Terminal is disconnected: " + deviceId);
        }
        Connection conn = connectionManager.getStore().get(deviceId);
        synchronized (ch) {
            SoltMachineMessage message = SoltMachineMessage.builder()
                    .header(conn.getHeader())
                    .index(conn.getIndex().getAndIncrement())
                    .idCode(conn.getIdCode())
                    .deviceId(deviceId)
                    .cmd((short) 129)
                    .data(data.getBytes())
                    .build();
            ByteBuf buf = Unpooled.wrappedBuffer(SlotMachineInBoundHandler.encode(message));
            SlotMachineInBoundHandler.logBytebuf(buf, "sending ascii");
            ch.writeAndFlush(buf).get();
            return ResponseEntity.ok("Message sent: " + message);
        }
    }
}
