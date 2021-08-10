package top.fengpingtech.solen.app.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import top.fengpingtech.solen.app.domain.EventDomain;
import top.fengpingtech.solen.app.repo.DeviceRepository;
import top.fengpingtech.solen.app.repo.EventRepository;
import top.fengpingtech.solen.server.EventProcessor;
import top.fengpingtech.solen.server.model.Event;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class EventService implements EventProcessor {
    private static final Logger logger = LoggerFactory.getLogger(EventService.class);

    private final DeviceRepository deviceRepository;

    private final EventRepository eventRepository;

    public EventService(DeviceRepository deviceRepository, EventRepository eventRepository) {
        this.deviceRepository = deviceRepository;
        this.eventRepository = eventRepository;
    }

    @Override
    public void processEvents(List<Event> events) {

        List<EventDomain> list = new ArrayList<>();

        for (Event event : events) {
            switch (event.getType()) {
                case CONNECT:

                    break;
                case CONTROL_SENDING:
                    break;
                case ATTRIBUTE_UPDATE:
                    break;
                case MESSAGE_RECEIVING:
                    break;
                case MESSAGE_SENDING:
                    break;
                case LOCATION_CHANGE:
                    break;
                default:
                    throw new IllegalStateException("unknown event type");
            }
        }

        eventRepository.saveAll(list);

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
//                break;
//            case 3:
//                details = Collections.singletonMap("ctrl", String.valueOf(msg.getData()[0]));
//                eventRepository.add(
//                        Event.builder()
//                                .deviceId(msg.getDeviceId())
//                                .type(EventType.CONTROL_SENDING)
//                                .details(details)
//                                .time(new Date())
//                                .build());
//                break;
//            case 5:
//                conn = connectionManager.getStore().get(msg.getDeviceId());
//                if (conn != null) {
//                    details = new HashMap<>();
//                    details.put("lat", String.valueOf(conn.getCoordinate().getLat()));
//                    details.put("lng", String.valueOf(conn.getCoordinate().getLng()));
//                    CoordinateTransformationService transform = new CoordinateTransformationService();
//                    Coordinate bd09 = transform.wgs84ToBd09(conn.getCoordinate());
//                    details.put("bd09Lat", String.valueOf(bd09.getLat()));
//                    details.put("bd09Lng", String.valueOf(bd09.getLng()));
//                    Coordinate gcj02 = transform.wgs84ToGcj02(conn.getCoordinate());
//                    details.put("gcj02Lat", String.valueOf(gcj02.getLat()));
//                    details.put("gcj02Lng", String.valueOf(gcj02.getLng()));
//
//                    eventRepository.add(
//                            Event.builder()
//                                    .deviceId(msg.getDeviceId())
//                                    .type(EventType.LOCATION_CHANGE)
//                                    .time(new Date())
//                                    .details(details)
//                                    .build());
//                } else {
//                    logger.warn("connection not found, skipped for event process {}", msg);
//                }
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
//                        Event.builder()
//                                .deviceId(msg.getDeviceId())
//                                .type(EventType.MESSAGE_RECEIVING)
//                                .time(d)
//                                .details(details)
//                                .build());
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
    }
}
