package top.fengpingtech.solen.server;

public interface SolenServer {
    void start();

    void stop();

    DeviceService getDeviceService();
}
