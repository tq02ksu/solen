package top.fengpingtech.solen.server.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import top.fengpingtech.solen.server.netty.EventModel;

import java.nio.ByteBuffer;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@EventModel(value = 0x0030, desc = "主动上传刷卡请求")
public class SwipingEvent extends Event {
    private String cardNumber;

    private Integer cardType;

    private String cardPassword;

    private String orderNumber;

    @Override
    public void fromData(byte[] data) {
        setType(EventType.SWIPING);

        ByteBuffer buffer = ByteBuffer.allocate(data.length);


    }
}
