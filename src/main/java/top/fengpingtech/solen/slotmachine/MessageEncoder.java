package top.fengpingtech.solen.slotmachine;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageEncoder extends MessageToByteEncoder<SoltMachineMessage> {
    private static final Logger logger = LoggerFactory.getLogger(MessageDecoder.class);

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
        out.writeByte((byte)msg.getCmd()); // cmd 不是小端
        out.writeBytes(msg.getData());

        byte checksum = 0;
        for (int i = 0; i < msg.getData().length + 26 - 1; i ++) {
            checksum ^= out.readByte();
        }
        out.readerIndex(startIndex);
        out.writeByte(checksum);
    }
}
