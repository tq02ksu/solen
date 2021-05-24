package top.fengpingtech.solen.server.netty;

import org.junit.Before;
import org.junit.Test;
import top.fengpingtech.solen.server.EventProcessor;
import top.fengpingtech.solen.server.config.ServerProperties;
import top.fengpingtech.solen.server.model.Event;

import java.util.List;

import static org.junit.Assert.*;

public class SolenNettyServerTest {

    private SolenNettyServer solenNettyServer;

    @Before
    public void prepare() {
        ServerProperties properties = new ServerProperties();
        properties.setPort(7889);
        properties.setEventProcessor(new EventProcessor() {
            @Override
            public void processEvents(List<Event> events) {
                System.out.println("receiving events: " + events);
            }
        });
        properties.setIoThreads(2);
        properties.setWorkerThreads(4);
        solenNettyServer = new SolenNettyServer(properties);
        solenNettyServer.start();
    }

    @Test
    public void test() {
        assertNotNull(solenNettyServer);
    }
}