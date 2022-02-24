package com.lifwear.bluetooth;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

/**
 * 蓝牙外设抽象
 *
 * @author WillXia
 * @date 2021/9/27.
 */
public interface PeripheralDevice {

    // 设备链接
    void onConnectionStateChange(BluetoothGatt gatt, int status, int newState);

    // 发现设备的 服务
    void onServiceDiscover(BluetoothGatt bluetoothGatt, int status);

    // 从当前从设备获取到数据
    void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status);

    // 写入数据到当前设备
    void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status);

    // 如果需要双向通信，可以在 BluetoothGattCallback onServicesDiscovered 中对某个特征值设置监听（前提是该 Characteristic 具有 NOTIFY 属性）：
    void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic);

    void onMtuChanged(BluetoothGatt gatt, int mtu, int status);

    default void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) { }

    // 更改蓝牙外设 MTU
    default boolean requestMtu(BluetoothGatt bluetoothGatt, int size) {
        Log.d("PeripheralDevice", "requestMtu");
        if (bluetoothGatt != null) {
            Log.d("PeripheralDevice", "bluetoothGatt!=null");
            return bluetoothGatt.requestMtu(size);
        } else {
            Log.d("PeripheralDevice", "bluetoothGatt=null");
        }
        return false;
    }


}
