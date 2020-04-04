package top.fengpingtech.solen.slotmachine;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

import java.nio.ByteOrder;

public class PacketPreprocessor extends LengthFieldBasedFrameDecoder {

    private static final int MAX_FRAME_LENGTH = 10 * 1024 * 1024;

    public PacketPreprocessor() {
        super(ByteOrder.LITTLE_ENDIAN, MAX_FRAME_LENGTH, 2, 2, -4,
                0, false);
    }
}
