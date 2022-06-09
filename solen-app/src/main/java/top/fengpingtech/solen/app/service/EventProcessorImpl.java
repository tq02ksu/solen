package top.fengpingtech.solen.app.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import top.fengpingtech.solen.app.domain.ConnectionDomain;
import top.fengpingtech.solen.app.domain.DeviceDomain;
import top.fengpingtech.solen.app.domain.EventDomain;
import top.fengpingtech.solen.app.model.Coordinate;
import top.fengpingtech.solen.app.repository.ConnectionRepository;
import top.fengpingtech.solen.app.repository.DeviceRepository;
import top.fengpingtech.solen.app.repository.EventRepository;
import top.fengpingtech.solen.server.EventProcessor;
import top.fengpingtech.solen.server.model.ConnectionEvent;
import top.fengpingtech.solen.server.model.Event;
import top.fengpingtech.solen.server.model.LocationEvent;
import top.fengpingtech.solen.server.model.MessageEvent;

import javax.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;

import static top.fengpingtech.solen.app.model.CoordinateSystem.WGS84;

@Service
@Transactional
public class EventProcessorImpl implements EventProcessor {
    private static final Logger logger = LoggerFactory.getLogger(EventProcessorImpl.class);

    private final DeviceRepository deviceRepository;

    private final ConnectionRepository connectionRepository;

    private final EventRepository eventRepository;

    public EventProcessorImpl(DeviceRepository deviceRepository,
                              ConnectionRepository connectionRepository, EventRepository eventRepository) {
        this.deviceRepository = deviceRepository;
        this.connectionRepository = connectionRepository;
        this.eventRepository = eventRepository;
    }

    @Override
    public void processEvents(List<Event> events) {
        List<EventDomain> list = new ArrayList<>();

        for (Event event : events) {
            switch (event.getType()) {
                case CONNECT:
                    List<EventDomain> eventDomains = processConnect((ConnectionEvent) event);
                    list.addAll(eventDomains);
                    break;
                case DISCONNECT:
                    eventDomains = processDisconnect(event);
                    list.addAll(eventDomains);
                    break;
                case CONTROL_SENDING:
                    eventDomain = processControlSend((MessageEvent) event);
                    if (eventDomain != null) {
                        list.add(eventDomain);
                    }
                    break;
                case ATTRIBUTE_UPDATE:
                    eventDomain = processAttributeUpdate((MessageEvent) event);
                    break;
                case MESSAGE_RECEIVING:
                    break;
                case MESSAGE_SENDING:
                    break;
                case LOCATION_CHANGE:
                    eventDomain = processLocationChange(event);
                    if (eventDomain != null) {
                        list.add(eventDomain);
                    }
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

    private EventDomain processLocationChange(LocationEvent event) {
        Optional<DeviceDomain> optionalDeviceDomain = deviceRepository.findById(event.getDeviceId());

        if (optionalDeviceDomain.isPresent()) {
            Coordinate coordinate = new Coordinate(WGS84, event.getLng(), event.getLat());
            DeviceDomain device = optionalDeviceDomain.get();
            if (!device.getCoordinate().getLat().equals(event.getLat())
                    || !device.getCoordinate().getLng().equals(event.getLng())) {
                device.setCoordinate(coordinate);
                deviceRepository.save(device);
            }

            Map<String, String> details = new HashMap<>();
            details.put("lat", String.valueOf(event.getLat()));
            details.put("lng", String.valueOf(event.getLng()));
            CoordinateTransformationService transform = new CoordinateTransformationService();
            Coordinate bd09 = transform.wgs84ToBd09(coordinate);
            details.put("bd09Lat", String.valueOf(bd09.getLat()));
            details.put("bd09Lng", String.valueOf(bd09.getLng()));
            Coordinate gcj02 = transform.wgs84ToGcj02(coordinate);
            details.put("gcj02Lat", String.valueOf(gcj02.getLat()));
            details.put("gcj02Lng", String.valueOf(gcj02.getLng()));

            return EventDomain.builder()
                    .eventId(event.getEventId())
                    .device(device.)
                    .time(event.getTime())
                    .type(event.getType())
                    .details(details)
                    .build();
        }
        return null;
    }

    private EventDomain processAttributeUpdate(MessageEvent event) {
        Optional<DeviceDomain> device = deviceRepository.findById(event.getDeviceId());
    }

    private EventDomain processControlSend(MessageEvent event) {
        Optional<DeviceDomain> device = deviceRepository.findById(event.getDeviceId());
        return device.map(deviceDomain -> EventDomain.builder()
                .eventId(event.getEventId())
                .type(event.getType())
                .device(deviceDomain)
                .details(Collections.singletonMap("ctrl", event.getMessage()))
                .build()).orElse(null);
    }

    private List<EventDomain> processDisconnect(Event event) {
        Optional<DeviceDomain> device = deviceRepository.findById(event.getDeviceId());

        if (!device.isPresent()) {
            return Collections.emptyList();
        }

        connectionRepository.deleteById(event.getConnectionId());
        List<ConnectionDomain> connections = connectionRepository.findByDevice(device.get());
        DeviceDomain deviceDomain = device.get();
        if (connections.isEmpty()) {
            deviceDomain.setStatus();
        }

    }

    private List<EventDomain> processConnect(ConnectionEvent event) {
        String connectionId = event.getConnectionId();
        Optional<ConnectionDomain> connection = connectionRepository.findById(connectionId);

        if (!connection.isPresent()) {
            connection = Optional.of(ConnectionDomain.builder().build());
        }

        Optional<DeviceDomain> device = deviceRepository.findById(event.getDeviceId());

        if (!device.isPresent()) {
            DeviceDomain deviceDomain = DeviceDomain.builder()
                    .deviceId(event.getDeviceId())
                    .build();
            deviceRepository.save(deviceDomain);
            device = Optional.of(deviceDomain);
        }
        // save
        ConnectionDomain domain = connection.get();
        domain.setDevice(device.get());
        domain.setLac(event.getLac());
        domain.setCi(event.getCi());
        domain = connectionRepository.save(domain);

        EventDomain.builder()
                .eventId(event.getEventId())
                .device(device.get())
                .type()
                .build();
    }
}
