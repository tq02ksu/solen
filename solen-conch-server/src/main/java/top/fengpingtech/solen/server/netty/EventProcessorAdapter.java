package top.fengpingtech.solen.server.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.fengpingtech.solen.server.EventProcessor;
import top.fengpingtech.solen.server.IdGenerator;
import top.fengpingtech.solen.server.model.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class EventProcessorAdapter extends ChannelDuplexHandler {
    private static final Logger logger = LoggerFactory.getLogger(EventProcessorAdapter.class);

    private static final Class<Event> modelBaseClass = Event.class;

    private final Map<Integer, Class<? extends Event>> modelClassMap;


    private static final AttributeKey<String> DEVICE_ID_ATTRIBUTE_KEY = AttributeKey.valueOf("DeviceId");

    private final EventProcessor delegate;

    private final IdGenerator eventIdGenerator;


    public EventProcessorAdapter(EventProcessor delegate, IdGenerator eventIdGenerator) {
        this.delegate = delegate;
        this.eventIdGenerator = eventIdGenerator;

        modelClassMap = new HashMap<>();
        String packageName = modelBaseClass.getPackage().getName();
        String packagePath = packageName.replace(".", "/");
        try {
            Enumeration<URL> urls = modelBaseClass.getClassLoader().getResources(packagePath);
            List<File> classFiles = new ArrayList<>();
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                File file = new File(url.getFile());
                classFiles.add(file);
            }
            List<String> classNames = new ArrayList<>();
            for (File file : classFiles) {
                String fileName = file.getName();
                classNames.add(fileName.substring(0, fileName.lastIndexOf('.')));
            }
            for (String className : classNames) {
                Class<?> clazz = modelBaseClass.getClassLoader().loadClass(packageName + "." + className);
                if (!modelBaseClass.isAssignableFrom(clazz)) {
                    continue;
                }

                EventModel annotation = clazz.getAnnotation(EventModel.class);
                if (annotation != null) {
                    modelClassMap.put(annotation.value(), (Class<? extends Event>) clazz);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof SoltMachineMessage) {
            processMessage(ctx, (SoltMachineMessage) msg);
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof SoltMachineMessage) {
            processMessage(ctx, (SoltMachineMessage) msg);
        }
        super.write(ctx, msg, promise);
    }

    private void processMessage(ChannelHandlerContext ctx, SoltMachineMessage msg) {
        Integer cmd = msg.getCmd();
        Class<? extends Event> clazz = modelClassMap.get(cmd);
        Event event;
        try {
            event = clazz.getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("error while instance event class", e);
        }
        event.setCmd(cmd);
        event.setHeader(msg.getHeader());
        event.setSource(msg.getSource());
        event.setGun(msg.getGun());
        event.setTxType(msg.getTxType());
        event.setEnd(msg.getEnd());
        event.setDeviceId(msg.getDeviceId());
        event.fromData(msg.getData());

        delegate.processEvents(Collections.singletonList(event));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        String deviceId = ctx.channel().attr(DEVICE_ID_ATTRIBUTE_KEY).get();
        if (deviceId != null) {
            DisconnectEvent event = new DisconnectEvent();
            event.setType(EventType.DISCONNECT);
            event.setEventId(eventIdGenerator.nextVal());
            event.setDeviceId(deviceId);
            event.setConnectionId(ctx.channel().id().asLongText());
            delegate.processEvents(Collections.singletonList(event));
        }
    }
}
