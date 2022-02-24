package com.lifwear.bluetooth.fr80x;


import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.lifwear.bluetooth.fr80x.FR80xDevice;

import androidx.annotation.NonNull;

/**
 * 负责超时以及重试的逻辑
 *
 * @author WillXia
 * @date 2021/9/29.
 */
public class FR80XCommHandler extends Handler {

    private static final String TAG = "FR80XCommHandler";

    private final FR80xDevice fr80xDevice;

    //--------------------异常码 100 + ---------------------//
    public static final int OTA_TIME_OUT = 100;


    public FR80XCommHandler(@NonNull Looper looper, FR80xDevice fr80xDevice) {
        super(looper);
        this.fr80xDevice = fr80xDevice;

    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        super.handleMessage(msg);

        switch (msg.what) {
            case OTA_TIME_OUT:
                fr80xDevice.otaTimeOut();
                break;
        }
    }
}
