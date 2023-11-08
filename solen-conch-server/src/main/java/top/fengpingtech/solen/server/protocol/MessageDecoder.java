package top.fengpingtech.solen.server.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.fengpingtech.solen.server.model.SoltMachineMessage;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MessageDecoder extends ByteToMessageDecoder {
    private static final Logger logger = LoggerFactory.getLogger(MessageDecoder.class);

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) {
        // save start index for check sum
        int startIndex = msg.readerIndex();
        
        int header = msg.readShortLE();
        short length = msg.readShortLE();

        byte[] buffer = new byte[20];
        msg.readBytes(buffer);
        String deviceId = new String(buffer);

        int cmd = msg.readUnsignedShortLE();

        short source = abs(msg.readByte());

        short gun = abs(msg.readByte());

        short txType = abs(msg.readByte());

        // read body
        byte[] data = new byte[length - 32];
        msg.readBytes(data);

        byte end = msg.readByte();

        // check sum calculation
        msg.readerIndex(startIndex);
        int calc = 0;
        for (int i = 0; i < length + 2 - 4; i ++) {
            calc += abs(msg.readByte());
        }

        int checksum = msg.readInt();

        if (calc != checksum) {
            logger.warn("checksum failed, left is {}, right is {}", calc, checksum);
        }

        // skip invalid deviceId
        if (!deviceId.matches("^\\w+$")) {
            String hex = Stream.iterate(0, i -> i + 1)
                    .limit(buffer.length)
                    .map(i -> String.format("%02x ", buffer[i] & 0xff))
                    .collect(Collectors.joining());
            logger.warn("skipped message because of invalid device id: 0x{}", hex);
            return;
        }

        out.add(SoltMachineMessage.builder()
                .connectionId(ctx.channel().id().asLongText())
                .header(header)
                .cmd(cmd)
                        .source(source)
                        .gun(gun)
                        .txType(txType)
                        .data(data)
                        .end(end)

                .deviceId(deviceId)
                .data(data)
                .build());

        logger.info("message received: " + out);
    }
    
    short abs(byte b) {
        return b < 0 ? (short) -b : b;
    }
}
