package top.fengpingtech.solen.server.protocol;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.fengpingtech.solen.server.EventProcessor;
import top.fengpingtech.solen.server.IdGenerator;
import top.fengpingtech.solen.server.model.ConnectionEvent;
import top.fengpingtech.solen.server.model.EventType;

import java.util.Collections;

public class ConnectionKeeperHandler extends ChannelDuplexHandler {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionKeeperHandler.class);

    private final EventProcessor eventProcessor;

    private final IdGenerator idGenerator;

    public ConnectionKeeperHandler(EventProcessor eventProcessor, IdGenerator idGenerator) {
        this.eventProcessor = eventProcessor;
        this.idGenerator = idGenerator;
    }

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        if (cause instanceof ReadTimeoutException) {
            logger.info("{} read timeout, closing channel", ctx.channel().toString());
            String deviceId = ctx.channel().attr(AttributeKey.<String>valueOf("DeviceId")).get();
            ConnectionEvent event = new ConnectionEvent();
            event.setType(EventType.DISCONNECT);
            event.setEventId(idGenerator.nextVal());
            event.setDeviceId(deviceId);
            eventProcessor.processEvents(Collections.singletonList(event));
            ctx.channel().closeFuture();
        } else {
              super.exceptionCaught(ctx, cause);
          }
      }
}
