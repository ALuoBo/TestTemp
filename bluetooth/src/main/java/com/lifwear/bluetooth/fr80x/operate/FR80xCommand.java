package com.lifwear.bluetooth.fr80x.operate;

/**
 * @author WillXia
 * @date 2021/9/29.
 */
public enum FR80xCommand {

    /**
     * 发送测试开始指令
     */
    StartTest("10", "", false),

    /**
     * 接收测试结果
     */
    ReceiveTestData("11", "", false),

    /**
     * 接收校准参数
     */
    ReceiveCalibration("12", "", false),

    /**
     * 获取当前 MCU 硬件类型
     */
    GetNvdsType("00", "", false),

    /**
     * 获取当前固件的可用存储基地址
     */
    GetAddrBase("01", "", true),

    /**
     * 获取当前 MCU 固件版本号
     */
    GetMcuVersion("02", "", false),

    /**
     * 擦除扇区
     */
    EraseSector("03", "", true),

    /**
     * 写入 OTA 升级包数据
     */
    WriteDataOtaFile("05", "", true),

    /**
     * 重启，其中可传入升级包文件的校验值
     */
    RebootMcu("09", "", true),

    /**
     * 向温度计下发时间戳(同步时间)
     */
    SendTimeStamp("44", "", false),

    /**
     * 发送MCU当前时间戳
     */
    ResponseTimeStamp("54", "", false),

    /**
     * 修改温度阈值
     */
    ChangeLimitTemp("43", "", false),

    testTemp("88", "", false),

    /**
     * 设备上传最新温度
     */
    ReceiveTemp("64", "", false);

    /**
     * 请求命令字
     */
    private final String commandHex;

    /**
     * 请求数据内容
     */
    private String paramsHex;

    /**
     * 该条指令是否需要进行超时检测
     */
    private final boolean needCheckTimeout;

    public String getCommandHex() {
        return commandHex;
    }

    public String getParamsHex() {
        return paramsHex;
    }

    public void setParamsHex(String paramsHex) {
        this.paramsHex = paramsHex;
    }

    public boolean isNeedCheckTimeout() {
        return needCheckTimeout;
    }

    FR80xCommand(String commandHex, String paramsHex, boolean needCheckTimeout) {
        this.commandHex = commandHex;
        this.paramsHex = paramsHex;
        this.needCheckTimeout = needCheckTimeout;
    }

    }
