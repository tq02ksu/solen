package top.fengpingtech.solen.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SerialMessagePacker extends MessageToMessageDecoder<SoltMachineMessage> {
    private static final String ATTRIBUTE_KEY_MESSAGE_BUFFER = "MESSAGE_BUFFER";

    private static final int TEXT_REPORT_TIMEOUT_SECONDS = 5;

    private static final List<Byte> TEXT_TERMINATORS = Collections.unmodifiableList(
            Arrays.asList((byte) 0x00, (byte) 0x0a));

    private final ConnectionManager connectionManager;

    public SerialMessagePacker(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, SoltMachineMessage msg, List<Object> out) throws Exception {
        if (msg.getCmd() != 128 || !connectionManager.getStore().containsKey(msg.getDeviceId())) {
            out.add(msg);
            return;
        }

        AttributeKey<byte[]> key = AttributeKey.valueOf(ATTRIBUTE_KEY_MESSAGE_BUFFER);
        Attribute<byte[]> val = ctx.channel().attr(key);
        SplitResult result;
        synchronized (val) {
            result = split(ctx, msg.getData(), val.getAndSet(null));
            val.getAndSet(result.buffer);
        }

        if (!result.segments.isEmpty()) {
            result.segments.forEach(s -> out.add(generateMessage(msg, s)));
        }

        // schedule process
        if (val.get() != null) {
            ctx.executor().schedule(() -> {
                for (byte[] message = val.get(); message != null; message = val.get()) {
                    if (val.compareAndSet(message, null)) {
                        ctx.fireChannelRead(generateMessage(msg, message));
                    }
                }

            }, TEXT_REPORT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
    }

    private Object generateMessage(SoltMachineMessage msg, byte[] bytes) {
        SoltMachineMessage message = new SoltMachineMessage();
        BeanUtils.copyProperties(msg, message);
        message.setData(bytes);
        return message;
    }

    private SplitResult split(ChannelHandlerContext ctx, byte[] data, byte[] buffer) {
        SplitResult result = new SplitResult();
        ByteBuf text = ctx.alloc().buffer();
        if (buffer != null) {
            text.writeBytes(buffer);
        }
        text.writeBytes(data);

        ByteBuf buf = ctx.alloc().buffer();

        while (text.isReadable()) {
            byte b = text.readByte();
            if (TEXT_TERMINATORS.contains(b) && buf.isReadable()) {
                result.segments.add(array(buf));
                buf.clear();
            } else if (!TEXT_TERMINATORS.contains(b)) {
                buf.writeByte(b);
            }
        }

        if (buf.isReadable()) {
            result.buffer = array(buf);
        }

        buf.release();
        text.release();
        return result;
    }

    private byte[] array(ByteBuf buf) {
        byte[] req = new byte[buf.readableBytes()];
        buf.readBytes(req);
        return req;
    }

    private static class SplitResult {
        List<byte[]> segments = new ArrayList<>();
        byte[] buffer;
    }
}
