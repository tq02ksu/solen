package top.fengpingtech.solen.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MessageDecoder extends ByteToMessageDecoder {
    private static final Logger logger = LoggerFactory.getLogger(MessageDecoder.class);

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) {
        int startIndex = msg.readerIndex();
        short header = msg.readShortLE();
        short length = msg.readShortLE();

        byte index = msg.readByte();

        long idCode = msg.readLongLE();

        byte[] buffer = new byte[11];
        msg.readBytes(buffer);

        String deviceId = new String(buffer);

        short cmd = (short) (msg.readByte() & 0xFF); // cmd 是大端

        byte[] data = new byte[length - 26];
        msg.readBytes(data);

        msg.readerIndex(startIndex);

        byte calc = 0;

        for (int i = 0; i < length - 1; i ++) {
            calc ^= msg.readByte();
        }

        byte checksum = msg.readByte();

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
                .header(header)
                .index(index & 0xFF)
                .idCode(idCode)
                .cmd(cmd)
                .deviceId(deviceId)
                .data(data)
                .build());

        logger.info("message received: " + out);
    }
}
