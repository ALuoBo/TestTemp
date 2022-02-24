package com.lifwear.bluetooth;

import java.util.ArrayList;
import java.util.List;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

/**
 * 蓝牙{开关}{连接}状态通知者
 *
 * @author WillXia
 * @date 2021/8/26.
 */
public class BtStateSubject{

    private final List<BtStateObserver> observers = new ArrayList<>();

    /**
     * 移除观察者
     *
     * @param observer
     */
    public void removeObserver(BtStateObserver observer) {
        if (observers.contains(observer)){
            observers.remove(observer);
        }
    }

    /**
     * 添加观察者
     */
    public void addObserver(BtStateObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }
    /**
     * 通知蓝牙开关状态改变
     */
    public void btStateChange(int state) {
        for (BtStateObserver observer :
                observers) {
            observer.stateChange(state);
        }
    }

    public void scanState(String action) {
        for (BtStateObserver observer :
                observers) {
            observer.scanChange(action);
        }

    }

}
