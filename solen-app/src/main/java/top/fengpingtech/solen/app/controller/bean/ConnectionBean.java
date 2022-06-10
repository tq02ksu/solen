package top.fengpingtech.solen.app.controller.bean;

import lombok.Data;
import top.fengpingtech.solen.app.domain.ConnectionStatus;
import top.fengpingtech.solen.app.domain.Coordinate;

import java.util.Date;
import java.util.List;

@Data
public class ConnectionBean {

    private String deviceId;
    private String serverHost;
    private String serverPort;
    private Long lac;
    private Long ci;
    private ConnectionStatus status;
    private Integer header;
    private Integer inputStat;
    private Integer outputStat;
    private Integer rssi;
    // 电压 单位V
    private Double voltage;
    // 温度 摄氏度
    private Double temperature;
    // 重力
    private Integer gravity;
    // 开机时长
    private Integer uptime;

    private List<Coordinate> coordinates;

    // sim id
    private String iccId;

    private Date lastHeartBeatTime = new Date();
}
