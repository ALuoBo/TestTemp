package com.lifwear.bluetooth.fr80x;

import android.bluetooth.BluetoothGatt;

import com.lifwear.bluetooth.BLEReceiver;
import com.lifwear.bluetooth.DeviceData;

import java.util.ArrayList;
import java.util.List;

/**
 * @author WillXia
 * @date 2021/11/19.
 */
public class FR80XCommSubject {

    private final List<BLEReceiver> observers = new ArrayList<>();

    public void addBleListener(BLEReceiver bleReceiver) {
        observers.add(bleReceiver);
    }

    public void removeListeners(BLEReceiver bleReceiver) {
        observers.remove(bleReceiver);
    }

    public void onDataChange(DeviceData deviceData) {
        for (BLEReceiver bleReceiver : observers) {
            bleReceiver.onDataChanged(deviceData);
        }
    }

    public void onConnectStateChanged(boolean connectState) {
        for (BLEReceiver bleReceiver : observers) {
            bleReceiver.onConnectStateChanged(connectState);
        }
    }

    public void onMtuSizeChanged() {
        for (BLEReceiver bleReceiver : observers) {
            bleReceiver.onMtuSizeChanged();
        }
    }

    public void onServiceDiscovered() {
        for (BLEReceiver bleReceiver : observers) {
            bleReceiver.onServiceDiscovered();
        }
    }

    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        for (BLEReceiver bleReceiver : observers) {
            bleReceiver.onReadRemoteRssi(gatt,rssi,status);
        }
    }

    public void onOTATimeOut() {
        for (BLEReceiver bleReceiver : observers) {
            bleReceiver.otaTimeOut();
        }
    }

}
