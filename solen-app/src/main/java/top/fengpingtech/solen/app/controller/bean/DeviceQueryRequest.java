package top.fengpingtech.solen.app.controller.bean;

import lombok.Data;

@Data
public class DeviceQueryRequest {
    private String deviceId;

    private String status;

    private String sort;

    private String order;

    private Integer pageNo;

    private Integer pageSize;
}
