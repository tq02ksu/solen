package top.fengpingtech.solen.server.protocol;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.net.SocketAddress;

public class TracingLogHandler extends ChannelDuplexHandler {

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) {
        boolean tracing = setTracing(ctx);
        ctx.fireChannelRegistered();
        if (tracing) {
            clean();
        }
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) {
        wrapTracing(ctx, ctx::fireChannelUnregistered);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        wrapTracing(ctx, ctx::fireChannelActive);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        wrapTracing(ctx, ctx::fireChannelInactive);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        wrapTracing(ctx, () -> ctx.fireExceptionCaught(cause));
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        wrapTracing(ctx, () -> ctx.fireUserEventTriggered(evt));
    }

    @Override
    public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise) {
        wrapTracing(ctx, () -> ctx.bind(localAddress, promise));
    }

    @Override
    public void connect(
            ChannelHandlerContext ctx,
            SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
        wrapTracing(ctx, () -> ctx.connect(remoteAddress, localAddress, promise));
    }

    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) {
        wrapTracing(ctx, () -> ctx.disconnect(promise));
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) {
        wrapTracing(ctx, () ->  ctx.close(promise));
    }

    @Override
    public void deregister(ChannelHandlerContext ctx, ChannelPromise promise) {
        wrapTracing(ctx, () ->  ctx.deregister(promise));
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        wrapTracing(ctx, ctx::fireChannelReadComplete);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        wrapTracing(ctx, () ->  ctx.fireChannelRead(msg));
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        wrapTracing(ctx, () ->  ctx.write(msg, promise));
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) {
        wrapTracing(ctx, ctx::fireChannelWritabilityChanged);
    }

    @Override
    public void flush(ChannelHandlerContext ctx) {
        wrapTracing(ctx, ctx::flush);
    }

    private void wrapTracing(ChannelHandlerContext ctx, Runnable logic) {
        boolean tracing = setTracing(ctx);
        try {
            logic.run();
        } finally {
            if (tracing) {
                clean();
            }
        }
    }

    private boolean setTracing(ChannelHandlerContext ctx) {
        if (MDC.get("X-B3-TraceId") != null) {
            return false;
        }
        String channelId = "0x" + ctx.channel().id().asShortText();
        MDC.put("X-B3-SpanId", channelId);

        String deviceId = ctx.channel().attr( AttributeKey.<String>valueOf("DeviceId")).get();
        if (deviceId != null) {
            MDC.put("X-B3-TraceId", deviceId);
        } else {
            MDC.remove("X-B3-TraceId");
        }
        return true;
    }

    private void clean() {
        MDC.remove("X-B3-TraceId");
        MDC.remove("X-B3-SpanId");
    }
}
