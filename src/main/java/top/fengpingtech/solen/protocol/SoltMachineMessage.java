package top.fengpingtech.solen.protocol;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SoltMachineMessage {

    /**
     * new byte[] {0x77, 0x33}
     */
    @Builder.Default
    private int header = 0x3377;

    /**
     * 包序号
     */
    private int index;

    /**
     * 识别码，小端表示为0x79 0x75 0x77 0x65 0x6E 0x36 0x30 0x32
     */
    @Builder.Default
    private long idCode = 0x230363E656775797L;

    private String deviceId;

    /**
     * cmd=0 注册
     * cmd=1 心跳
     * cmd=2 应答
     * cmd=3 远程开关机
     * cmd=128 串口数据上传
     * cmd=129 串口数据发送
     * cmd=4 请求高德接口
     * cmd=5 上报高德定位
     * cmd=6 请求高德接口
     * cmd=7 写入本地flash空间
     * cmd=8 上报回复写入数据结果
     * cmd=9 读取flash 数据
     * cmd=10 上报本地存储内存
     */
    private short cmd;

    private byte[] data;
}
