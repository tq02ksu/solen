package top.fengpingtech.solen.app.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import top.fengpingtech.solen.app.domain.*;
import top.fengpingtech.solen.app.repository.ConnectionRepository;
import top.fengpingtech.solen.app.repository.DeviceRepository;
import top.fengpingtech.solen.app.repository.EventRepository;
import top.fengpingtech.solen.server.EventProcessor;
import top.fengpingtech.solen.server.model.*;

import javax.transaction.Transactional;
import java.util.*;

import static top.fengpingtech.solen.app.domain.CoordinateSystem.WGS84;

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
                    EventDomain eventDomain = processConnect((ConnectionEvent) event);
                    Optional.ofNullable(eventDomain).ifPresent(list::add);
                    break;
                case DISCONNECT:
                    eventDomain = processDisconnect(event);
                    Optional.ofNullable(eventDomain).ifPresent(list::add);
                    break;
                case ATTRIBUTE_UPDATE:
                    eventDomain = processAttributeUpdate((AttributeEvent) event);
                    Optional.ofNullable(eventDomain).ifPresent(list::add);
                    break;
                case CONTROL_SENDING:
                case MESSAGE_RECEIVING:
                case MESSAGE_SENDING:
                    eventDomain = processMessageEvent((MessageEvent) event);
                    Optional.ofNullable(eventDomain).ifPresent(list::add);
                    break;
                case LOCATION_CHANGE:
                    eventDomain = processLocationChange((LocationEvent) event);
                    Optional.ofNullable(eventDomain).ifPresent(list::add);
                    break;
                default:
                    throw new IllegalStateException("unknown event type");
            }
        }

        eventRepository.saveAll(list);
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
                    .device(device)
                    .time(event.getTime())
                    .type(event.getType())
                    .details(details)
                    .build();
        }

        return null;
    }

    private EventDomain processAttributeUpdate(AttributeEvent event) {
        Optional<DeviceDomain> device = deviceRepository.findById(event.getDeviceId());

        if (!device.isPresent()) {
            return null;
        }

        DeviceDomain deviceDomain = device.get();

        Map<String, String> details = new HashMap<>();

        if (!event.getInputStat().equals(deviceDomain.getInputStat())) {
            details.put("inputStat", String.valueOf(event.getInputStat()));
            deviceDomain.setInputStat(event.getInputStat());
        }

        if (!event.getOutputStat().equals(deviceDomain.getOutputStat())) {
            details.put("outputStat", String.valueOf(event.getOutputStat()));
            deviceDomain.setOutputStat(event.getOutputStat());
        }

        if (!event.getRssi().equals(deviceDomain.getRssi())) {
            details.put("rssi", String.valueOf(event.getRssi()));
            deviceDomain.setRssi(event.getRssi());
        }

        if (!event.getVoltage().equals(deviceDomain.getVoltage())) {
            details.put("voltage", String.valueOf(event.getVoltage()));
            deviceDomain.setVoltage(event.getVoltage());
        }

        if (!event.getTemperature().equals(deviceDomain.getTemperature())) {
            details.put("temperature", String.valueOf(event.getTemperature()));
            deviceDomain.setTemperature(event.getTemperature());
        }

        if (!event.getGravity().equals(deviceDomain.getGravity())) {
            details.put("gravity", String.valueOf(event.getGravity()));
            deviceDomain.setGravity(event.getGravity());
        }

        if (!event.getUptime().equals(deviceDomain.getUptime())) {
            details.put("uptime", String.valueOf(event.getUptime()));
            deviceDomain.setUptime(event.getUptime());
        }

        deviceRepository.save(deviceDomain);

        return EventDomain.builder()
                .eventId(event.getEventId())
                .time(event.getTime())
                .type(event.getType())
                .details(details)
                .device(deviceDomain)
                .build();
    }

    private EventDomain processMessageEvent(MessageEvent event) {
        Optional<DeviceDomain> device = deviceRepository.findById(event.getDeviceId());
        String key = event.getType() == EventType.CONTROL_SENDING ? "ctrl" : "content";
        return device.map(deviceDomain ->
                EventDomain.builder()
                        .eventId(event.getEventId())
                        .type(event.getType())
                        .device(deviceDomain)
                        .details(Collections.singletonMap(key, event.getMessage()))
                        .build())
                .orElse(null);
    }

    private EventDomain processDisconnect(Event event) {
        Optional<DeviceDomain> device = deviceRepository.findById(event.getDeviceId());

        if (!device.isPresent()) {
            return null;
        }

        connectionRepository.deleteById(event.getConnectionId());
        List<ConnectionDomain> connections = connectionRepository.findByDevice(device.get());
        DeviceDomain deviceDomain = device.get();
        if (connections.isEmpty()) {
            deviceDomain.setStatus(ConnectionStatus.DISCONNECTED);
        }

        deviceRepository.save(deviceDomain);
        connectionRepository.deleteById(event.getConnectionId());

        if (connections.isEmpty()) {
            return EventDomain.builder()
                    .device(deviceDomain)
                    .type(event.getType())
                    .time(event.getTime())
                    .eventId(event.getEventId())
                    .build();

//                    EventDomain.builder()
//                            .device(deviceDomain)
//                            .type(EventType.ATTRIBUTE_UPDATE)
//                            .time(event.getTime())
//                            .eventId(event.getEventId())
//                            .details(Collections.singletonMap("status", ConnectionStatus.DISCONNECTED.name()))
//                            .build()
        }

        return null;
    }

    private EventDomain processConnect(ConnectionEvent event) {
        String connectionId = event.getConnectionId();
        Optional<ConnectionDomain> connection = connectionRepository.findById(connectionId);

        if (!connection.isPresent()) {
            connection = Optional.of(ConnectionDomain.builder().build());
        }

        Optional<DeviceDomain> device = deviceRepository.findById(event.getDeviceId());
        DeviceDomain deviceDomain = device.orElseGet(() -> DeviceDomain.builder()
                .deviceId(event.getDeviceId())
                .build());
        deviceDomain.setStatus(ConnectionStatus.NORMAL);
        deviceRepository.save(deviceDomain);
        // connection
        ConnectionDomain domain = connection.get();
        domain.setDevice(deviceDomain);
        domain.setLac(event.getLac());
        domain.setCi(event.getCi());
        connectionRepository.save(domain);

        return EventDomain.builder()
                        .eventId(event.getEventId())
                        .device(deviceDomain)
                        .type(event.getType())
                        .time(event.getTime())
                        .build();
//                EventDomain.builder()
//                        .eventId(event.getEventId())
//                        .device(deviceDomain)
//                        .type(EventType.ATTRIBUTE_UPDATE)
//                        .time(event.getTime())
//                        .details(Collections.singletonMap("status", ConnectionStatus.NORMAL.name()))
//                        .build()
    }
}
