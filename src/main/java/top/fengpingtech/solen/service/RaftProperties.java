package top.fengpingtech.solen.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = RaftProperties.PREFIX)
public class RaftProperties {
    static final String PREFIX = "solen.raft";

    private String clusterName = "solen-raft";

    private int port = 34143;

    private String allNodeAddresses = "127.0.0.1:" + port;

    private String dataPath = "data/raft-data";

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getClusterName() {
        return clusterName;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getAllNodeAddresses() {
        return allNodeAddresses;
    }

    public void setAllNodeAddresses(String allNodeAddresses) {
        this.allNodeAddresses = allNodeAddresses;
    }

    public String getDataPath() {
        return dataPath;
    }

    public void setDataPath(String dataPath) {
        this.dataPath = dataPath;
    }
}
