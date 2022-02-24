package com.lifwear.bluetooth;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.LifecycleService;

/**
 * 与 BLE 设备连接通信的前台服务
 */
public class BLECommService extends LifecycleService {

    private static final String TAG = "BLECommService";

    private String NOTIFY_BLE = "蓝牙连接";

    private final Binder binder = new LocalBinder();

    private BluetoothAdapter bluetoothAdapter;

    private BluetoothGatt bluetoothGatt;

    /**
     * 注意设备只在连接的时候实际进行初始化
     */
    private PeripheralDevice peripheralDevice;

    @Nullable
    public PeripheralDevice getPeripheralDevice() {
        return peripheralDevice;
    }

    @Nullable
    @Override
    public IBinder onBind(@NonNull Intent intent) {
        super.onBind(intent);
        Log.e(TAG, "onCreate");
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "onCreate");
        // 发送通知，把service置于前台
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        // 从Android 8.0开始，需要注册通知通道
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFY_BLE,
                    "蓝牙连接", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(notificationChannel);
            notificationChannel.enableVibration(false);
            notificationChannel.setVibrationPattern(new long[]{0});

        }
        Notification notification =
                new NotificationCompat.Builder(this, NOTIFY_BLE)
                        // .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("蓝牙连接")
                        .build();
        // 注意第一个参数不能为0
        startForeground(666, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "onStartCommand");
        return super.onStartCommand(intent, flags, startId);

    }

    /**
     * 获取 bluetoothAdapter
     *
     * @return
     */
    public boolean initialize() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        return true;
    }

    /**
     * 连接蓝牙
     *
     * @param address 设备 mac
     * @return
     */
    public boolean connect(final @NonNull String address, PeripheralDevice deviceType) {
        Log.d(TAG, "connect");
        if (bluetoothAdapter == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        peripheralDevice = deviceType;
        try {
            Log.d(TAG, "thread name " + Thread.currentThread().getName());
            final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
            // connect to the GATT server on the device
            bluetoothGatt = device.connectGatt(this, false, bluetoothGattCallback);
            Log.w(TAG, "Device connect");
            return true;
        } catch (IllegalArgumentException exception) {
            Log.w(TAG, "Device not found with provided address.");
            return false;
        }

    }

    public void disConnect() {
        Log.w(TAG, "disConnect");
        if (bluetoothGatt != null) {
            Log.w(TAG, "bluetoothGatt != null ");
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
        }
    }

    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d(TAG, "Device connected" + Thread.currentThread().getName());
            peripheralDevice.onConnectionStateChange(gatt, status, newState);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Log.d(TAG, "onServicesDiscovered");
            peripheralDevice.onServiceDiscover(gatt, status);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            peripheralDevice.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            peripheralDevice.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            peripheralDevice.onCharacteristicChanged(gatt, characteristic);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            peripheralDevice.onReadRemoteRssi(gatt, rssi, status);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            Log.d(TAG,"onMtuChanged");
            peripheralDevice.onMtuChanged(gatt, mtu, status);
        }

    };

    public class LocalBinder extends Binder {
        public BLECommService getService() {
            return BLECommService.this;
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "is destroyed");
    }
}

