package top.fengpingtech.solen.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import static org.junit.Assert.*;

public class SlotMachineInBoundHandlerTest {


    @Test
    public void testLacCi() {
        ByteBuf location = Unpooled.wrappedBuffer(new byte[] {
                0x0a, 0x41, 0, 0, 0x4c, 0x47, 0, 0
        });
        int lac = location.readByte() + (location.readByte() << 8);
        location.readBytes(2);

        int ci = location.readByte() + (location.readByte() << 8);
        assertEquals(lac, 0x410a);
        assertEquals(ci, 0x474c);

        location = Unpooled.wrappedBuffer(new byte[] {
               10, 65, 0, 0, -86, 97, 0, 0
        });

        lac = location.readByte() + ((location.readByte() & 0xff) << 8);
        location.readBytes(2);

        ci = (location.readByte() & 0xff) + ((location.readByte() & 0xff) << 8);
        assertEquals(lac, 0x410A);
        assertEquals(ci, 0x61AA);
    }

    @Test
    public void reverse() {
    }

    @Test
    public void logBytebuf() {
    }
}
