package top.fengpingtech.solen.slotmachine;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteOrder;

/**
 *
 * <pre>
 *     + ------------- + ------------------------------------ + ------------------------------- +
 *     | seg \ decode  | null                                 | not null                        |
 *     + ------------- + ------------------------------------ + ------------------------------- +
 *     |       seg = 0 | strip to header, decode, seg++       | strip to header, decode, return |
 *     + ------------- + ------------------------------------ + ------------------------------- +
 *     |           1-2 | seg ++                               | seg = 0, return                 |
 *     + ------------- + ------------------------------------ + ------------------------------- +
 *     |           >=3 | readByte(), strip to header, seg = 0 | seg = 0, return                 |
 *     + ------------- + ------------------------------------ + ------------------------------- +
 *
 * </pre>
 */
public class PacketPreprocessor extends LengthFieldBasedFrameDecoder {
    private static final Logger logger = LoggerFactory.getLogger(PacketPreprocessor.class);

    private static final int MAX_FRAME_LENGTH = 10 * 1024 * 1024;

    private static final int MAX_SEGMENTS = 3;

    private int segments = 0;

    public PacketPreprocessor() {
        super(ByteOrder.LITTLE_ENDIAN, MAX_FRAME_LENGTH, 2, 2, -4,
                0, false);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        if (segments == 0) {
            stripBeforeHeader(ctx, in, 0);
        }
        Object result = super.decode(ctx, in);
        if (result != null) {
            segments = 0;
            return result;
        }

        segments ++;
        if (segments > MAX_SEGMENTS && in.isReadable()) {
            // skip to header
            stripBeforeHeader(ctx, in, 1);
            segments = 0;
        }

        return null;
    }

    private void stripBeforeHeader(ChannelHandlerContext ctx, ByteBuf in, int offset) {
        int beforeRead = in.readerIndex();
        for (int idx = in.readerIndex() + offset; in.isReadable(2); in.readerIndex(++idx)) {
            short header = in.getShortLE(idx);
            if (header == 0x3377) {
                if (idx > beforeRead) {
                    ByteBuf buf = in.slice(beforeRead, idx - beforeRead);
                    MessageDebugger.logByteBuf(buf, "DISCARD", ctx.channel().toString());
                    buf.release();
                }
                break;
            }
        }
    }
}
