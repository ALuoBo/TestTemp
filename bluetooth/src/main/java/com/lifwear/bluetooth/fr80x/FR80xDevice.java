package com.lifwear.bluetooth.fr80x;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;

import com.lifwear.FileUtil;
import com.lifwear.bluetooth.BLEReceiver;
import com.lifwear.bluetooth.ByteUtil;
import com.lifwear.bluetooth.IBleRecNotify;
import com.lifwear.bluetooth.PeripheralDevice;
import com.lifwear.bluetooth.fr80x.data.OTAStatus;
import com.lifwear.bluetooth.fr80x.data.TestResult;
import com.lifwear.bluetooth.fr80x.data.TestTemp;
import com.lifwear.bluetooth.fr80x.operate.FR80xCommand;
import com.lifwear.bluetooth.fr80x.operate.FR80xOperate;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import androidx.annotation.NonNull;

/**
 * 温度计设备，数据处理和收发
 *
 * @author WillXia
 * @date 2021/9/27.
 */
public class FR80xDevice implements PeripheralDevice, IBleRecNotify {
    private static final String TAG = "FR80xVDevice";
    // 设备广播使用的 UUID ，用于扫描过滤
    public static final String GAP_UUID = "0000AEFF-0000-1000-8000-00805f9b34fb";

    private final UUID OTA_SERVICE_UUID, COMM_SERVICE_UUID;
    private final UUID OTA_CHARACTERISTIC_WRITE_UUID, OTA_CHARACTERISTIC_NOTIFY_UUID, OTA_UUID_DES;
    private final UUID COMM_CHARACTERISTIC_WRITE_UUID, COMM_CHARACTERISTIC_NOTIFY_UUID, COMM_UUID_DES;
    private final UUID[] otaCharacteristicUUIDs, commCharacteristicUUIDs;
    private BluetoothGatt bluetoothGatt;
    // notify 用于获取从设备的数据
    private BluetoothGattCharacteristic otaWriteGattCharacteristic, otaNotifyGattCharacteristic;
    private BluetoothGattCharacteristic commWriteGattCharacteristic, commNotifyGattCharacteristic;
    private long fileSize;
    private List<byte[]> fileBytePackage;
    // 升级文件分包个数
    private long filePackageCount;
    // 扇区大小 4 kb  , 4096 个字节占用内存 4KB
    private final int sectorSize = 4096;
    private int payloadLength;

    private long currentEraseAddr;
    private int currentFileCount;
    private long currentWriteAddr;

    {

        OTA_SERVICE_UUID = UUID.fromString("02f00000-0000-0000-0000-00000000fe00");
        OTA_CHARACTERISTIC_WRITE_UUID = UUID.fromString("02f00000-0000-0000-0000-00000000ff01");
        OTA_CHARACTERISTIC_NOTIFY_UUID = UUID.fromString("02f00000-0000-0000-0000-00000000ff02");
        OTA_UUID_DES = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

        COMM_SERVICE_UUID = UUID.fromString("0000FFF0-0000-1000-8000-00805F9B34FB");
        COMM_CHARACTERISTIC_WRITE_UUID = UUID.fromString("0000FFF3-0000-1000-8000-00805F9B34FB");
        COMM_CHARACTERISTIC_NOTIFY_UUID = UUID.fromString("0000FFF4-0000-1000-8000-00805F9B34FB");
        COMM_UUID_DES = UUID.fromString("00002901-0000-1000-8000-00805F9B34FB");
        otaCharacteristicUUIDs = new UUID[]{OTA_CHARACTERISTIC_WRITE_UUID, OTA_CHARACTERISTIC_NOTIFY_UUID};
        commCharacteristicUUIDs = new UUID[]{COMM_CHARACTERISTIC_WRITE_UUID, COMM_CHARACTERISTIC_NOTIFY_UUID};

    }

    private final FR80XCommSubject fr80XCommSubject;

    public void addDataObserver(BLEReceiver bleReceiver) {
        if (fr80XCommSubject != null) {
            fr80XCommSubject.addBleListener(bleReceiver);
        }
    }

    public void removeDataObserver(BLEReceiver bleReceiver) {
        if (fr80XCommSubject != null) {
            fr80XCommSubject.removeListeners(bleReceiver);
        }
    }

    /**
     * 是否处于 OTA 如果处于 OTA 则不在处理非 OTA 流程数据
     */
    private boolean inOTA = false;

    public boolean isInOTA() {
        return inOTA;
    }

    public void setInOTA(boolean inOTA) {
        this.inOTA = inOTA;
    }

    private final FR80XCommHandler mHandler;

    // 蓝牙操作实现类，需要传入 Gatt
    private final FR80xOperate operate;

    public FR80xDevice() {
        fr80XCommSubject = new FR80XCommSubject();
        HandlerThread handlerThread = new HandlerThread("TimerWorker");
        handlerThread.start();
        mHandler = new FR80XCommHandler(handlerThread.getLooper(), this);
        operate = new FR80xOperate(mHandler);
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

        if (newState == BluetoothProfile.STATE_CONNECTED) {
            fr80XCommSubject.onConnectStateChanged(true);
            // successfully connected to the GATT Server
            Log.d(TAG, "Device STATE_CONNECTED");
            // 修改 MTU
            requestMtu(gatt, 512);

        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            // 断开连接需要关闭上次的 GATT 否则会导致多个 GATT 在工作
            // disconnected from the GATT Server
            gatt.disconnect();
            gatt.close();
            bluetoothGatt = null;
            fr80XCommSubject.onConnectStateChanged(false);
            Log.d(TAG, "Device STATE_DISCONNECTED");
        }
    }

    @Override
    public void onServiceDiscover(BluetoothGatt bluetoothGatt, int status) {
        //获取到特定的服务不为空
        Log.d(TAG, "OnServiceDiscover current thread name " + Thread.currentThread().getName());
        this.bluetoothGatt = bluetoothGatt;
        if (status == BluetoothGatt.GATT_SUCCESS) {
            Log.d(TAG, "GATT_SUCCESS");

            // OTA service
            BluetoothGattService otaGattService = bluetoothGatt.getService(OTA_SERVICE_UUID);
            if (otaGattService != null) {
                Log.d(TAG, "otaGattService != null");
                for (UUID characteristicUUID : otaCharacteristicUUIDs) {
                    BluetoothGattCharacteristic gattCharacteristic = otaGattService.getCharacteristic(characteristicUUID);
                    if (gattCharacteristic.getUuid().equals(OTA_CHARACTERISTIC_WRITE_UUID)) {
                        otaWriteGattCharacteristic = gattCharacteristic;
                    } else if (gattCharacteristic.getUuid().equals(OTA_CHARACTERISTIC_NOTIFY_UUID)) {
                        otaNotifyGattCharacteristic = gattCharacteristic;
                        BluetoothGattDescriptor descriptor = gattCharacteristic.getDescriptor(OTA_UUID_DES);
                        if (descriptor != null) {
                            bluetoothGatt.setCharacteristicNotification(gattCharacteristic, true);
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            bluetoothGatt.writeDescriptor(descriptor);
                        }
                    }
                }

            }

            // Comm Service
            BluetoothGattService commGattService = bluetoothGatt.getService(COMM_SERVICE_UUID);
            if (commGattService != null) {
                Log.d(TAG, "commGattService != null");
                for (UUID characteristicUUID : commCharacteristicUUIDs) {
                    Log.d(TAG, "characteristicUUID: " + characteristicUUID.toString());
                    BluetoothGattCharacteristic gattCharacteristic = commGattService.getCharacteristic(characteristicUUID);
                    if (gattCharacteristic.getUuid().equals(COMM_CHARACTERISTIC_WRITE_UUID)) {
                        commWriteGattCharacteristic = gattCharacteristic;
                    } else if (gattCharacteristic.getUuid().equals(COMM_CHARACTERISTIC_NOTIFY_UUID)) {
                        commNotifyGattCharacteristic = gattCharacteristic;
                        BluetoothGattDescriptor descriptor = gattCharacteristic.getDescriptor(COMM_UUID_DES);
                        if (descriptor != null) {
                            bluetoothGatt.setCharacteristicNotification(gattCharacteristic, true);
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            bluetoothGatt.writeDescriptor(descriptor);
                        }
                    }
                }

            }

            // 过滤特征值为 null 时的情况，防止导致 NPL
            if (commWriteGattCharacteristic != null && commNotifyGattCharacteristic != null) {
                fr80XCommSubject.onServiceDiscovered();
            } else {
                Log.d(TAG, "GATT_SUCCESS_BUT_CHARACTER_NULL");
            }

        } else {
            Log.d(TAG, "GATT_FAIL");
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            Log.d(TAG, Arrays.toString(characteristic.getValue()));
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

    }

    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        fr80XCommSubject.onReadRemoteRssi(gatt, rssi, status);
    }

    @Override
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
        Log.d(TAG, "O M C---MTU = " + mtu);
        // MTU 修改之后执行通道建立
        gatt.discoverServices();
        // 重置 MTU 的大小
        mtuSize = mtu;
        // 计算每包大小
        payloadLength = mtuSize - 3 - 9;
        // 接口参数回传
        fr80XCommSubject.onMtuSizeChanged();
    }

    /*
     * MTU 的大小
     */
    private int mtuSize = 20;
    /**
     * 新固件可用的存储基地址
     */
    private long mcuBaseAddr = 0L;


    /**
     * 从此获取外设传过来的数据
     *
     * @param gatt
     * @param characteristic
     */
    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

        Log.d(TAG, "OTA?" + inOTA);

        // 成功
        String resultSuccessFlag = "00";
        // 失败
        String resultFailFlag = "01";
        // 数据结束
        String resultFinishFlag = "02";

        Log.e(TAG, "characteristic---NotifyGattCharacteristic");
        byte[] receiveValue = characteristic.getValue();
        // 获取应答返回码
        byte result = receiveValue[0];
        // 获取应答的命令字
        byte opCode = receiveValue[1];
        String opCodeStr = ByteUtil.Byte2Hex(opCode);
        String resultStr = ByteUtil.Byte2Hex(result);
        // 获取应答的数据内容
        byte[] content = Arrays.copyOfRange(receiveValue, 2, receiveValue.length);

        Log.e(TAG, "REC = " + ByteUtil.ByteArrToHex(receiveValue));

        if (characteristic.equals(otaNotifyGattCharacteristic)) {

            // 每收到一条 OTA 回复，即清除上一条命令的超时 handler message
            mHandler.removeMessages(FR80XCommHandler.OTA_TIME_OUT);
            // 判断应答的命令字
            if (FR80xCommand.GetAddrBase.getCommandHex().equals(opCodeStr)) {
                Log.d(TAG, "GetStrBase");
                // 接收到新固件可用存储基地址的应答
                onGetStrBase(receiveValue);
            } else if (FR80xCommand.GetMcuVersion.getCommandHex().equals(opCodeStr)) {
                Log.d(TAG, "RecMcuVersion");
                // 接收到获取固件版本号的应答
                onRecMcuVersion(receiveValue);

            } else if (FR80xCommand.EraseSector.getCommandHex().equals(opCodeStr)) {
                Log.d(TAG, "RecEraseSector");
                // 接收到擦除扇区的应答
                onRecEraseSector();
            } else if (FR80xCommand.WriteDataOtaFile.getCommandHex().equals(opCodeStr)) {
                Log.d(TAG, "RecWriteDataOtaFile");
                // 接收到写入烧写文件的应答
                onRecWriteDataOtaFile();
            } else if (FR80xCommand.RebootMcu.getCommandHex().equals(opCodeStr)) {
                Log.d(TAG, "RecRebootMcu");
                // 接收到重启 MCU 的应答
                onRecRebootMcu();
                // 升级成功
                OTAStatus otaStatus = new OTAStatus();
                otaStatus.setStatus(OTAStatus.END_SUCCESS);
                fr80XCommSubject.onDataChange(otaStatus);

            }

        }

        // fr80x 从 notify 中接收设备发送的数据
        if (characteristic.equals(commNotifyGattCharacteristic)) {

            if (FR80xCommand.testTemp.getCommandHex().equals(opCodeStr) && resultStr.equals(resultSuccessFlag)) {
                TestTemp testTemp = xTestTemp(content);
                fr80XCommSubject.onDataChange(testTemp);
                //sendTestTempResult(true);
            }

        }

    }

    private TestTemp xTestTemp(byte[] content) {

        TestTemp testTemp = new TestTemp();

        int aimPrefix = Integer.parseInt(ByteUtil.Byte2Hex(content[2]), 16);
        int aimSuffix = Integer.parseInt(ByteUtil.Byte2Hex(content[3]), 16);
        int enPrefix = Integer.parseInt(ByteUtil.Byte2Hex(content[4]), 16);
        int enSuffix = Integer.parseInt(ByteUtil.Byte2Hex(content[5]), 16);

        byte[] time = new byte[4];

        System.arraycopy(content, 6, time, 0, 4);

        testTemp.setAimTemp(Float.parseFloat(aimPrefix + "." + aimSuffix));
        testTemp.setEnvironmentTemp(Float.parseFloat(enPrefix + "." + enSuffix));

        testTemp.setTimestamp(ByteUtil.byteArr2Long(time, 0, 4));

        Log.d(TAG, "TestTemp === " + testTemp.toString());

        return testTemp;

    }

    private void sendTestTempResult(boolean isCorrect) {
        byte[] buffer = new byte[1];
        if (isCorrect) {
            buffer[0] = 0x00;
        } else {
            buffer[0] = 0x01;
        }
        sendData(FR80xCommand.testTemp, buffer, commWriteGattCharacteristic);
    }

    /*
     *//**
     * 发送时间戳
     *
     * @return
     *//*
    public boolean responseCurrentTimeStamp(boolean isCorrect) {
        byte[] resultTimeStamp = new byte[5];
        byte[] resultCode;
        if (isCorrect) {
            resultCode = ByteUtil.HexString2Bytes("00");
        } else {
            resultCode = ByteUtil.HexString2Bytes("01");
        }
        resultTimeStamp[0] = resultCode[0];
        long timeStamp = System.currentTimeMillis() / 1000;
        byte[] bytes = ByteUtil.long2ByteArr(timeStamp);
        System.arraycopy(bytes, 4, resultTimeStamp, 1, 4);

        return sendData(FR80xCommand.ResponseTimeStamp, resultTimeStamp, commWriteGattCharacteristic);

    }*/

    /**
     * 获取版本
     *
     * @return
     */
    public boolean getVersion() {
        return sendData(FR80xCommand.GetMcuVersion, otaWriteGattCharacteristic);
    }

    /**
     * 获取新固件的可用存储基地址
     *
     * @return
     */
    public boolean getBaseAddress() {

        return sendData(FR80xCommand.GetAddrBase, otaWriteGattCharacteristic);
    }

    /**
     * 发送重启指令，不带校验和
     *
     * @return
     */
    public boolean rebootMcu() {
        return sendData(FR80xCommand.RebootMcu, otaWriteGattCharacteristic);
    }

    /**
     * OTA 最后一步，发送重启指令，带文件大小和校验和
     *
     * @return
     */
    public boolean rebootMcuWithChecksum() {
        Log.d(TAG, "rebootMcuWithChecksum---fileSize = " + fileSize + ", crcCode = " + crcCode);
        byte[] buffer = new byte[8];
        // 文件大小，转为字节数组，小端模式
        buffer[0] = (byte) (fileSize & 0xff);
        buffer[1] = (byte) ((fileSize & 0xff00) >> 8);
        buffer[2] = (byte) ((fileSize & 0xff0000) >> 16);
        buffer[3] = (byte) ((fileSize & 0xff000000) >> 24);
        // 文件校验和，转为字节数组，小端模式
        buffer[4] = (byte) (crcCode & 0xff);
        buffer[5] = (byte) ((crcCode & 0xff00) >> 8);
        buffer[6] = (byte) ((crcCode & 0xff0000) >> 16);
        buffer[7] = (byte) ((crcCode & 0xff000000) >> 24);
        return sendData(FR80xCommand.RebootMcu, buffer, otaWriteGattCharacteristic);
    }

    /**
     * 从指定地址开始擦除扇区
     *
     * @return
     */
    public boolean eraseSector(long addr) {
        byte[] buffer = new byte[4];
        buffer[0] = (byte) (addr & 0xff);
        buffer[1] = (byte) ((addr >> 8) & 0xff);
        buffer[2] = (byte) ((addr >> 16) & 0xff);
        buffer[3] = (byte) ((addr >> 24) & 0xff);
        return sendData(FR80xCommand.EraseSector, buffer, otaWriteGattCharacteristic);
    }


    /**
     * 写入文件
     *
     * @return
     */
    public boolean sendFileData(long addr, byte[] dataPackage) {

        if (null == dataPackage) {
            Log.e(TAG, "DataPackage is null");
            return false;
        }
        // 获取当前文件分包的长度
        int payloadLength_ = dataPackage.length;
        // Base_addr的长度占用字节数 + Payload长度占用字节数 + 当前文件分包的长度
        int length_ = 2 + 4 + payloadLength_;
        // 创建预分包数组
        byte[] buffer = new byte[length_];
        // Base_addr
        buffer[0] = (byte) (addr & 0xff);
        buffer[1] = (byte) ((addr >> 8) & 0xff);
        buffer[2] = (byte) ((addr >> 16) & 0xff);
        buffer[3] = (byte) ((addr >> 24) & 0xff);
        // Payload_Length
        buffer[4] = (byte) (payloadLength_ & 0xff);
        buffer[5] = (byte) ((payloadLength_ >> 8) & 0xff);
        // 文件分包的字节数组，6~end
        System.arraycopy(dataPackage, 0, buffer, 6, payloadLength_);
        Log.d(TAG, "sendFileData = " + ByteUtil.bytes2HexString(buffer, buffer.length));

        return sendData(FR80xCommand.WriteDataOtaFile, buffer, otaWriteGattCharacteristic);
    }

    private long transferMcuBaseAddr(byte[] contentByteArr) {
        long addr = 0L;
        if (contentByteArr.length == 0) {
            return addr;
        }
        // 基地址的长度为 4 个字节，4567
        byte[] addrByteArr = Arrays.copyOfRange(contentByteArr, 4, contentByteArr.length);
        // 字节数组转换为整型，小端模式
        addr = ByteUtil.byteArr2LongSmallEnd(addrByteArr, 0, addrByteArr.length);
        Log.d(TAG, "addr = " + addr);
        return addr;
    }


    /**
     * 无内容，只有命令的数据包
     *
     * @param command
     * @param gattCharacteristic
     * @return
     */
    private boolean sendData(FR80xCommand command, BluetoothGattCharacteristic gattCharacteristic) {
        return sendData(command, null, gattCharacteristic);
    }

    /**
     * @param command
     * @param buffer  单包文件的长度
     * @return
     */

    private boolean sendData(FR80xCommand command, byte[] buffer, @NonNull BluetoothGattCharacteristic gattCharacteristic) {

        // length 字段，数据内容的长度
        int length_ = 0;
        if (null != buffer) {
            length_ = buffer.length;
        }

        int frameLength_ = length_ + 1 + 2;
        // 最终发送的通讯帧
        byte[] finalCmd = new byte[frameLength_];

        // 命令字
//        finalCmd[0] = (byte) (Integer.parseInt(command.getCommandHex(), 16) & 0xff);
        finalCmd[0] = ByteUtil.HexToByte(command.getCommandHex());
        // 数据内容长度
        finalCmd[1] = (byte) (length_ & 0xff);
        finalCmd[2] = (byte) ((length_ >> 8) & 0xff);
        // 数据内容
        if (null != buffer) {
            System.arraycopy(buffer, 0, finalCmd, 3, length_);
        }

        Log.e(TAG, "SEND: " + ByteUtil.ByteArrToHex(finalCmd));
        // 判断是否需要超时重试
        if (command.isNeedCheckTimeout()) {

            mHandler.sendEmptyMessageDelayed(FR80XCommHandler.OTA_TIME_OUT, 10000);
        }

        gattCharacteristic.setValue(finalCmd);
        return bluetoothGatt.writeCharacteristic(gattCharacteristic);

    }


    /**
     * ***************************************************************************************
     * BLE 获取 MCU 的应答处理
     * ***************************************************************************************
     */

    @Override
    public void onRecNvdsType() {
        Log.d(TAG, "onRecNvdsType");
    }

    /**
     * 擦除扇区的个数
     */
    private long eraseSectorSize = 0;
    /**
     * 当前擦除扇区的个数
     */
    private long eraseSectorCount = 0;

    @Override
    public void onRecMcuVersion(byte[] receive) {
        Log.d(TAG, "onRecMcuVersion = " + ByteUtil.ByteArrToHex(receive));
        // 截取得到版本号对应的字节数组
        byte[] versionByteArr = Arrays.copyOfRange(receive, 4, receive.length);
        Log.d(TAG, "onRecMcuVersion = " + ByteUtil.ByteArrToHex(versionByteArr));
        // 将该字节数组通过小端模式进行解析
        long mcuVersion = ByteUtil.byteArr2LongSmallEnd(versionByteArr, 0, versionByteArr.length);
        Log.d(TAG, "mcuVersion = " + mcuVersion);

    }

    @Override
    public void onGetStrBase(byte[] receive) {
        Log.d(TAG, "onGetStrBase REC = " + ByteUtil.ByteArrToHex(receive));
        // 解析获取新固件的可用存储基地址
        mcuBaseAddr = transferMcuBaseAddr(receive);
        Log.d(TAG, "onGetStrBase mcuBaseAddr = " + mcuBaseAddr);

        // 初始化变量，需要擦除扇区的总个数
        eraseSectorSize = fileSize / sectorSize;
        if ((fileSize % sectorSize) != 0) {
            eraseSectorSize++;
        }
        // 初始化变量，当前擦除扇区数
        eraseSectorCount = 0;
        // 初始化变量，当前擦除扇区首地址
        currentEraseAddr = mcuBaseAddr;

        // 开始擦除
        Log.d(TAG, "2---开始擦除扇区，current: " + eraseSectorCount + "---total: " + eraseSectorSize);
        eraseSector(currentEraseAddr);

    }


    @Override
    public void onRecEraseSector() {
        Log.d(TAG, "接收擦除扇区指令应答，current: " + eraseSectorCount + "---total: " + eraseSectorSize);
        if (eraseSectorCount < eraseSectorSize - 1) {
            // 继续擦下一个扇区
            eraseSectorCount++;
            currentEraseAddr += sectorSize;
            eraseSector(currentEraseAddr);
        } else {
            Log.d(TAG, "擦除完成，开始发送升级包");
            eraseSectorCount = 0;
            // 如果擦除完成，进行下一步，传输文件
            if (null != fileBytePackage && !fileBytePackage.isEmpty()) {
                currentWriteAddr = mcuBaseAddr;
                currentFileCount = 0;
                Log.d(TAG, "开始发送升级包，current: " + currentFileCount + "---total: " + filePackageCount);
                sendFileData(currentWriteAddr, fileBytePackage.get(currentFileCount));
            }
        }
    }

    @Override
    public void onRecWriteDataOtaFile() {
        // FR80xDeviceData updateData = new FR80xDeviceData();

        long progress = (long) ((float) currentFileCount / (float) filePackageCount * 100);

        Log.d(TAG, "接收升级包的应答---progress = " + progress);
        // updateData.setCurrentLoad(progress);
        //  fr80XCommSubject.onDataChange(updateData);
        Log.d(TAG, "接收升级包的应答---current = " + currentFileCount + ", total = " + filePackageCount);
        if (currentFileCount < filePackageCount - 1) {
            // 上一包写入成功之后 当前包数+1
            currentFileCount++;
            currentWriteAddr += payloadLength;
            sendFileData(currentWriteAddr, fileBytePackage.get(currentFileCount));
        } else {
            Log.d(TAG, "升级包发送完成，开始发送带校验和的重启指令");
            currentFileCount = 0;
            rebootMcuWithChecksum();
        }
    }

    @Override
    public void onRecRebootMcu() {
        Log.d(TAG, "onRecRebootMcu");

        // FR80xDeviceData deviceData = new FR80xDeviceData();
        // deviceData.setOtaFinish(true);
        //  fr80XCommSubject.onDataChange(deviceData);
    }

    public void changeMtu() {
        requestMtu(bluetoothGatt, 512);
        Log.d(TAG, "changeMtu: changeMtu");
    }

    /**
     * @param fileByte
     * @return
     */

    public void subpackageFile(byte[] fileByte) {

        fileBytePackage = new ArrayList<>();
        // 计算分包数
        fileSize = fileByte.length;
        filePackageCount = fileSize / payloadLength;
        if (fileByte.length % payloadLength != 0) {
            filePackageCount++;
        }

        for (int i = 0; i < filePackageCount; i++) {
            // 每次截取 packageSize 的 字节 作为一包
            byte[] dataPackage;
            // 不补全，最后一帧长度可能不满 (mtuSize - 3 - 9)
            if (i < filePackageCount - 1) {
                dataPackage = Arrays.copyOfRange(fileByte, i * payloadLength, (i + 1) * payloadLength);
            } else {
                dataPackage = Arrays.copyOfRange(fileByte, i * payloadLength, fileByte.length);
            }
            fileBytePackage.add(dataPackage);
        }
        getBaseAddress();
    }

    private int crcCode;

    public void setCRCCode(int crcCode) {
        this.crcCode = crcCode;
        Log.d(TAG, "crcCode " + crcCode);
    }


    /**
     * OTA 超时回调
     */
    void otaTimeOut() {
        // 复位 OTA 标识
        inOTA = false;
        mHandler.removeMessages(FR80XCommHandler.OTA_TIME_OUT);
        fr80XCommSubject.onOTATimeOut();

    }


    /**
     * 获取设备 Rssi
     */
    public void readRssi() {
        if (bluetoothGatt != null) {
            bluetoothGatt.readRemoteRssi();
        }
    }

    private TestResult translateTestResult(byte[] content) {

        int testKind = Integer.parseInt(ByteUtil.Byte2Hex(content[2]), 16);
        int wearState = Integer.parseInt(ByteUtil.Byte2Hex(content[3]), 16);
        int chargeState = Integer.parseInt(ByteUtil.Byte2Hex(content[4]), 16);
        int tempState = Integer.parseInt(ByteUtil.Byte2Hex(content[5]), 16);
        int tempPrefix = Integer.parseInt(ByteUtil.Byte2Hex(content[6]), 16);
        int tempSuffix = Integer.parseInt(ByteUtil.Byte2Hex(content[7]), 16);

        String strTemp = tempPrefix + "." + tempSuffix;
        float temp = Float.parseFloat(strTemp);

        Log.d(TAG, "translateTestResult: " + testKind + wearState + chargeState + tempState + temp);
        return new TestResult(testKind, wearState, chargeState, tempState, temp);
    }


    public void sendFile(String mcuFilePath) {
        InputStream inputStream = null;
        String fileHexStr = "";
        // 获取文件的 HexStr
        if (null != mcuFilePath && !TextUtils.isEmpty(mcuFilePath)) {
            // 路径不为空，则从 SD 卡中获取文件流
            FileInputStream fileInputStream;
            try {
                fileInputStream = new FileInputStream(mcuFilePath);

                fileHexStr = FileUtil.streamToHexMcu(fileInputStream);

                // 获取计算 CRC 所需的 inputStream，需要重新从文件生成流对象， FileUtil.streamToHexMcu 操作会将流写空
                inputStream = new FileInputStream(mcuFilePath);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        //crc 校验
        int fileCRC = 0;
        try {
            fileCRC = getCRC32new(inputStream);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 非空判断
        if (TextUtils.isEmpty(fileHexStr)) {
            Log.e(TAG, "FILE HEX STR NULL!");
            return;
        }
        // 转为字节数组
        Log.d(TAG, "HexString2Bytes---START");
        byte[] fileByte = ByteUtil.HexString2Bytes(fileHexStr);
        Log.d(TAG, "HexString2Bytes---END");
        // 文件校验和
        Log.d(TAG, "checkBinSum---START");
        byte[] fileCheckSumByte = ByteUtil.checkBinSum(fileByte);
        Log.d(TAG, "checkBinSum---END");

        Log.d(TAG, "bytes2HexString---START");
        String fileCheckSumHex = ByteUtil.bytes2HexString(fileCheckSumByte, fileCheckSumByte.length);
//        String fileCheckSumHex = bytes2HexString(fileCheckSumByte, fileCheckSumByte.length);
        Log.d(TAG, "bytes2HexString---END");
        // 文件大小
        Log.d(TAG, "fileByte.length---START");
        int fileSize = fileByte.length;
        Log.d(TAG, "fileByte.length---END");
        byte[] bytes = ByteUtil.int2byte(fileSize);
        String fileSizeHex = ByteUtil.bytes2HexString(bytes, bytes.length);
        // 将文件的字节数组发送至蓝牙 Module
        subpackageFile(fileByte);
        setCRCCode(fileCRC);
    }


    private int Crc32CalByByte(int oldcrc, byte[] ptr, int offset, int len) {
        int crc = oldcrc;
        int i = offset;
        while (len-- != 0) {
            int high = crc / 256; //取CRC高8位
            crc <<= 8;
            crc ^= crc_ta_8[(high ^ ptr[i]) & 0xff];
            crc &= 0xFFFFFFFF;
            i++;
        }
        return crc & 0xFFFFFFFF;
    }

    public int getCRC32new(InputStream inputStream) throws IOException {

        int read_count;
        InputStream input = new BufferedInputStream(inputStream);
        byte[] inputBuffer = new byte[256];
        int crcInit = 0;
        int couts = 0;
        while (((read_count = input.read(inputBuffer, 0, 256)) != -1)) {
            if (couts != 0) {
                crcInit = Crc32CalByByte(crcInit, inputBuffer, 0, read_count);
            }
            couts++;
        }
        inputStream.close();
        input.close();
        return crcInit;
    }

    /* CRC 字节余式表 */
    private final int crc_ta_8[] = new int[]{
            0x00000000, 0x77073096, 0xee0e612c, 0x990951ba,
            0x076dc419, 0x706af48f, 0xe963a535, 0x9e6495a3, 0x0edb8832,
            0x79dcb8a4, 0xe0d5e91e, 0x97d2d988, 0x09b64c2b, 0x7eb17cbd,
            0xe7b82d07, 0x90bf1d91, 0x1db71064, 0x6ab020f2, 0xf3b97148,
            0x84be41de, 0x1adad47d, 0x6ddde4eb, 0xf4d4b551, 0x83d385c7,
            0x136c9856, 0x646ba8c0, 0xfd62f97a, 0x8a65c9ec, 0x14015c4f,
            0x63066cd9, 0xfa0f3d63, 0x8d080df5, 0x3b6e20c8, 0x4c69105e,
            0xd56041e4, 0xa2677172, 0x3c03e4d1, 0x4b04d447, 0xd20d85fd,
            0xa50ab56b, 0x35b5a8fa, 0x42b2986c, 0xdbbbc9d6, 0xacbcf940,
            0x32d86ce3, 0x45df5c75, 0xdcd60dcf, 0xabd13d59, 0x26d930ac,
            0x51de003a, 0xc8d75180, 0xbfd06116, 0x21b4f4b5, 0x56b3c423,
            0xcfba9599, 0xb8bda50f, 0x2802b89e, 0x5f058808, 0xc60cd9b2,
            0xb10be924, 0x2f6f7c87, 0x58684c11, 0xc1611dab, 0xb6662d3d,
            0x76dc4190, 0x01db7106, 0x98d220bc, 0xefd5102a, 0x71b18589,
            0x06b6b51f, 0x9fbfe4a5, 0xe8b8d433, 0x7807c9a2, 0x0f00f934,
            0x9609a88e, 0xe10e9818, 0x7f6a0dbb, 0x086d3d2d, 0x91646c97,
            0xe6635c01, 0x6b6b51f4, 0x1c6c6162, 0x856530d8, 0xf262004e,
            0x6c0695ed, 0x1b01a57b, 0x8208f4c1, 0xf50fc457, 0x65b0d9c6,
            0x12b7e950, 0x8bbeb8ea, 0xfcb9887c, 0x62dd1ddf, 0x15da2d49,
            0x8cd37cf3, 0xfbd44c65, 0x4db26158, 0x3ab551ce, 0xa3bc0074,
            0xd4bb30e2, 0x4adfa541, 0x3dd895d7, 0xa4d1c46d, 0xd3d6f4fb,
            0x4369e96a, 0x346ed9fc, 0xad678846, 0xda60b8d0, 0x44042d73,
            0x33031de5, 0xaa0a4c5f, 0xdd0d7cc9, 0x5005713c, 0x270241aa,
            0xbe0b1010, 0xc90c2086, 0x5768b525, 0x206f85b3, 0xb966d409,
            0xce61e49f, 0x5edef90e, 0x29d9c998, 0xb0d09822, 0xc7d7a8b4,
            0x59b33d17, 0x2eb40d81, 0xb7bd5c3b, 0xc0ba6cad, 0xedb88320,
            0x9abfb3b6, 0x03b6e20c, 0x74b1d29a, 0xead54739, 0x9dd277af,
            0x04db2615, 0x73dc1683, 0xe3630b12, 0x94643b84, 0x0d6d6a3e,
            0x7a6a5aa8, 0xe40ecf0b, 0x9309ff9d, 0x0a00ae27, 0x7d079eb1,
            0xf00f9344, 0x8708a3d2, 0x1e01f268, 0x6906c2fe, 0xf762575d,
            0x806567cb, 0x196c3671, 0x6e6b06e7, 0xfed41b76, 0x89d32be0,
            0x10da7a5a, 0x67dd4acc, 0xf9b9df6f, 0x8ebeeff9, 0x17b7be43,
            0x60b08ed5, 0xd6d6a3e8, 0xa1d1937e, 0x38d8c2c4, 0x4fdff252,
            0xd1bb67f1, 0xa6bc5767, 0x3fb506dd, 0x48b2364b, 0xd80d2bda,
            0xaf0a1b4c, 0x36034af6, 0x41047a60, 0xdf60efc3, 0xa867df55,
            0x316e8eef, 0x4669be79, 0xcb61b38c, 0xbc66831a, 0x256fd2a0,
            0x5268e236, 0xcc0c7795, 0xbb0b4703, 0x220216b9, 0x5505262f,
            0xc5ba3bbe, 0xb2bd0b28, 0x2bb45a92, 0x5cb36a04, 0xc2d7ffa7,
            0xb5d0cf31, 0x2cd99e8b, 0x5bdeae1d, 0x9b64c2b0, 0xec63f226,
            0x756aa39c, 0x026d930a, 0x9c0906a9, 0xeb0e363f, 0x72076785,
            0x05005713, 0x95bf4a82, 0xe2b87a14, 0x7bb12bae, 0x0cb61b38,
            0x92d28e9b, 0xe5d5be0d, 0x7cdcefb7, 0x0bdbdf21, 0x86d3d2d4,
            0xf1d4e242, 0x68ddb3f8, 0x1fda836e, 0x81be16cd, 0xf6b9265b,
            0x6fb077e1, 0x18b74777, 0x88085ae6, 0xff0f6a70, 0x66063bca,
            0x11010b5c, 0x8f659eff, 0xf862ae69, 0x616bffd3, 0x166ccf45,
            0xa00ae278, 0xd70dd2ee, 0x4e048354, 0x3903b3c2, 0xa7672661,
            0xd06016f7, 0x4969474d, 0x3e6e77db, 0xaed16a4a, 0xd9d65adc,
            0x40df0b66, 0x37d83bf0, 0xa9bcae53, 0xdebb9ec5, 0x47b2cf7f,
            0x30b5ffe9, 0xbdbdf21c, 0xcabac28a, 0x53b39330, 0x24b4a3a6,
            0xbad03605, 0xcdd70693, 0x54de5729, 0x23d967bf, 0xb3667a2e,
            0xc4614ab8, 0x5d681b02, 0x2a6f2b94, 0xb40bbe37, 0xc30c8ea1,
            0x5a05df1b, 0x2d02ef8d,
    };


    /**
     * 当校准通过时，从数据内容中获取校准参数
     *
     * @param params 不含校准结果，3 个校准参数组成的 byte[]
     */
    private float[] xCalibration(byte[] params) {

        byte[] param1 = new byte[5];
        byte[] param2 = new byte[5];
        byte[] param3 = new byte[5];

        System.arraycopy(params, 0, param1, 0, 5);
        System.arraycopy(params, 5, param2, 0, 5);
        System.arraycopy(params, 10, param3, 0, 5);

        return new float[]{calibrationParam(param1),
                calibrationParam(param2),
                calibrationParam(param3)};

    }

    /**
     * 每个校准参数 byte -> float
     */
    private float calibrationParam(byte[] param) {
        // 符号位
        int isPos = Integer.parseInt(ByteUtil.Byte2Hex(param[0]), 16);

        byte[] prefixByte = new byte[2];
        byte[] suffixByte = new byte[2];

        System.arraycopy(param, 1, prefixByte, 0, 2);
        System.arraycopy(param, 3, suffixByte, 0, 2);
        // 整数位
        int prefix = Integer.parseInt(ByteUtil.ByteArrToHex(prefixByte), 16);
        int suffix = Integer.parseInt(ByteUtil.ByteArrToHex(suffixByte), 16);
        // 小数位
        String paramStr = prefix + "." + suffix;
        if (isPos == 0) {
            //正数
            return Float.parseFloat(paramStr);
        } else {
            // 负数
            return 0 - Float.parseFloat(paramStr);
        }

    }


    /**
     * 同步时间
     *
     * @return
     */
    public boolean setTimeStamp() {
        byte[] buffer = new byte[4];
        long timeStamp = System.currentTimeMillis() / 1000;
        Log.d(TAG, "timeStamp" + timeStamp);
        byte[] bytes = ByteUtil.long2ByteArr(timeStamp);
        System.arraycopy(bytes, 4, buffer, 0, 4);
        return sendData(FR80xCommand.SendTimeStamp, buffer, commWriteGattCharacteristic);
    }


    /**
     * 下发温度阈值
     *
     * @param limits
     * @return
     */
    public boolean setLimitTemp(float[] limits) {
        byte[] buffer = new byte[18];
        for (int i = 0; i < limits.length; i++) {
            float limit = limits[i];

            DecimalFormat decimalFormat = new DecimalFormat("00.00");
            String format = decimalFormat.format(limit);
            String[] nums = format.split("\\.");
            // 小数点前的整数
            int subInt = Integer.parseInt(nums[0]);
            if (subInt < 0) {
                // 去掉 “ - ”
                subInt = Integer.parseInt(nums[0].substring(1));
            }
            // 小数
            int subDecimal = Integer.parseInt(nums[1]);
            byte[] limitItem = new byte[3];
            if (limit < 0) {
                limitItem[0] = (byte) 0x01;
            }
            limitItem[1] = (byte) subInt;
            limitItem[2] = (byte) subDecimal;
            System.arraycopy(limitItem, 0, buffer, limitItem.length * i, 3);
            Log.d(TAG, "limitItem: " + ByteUtil.ByteArrToHex(limitItem));
            Log.d(TAG, "limitItem length: " + limitItem.length);
        }
        Log.d(TAG, "buffer: " + ByteUtil.ByteArrToHex(buffer));
        return sendData(FR80xCommand.ChangeLimitTemp, buffer, commWriteGattCharacteristic);
    }


    public void sendPCBA() {
        operate.sendPCBA(bluetoothGatt, commWriteGattCharacteristic);
    }

    public void sendAssembly() {
        operate.sendAssembly(bluetoothGatt, commWriteGattCharacteristic);
    }

    public void sendTotal() {
        operate.sendTotal(bluetoothGatt, commWriteGattCharacteristic);
    }

    public void calibrationTemp(String temp, int step, int totalStep) {
        operate.calibrationTemp(temp, step, totalStep, bluetoothGatt, commWriteGattCharacteristic);
    }

    public void getTemp(String temp) {
        operate.getTemp(temp, bluetoothGatt, commWriteGattCharacteristic);
    }

}
