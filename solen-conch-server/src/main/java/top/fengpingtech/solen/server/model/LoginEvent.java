package top.fengpingtech.solen.server.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import top.fengpingtech.solen.server.netty.EventModel;

import java.nio.ByteBuffer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@EventModel(value = 0x0002, desc = "登录事件")
public class LoginEvent extends Event {
    private Date loginTime;

    private Integer stationType;

    private Integer loginPreserve;

    private Integer monitorBoardVersion;

    private Integer masterBoardVersion;

    private byte[] preserveInformation;

    @Override
    public void fromData(byte[] data) {
        setType(EventType.LOGIN);

        String s = String.format("%02x%02x-%02x-%02x %02x:%02x:%02x",
                data[0], data[1], data[2], data[3], data[4], data[5], data[6]);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            loginTime = dateFormat.parse(s);
        } catch (ParseException e) {
            throw new RuntimeException("error while parse login time", e);
        }

        ByteBuffer buffer = ByteBuffer.allocate(data.length - 7);
        buffer.put(data, 7, data.length - 7).flip();

        stationType = (int) buffer.get();

        loginPreserve = buffer.getInt();

        monitorBoardVersion = buffer.getInt();

        masterBoardVersion = buffer.getInt();

        preserveInformation = new byte[16];
        buffer.get(preserveInformation);
    }
}
