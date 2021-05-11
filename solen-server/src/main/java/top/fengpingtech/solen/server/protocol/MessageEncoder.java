package top.fengpingtech.solen.server.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.fengpingtech.solen.server.model.SoltMachineMessage;

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
    }
}
