package top.fengpingtech.solen.controller;

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
import top.fengpingtech.solen.auth.AuthService;
import top.fengpingtech.solen.bean.ConnectionBean;
import top.fengpingtech.solen.model.Connection;
import top.fengpingtech.solen.model.DeviceAuth;
import top.fengpingtech.solen.model.Tenant;
import top.fengpingtech.solen.protocol.ConnectionManager;
import top.fengpingtech.solen.protocol.DeviceStorageCodec;
import top.fengpingtech.solen.protocol.SoltMachineMessage;
import top.fengpingtech.solen.service.AntMatchService;
import top.fengpingtech.solen.service.CoordinateTransformationService;

import java.beans.PropertyDescriptor;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
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
public class DeviceController {
    private static final Logger logger = LoggerFactory.getLogger(DeviceController.class);

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

    private final AntMatchService antMatchService;

    private final AuthService authService;

    private final ConnectionManager connectionManager;

    private final CoordinateTransformationService coordinateTransformationService;

    public DeviceController(AntMatchService antMatchService, AuthService authService,
                            ConnectionManager connectionManager,
                            CoordinateTransformationService coordinateTransformationService) {
        this.antMatchService = antMatchService;
        this.authService = authService;
        this.connectionManager = connectionManager;
        this.coordinateTransformationService = coordinateTransformationService;
    }

    @GetMapping("/device/{deviceId}")
    public ResponseEntity<Object> detail(
            @RequestHeader(name = "Authorization-Principal", required = false) String appKey,
            @PathVariable ("deviceId") String deviceId) {

        if (!connectionManager.getStore().containsKey(deviceId)) {
            return ResponseEntity.notFound().build();
        }

        Connection conn = connectionManager.getStore().get(deviceId);
        Tenant tenant = authService.getTenant(appKey);
        if (!authService.canVisit(tenant, conn)) {
            return ResponseEntity.status(401).body("unauthorized!");
        }

        ConnectionBean bean = buildBean(connectionManager.getStore().get(deviceId));
        return ResponseEntity.ok(bean);
    }

    /**
     * 设备绑定
     *
     * @param appKey
     * @param deviceId
     * @param auth
     * @return
     */
    @PostMapping("/device/{deviceId}/auth")
    public ResponseEntity<Object> modifyAuth(
            @RequestHeader(name = "Authorization-Principal", required = false) String appKey,
            @PathVariable("deviceId") String deviceId, @RequestBody DeviceAuth auth) {
        List<String> owners = auth.getOwners();
        if (appKey == null && owners == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("user not login and owners can not be both null");
        }

        if (appKey != null) {
            Tenant tenant = authService.getTenant(appKey);
            if (!tenant.getRoles().contains(AuthService.ROLE_ADMIN) && owners != null
                    && owners.stream().anyMatch(r -> !r.equals(appKey))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("can not authorize to other user");
            }
        }

        if (!connectionManager.getStore().containsKey(deviceId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("device not found");
        }

        Connection conn = connectionManager.getStore().get(deviceId);
        List<String> current = conn.getAuth().getOwners();
        if ((owners == null && current.contains(appKey) && current.size() == 1) ||
                (owners != null && current.containsAll(owners) && owners.containsAll(current))) {
            return ResponseEntity.ok(buildBean(conn));
        }

        List<String> authorizing;
        if (owners == null) {
            authorizing = new ArrayList<>(current);
            authorizing.add(appKey);
        } else {
            authorizing = owners;
        }

        // write flash
        // 1. construct
        byte[] flashData = new DeviceStorageCodec().encode(
                Connection.builder().auth(DeviceAuth.builder().owners(authorizing).build()).build());
        SoltMachineMessage msg = SoltMachineMessage.builder()
                .header(conn.getHeader())
                .index(conn.getIndex().getAndIncrement())
                .deviceId(deviceId)
                .cmd((short) 7)
                .data(flashData)
                .build();

        // build hook
        Connection.WriteFlashHook hook = Connection.WriteFlashHook.builder()
                .latch(new CountDownLatch(1))
                .build();
        synchronized (conn.getCtx().channel()) {
            try {
                conn.getWriteFlashSyncs().add(hook);
                conn.getCtx().writeAndFlush(msg);
                boolean success = hook.getLatch().await(20, TimeUnit.SECONDS);
                if (success && hook.getResult() != null && hook.getResult()) {
                    return ResponseEntity.ok().body(buildBean(conn));
                } else if (success) {
                    // wait less than timeout duration, and return failure
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("flash write error, maybe try again later");
                }
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("wait write flash result timeout, maybe device not supported");
            } catch (InterruptedException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("wait write flash result interrupted");
            } finally {
                conn.getWriteFlashSyncs().remove(hook);
            }
        }
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
        if (conn.getCtx().channel().isActive() && !force) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body("can not delete connecting device");
        }

        Tenant tenant = authService.getTenant(appKey);
        if (!authService.canVisit(tenant, conn)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("can not operation this device!");
        }
        connectionManager.close(conn);
        connectionManager.getStore().remove(deviceId);

        return ResponseEntity.ok(buildBean(conn));
    }

    @RequestMapping("/list")
    public Object list(
            @RequestHeader(name = "Authorization-Principal", required = false) String appKey,
            @RequestParam(value = "deviceId", required = false) String deviceId,
            @RequestParam(value = "sort", defaultValue = "deviceId") String sort,
            @RequestParam(value = "order", defaultValue = "ASC") String order,
            @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
            @RequestParam(value = "pageSize", defaultValue = "100") int pageSize) {
        Tenant tenant = authService.getTenant(appKey);
        Comparator<Connection> comparator = COMPARATORS.containsKey(sort) ? COMPARATORS.get(sort) : COMPARATORS.get("default");
        comparator = order.equalsIgnoreCase("DESC") ? comparator.reversed() : comparator;
        List<String> patterns = authService.getPatterns(tenant, deviceId);

        List<Connection> list = connectionManager.getStore().values().stream()
                .filter(c -> antMatchService.antMatch(patterns, c.getDeviceId()))
                .collect(Collectors.toList());

        int total = list.size();
        int start = Integer.max(0, (pageNo - 1) * pageSize);
        List<ConnectionBean> data = list.stream()
                .sorted(comparator)
                .skip(start)
                .limit(pageSize)
                .map(this::buildBean)
                .peek(b -> b.setReports(null))
                .collect(Collectors.toList());
        return new HashMap<String, Object> () {
            {
                put("total", total);
                put("data", data);
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

        Tenant tenant = authService.getTenant(appKey);
        return values.stream()
                .filter(authService.filter(tenant))
                .collect(Collectors.groupingBy(getter, Collectors.counting()));
    }

    @PostMapping("/sendControl")
    public ResponseEntity<Object> sendControl(
            @RequestHeader(name = "Authorization-Principal", required = false) String appKey,
            @RequestBody SendRequest request) throws ExecutionException, InterruptedException {
        Tenant tenant = authService.getTenant(appKey);

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
        if (!conn.getCtx().channel().isActive()) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(
                    "Terminal is disconnected: " + deviceId);
        }

        SoltMachineMessage message;
        CountDownLatch latch = new CountDownLatch(1);

        try {
            synchronized (conn.getCtx().channel()) {
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

                logger.info("sending control: {}", message);
                conn.getCtx().pipeline().writeAndFlush(message).get();
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
        Tenant tenant = authService.getTenant(appKey);

        String deviceId = request.getDeviceId();
        String data = request.getData();

        if (data == null) {
            return ResponseEntity.badRequest().body("data can not be null");
        }

        if (deviceId == null) {
            return ResponseEntity.badRequest().body("deviceId can not be null");
        }

        if (!connectionManager.getStore().containsKey(deviceId)) {
            return ResponseEntity.notFound().build();
        }

        Channel ch = connectionManager.getStore().get(deviceId).getCtx().channel();
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

            logger.info("sending message: {}", message);
            conn.getCtx().pipeline().writeAndFlush(message).get();
            return ResponseEntity.ok("Message sent: " + message);
        }
    }

    private ConnectionBean buildBean(Connection connection) {
        ConnectionBean bean = ConnectionBean.build(connection);
        if (connection.getCoordinate() != null) {
            bean.setCoordinates(Arrays.asList(
                    connection.getCoordinate(),
                    coordinateTransformationService.wgs84ToBd09(connection.getCoordinate()),
                    coordinateTransformationService.wgs84ToGcj02(connection.getCoordinate())
            ));
        }
        return bean;
    }
}
