package top.fengpingtech.solen.app.controller.bean;

import lombok.Data;

import java.util.Date;

@Data
public class EventQueryRequest {
    private Date startTime;

    private Date endTime;

    private String deviceId;

    private String startId;

    private String type;

    private Integer pageNo;

    private Integer pageSize;
}
