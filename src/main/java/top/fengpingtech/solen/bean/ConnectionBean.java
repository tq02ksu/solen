package top.fengpingtech.solen.bean;

import lombok.Data;
import org.springframework.beans.BeanUtils;
import top.fengpingtech.solen.model.Connection;
import top.fengpingtech.solen.model.ConnectionStatus;
import top.fengpingtech.solen.model.DeviceAuth;

import java.util.Date;
import java.util.LinkedList;
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

    // icc id
    private String iccId;

    private Date lastHeartBeatTime = new Date();

    private List<Connection.Report> reports = new LinkedList<>();

    private DeviceAuth auth;

    public static ConnectionBean build(Connection connection) {
        ConnectionBean bean = new ConnectionBean();
        BeanUtils.copyProperties(connection, bean);

        if (connection.getCtx() == null) {
            bean.status = ConnectionStatus.UNKNOWN;
        } else if (connection.getCtx().channel().isOpen() && connection.getCtx().channel().isActive()
                && connection.getLastHeartBeatTime() != null
                && connection.getLastHeartBeatTime().getTime() + Connection.HEARTBEAT_TIMEOUT_MS < System.currentTimeMillis()) {
            bean.status = ConnectionStatus.LOST;
        } else if (connection.getCtx().channel().isOpen() && connection.getCtx().channel().isActive()) {
            bean.status = ConnectionStatus.NORMAL;
        } else {
            bean.status = ConnectionStatus.DISCONNECTED;
        }
        return bean;
    }
}
