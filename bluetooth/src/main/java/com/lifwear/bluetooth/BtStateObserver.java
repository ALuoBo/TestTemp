package com.lifwear.bluetooth;
/**
 * @author WillXia
 * @date 2021/8/26.
 */
public interface BtStateObserver {
    void stateChange(int state);
    default void scanChange(String action){}
}
