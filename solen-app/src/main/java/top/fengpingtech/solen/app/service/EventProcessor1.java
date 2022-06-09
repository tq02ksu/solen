//package top.fengpingtech.solen.app.service;
//
//import io.netty.channel.ChannelDuplexHandler;
//import io.netty.channel.ChannelHandlerContext;
//import io.netty.channel.ChannelPromise;
//import io.netty.util.Attribute;
//import io.netty.util.AttributeKey;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.nio.charset.StandardCharsets;
//import java.util.Collections;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.Map;
//
//public class EventProcessor1 extends ChannelDuplexHandler {
//    private static final Logger logger = LoggerFactory.getLogger(EventProcessor1.class);
//
//    private final ConnectionManager connectionManager;
//    private final EventRepository eventRepository;
//
//    public EventProcessor1(ConnectionManager connectionManager, EventRepository eventRepository) {
//        this.connectionManager = connectionManager;
//        this.eventRepository = eventRepository;
//    }
//
//

//
//    @Override
//    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
//        try {
//            Attribute<Object> attr = ctx.channel().attr(AttributeKey.valueOf("Event-Skipped"));
//            if (attr.get() != null) {
//                return;
//            }
//
//            String deviceId = ctx.channel().attr(AttributeKey.<String>valueOf("DeviceId")).get();
//            if (deviceId != null) {
//                Date d = new Date();
//                eventRepository.add(
//                        Event.builder()
//                                .deviceId(deviceId)
//                                .type(EventType.DISCONNECT)
//                                .time(d)
//                                .build());
//                eventRepository.add(
//                        Event.builder()
//                                .deviceId(deviceId)
//                                .type(EventType.STATUS_UPDATE)
//                                .time(d)
//                                .details(Collections.singletonMap("status", ConnectionStatus.DISCONNECTED.name()))
//                                .build());
//            }
//        } finally {
//            super.channelUnregistered(ctx);
//        }
//    }
//
//    private void processEvent(ChannelHandlerContext ctx, SoltMachineMessage msg) {
//        Map<String, String> details;
//        Connection conn;
//        switch (msg.getCmd()) {
//            case 0:
//                Date d = new Date();
//                eventRepository.add(
//                        Event.builder()
//                                .deviceId(msg.getDeviceId())
//                                .type(EventType.CONNECT)
//                                .time(d)
//                                .build());
//                eventRepository.add(
//                        Event.builder()
//                                .deviceId(msg.getDeviceId())
//                                .type(EventType.STATUS_UPDATE)
//                                .time(d)
//                                .details(Collections.singletonMap("status", ConnectionStatus.NORMAL.name()))
//                                .build());
//                break;
//            case 1:
//                conn = connectionManager.getStore().get(msg.getDeviceId());
//                if (conn != null) {
//                    ConnectionAttribute currentAttribute = new ConnectionAttribute(conn);
//                    ConnectionAttribute beforeAttribute = ctx.channel().attr(
//                            AttributeKey.<ConnectionAttribute>valueOf("ConnectionAttribute")).get();
//
//                    details = new HashMap<>();
//                    currentAttribute.forEach((key, val) -> {
//                        if (!beforeAttribute.containsKey(key) || !beforeAttribute.get(key).equals(val)) {
//                            details.put(key, val);
//                        }
//                    });
//                    if (!details.isEmpty()) {
//                        eventRepository.add(
//                                Event.builder()
//                                        .deviceId(msg.getDeviceId())
//                                        .type(EventType.ATTRIBUTE_UPDATE)
//                                        .details(details)
//                                        .time(new Date())
//                                        .build());
//                    }
//                } else {
//                    logger.warn("connection not found, skipped for event process {}", msg);
//                }
//
//
//
//                break;
//            case 128:
//                conn = connectionManager.getStore().get(msg.getDeviceId());
//                if (conn == null) {
//                    d = new Date();
//                } else {
//                    Connection.Report r = conn.getReports().get(0);
//                    d = r.getTime();
//                }
//
//                details = Collections.singletonMap("content", new String(msg.getData(), StandardCharsets.UTF_8));
//                eventRepository.add(
//                    Event.builder()
//                            .deviceId(msg.getDeviceId())
//                            .type(EventType.MESSAGE_RECEIVING)
//                            .time(d)
//                            .details(details)
//                            .build());
//                break;
//            case 129:
//                d = new Date();
//                details = Collections.singletonMap("content", new String(msg.getData(), StandardCharsets.UTF_8));
//                eventRepository.add(
//                        Event.builder()
//                                .deviceId(msg.getDeviceId())
//                                .type(EventType.MESSAGE_SENDING)
//                                .time(d)
//                                .details(details)
//                                .build());
//                break;
//            default:
//                // do nothing
//        }
//    }
//}
