package top.fengpingtech.solen.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class MessageEncoderTest {
    MessageEncoder encoder = new MessageEncoder();

    @Test
    public void encode() {
        ByteBuf byteBuf = Unpooled.buffer();
        encoder.encode(SoltMachineMessage.builder()
                        .header(13175)
                        .index(18)
                        .idCode(8389750987502775627L)
                        .deviceId("10619030006")
                        .cmd((short) 3)
                        .data("test".getBytes())
                        .build(), byteBuf);

        MessageDebugger.logByteBuf(byteBuf, "test", null);
    }

    @Test
    public void testRegister() {
        ByteBuf buf = Unpooled.buffer();
        encoder.encode(SoltMachineMessage.builder()
                .header(13175)
                .index(0)
                .idCode(12345L)
                .deviceId("10619030001")
                .cmd((short) 0)
                .data(new byte[] {10, 65, 0, 0, 76, 71, 0, 0})
                .build(), buf);
    }

    @Test
    public void testEncodeWithEmptyData() {
        ByteBuf buf = Unpooled.buffer();
        encoder.encode(SoltMachineMessage.builder()
                .header(13175)
                .index(0)
                .idCode(12345L)
                .deviceId("10619030001")
                .cmd((short) 0)
                .data(new byte[0])
                .build(), buf);
    }
}