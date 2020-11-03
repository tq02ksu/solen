package top.fengpingtech.solen.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 设备权限类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DeviceAuth {
    @Builder.Default
    private List<String> owners = new CopyOnWriteArrayList<>();
}
