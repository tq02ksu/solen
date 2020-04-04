package top.fengpingtech.solen.slotmachine;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageDebugger extends SimpleChannelInboundHandler<ByteBuf> {
    private static final Logger logger = LoggerFactory.getLogger(MessageDebugger.class);
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        if (logger.isDebugEnabled()) {
            logBytebuf(msg, "decode message");
        }
        msg.retain();
        ctx.fireChannelRead(msg);
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
        return (byte)  Integer.reverse(((int) b) <<24);
    }
}
