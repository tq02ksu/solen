//package top.fengpingtech.solen.app.service;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.dataformat.smile.SmileFactory;
//import org.junit.Test;
//import solen.model.Event;
//import solen.model.EventType;
//
//import java.util.Arrays;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//public class EventRepositoryTest {
//    @Test
//    public void testSerialization() throws Exception {
//        Map<String, String> content = new HashMap<>();
//        content.put("content", "asdfasdfasdfa");
//        Event event = Event.builder()
//                .eventId(100L)
//                .deviceId("101010101010")
//                .type(EventType.MESSAGE_RECEIVING)
//                .time(new Date())
//                .details(content)
//                .build();
//        Event event2 = Event.builder()
//                .eventId(100L)
//                .deviceId("1010101010110")
//                .type(EventType.MESSAGE_RECEIVING)
//                .time(new Date())
//                .details(content)
//                .build();
//
//        Event event3 = Event.builder()
//                .eventId(100L)
//                .deviceId("10101010101101")
//                .type(EventType.STATUS_UPDATE)
//                .time(new Date())
//                .details(content)
//                .build();
//        List<Event> events = Arrays.asList(event, event2, event3, event, event2);
//
//        System.out.println(new ObjectMapper(new SmileFactory()).writeValueAsBytes(events));
//        System.out.println(new ObjectMapper().writeValueAsString(events));
//
//    }
//
//    @Test
//    public void testGenerateKey() {
//        EventRepository eventRepository = new EventRepository(null, null);
//
//        Date d = new Date();
//        String deviceId = "40620060088";
//        Long eventId = 0L;
//        byte[] key = eventRepository.generateIndexKey(d, deviceId, eventId, null);
//        System.out.println(new String(key));
//
//    }
//
//    @Test
//    public void test() throws Exception {
//        System.out.println(String.format("%016x", ~new Date().getTime()));
//        Thread.sleep(10);
//        System.out.println(String.format("%016x", ~new Date().getTime()));
//        Thread.sleep(16);
//        System.out.println(String.format("%016x", ~new Date().getTime()));
//        System.out.println(String.format("%16s", "as"));
//    }
//
//}
