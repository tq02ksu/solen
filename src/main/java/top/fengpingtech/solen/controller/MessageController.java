package top.fengpingtech.solen.controller;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.fengpingtech.solen.auth.AuthProperties;
import top.fengpingtech.solen.auth.AuthService;
import top.fengpingtech.solen.bean.ConnectionBean;
import top.fengpingtech.solen.model.Connection;
import top.fengpingtech.solen.model.Tenant;
import top.fengpingtech.solen.slotmachine.ConnectionManager;
import top.fengpingtech.solen.slotmachine.MessageDebugger;
import top.fengpingtech.solen.slotmachine.MessageEncoder;
import top.fengpingtech.solen.slotmachine.SoltMachineMessage;

import java.beans.PropertyDescriptor;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
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
            put("rssi", Comparator.comparing(Connection::getRssi));
            put("uptime", Comparator.comparing(Connection::getUptime));
        }
    };

    private final AuthProperties authProperties;

    private final AuthService authService;

    private final ConnectionManager connectionManager;

    public MessageController(AuthProperties authProperties, AuthService authService, ConnectionManager connectionManager) {
        this.authProperties = authProperties;
        this.authService = authService;
        this.connectionManager = connectionManager;
    }

    @GetMapping("/device/{deviceId}")
    public ResponseEntity<Object> detail(
            @RequestHeader(name = "Authorization-Principal", required = false) String appKey,
            @PathVariable ("deviceId") String deviceId) {

        if (!connectionManager.getStore().containsKey(deviceId)) {
            return ResponseEntity.notFound().build();
        }

        Connection conn = connectionManager.getStore().get(deviceId);
        Tenant tenant = getTenant(appKey);
        if (!authService.canVisit(tenant, conn)) {
            return ResponseEntity.status(401).body("unauthorized!");
        }

        ConnectionBean bean = ConnectionBean.build(connectionManager.getStore().get(deviceId));
        return ResponseEntity.ok(bean);
    }

    @DeleteMapping("/device/{deviceId}")
    public ResponseEntity<Object> delete(
            @RequestHeader(name = "Authorization-Principal", required = false) String appKey,
            @PathVariable("deviceId") String deviceId,
            @RequestParam(required = false, defaultValue = "false") boolean force) {
        if (!connectionManager.getStore().containsKey(deviceId)) {
            return  ResponseEntity.notFound().build();
        }

        Connection conn = connectionManager.getStore().get(deviceId);
        if (conn.getChannel().isActive() && !force) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body("can not delete connecting device");
        }

        Tenant tenant = getTenant(appKey);
        if (!authService.canVisit(tenant, conn)) {
            return ResponseEntity.status(401).body("unauthorized!");
        }
        connectionManager.close(conn);
        connectionManager.getStore().remove(deviceId);

        return ResponseEntity.ok(ConnectionBean.build(conn));
    }

    @RequestMapping("/list")
    public Object list(
            @RequestHeader(name = "Authorization-Principal", required = false) String appKey,
            @RequestParam(value = "sort", defaultValue = "deviceId") String sort,
                                   @RequestParam(value = "order", defaultValue = "ASC") String order,
                                   @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
                                   @RequestParam(value = "pageSize", defaultValue = "100") int pageSize) {
        Tenant tenant = getTenant(appKey);
        Comparator<Connection> comparator = COMPARATORS.containsKey(sort) ? COMPARATORS.get(sort) : COMPARATORS.get("default");
        comparator = order.equalsIgnoreCase("DESC") ? comparator.reversed() : comparator;

        List<Connection> list = connectionManager.getStore().values().stream()
                .filter(authService.filter(tenant))
                .map(c -> Connection.builder()
                        .deviceId(c.getDeviceId())
                        .lac(c.getLac())
                        .ci(c.getCi())
                        .channel(c.getChannel())
                        .idCode(c.getIdCode())
                        .inputStat(c.getInputStat())
                        .outputStat(c.getOutputStat())
                        .northLat(c.getNorthLat())
                        .eastLong(c.getEastLong())
                        .iccId(c.getIccId())
                        .build())
                .sorted(comparator)
                .collect(Collectors.toList());

        int total = list.size();
        int start = Integer.max(0, (pageNo - 1) * pageSize);
        int size = Integer.max(0, Integer.min(pageSize + start, total));
        return new HashMap<String, Object> () {
            {
                put("total", total);
                put("data",
                        list.subList(start, size).stream()
                                .map(ConnectionBean::build).collect(Collectors.toList()));
            }
        };
    }

    @RequestMapping("/statByField")
    public Map<String, Long> statByField(
            @RequestHeader(name = "Authorization-Principal", required = false) String appKey,
            @RequestParam String field) {
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

        Tenant tenant = getTenant(appKey);
        return values.stream()
                .filter(authService.filter(tenant))
                .collect(Collectors.groupingBy(getter, Collectors.counting()));
    }

    @PostMapping("/sendControl")
    public ResponseEntity<Object> sendControl(
            @RequestHeader(name = "Authorization-Principal", required = false) String appKey,
            @RequestBody SendRequest request) throws ExecutionException, InterruptedException {
        Tenant tenant = getTenant(appKey);

        String deviceId = request.getDeviceId();
        if (!connectionManager.getStore().containsKey(deviceId)) {
            return ResponseEntity.notFound().build();
        }

        Connection conn = connectionManager.getStore().get(deviceId);
        if (!authService.canVisit(tenant, conn)) {
            return ResponseEntity.status(401).body("unauthorized");
        }

        if (conn.getOutputStatSyncs().size() > 3) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(
                    "too many request for this device: " + deviceId);
        }
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

                MessageEncoder encoder = new MessageEncoder();
                ByteBuf buf = Unpooled.buffer();
                encoder.encode(message, buf);
                MessageDebugger.logByteBuf(buf, "sending control", conn.getChannel().toString());
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
    public ResponseEntity<Object> sendAscii(
            @RequestHeader(name = "Authorization-Principal", required = false) String appKey,
            @RequestBody SendRequest request) throws Exception {
        Tenant tenant = getTenant(appKey);

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

        if (!authService.canVisit(tenant, conn)) {
            return ResponseEntity.status(401).body("unauthorized");
        }

        synchronized (ch) {
            SoltMachineMessage message = SoltMachineMessage.builder()
                    .header(conn.getHeader())
                    .index(conn.getIndex().getAndIncrement())
                    .idCode(conn.getIdCode())
                    .deviceId(deviceId)
                    .cmd((short) 129)
                    .data(data.getBytes(StandardCharsets.UTF_8))
                    .build();
            MessageEncoder encoder = new MessageEncoder();
            ByteBuf buf = Unpooled.buffer();
            encoder.encode(message, buf);
            MessageDebugger.logByteBuf(buf, "sending ascii", ch.toString());
            ch.writeAndFlush(buf).get();
            return ResponseEntity.ok("Message sent: " + message);
        }
    }

    Tenant getTenant(String appKey) {
        if (appKey == null) {
            return null;
        }
        return authProperties.getTenants()
                .stream()
                .filter(t -> t.getAppKey().equalsIgnoreCase(appKey))
                .findFirst().orElseThrow(IllegalStateException::new);
    }
}
