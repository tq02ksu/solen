package top.fengpingtech.solen.server.netty;

import top.fengpingtech.solen.server.EventProcessor;
import top.fengpingtech.solen.server.IdGenerator;
import top.fengpingtech.solen.server.config.ServerProperties;
import top.fengpingtech.solen.server.model.Event;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class SolenNettyServerMain {
    public static void main(String[] args) throws Exception {
        ServerProperties properties = new ServerProperties();
        properties.setPort(7889);
        properties.setEventProcessor(events -> System.out.println("receiving events: " + events));
//        properties.setEventIdGenerator(new IdGenerator() {
//            final AtomicLong counter = new AtomicLong(0);
//            @Override
//            public Long nextVal() {
//                return counter.getAndIncrement();
//            }
//        });
        properties.setEventIdGenerator(() -> 0L);
        properties.setIoThreads(2);
        properties.setWorkerThreads(4);
        SolenNettyServer solenNettyServer = new SolenNettyServer(properties);
        solenNettyServer.start();

        System.in.read();
    }
}
