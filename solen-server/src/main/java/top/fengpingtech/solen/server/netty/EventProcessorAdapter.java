package top.fengpingtech.solen.server.netty;

import io.netty.channel.ChannelDuplexHandler;
import top.fengpingtech.solen.server.EventProcessor;

public class EventProcessorAdapter extends ChannelDuplexHandler {
    private final EventProcessor delegate;

    public EventProcessorAdapter(EventProcessor delegate) {
        this.delegate = delegate;
    }

}
