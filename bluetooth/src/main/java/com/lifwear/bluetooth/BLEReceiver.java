package com.lifwear.bluetooth;

import android.bluetooth.BluetoothGatt;

/**
 * BLE 参数回传的接口
 * Author: Corey
 * Date: 10/11/21 1:10 PM
 */
public interface BLEReceiver {
    /**
     * 设备连接状态发生改变
     */
    default void onConnectStateChanged(boolean connectState){}

    default void onServiceDiscovered(){}

    /**
     * 设备 MTU 发生改变
     * @param deviceData    设备封装对象，判断哪种设备
     */
    default void onMtuSizeChanged(){}
    /**
     * 设备某些参数字段发生改变
     * @param deviceData    设备封装对象
     */
    default void onDataChanged(DeviceData deviceData){}

    default void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) { }

    /**
     * OTA 超时
     */
    default void otaTimeOut(){}


}
