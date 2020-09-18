package top.fengpingtech.solen.model;

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
     * 状态更新
     */
    STATUS_UPDATE,

    /**
     * 定位信息
     */
    LOCATION_CHANGE,
}
