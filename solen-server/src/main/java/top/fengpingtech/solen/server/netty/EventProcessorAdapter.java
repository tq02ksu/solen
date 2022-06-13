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

import java.nio.charset.StandardCharsets;
import java.util.*;

public class EventProcessorAdapter extends ChannelDuplexHandler {
    private static final Logger logger = LoggerFactory.getLogger(EventProcessorAdapter.class);

    private static final AttributeKey<String> DEVICE_ID_ATTRIBUTE_KEY = AttributeKey.valueOf("DeviceId");

    private final EventProcessor delegate;

    private final IdGenerator eventIdGenerator;

    public EventProcessorAdapter(EventProcessor delegate, IdGenerator eventIdGenerator) {
        this.delegate = delegate;
        this.eventIdGenerator = eventIdGenerator;
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
        if (msg.getCmd() == 0) {
            ByteBuf data = ctx.alloc().heapBuffer(msg.getData().length).writeBytes(msg.getData());
            isTrue(data.readableBytes() == 8,
                    "register packet length expect to 8, but is " + data.readableBytes());
            long lac = data.readUnsignedIntLE();
            long ci = data.readUnsignedIntLE();
            data.release();
            ctx.channel().attr(AttributeKey.valueOf("DeviceId")).set(msg.getDeviceId());

            ConnectionEvent event = new ConnectionEvent();
            setEventValue(msg, event, EventType.CONNECT);
            event.setLac(lac);
            event.setCi(ci);

            StatusEvent status = new StatusEvent();
            setEventValue(msg, status, EventType.STATUS_UPDATE);
            status.setStatus(StatusEvent.STATUS_NORMAL);

            delegate.processEvents(Arrays.asList(event, status));
        } else if (msg.getCmd() == 1) {
            ByteBuf data = ctx.alloc().heapBuffer(msg.getData().length).writeBytes(msg.getData());
            byte stat = data.readByte();

            int outputStat = (stat & 0x02) >> 1;
            int inputStat = stat & 0x01;
            int rssi = data.readShortLE();
            double voltage = (double) data.readShortLE() / 10;
            double temperature = (double) data.readShortLE() / 10;
            int gravity = data.readShortLE();
            int uptime = data.readIntLE();
            data.release();

            AttributeEvent event = new AttributeEvent();
            setEventValue(msg, event, EventType.ATTRIBUTE_UPDATE);
            event.setInputStat(inputStat);
            event.setOutputStat(outputStat);
            event.setRssi(rssi);
            event.setVoltage(voltage);
            event.setTemperature(temperature);
            event.setGravity(gravity);
            event.setUptime(uptime);
            delegate.processEvents(Collections.singletonList(event));
        } else if (msg.getCmd() == 128) {
            MessageEvent event = new MessageEvent();
            setEventValue(msg, event, EventType.MESSAGE_RECEIVING);
            event.setMessage(new String(msg.getData(), StandardCharsets.UTF_8));
            delegate.processEvents(Collections.singletonList(event));
        } else if (msg.getCmd() == 129) {
            MessageEvent event = new MessageEvent();
            setEventValue(msg, event, EventType.MESSAGE_SENDING);
            event.setMessage(String.valueOf(msg.getData()[0]));
            delegate.processEvents(Collections.singletonList(event));
        } else if (msg.getCmd() == 5) {
            ByteBuf data = ctx.alloc().heapBuffer(msg.getData().length).writeBytes(msg.getData());
            int accessType = data.readByte();
            byte[] imeiBuffer = new byte[15];
            data.readBytes(imeiBuffer);
            String imei = new String(imeiBuffer, StandardCharsets.UTF_8);
            boolean cdma = data.readByte() != 0;
            int networkType = data.readByte();
            List<BaseStation> stations = parseStations(data);
            byte[] doubleBuf = new byte[16];
            data.readBytes(doubleBuf);
            double lat = Double.parseDouble(new String(doubleBuf));
            data.readBytes(doubleBuf);
            double lng = Double.parseDouble(new String(doubleBuf));
            byte[] iccIdBuf = new byte[20];
            data.readBytes(iccIdBuf);
            String iccId = new String(iccIdBuf);

            data.release();
            logger.info("receiving gprs data: accessType={}, imei={}, cdma={}, "
                            + "networkType={}, stations={}, iccId={}",
                    accessType, imei, cdma, networkType, stations, iccId);

            LocationEvent event = new LocationEvent();
            setEventValue(msg, event, EventType.LOCATION_CHANGE);
            event.setLat(lat);
            event.setLng(lng);
            event.setIccId(iccId);
            event.setImei(imei);

            AttributeEvent attribute = new AttributeEvent();
            setEventValue(msg, attribute, EventType.ATTRIBUTE_UPDATE);
            attribute.setIccId(iccId);
            attribute.setImei(imei);

            delegate.processEvents(Arrays.asList(event, attribute));
        }
    }

    private void isTrue(boolean b, String msg) {
        if (!b) {
            throw new IllegalArgumentException(msg);
        }
    }

    void setEventValue(SoltMachineMessage msg, Event event, EventType type) {
        event.setConnectionId(msg.getConnectionId());
        event.setEventId(eventIdGenerator.nextVal());
        event.setDeviceId(msg.getDeviceId());
        event.setType(type);
        event.setIndex(msg.getIndex());
        event.setTime(new Date());
    }


    private List<BaseStation> parseStations(ByteBuf data) {
        List<BaseStation> stations = new ArrayList<>();

        for (int i = 0; i < 6; i++) {
            stations.add(
                    BaseStation.builder()
                            .valid(data.readByte())
                            .mcc(data.readShortLE())
                            .mnc(data.readByte())
                            .lac(data.readIntLE())
                            .cellId(data.readIntLE())
                            .signal(data.readShortLE())
                            .build());
        }
        return stations;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        String deviceId = ctx.channel().attr(DEVICE_ID_ATTRIBUTE_KEY).get();
        if (deviceId != null) {
            Date d = new Date();
            ConnectionEvent event = new ConnectionEvent();
            event.setType(EventType.DISCONNECT);
            event.setEventId(eventIdGenerator.nextVal());
            event.setDeviceId(deviceId);
            event.setConnectionId(ctx.channel().id().asLongText());
            event.setTime(d);

            StatusEvent status = new StatusEvent();
            status.setType(EventType.STATUS_UPDATE);
            status.setEventId(eventIdGenerator.nextVal());
            status.setDeviceId(deviceId);
            status.setConnectionId(ctx.channel().id().asLongText());
            status.setTime(d);
            status.setStatus(StatusEvent.STATUS_DISCONNECT);

            delegate.processEvents(Arrays.asList(event, status));
        }
    }
}
