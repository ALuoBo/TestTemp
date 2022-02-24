package com.lifwear.bluetooth.fr80x.operate;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import com.lifwear.bluetooth.ByteUtil;
import com.lifwear.bluetooth.fr80x.FR80XCommHandler;

import androidx.annotation.NonNull;

/**
 * 封装所有自定义的蓝牙操作（除 OTA ： OTA 为芯片厂商固定流程）
 *
 * @author WillXia
 * @date 2022/1/6.
 */
public class FR80xOperate {
    private static final String TAG = "FR80xOperate";

    private FR80XCommHandler handler;

    public FR80xOperate(FR80XCommHandler handler) {
        this.handler = handler;
    }

    /**
     * PCBA 测试
     */
    public void sendPCBA(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        byte[] content = new byte[8];
        content[0] = ByteUtil.HexToByte("1");
        sendOperate(FR80xCommand.StartTest, content, gatt, characteristic);
    }

    /**
     * 总成测试
     */
    public void sendAssembly(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

        byte[] content = new byte[8];
        content[0] = ByteUtil.HexToByte("2");
        sendOperate(FR80xCommand.StartTest, content, gatt, characteristic);
    }

    /**
     * 整机测试
     */
    public void sendTotal(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        byte[] content = new byte[8];
        content[0] = ByteUtil.HexToByte("3");
        sendOperate(FR80xCommand.StartTest, content, gatt, characteristic);
    }

    /**
     * 校准温度计
     *
     * @param step 代表当前的温度值为第 N 次下发的温度
     * @param totalStep 代表当前校准一共需要多少个温度值
     */
    public void calibrationTemp(String temp, int step, int totalStep, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        byte[] content = new byte[8];
        content[0] = ByteUtil.HexToByte("4");
        String[] tempNum = temp.split("\\.");

        // 温度值整数位
        content[1] = ByteUtil.int2byte1(Integer.parseInt(tempNum[0]));
        // 温度值小数位
        content[2] = ByteUtil.int2byte1(Integer.parseInt(tempNum[1]));

        content[3] = ByteUtil.int2byte1(step);
        content[4] = ByteUtil.int2byte1(totalStep);
        sendOperate(FR80xCommand.StartTest, content, gatt, characteristic);
    }

    /**
     * 获取温度测试
     */
    public void getTemp(String temp, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        byte[] content = new byte[8];
        content[0] = ByteUtil.HexToByte("5");
        String[] tempNum = temp.split("\\.");
        // 温度值整数位
        content[1] = ByteUtil.int2byte1(Integer.parseInt(tempNum[0]));
        // 温度值小数位
        content[2] = ByteUtil.int2byte1(Integer.parseInt(tempNum[1]));
        sendOperate(FR80xCommand.StartTest, content, gatt, characteristic);
    }

    private void sendOperate(FR80xCommand command, byte[] buffer, @NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic gattCharacteristic) {

        // length 字段，数据内容的长度
        int length_ = 0;
        if (null != buffer) {
            length_ = buffer.length;
        }

        int frameLength_ = length_ + 1 + 2;
        // 最终发送的通讯帧
        byte[] finalCmd = new byte[frameLength_];

        // 命令字
        finalCmd[0] = ByteUtil.HexToByte(command.getCommandHex());
        // 数据内容长度
        finalCmd[1] = (byte) (length_ & 0xff);
        finalCmd[2] = (byte) ((length_ >> 8) & 0xff);

        // 数据内容
        if (null != buffer) {
            System.arraycopy(buffer, 0, finalCmd, 3, length_);
        }
        Log.e(TAG, "SEND: " + ByteUtil.ByteArrToHex(finalCmd));
        gattCharacteristic.setValue(finalCmd);
        gatt.writeCharacteristic(gattCharacteristic);
    }
}
