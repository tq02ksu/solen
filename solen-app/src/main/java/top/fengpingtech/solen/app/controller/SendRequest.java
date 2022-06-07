package top.fengpingtech.solen.app.controller;

import lombok.Data;

@Data
public class SendRequest {
    private Long deviceId;
    private String data;
    private int ctrl;
}
