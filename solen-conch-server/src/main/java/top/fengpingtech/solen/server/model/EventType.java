package top.fengpingtech.solen.server.model;

public enum EventType {
    /**
     * device login
     */
    LOGIN,

    /**
     * heartbeat
     */
    HEARTBEAT,

    HISTORY_DATA,

    ALARM_DATA,

    TIMING,

    REALTIME_DATA,
    /**
     * device disconnect
     */
    DISCONNECT,

    /**
     * 串口接收
     */
    MESSAGE_RECEIVING,

    /**
     * 串口发送
     */
    MESSAGE_SENDING,

    /**
     * 属性更新
     */
    ATTRIBUTE_UPDATE,

    /**
     * 定位信息
     */
    LOCATION_CHANGE,

    /**
     * 发送开关机
     */
    CONTROL_SENDING, SWIPING;
}
