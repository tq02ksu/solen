package com.neusoft.solen.slotmachine;

import io.netty.channel.ChannelOutboundHandlerAdapter;

public class SlotMachineOutBoundHandler extends ChannelOutboundHandlerAdapter {

    private final ConnectionManager connectionManager;

    public SlotMachineOutBoundHandler(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }


}
