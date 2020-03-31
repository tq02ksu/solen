package top.fengpingtech.solen.model;

import io.netty.channel.Channel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private int lac;
    private int ci;
    private Channel channel;

    private int header;

    private long idCode;
    private int inputStat;
    private int outputStat;
    private Date lastHeartBeatTime = new Date();

    @Builder.Default
    private List<Report> reports = new LinkedList<>();

    private transient AtomicInteger index = new AtomicInteger(0);

    private transient List<CountDownLatch> outputStatSyncs = new CopyOnWriteArrayList<>();

    @Builder
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Report {
        private Date time;
        private String content;
    }
}
