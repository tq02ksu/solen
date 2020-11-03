package top.fengpingtech.solen.service;

import org.junit.Test;

import java.util.Date;

public class EventRepositoryTest {


    @Test
    public void testGenerateKey() {
        EventRepository eventRepository = new EventRepository(null, null, null);

        Date d = new Date();
        String deviceId = "";
        Long eventId = 0L;
        String key = eventRepository.generateKey(d, deviceId, eventId);
        System.out.println(key);

    }
    
    @Test
    public void test() throws Exception {
        System.out.println(String.format("%016x", ~new Date().getTime()));
        Thread.sleep(10);
        System.out.println(String.format("%016x", ~new Date().getTime()));
        Thread.sleep(16);
        System.out.println(String.format("%016x", ~new Date().getTime()));
        System.out.println(String.format("%16s", "as"));
    }

}
