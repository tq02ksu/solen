package top.fengpingtech.solen.app.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import top.fengpingtech.solen.app.domain.*;
import top.fengpingtech.solen.app.repository.ConnectionRepository;
import top.fengpingtech.solen.app.repository.DeviceRepository;
import top.fengpingtech.solen.app.repository.EventRepository;
import top.fengpingtech.solen.server.EventProcessor;
import top.fengpingtech.solen.server.model.*;

import java.util.*;
import java.util.stream.Collectors;

import static top.fengpingtech.solen.app.domain.CoordinateSystem.WGS84;

@Service
public class EventProcessorImpl implements EventProcessor {
    private static final Logger logger = LoggerFactory.getLogger(EventProcessorImpl.class);

    private final DeviceRepository deviceRepository;

    private final ConnectionRepository connectionRepository;

    private final EventRepository eventRepository;

    private final TransactionTemplate transactionTemplate;

    public EventProcessorImpl(DeviceRepository deviceRepository,
                              ConnectionRepository connectionRepository, EventRepository eventRepository,
                              TransactionTemplate transactionTemplate) {
        this.deviceRepository = deviceRepository;
        this.connectionRepository = connectionRepository;
        this.eventRepository = eventRepository;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public void processEvents(List<Event> events) {
        Map<String, List<Event>> groups = events.stream().collect(Collectors.groupingBy(Event::getDeviceId));

        for (Map.Entry<String, List<Event>> entry : groups.entrySet()) {
            for (int i = 0; i < 5; i++) {
                try {
                    transactionTemplate.execute(action -> {
                        processEventsInternal(entry.getValue());
                        return null;
                    });
                    return;
                } catch (Throwable e) {
                    logger.error("error while process events with take {}: {}", i, events, e);
                }
            }
        }
    }

    public void processEventsInternal(List<Event> events) {
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
                case STATUS_UPDATE:
                    eventDomain = processStatusUpdate((StatusEvent) event);
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

    private EventDomain processStatusUpdate(StatusEvent event) {
        Optional<DeviceDomain> optionalDeviceDomain = deviceRepository.findById(event.getDeviceId());

        if (!optionalDeviceDomain.isPresent()) {
            return null;
        }

        DeviceDomain device = optionalDeviceDomain.get();

        return EventDomain.builder()
                .eventId(event.getEventId())
                .device(device)
                .time(event.getTime())
                .type(event.getType())
                .details(Collections.singletonMap("status", device.getStatus().name()))
                .build();
    }

    private EventDomain processLocationChange(LocationEvent event) {
        Optional<DeviceDomain> optionalDeviceDomain = deviceRepository.findById(event.getDeviceId());

        if (optionalDeviceDomain.isPresent()) {
            Coordinate coordinate = new Coordinate(WGS84, event.getLng(), event.getLat());
            DeviceDomain device = optionalDeviceDomain.get();
            if (!event.getLat().equals(device.getLat())
                    || !event.getLng().equals(device.getLng())) {
                device.setLng(event.getLng());
                device.setLat(event.getLat());
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

        if (deviceDomain.getStatus() != ConnectionStatus.NORMAL) {
            details.put("status", ConnectionStatus.NORMAL.name());
            deviceDomain.setStatus(ConnectionStatus.NORMAL);
        }

        if (event.getInputStat() != null && !event.getInputStat().equals(deviceDomain.getInputStat())) {
            details.put("inputStat", String.valueOf(event.getInputStat()));
            deviceDomain.setInputStat(event.getInputStat());
        }

        if (event.getOutputStat() != null && !event.getOutputStat().equals(deviceDomain.getOutputStat())) {
            details.put("outputStat", String.valueOf(event.getOutputStat()));
            deviceDomain.setOutputStat(event.getOutputStat());
        }

        if (event.getRssi() != null && !event.getRssi().equals(deviceDomain.getRssi())) {
//            details.put("rssi", String.valueOf(event.getRssi()));
            deviceDomain.setRssi(event.getRssi());
        }

        if (event.getVoltage() != null && !event.getVoltage().equals(deviceDomain.getVoltage())) {
//            details.put("voltage", String.valueOf(event.getVoltage()));
            deviceDomain.setVoltage(event.getVoltage());
        }

        if (event.getTemperature() != null && !event.getTemperature().equals(deviceDomain.getTemperature())) {
            details.put("temperature", String.valueOf(event.getTemperature()));
            deviceDomain.setTemperature(event.getTemperature());
        }

        if (event.getGravity() != null && !event.getGravity().equals(deviceDomain.getGravity())) {
//            details.put("gravity", String.valueOf(event.getGravity()));
            deviceDomain.setGravity(event.getGravity());
        }

        if (event.getUptime() != null && !event.getUptime().equals(deviceDomain.getUptime())) {
//            details.put("uptime", String.valueOf(event.getUptime()));
            deviceDomain.setUptime(event.getUptime());
        }

        if (event.getIccId() != null && !event.getIccId() .equals(deviceDomain.getIccId())) {
//            details.put("iccId", event.getIccId());
            deviceDomain.setIccId(event.getIccId());
        }

        if (details.isEmpty()) {
            return null;
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
        DeviceDomain deviceDomain = device.orElse(null);

        if (deviceDomain == null) {
            return null;
        }

        if (event.getType() == EventType.MESSAGE_RECEIVING
                && deviceDomain.getStatus() != ConnectionStatus.NORMAL) {
            deviceDomain.setStatus(ConnectionStatus.NORMAL);
            deviceRepository.save(deviceDomain);
        }

        String key = event.getType() == EventType.CONTROL_SENDING ? "ctrl" : "content";
        return EventDomain.builder()
                .eventId(event.getEventId())
                .time(event.getTime())
                .type(event.getType())
                .device(deviceDomain)
                .details(Collections.singletonMap(key, event.getMessage()))
                .build();
    }

    private EventDomain processDisconnect(Event event) {
        Optional<DeviceDomain> device = deviceRepository.findById(event.getDeviceId());

        if (!device.isPresent()) {
            return null;
        }

        List<ConnectionDomain> connections = connectionRepository.findByDevice(device.get());
        // delete current connection
        connections.stream().filter(c -> c.getConnectionId().equals(event.getConnectionId()))
                .forEach(connectionRepository::delete);
        boolean statusChanged = connections.size() == 1;
        DeviceDomain deviceDomain = device.get();
        if (statusChanged) {
            deviceDomain.setStatus(ConnectionStatus.DISCONNECTED);
            deviceRepository.save(deviceDomain);
        }

        return statusChanged ? EventDomain.builder()
                .device(deviceDomain)
                .type(event.getType())
                .time(event.getTime())
                .eventId(event.getEventId())
                .build() : null;
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
        deviceDomain.setLac(event.getLac());
        deviceDomain.setCi(event.getCi());
        deviceRepository.save(deviceDomain);
        // connection
        ConnectionDomain domain = connection.get();
        domain.setConnectionId(event.getConnectionId());
        domain.setDevice(deviceDomain);
        connectionRepository.save(domain);

        return EventDomain.builder()
                        .eventId(event.getEventId())
                        .device(deviceDomain)
                        .type(event.getType())
                        .time(event.getTime())
                        .build();
    }
}
