package top.fengpingtech.solen.server.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.fengpingtech.solen.server.model.SoltMachineMessage;

import java.nio.charset.StandardCharsets;

public class MessageEncoder extends MessageToByteEncoder<SoltMachineMessage> {
    private static final Logger logger = LoggerFactory.getLogger(MessageEncoder.class);

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        super.write(ctx, msg, promise);

        if (msg instanceof SoltMachineMessage) {
            ctx.flush();
        }
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, SoltMachineMessage msg, ByteBuf out) {
       encode(msg, out);
    }

    public void encode(SoltMachineMessage msg, ByteBuf out) {
        logger.info("message sent: " + msg);

        int startIndex = out.readerIndex();
        out.writeShortLE(msg.getHeader());
        out.writeShortLE(msg.getData().length + 26);
        out.writeByte(msg.getIndex());
        out.writeLongLE(msg.getIdCode());
        out.writeBytes(msg.getDeviceId().getBytes());
        for (int i = 0; i < 11 - msg.getDeviceId().getBytes(StandardCharsets.UTF_8).length; i++) {
            out.writeByte('0');
        }

        out.writeByte(msg.getCmd());
        out.writeBytes(msg.getData());

        byte checksum = 0;
        for (int i = 0; i < msg.getData().length + 26 - 1; i++) {
            checksum ^= out.readByte();
        }
        out.readerIndex(startIndex);
        out.writeByte(checksum);
    }
}
