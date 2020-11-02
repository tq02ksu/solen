package top.fengpingtech.solen.model;

import io.netty.channel.ChannelHandlerContext;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.fengpingtech.solen.bean.Coordinate;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Connection {
    public static final long HEARTBEAT_TIMEOUT_MS = 5L * 60 * 1000;

    private String deviceId;
    private String serverHost;
    private String serverPort;
    private Long lac;
    private Long ci;
    private ChannelHandlerContext ctx;

    private Integer header;

    private long idCode;
    private Integer inputStat;
    private Integer outputStat;

    @Builder.Default
    private Date lastHeartBeatTime = new Date();

    // 信号强度
    private Integer rssi;

    // 电压 单位V
    private Double voltage;
    // 温度 摄氏度
    private Double temperature;
    // 重力 (N/kg, m/s2)
    private Integer gravity;
    // 开机时长
    private Integer uptime;

    /**
     * GWS84
     */
    private Coordinate coordinate;

    // sim id
    private String iccId;

    @Builder.Default
    private List<Report> reports = new LinkedList<>();

    @Builder.Default
    private DeviceAuth auth = new DeviceAuth();

    @Builder.Default
    private transient AtomicInteger index = new AtomicInteger(0);

    @Builder.Default
    private transient List<CountDownLatch> outputStatSyncs = new CopyOnWriteArrayList<>();

    @Builder.Default
    private transient List<WriteFlashHook> writeFlashSyncs = new CopyOnWriteArrayList<>();

    @Builder
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Report {
        private Date time;
        private String content;
    }

    @Builder
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class WriteFlashHook {
        private Boolean result;

        private CountDownLatch latch;
    }
}
