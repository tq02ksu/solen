package top.fengpingtech.solen.server.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

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
    private static final int MAX_FRAME_LENGTH = 10 * 1024 * 1024;

    private static final int MAX_SEGMENTS = 3;

    private static final int LENGTH_FIELD_OFFSET = 2;

    private static final ByteOrder BYTE_ORDER = ByteOrder.LITTLE_ENDIAN;

    private static final int LENGTH_FIELD_LENGTH = 2;

    private static final int LENGTH_ADJUSTMENT = -4;

    private int segments = 0;


    public PacketPreprocessor() {
        super(BYTE_ORDER, MAX_FRAME_LENGTH, LENGTH_FIELD_OFFSET, LENGTH_FIELD_LENGTH, LENGTH_ADJUSTMENT,
                0, false);
    }

    int getFrameLength(ByteBuf in) {
        if (in.readableBytes() < LENGTH_FIELD_OFFSET) {
            return -1;
        }

        int actualLengthFieldOffset = in.readerIndex() + LENGTH_FIELD_OFFSET;
        int frameLength = (int) getUnadjustedFrameLength(in, actualLengthFieldOffset, LENGTH_FIELD_LENGTH, BYTE_ORDER);
        frameLength += LENGTH_ADJUSTMENT + LENGTH_FIELD_OFFSET;
        return frameLength;
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        if (segments == 0) {
            stripBeforeHeader(ctx, in, 0);
        }

        for (int frameLength = getFrameLength(in);
             frameLength > 0 && in.readableBytes() >= frameLength + LENGTH_FIELD_OFFSET;
             frameLength = getFrameLength(in)) {
            // calculate checksum
            byte calc = 0;
            for (int i = 0; i < frameLength + LENGTH_FIELD_OFFSET - 1; i ++) {
                calc ^= in.getByte(in.readerIndex() + i);
            }
            byte checksum = in.getByte(in.readerIndex() + frameLength + LENGTH_FIELD_OFFSET - 1);

            if (calc == checksum) {
                Object result = super.decode(ctx, in);
                if (result != null) {
                    segments = 0;
                    return result;
                }
            } else {
                stripBeforeHeader(ctx, in, 1);
            }
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
                    MessageDebugger.logByteBuf(buf, "DISCARD(" + (idx - beforeRead) + ")",
                            ctx.channel().toString());
                }
                break;
            }
        }
    }
}
