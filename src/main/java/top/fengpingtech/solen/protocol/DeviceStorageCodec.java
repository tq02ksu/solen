package top.fengpingtech.solen.protocol;

import top.fengpingtech.solen.model.Connection;
import top.fengpingtech.solen.model.DeviceAuth;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * encode and decode device storage.
 * format: tab separated values,
 */
public class DeviceStorageCodec {
    public byte[] encode(Connection conn) {
        List<String> owners = conn.getAuth().getOwners();
        return String.join(",", owners).getBytes(StandardCharsets.UTF_8);
    }

    public void decode(byte[] bytes, Connection conn) {
        String[] arr = bytes == null || bytes.length == 0 ? new String[0]
                : new String(bytes, StandardCharsets.UTF_8).split("\t");
        DeviceAuth auth = conn.getAuth();
        if (arr.length > 0) {
            auth.setOwners(new ArrayList<>(Arrays.asList(arr[0].split(","))));
        }
    }
}
