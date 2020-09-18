package top.fengpingtech.solen.slotmachine;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.util.AttributeKey;
import top.fengpingtech.solen.model.Connection;
import top.fengpingtech.solen.model.ConnectionAttribute;

import java.util.List;

public class ConnectionAttributeHolder extends MessageToMessageEncoder<SoltMachineMessage> {
    private final ConnectionManager connectionManager;

    public ConnectionAttributeHolder(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, SoltMachineMessage msg, List<Object> out) throws Exception {
        out.add(msg);

        String deviceId = msg.getDeviceId();
        Connection conn = connectionManager.getStore().get(deviceId);
        if (conn == null) {
            return;
        }

        ConnectionAttribute connectionAttribute = new ConnectionAttribute(conn);
        ctx.channel().attr(AttributeKey.<ConnectionAttribute>valueOf("ConnectionAttribute")).set(connectionAttribute);
    }
}
