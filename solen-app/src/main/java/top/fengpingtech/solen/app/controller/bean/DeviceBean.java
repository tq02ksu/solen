package top.fengpingtech.solen.app.controller.bean;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.fengpingtech.solen.app.model.Coordinate;

import java.util.Date;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeviceBean {
    private Long deviceId;

    private Long lac;

    private Long ci;

    private Integer header;

    private Long idCode;

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
