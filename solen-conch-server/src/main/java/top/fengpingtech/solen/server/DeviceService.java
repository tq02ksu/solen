package top.fengpingtech.solen.server;

public interface DeviceService {
    void sendMessage(String deviceId, String message);

    void sendControl(String deviceId, int stat);
}
