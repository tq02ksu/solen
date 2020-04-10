package top.fengpingtech.solen.slotmachine;

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
                logBytebuf((ByteBuf) msg, "receiving bytes");
            }
            ((ByteBuf) msg).retain();
        }
        ctx.fireChannelRead(msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        if (msg instanceof ByteBuf) {
            if (logger.isDebugEnabled()) {
                logBytebuf((ByteBuf) msg, "sending bytes");
                ((ByteBuf) msg).retain();
            }
        }
        ctx.write(msg, promise);
    }

    public static void logBytebuf(ByteBuf byteBuf, String comment) {
        if (logger.isDebugEnabled()) {
            StringBuilder tmp = new StringBuilder("0x");
            while (byteBuf.isReadable()) {
                tmp.append(String.format("%02x ", byteBuf.readByte() & 0xFF));
            }
            logger.debug(comment + "(le): {}", tmp.toString());
            byteBuf.resetReaderIndex();

            tmp = new StringBuilder("0x");

            while (byteBuf.isReadable()) {
                tmp.append(String.format("%02x ", reverse(byteBuf.readByte()) & 0xFF));
            }
            logger.trace(comment + "(be): {}", tmp.toString());
            byteBuf.resetReaderIndex();
        }
    }

    private static byte reverse(byte b) {
        return (byte) Integer.reverse(((int) b) <<24);
    }
}
