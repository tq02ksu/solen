package top.fengpingtech.solen.server.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import top.fengpingtech.solen.server.netty.EventModel;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@EventModel(value = 0x0008, desc = "主站对时")
public class TimingEvent extends Event {
    private Date time;
    @Override
    public void fromData(byte[] data) {
        setType(EventType.TIMING);

        String s = String.format("%02x%02x-%02x-%02x %02x:%02x:%02x",
                data[0], data[1], data[2], data[3], data[4], data[5], data[6]);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            time = dateFormat.parse(s);
        } catch (ParseException e) {
            throw new RuntimeException("error while parse login time", e);
        }
    }
}
