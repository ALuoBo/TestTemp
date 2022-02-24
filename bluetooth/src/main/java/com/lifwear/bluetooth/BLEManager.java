package com.lifwear.bluetooth;

import android.app.Activity;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.util.Log;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 处理蓝牙开关
 *
 * @author WillXia
 * @date 2021/8/25.
 */
public class BLEManager {

    private static final String TAG = "BLEManager";

    public static final String SCAN_START = "SCAN_START";
    public static final String SCAN_STOP = "SCAN_STOP";

    private static volatile BLEManager INSTANCE;
    private BluetoothAdapter bluetoothAdapter;

    private BroadcastReceiver broadcastReceiver;

    /**
     * 蓝牙状态的通知者
     */
    private BtStateSubject btStateSubject;
    private boolean scanning;
    private static final long SCAN_PERIOD = 30000;
    /**
     * 用于防止
     */
    private final ScheduledExecutorService executorService;
    private ScheduledFuture<?> scanSchedule;

    private boolean isInitialize = false;

    private BLEManager() {
        executorService = Executors.newScheduledThreadPool(1);
    }


    public static BLEManager getInstance(Application application) {
        if (INSTANCE == null) {
            INSTANCE = new BLEManager();
            INSTANCE.initialize(application);
        }
        return INSTANCE;
    }

    private void initialize(Application application) {
        //检查是否支持BLE
        if (!application.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            return;
        }
        //获取蓝牙配置器
        BluetoothManager bluetoothManager = (BluetoothManager) application.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null || bluetoothManager.getAdapter() == null) {
            return;
        }

        bluetoothAdapter = bluetoothManager.getAdapter();

        btStateSubject = new BtStateSubject();

        Log.d(TAG, "initialize");
        // 添加蓝牙各种状态的广播
        if (broadcastReceiver == null) {
            broadcastReceiver = new BtStateReceiver();
            IntentFilter filter;
            filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            application.registerReceiver(broadcastReceiver, filter);
        }
        isInitialize = true;
    }


    //判断蓝牙是否开启
    public boolean bleIsEnable() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    //判断蓝牙是否开启，如果关闭则请求打开蓝牙
    public void EnableBT(Activity activity) {
        if (!bleIsEnable()) {
            // 请求打开蓝牙
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(intent, 1);

        }
    }

    public void startScan(ScanCallback scanCallback) {
        Log.d(TAG, " short");

        startScan(scanCallback, false);
    }


    public void startScan(ScanCallback scanCallback, boolean persistentScan) {
        Log.d(TAG, " short");

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        startScan(null, settings, scanCallback, persistentScan, SCAN_PERIOD);
    }

    /**
     * BLE 扫描
     *
     * @param filters        扫描过滤条件
     * @param scanSettings   扫描配置
     * @param scanCallback   扫描结果回调
     * @param persistentScan 是否持续扫描
     */
    public void startScan(List<ScanFilter> filters, ScanSettings scanSettings, ScanCallback scanCallback, boolean persistentScan, long scanTime) {
        if (bleIsEnable()) {
            Log.d(TAG, "startScan");
            BluetoothLeScanner bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner(); //如果未启用蓝牙，则 getBluetoothLeScanner() 返回 null。
            if (!scanning) {
                if (!persistentScan) {
                    // If not persistentScan need  Stops scanning after a predefined scan period.
                    Runnable runnable = () -> {
                        if (scanning) {
                            Log.d(TAG, "schedule stopScan");
                            scanning = false;
                            bluetoothLeScanner.stopScan(scanCallback);
                            btStateSubject.scanState(SCAN_STOP);
                        }
                    };
                    scanSchedule = executorService.schedule(runnable, scanTime, TimeUnit.MILLISECONDS);
                }

                btStateSubject.scanState(SCAN_START);
                scanning = true;
                bluetoothLeScanner.startScan(filters, scanSettings, scanCallback);
            } else {
                Log.d(TAG, "startScan  but is already scanning , so continue last time scan task");
                btStateSubject.scanState(SCAN_START);
            }
        }
    }

    public class BtStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive" + intent.getAction());
            //获取蓝牙广播  本地蓝牙适配器的状态改变时触发
            String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                //获取蓝牙广播中的蓝牙新状态
                int state = bluetoothAdapter.getState();
                btStateSubject.btStateChange(bluetoothAdapter.getState());
                switch (state) {
                    //正在打开蓝牙
                    case BluetoothAdapter.STATE_TURNING_ON:
                        break;
                    //蓝牙已打开
                    case BluetoothAdapter.STATE_ON:
                        break;
                    //正在关闭蓝牙
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        break;
                    //蓝牙已关闭
                    case BluetoothAdapter.STATE_OFF:
                        // 蓝牙关闭，扫描立刻停止
                        scanning = false;
                        btStateSubject.scanState(SCAN_STOP);
                        break;
                }
            } else if (action.equals(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)) {

            }

        }

    }

    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    public void addObserver(BtStateObserver observer) {
        btStateSubject.addObserver(observer);
    }

    public void removeObserver(BtStateObserver observer) {
        btStateSubject.removeObserver(observer);
    }

    public boolean isScanning() {
        return scanning;
    }

    public void stopScan(ScanCallback scanCallback) {
        Log.d(TAG, "stopScan() ");
        scanning = false;
        bluetoothAdapter.getBluetoothLeScanner().stopScan(scanCallback);
        if (scanSchedule != null) {
            scanSchedule.cancel(false);
        }
        btStateSubject.scanState(SCAN_STOP);
    }
}
