package top.fengpingtech.solen.server.model;


import lombok.Data;
import lombok.EqualsAndHashCode;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class DataEvent extends Event {

    private List<DataItem> dataItems;

    @Override
    public void fromData(byte[] data) {
        ByteBuffer buffer = ByteBuffer.allocate(data.length);

        short count = buffer.getShort();
        dataItems = new ArrayList<>(count);
        for (short i = 0; i < count; i ++) {
            DataItem item = new DataItem();
            item.setId(buffer.getShort());
            int len = Math.abs(buffer.get());
            byte[] val = new byte[len];
            buffer.get(val);
            item.setValue(val);

            dataItems.add(item);
        }
    }

    @Data
    public static class DataItem {
        private Integer index;

        private Short id;

        private byte[] value;
    }
}
