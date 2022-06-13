package top.fengpingtech.solen.app.controller.bean;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.fengpingtech.solen.app.domain.ConnectionStatus;
import top.fengpingtech.solen.app.domain.Coordinate;

import java.util.Date;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeviceBean {
    private String deviceId;

    private Long lac;

    private Long ci;

    private ConnectionStatus status;

    private Integer inputStat;

    private Integer outputStat;

    private Integer rssi;

    private Double voltage;

    private Double temperature;

    private Integer gravity;

    private Integer uptime;

    private List<Coordinate> coordinates;

    private String iccId;

    private List<Report> reports;

    @Builder
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Report {
        private Date time;
        private String content;
    }
}
