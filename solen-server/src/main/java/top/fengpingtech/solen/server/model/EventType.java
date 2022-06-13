package top.fengpingtech.solen.server.model;

public enum EventType {
    /**
     * device connect
     */
    CONNECT,

    /**
     * device disconnect
     */
    DISCONNECT,

    /**
     * 状态更新
     */
    STATUS_UPDATE,

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
    CONTROL_SENDING;
}
