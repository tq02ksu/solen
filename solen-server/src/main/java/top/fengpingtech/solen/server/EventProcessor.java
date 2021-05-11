package top.fengpingtech.solen.server;

import top.fengpingtech.solen.server.model.Event;

import java.util.List;

public interface EventProcessor {
    void processEvents(List<Event> events);
}
