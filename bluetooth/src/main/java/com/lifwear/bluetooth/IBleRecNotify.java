package com.lifwear.bluetooth;

/**
 * 接收到 MCU 的 BLE 应答之后，根据命令字，进行相应判断，走后续逻辑
 *
 * @author WillXia
 * @date 2021/9/29.
 */
public interface IBleRecNotify {

    /**
     * 获取 MCU 硬件类型成功
     */
    void onRecNvdsType();

    /**
     * 获取新固件的可用存储基地址
     */
    /**
     * 获取 MCU 版本号成功
     */
    void onRecMcuVersion(byte[] receive);

    void onGetStrBase(byte[] receive);

    /**
     * 擦除扇区成功
     */
    void onRecEraseSector();

    /**
     * 写入 MCU 升级包文件成功
     */
    void onRecWriteDataOtaFile();

    /**
     * 重启 MCU 成功
     */
    void onRecRebootMcu();
}
