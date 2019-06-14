package com.neusoft.solen.server;

import org.springframework.boot.context.properties.ConfigurationProperties;

import static com.neusoft.solen.server.ServerProperties.PREFIX;

@ConfigurationProperties(prefix = PREFIX)
public class ServerProperties {
    static final String PREFIX = "solen.server";

    private int port = 7889;

    private int ioThreads = 2;

    private int workerThreads = Runtime.getRuntime().availableProcessors() * 2;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getIoThreads() {
        return ioThreads;
    }

    public void setIoThreads(int ioThreads) {
        this.ioThreads = ioThreads;
    }

    public int getWorkerThreads() {
        return workerThreads;
    }

    public void setWorkerThreads(int workerThreads) {
        this.workerThreads = workerThreads;
    }
}
