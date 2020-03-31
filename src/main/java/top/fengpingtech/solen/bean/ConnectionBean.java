package top.fengpingtech.solen.bean;

import lombok.Data;
import org.springframework.beans.BeanUtils;
import top.fengpingtech.solen.model.Connection;
import top.fengpingtech.solen.model.ConnectionStatus;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

@Data
public class ConnectionBean {

    private String deviceId;
    private String serverHost;
    private String serverPort;
    private int lac;
    private int ci;
    private ConnectionStatus status;
    private int header;
    private int inputStat;
    private int outputStat;

    private Date lastHeartBeatTime = new Date();

    private List<Connection.Report> reports = new LinkedList<>();

    public static ConnectionBean build(Connection connection) {
        ConnectionBean bean = new ConnectionBean();
        BeanUtils.copyProperties(connection, bean);

        if (connection.getChannel() == null) {
            bean.status = ConnectionStatus.UNKNOWN;
        } else if (connection.getChannel().isOpen() && connection.getChannel().isActive()
                && connection.getLastHeartBeatTime() != null
                && connection.getLastHeartBeatTime().getTime() + Connection.HEARTBEAT_TIMEOUT_MS < System.currentTimeMillis()) {
            bean.status = ConnectionStatus.LOST;
        } else if (connection.getChannel().isOpen() && connection.getChannel().isActive()) {
            bean.status = ConnectionStatus.NORMAL;
        } else {
            bean.status = ConnectionStatus.DISCONNECTED;
        }
        return bean;
    }
}
