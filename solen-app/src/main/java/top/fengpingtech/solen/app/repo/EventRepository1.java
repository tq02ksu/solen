//package top.fengpingtech.solen.app.repo;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
//import org.springframework.stereotype.Service;
//import org.springframework.util.Assert;
//
//import javax.annotation.PreDestroy;
//import java.io.IOException;
//import java.nio.ByteBuffer;
//import java.util.Date;
//import java.util.List;
//import java.util.Spliterator;
//import java.util.Spliterators;
//import java.util.concurrent.Executors;
//import java.util.concurrent.ScheduledExecutorService;
//import java.util.concurrent.TimeUnit;
//import java.util.stream.Collectors;
//import java.util.stream.StreamSupport;
//
//@Service
//public class EventRepository1 {
//    private static final Logger logger = LoggerFactory.getLogger(EventRepository1.class);
//
//    private static final int KEEPER_SCHEDULE_DELAY_SECONDS = 60;
//
//    private static final long EVENT_STORE_RESERVE_SECONDS = 86400 * 3;
//
//    private final ScheduledExecutorService storeKeeperExecutor;
//
//    private final ObjectMapper objectMapper;
//
//    public EventRepository1() {
//        this.store = store;
//        this.objectMapper = new ObjectMapper();
//        CustomizableThreadFactory factory = new CustomizableThreadFactory();
//        factory.setDaemon(true);
//        factory.setThreadNamePrefix("EventStoreKeeper-");
//        storeKeeperExecutor = Executors.newSingleThreadScheduledExecutor(factory);
//        storeKeeperExecutor.scheduleWithFixedDelay(this::clean,
//                KEEPER_SCHEDULE_DELAY_SECONDS / 2, KEEPER_SCHEDULE_DELAY_SECONDS, TimeUnit.SECONDS);
//    }
//
//    @PreDestroy
//    public void destroy() {
//        storeKeeperExecutor.shutdown();
//    }
//
//    public void add(Event event) {
//        Long id = store.bGetSequence(SEQ_EVENT_ID, 1).getStartValue();
//        event.setEventId(id);
//        byte[] indexKey = generateIndexKey(event.getTime(), event.getDeviceId(), id, event.getType());
//        byte[] indexData = generateDataKey(id);
//        try {
//            byte[] data = objectMapper.writeValueAsBytes(event);
//            store.put(indexData, data);
//            store.put(indexKey, indexData);
//        } catch (JsonProcessingException e) {
//            throw new IllegalStateException("error while encode json", e);
//        }
//    }
//
//    private byte[] generateDataKey(Long id) {
//        ByteBuffer buffer = ByteBuffer.allocate(EVENT_DATA_PREFIX.length + 8);
//        buffer.put(EVENT_DATA_PREFIX);
//        buffer.putLong(id);
//        return buffer.array();
//    }
//
//    public List<Event> find(Date startTime, Date endTime, List<String> patterns, Long startId, List<EventType> types,
//                          int start, int limit) {
//        startTime = startTime == null ? new Date(0) : startTime;
//        endTime = endTime == null ? new Date(Long.MAX_VALUE) : endTime;
//        byte[] startKey = generateIndexKey(startTime, null, null, null);
//        byte[] endKey = generateIndexKey(endTime, null, null, null);
//        RheaIterator<KVEntry> it = store.iterator(endKey, startKey, 100);
//        Iterator<KVEntry> iterator = new Iterator<KVEntry>() {
//            @Override
//            public boolean hasNext() {
//                return it.hasNext();
//            }
//
//            @Override
//            public KVEntry next() {
//                return it.next();
//            }
//        };
//
//        Stream<EventIndex> stream = StreamSupport.stream(
//                Spliterators.spliterator(iterator, 100, Spliterator.ORDERED), false
//        )
//                .map(e -> readIndex(e.getKey(), e.getValue()))
//                .filter(e -> antMatchService.antMatch(patterns, e.getDeviceId()));
//
//        if (types != null && !types.isEmpty()) {
//            stream = stream.filter(e -> types.contains(e.getType()));
//        }
//
//        if (startId != null) {
//            stream = stream.filter(e -> e.getEventId() < startId);
//        }
//
//
//        List<byte[]> keys = stream
//                .skip(start)
//                .limit(limit)
//                .map(EventIndex::getDataKey).collect(Collectors.toList());
//        Map<ByteArray, byte[]> values = store.bMultiGet(keys);
//
//        return keys.stream()
//                .map(e -> values.get(ByteArray.wrap(e)))
//                .map(v -> {
//                    try {
//                        return objectMapper.readValue(v, Event.class);
//                    } catch (IOException e) {
//                        throw new IllegalStateException("error while decode", e);
//                    }
//                })
//                .collect(Collectors.toList());
//    }
//
//    private void clean() {
//        Date deadline = new Date(System.currentTimeMillis() - EVENT_STORE_RESERVE_SECONDS * 1000);
//        byte[] endKey = generateIndexKey(deadline, null, null, null);
//        byte[] startKey = generateIndexKey(new Date(0), null, null, null);
//        List<KVEntry> list = store.bScan(startKey, endKey);
//        store.delete(list.stream().map(KVEntry::getValue).collect(Collectors.toList()));
//        store.deleteRange(startKey, endKey);
//    }
//
//
//
//    /**
//     *
//     * @param date
//     * @param deviceId
//     * @param eventId
//     * @return
//     */
//    byte[] generateIndexKey(Date date, String deviceId, Long eventId, EventType type) {
//        if (date == null) {
//            return null;
//        }
//        if (deviceId == null) {
//            deviceId = "";
//        }jp
//        if (eventId == null) {
//            eventId = 0L;
//        }
//
//        ByteBuffer buffer = ByteBuffer.allocate(EVENT_INDEX_PREFIX.length + 8 + 11 + 8 + 2);
//        buffer.put(EVENT_INDEX_PREFIX);
//        buffer.putLong(~date.getTime());
//        buffer.put(String.format("%11s", deviceId).getBytes());
//        buffer.putLong(~eventId);
//        buffer.putShort(type == null ? -1 : (short) type.ordinal());
//        return buffer.array();
//    }
//
//    EventIndex readIndex(byte[] key, byte[] dataKey) {
//        ByteBuffer buffer = ByteBuffer.wrap(key);
//        byte[] header = new byte[2];
//        buffer.get(header);
//        Assert.isTrue(header[0] == EVENT_INDEX_PREFIX[0], "header invalid");
//        Assert.isTrue(header[1] == EVENT_INDEX_PREFIX[1], "header invalid");
//
//        long time = ~buffer.getLong();
//        byte[] deviceId = new byte[11];
//        buffer.get(deviceId);
//        long eventId = ~buffer.getLong();
//        short type = buffer.getShort();
//        return EventIndex.builder()
//                .deviceId(new String(deviceId))
//                .eventId(eventId)
//                .type(type == -1 ? null : EventType.values()[type])
//                .dataKey(dataKey)
//                .build();
//    }
//}
