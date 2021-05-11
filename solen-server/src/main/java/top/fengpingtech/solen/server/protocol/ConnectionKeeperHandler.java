package top.fengpingtech.solen.server.protocol;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.ReadTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectionKeeperHandler extends ChannelDuplexHandler {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionKeeperHandler.class);

    public void exceptionCaught( ChannelHandlerContext ctx, Throwable cause)
              throws  Exception {
          if (cause instanceof ReadTimeoutException) {
              logger.info("{} read timeout, closing channel", ctx.channel().toString());
          } else {
              super.exceptionCaught(ctx, cause);
          }
      }
}
