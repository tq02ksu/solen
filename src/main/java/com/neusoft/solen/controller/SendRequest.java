package com.neusoft.solen.controller;

import lombok.Data;

@Data
public class SendRequest {
    private String deviceId;
    private String data;
    private int ctrl;
    private short cmd;
}
