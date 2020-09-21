package top.fengpingtech.solen.service;

import com.alipay.sofa.jraft.rhea.client.RheaIterator;
import com.alipay.sofa.jraft.rhea.client.RheaKVStore;
import com.alipay.sofa.jraft.rhea.storage.KVEntry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Service;
import top.fengpingtech.solen.model.Event;
import top.fengpingtech.solen.model.EventType;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
public class EventRepository {
    private static final Logger logger = LoggerFactory.getLogger(EventRepository.class);

    private static final int KEEPER_SCHEDULE_DELAY_SECONDS = 60;

    private static final long EVENT_STORE_RESERVE_SECONDS = 86400 * 3;

    private static final String SEQ_EVENT_ID = "seq:event:id";

    private final RheaKVStore store;

    private final ScheduledExecutorService storeKeeperExecutor;

    private final AntMatchService antMatchService;

    private final ObjectMapper objectMapper;

    public EventRepository(RheaKVStore store, AntMatchService antMatchService, ObjectMapper objectMapper) {
        this.store = store;
        this.antMatchService = antMatchService;
        this.objectMapper = objectMapper;
        CustomizableThreadFactory factory = new CustomizableThreadFactory();
        factory.setDaemon(true);
        factory.setThreadNamePrefix("EventStoreKeeper-");
        storeKeeperExecutor = Executors.newSingleThreadScheduledExecutor(factory);
        storeKeeperExecutor.scheduleWithFixedDelay(this::clean,
                KEEPER_SCHEDULE_DELAY_SECONDS / 2, KEEPER_SCHEDULE_DELAY_SECONDS, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void destroy() {
        storeKeeperExecutor.shutdown();
    }

    public void add(Event event) {
        Long id = store.bGetSequence(SEQ_EVENT_ID, 1).getStartValue();
        event.setEventId(id);
        String key = generateKey(event.getTime(), event.getDeviceId(), id);
        try {
            byte[] value = objectMapper.writeValueAsBytes(event);
            store.bPut(key, value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("error while encode json", e);
        }
    }

    public List<Event> find(Date startTime, Date endTime, List<String> patterns, Long startId, List<EventType> types,
                          int start, int limit) {
        startTime = startTime == null ? new Date(0) : startTime;
        endTime = endTime == null ? new Date(Long.MAX_VALUE) : endTime;
        String startKey = generateKey(startTime, null, null);
        String endKey = generateKey(endTime, null, null);
        RheaIterator<KVEntry> it = store.iterator(endKey, startKey, 100);
        Iterator<KVEntry> iterator = new Iterator<KVEntry>() {
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public KVEntry next() {
                return it.next();
            }
        };

        Stream<Event> stream = StreamSupport.stream(
                Spliterators.spliterator(iterator, 100, Spliterator.ORDERED), false)
                .map(entry -> {
                    try {
                        return objectMapper.readValue(entry.getValue(), Event.class);
                    } catch (IOException e) {
                        logger.warn("error while read json: {}", new String(entry.getValue()), e);
                        return null;
                    }
                }).filter(Objects::nonNull);
        if (patterns != null && !patterns.isEmpty()) {
            stream = stream.filter(e -> antMatchService.antMatch(patterns, e.getDeviceId()));
        }

        if (types != null && !types.isEmpty()) {
            stream = stream.filter(e -> types.contains(e.getType()));
        }

        if (startId != null) {
            stream = stream.filter(e -> e.getEventId() < startId);
        }

        return stream
                .skip(start)
                .limit(limit)
                .collect(Collectors.toList());
    }

    private void clean() {
        Date deadline = new Date(System.currentTimeMillis() - EVENT_STORE_RESERVE_SECONDS * 1000);
        String endKey = generateKey(deadline, null, null);
        String startKey = generateKey(new Date(0), null, null);
        store.deleteRange(startKey, endKey);
    }

    /**
     *
     * @param date
     * @param deviceId
     * @param eventId
     * @return
     */
    String generateKey(Date date, String deviceId, Long eventId) {
        if (date == null) {
            return null;
        }
        if (deviceId == null) {
            deviceId = "";
        }
        if (eventId == null) {
            eventId = 0L;
        }
        return String.format("%016x:%11s:%d", ~date.getTime(), deviceId, eventId);
    }
}
