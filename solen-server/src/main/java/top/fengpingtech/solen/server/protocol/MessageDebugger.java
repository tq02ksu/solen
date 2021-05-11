package top.fengpingtech.solen.server.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageDebugger extends ChannelDuplexHandler {
    private static final Logger logger = LoggerFactory.getLogger(MessageDebugger.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            if (logger.isDebugEnabled()) {
                logByteBuf((ByteBuf) msg, "receiving bytes", ctx.channel().toString());
            }
        }
        ctx.fireChannelRead(msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        if (msg instanceof ByteBuf) {
            if (logger.isDebugEnabled()) {
                logByteBuf((ByteBuf) msg, "sending bytes", ctx.channel().toString());

            }
        }
        ctx.write(msg, promise);
    }

    public static void logByteBuf(ByteBuf byteBuf, String comment, String channel) {
        if (logger.isDebugEnabled()) {
            StringBuilder tmp = new StringBuilder("0x");
            while (byteBuf.isReadable()) {
                tmp.append(String.format("%02x ", byteBuf.readByte() & 0xFF));
            }
            logger.info("{}: {} {}", channel, comment, tmp.toString());
            byteBuf.resetReaderIndex();
        }
    }
}
