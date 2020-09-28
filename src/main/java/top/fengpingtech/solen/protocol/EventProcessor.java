package top.fengpingtech.solen.protocol;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.fengpingtech.solen.bean.Coordinate;
import top.fengpingtech.solen.model.Connection;
import top.fengpingtech.solen.model.ConnectionAttribute;
import top.fengpingtech.solen.model.ConnectionStatus;
import top.fengpingtech.solen.model.Event;
import top.fengpingtech.solen.model.EventType;
import top.fengpingtech.solen.service.CoordinateTransformationService;
import top.fengpingtech.solen.service.EventRepository;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class EventProcessor extends ChannelDuplexHandler {
    private static final Logger logger = LoggerFactory.getLogger(EventProcessor.class);

    private final ConnectionManager connectionManager;
    private final EventRepository eventRepository;

    public EventProcessor(ConnectionManager connectionManager, EventRepository eventRepository) {
        this.connectionManager = connectionManager;
        this.eventRepository = eventRepository;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof SoltMachineMessage) {
            processEvent(ctx, (SoltMachineMessage) msg);
        } else {
            super.channelRead(ctx, msg);
        }

    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        try {
            if (msg instanceof SoltMachineMessage) {
                processEvent(ctx, (SoltMachineMessage) msg);
            }
        } finally {
            super.write(ctx, msg, promise);
        }
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        try {
            String deviceId = ctx.channel().attr(AttributeKey.<String>valueOf("DeviceId")).get();
            if (deviceId != null) {
                Date d = new Date();
                eventRepository.add(
                        Event.builder()
                                .deviceId(deviceId)
                                .type(EventType.DISCONNECT)
                                .time(d)
                                .build());
                eventRepository.add(
                        Event.builder()
                                .deviceId(deviceId)
                                .type(EventType.STATUS_UPDATE)
                                .time(d)
                                .details(Collections.singletonMap("status", ConnectionStatus.DISCONNECTED.name()))
                                .build());
            }
        } finally {
            super.channelUnregistered(ctx);
        }
    }

    private void processEvent(ChannelHandlerContext ctx, SoltMachineMessage msg) {
        Attribute<Object> attr = ctx.channel().attr(AttributeKey.valueOf("Event-Skipped"));
        if (attr.get() != null) {
            return;
        }

        Map<String, String> details;
        Connection conn;
        switch (msg.getCmd()) {
            case 0:
                Date d = new Date();
                eventRepository.add(
                        Event.builder()
                                .deviceId(msg.getDeviceId())
                                .type(EventType.CONNECT)
                                .time(d)
                                .build());
                eventRepository.add(
                        Event.builder()
                                .deviceId(msg.getDeviceId())
                                .type(EventType.STATUS_UPDATE)
                                .time(d)
                                .details(Collections.singletonMap("status", ConnectionStatus.NORMAL.name()))
                                .build());
                break;
            case 1:
                conn = connectionManager.getStore().get(msg.getDeviceId());
                if (conn != null) {
                    ConnectionAttribute currentAttribute = new ConnectionAttribute(conn);
                    ConnectionAttribute beforeAttribute = ctx.channel().attr(
                            AttributeKey.<ConnectionAttribute>valueOf("ConnectionAttribute")).get();

                    details = new HashMap<>();
                    currentAttribute.forEach((key, val) -> {
                        if (!beforeAttribute.containsKey(key) || !beforeAttribute.get(key).equals(val)) {
                            details.put(key, val);
                        }
                    });
                    if (!details.isEmpty()) {
                        eventRepository.add(
                                Event.builder()
                                        .deviceId(msg.getDeviceId())
                                        .type(EventType.ATTRIBUTE_UPDATE)
                                        .details(details)
                                        .time(new Date())
                                        .build());
                    }
                } else {
                    logger.warn("connection not found, skipped for event process {}", msg);
                }

                break;
            case 3:
                details = Collections.singletonMap("ctrl", String.valueOf(msg.getData()[0]));
                eventRepository.add(
                        Event.builder()
                                .deviceId(msg.getDeviceId())
                                .type(EventType.CONTROL_SENDING)
                                .details(details)
                                .time(new Date())
                                .build());
                break;
            case 5:
                conn = connectionManager.getStore().get(msg.getDeviceId());
                if (conn != null) {
                    details = new HashMap<>();
                    details.put("lat", String.valueOf(conn.getCoordinate().getLat()));
                    details.put("lng", String.valueOf(conn.getCoordinate().getLng()));
                    CoordinateTransformationService transform = new CoordinateTransformationService();
                    Coordinate bd09 = transform.wgs84ToBd09(conn.getCoordinate());
                    details.put("bd09Lat", String.valueOf(bd09.getLat()));
                    details.put("bd09Lng", String.valueOf(bd09.getLng()));
                    Coordinate gcj02 = transform.wgs84ToGcj02(conn.getCoordinate());
                    details.put("gcj02Lat", String.valueOf(gcj02.getLat()));
                    details.put("gcj02Lng", String.valueOf(gcj02.getLng()));

                    eventRepository.add(
                            Event.builder()
                                    .deviceId(msg.getDeviceId())
                                    .type(EventType.LOCATION_CHANGE)
                                    .time(new Date())
                                    .details(details)
                                    .build());
                } else {
                    logger.warn("connection not found, skipped for event process {}", msg);
                }

                break;
            case 128:
                conn = connectionManager.getStore().get(msg.getDeviceId());
                if (conn == null) {
                    d = new Date();
                } else {
                    Connection.Report r = conn.getReports().get(0);
                    d = r.getTime();
                }

                details = Collections.singletonMap("content", new String(msg.getData(), StandardCharsets.UTF_8));
                eventRepository.add(
                    Event.builder()
                            .deviceId(msg.getDeviceId())
                            .type(EventType.MESSAGE_RECEIVING)
                            .time(d)
                            .details(details)
                            .build());
                break;
            case 129:
                d = new Date();
                details = Collections.singletonMap("content", new String(msg.getData(), StandardCharsets.UTF_8));
                eventRepository.add(
                        Event.builder()
                                .deviceId(msg.getDeviceId())
                                .type(EventType.MESSAGE_SENDING)
                                .time(d)
                                .details(details)
                                .build());
                break;
            default:
                // do nothing
        }
    }
}
