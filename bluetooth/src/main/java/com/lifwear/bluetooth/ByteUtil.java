package com.lifwear.bluetooth;

import android.util.Log;

import java.math.BigInteger;

/**
 * 字节操作/类型转换工具类
 *
 * @author benjaminwan
 * 数据转换工具
 */
public class ByteUtil {
    public static final String TAG = "ByteUtil";

    //-------------------------------------------------------
    // 判断奇数或偶数，位运算，最后一位是1则为奇数，为0是偶数
    static public int isOdd(int num) {
        return num & 0x1;
    }

    /**
     * HEX 字符串转 int
     *
     * @param inHex 传入 HEX 字符串
     * @return 返回 long 类型数据
     */
    public static int HexToInt(String inHex) {
        return Integer.parseInt(inHex, 16);
    }

    /**
     * HEX 字符串转 long
     *
     * @param inHex 传入 HEX 字符串
     * @return 返回 long 类型数据
     */
    public static long HexToLong(String inHex) {
        return Long.parseLong(inHex, 16);
    }

    /**
     * HEX 字符串转 byte
     *
     * @param inHex 传入 HEX 字符串
     * @return 返回 byte
     */
    public static byte HexToByte(String inHex) {
        return (byte) Integer.parseInt(inHex, 16);
    }

    /**
     * 1 字节转 2 个 HEX 字符
     *
     * @param inByte 传入 byte 字节
     * @return 返回两个 HEX 字符
     */
    public static String Byte2Hex(Byte inByte) {
        return String.format("%02x", inByte).toUpperCase();
    }

    /**
     * 字节数组转转 HEX 字符串
     *
     * @param inBytArr 传入字节数组
     * @return 返回 HEX 字符串
     */
    public static String ByteArrToHex(byte[] inBytArr) {
        StringBuilder strBuilder = new StringBuilder("");
        int j = inBytArr.length;
        for (int i = 1; i <= j; i++) {
            strBuilder.append(Byte2Hex(inBytArr[i - 1]));
        }
        return strBuilder.toString();
    }

    /**
     * 字节数组转转 HEX 字符串，可选长度
     *
     * @param inBytArr  传入字节数组
     * @param offset    偏移，起始的字节位置
     * @param byteCount 长度
     * @return 返回 HEX 字符串
     */
    public static String ByteArrToHex(byte[] inBytArr, int offset, int byteCount) {
        StringBuilder strBuilder = new StringBuilder();
        int j = byteCount;
        for (int i = offset; i < j; i++) {
            strBuilder.append(Byte2Hex(inBytArr[i]));
        }
        return strBuilder.toString();
    }

    /**
     * HEX 字符串转字节数组
     *
     * @param inHex 传入 HEX 字符串
     * @return 返回字节数组
     */
    public static byte[] HexToByteArr(String inHex) {
        int hexlen = inHex.length();
        byte[] result;
        if (isOdd(hexlen) == 1) {//奇数
            hexlen++;
            result = new byte[(hexlen / 2)];
            inHex = "0" + inHex;
        } else {//偶数
            result = new byte[(hexlen / 2)];
        }
        int j = 0;
        for (int i = 0; i < hexlen; i += 2) {
            result[j] = HexToByte(inHex.substring(i, i + 2));
            j++;
        }
        return result;
    }

    /**
     * 整型转为 byte 数组，4 字节
     *
     * @param res
     * @return
     */
    public static byte[] int2byte(int res) {
        byte[] targets = new byte[4];
        targets[3] = (byte) (res & 0xff);// 最低位
        targets[2] = (byte) ((res >> 8) & 0xff);// 次低位
        targets[1] = (byte) ((res >> 16) & 0xff);// 次高位
        targets[0] = (byte) (res >>> 24);// 最高位,无符号右移。
        return targets;
    }

    /**
     * 整型转为 byte 数组，4 字节
     *
     * @param res
     * @return
     */
    public static byte[] int2byte4(int res) {
        byte[] targets = new byte[4];
        targets[3] = (byte) (res & 0xff);// 最低位
        targets[2] = (byte) ((res >> 8) & 0xff);// 次低位
        targets[1] = (byte) ((res >> 16) & 0xff);// 次低位
        targets[0] = (byte) ((res >> 24) & 0xff);// 次高位
        return targets;
    }

    /**
     * 整型转为 byte 数组，3 字节
     *
     * @param res
     * @return
     */
    public static byte[] int2byte3(int res) {
        byte[] targets = new byte[3];
        targets[2] = (byte) (res & 0xff);// 最低位
        targets[1] = (byte) ((res >> 8) & 0xff);// 次低位
        targets[0] = (byte) ((res >> 16) & 0xff);// 次高位
        return targets;
    }

    /**
     * 整型转为 byte 数组，2 字节
     *
     * @param res
     * @return
     */
    public static byte[] int2byte2(int res) {
        byte[] targets = new byte[2];
        targets[1] = (byte) (res & 0xff);// 最低位
        targets[0] = (byte) ((res >> 8) & 0xff);// 次低位
        return targets;
    }

    /**
     * 整型转为 byte 数组，1 字节
     *
     * @param res
     * @return
     */
    public static byte int2byte1(int res) {
        return (byte) (res & 0xff);// 最低位
    }

    public static byte[] date2HexString(String date) {
        String code0 = date.substring(0, 2);
        String code1 = date.substring(2, 4);
        String code2 = date.substring(4, 6);
        String code3 = date.substring(6, 8);
        byte[] bytes = new byte[4];
        bytes[0] = int2byte1(Integer.parseInt(code0));
        bytes[1] = int2byte1(Integer.parseInt(code1));
        bytes[2] = int2byte1(Integer.parseInt(code2));
        bytes[3] = int2byte1(Integer.parseInt(code3));

//        return int2byte1(Integer.parseInt(code0)) + "" + int2byte1(Integer.parseInt(code1)) + int2byte1(Integer.parseInt(code2)) + int2byte1(Integer.parseInt(code3));
        return bytes;
    }

    /**
     * byte 数组转换为整型
     *
     * @param b
     * @return
     */
    public static int byteArrayToInt(byte[] b) {
        byte[] a = new byte[4];
        int i = a.length - 1, j = b.length - 1;
        for (; i >= 0; i--, j--) {//从b的尾部(即int值的低位)开始copy数据
            if (j >= 0)
                a[i] = b[j];
            else
                a[i] = 0;//如果b.length不足4,则将高位补0
        }
        int v0 = (a[0] & 0xff) << 24;//&0xff将byte值无差异转成int,避免Java自动类型提升后,会保留高位的符号位
        int v1 = (a[1] & 0xff) << 16;
        int v2 = (a[2] & 0xff) << 8;
        int v3 = (a[3] & 0xff);
        return v0 + v1 + v2 + v3;
    }

    /**
     * byte 数组转换为整型
     * 小端
     *
     * @param b
     * @return
     */
    public static int byteArrayToIntSmallEnd(byte[] b) {
        byte[] a = new byte[4];
        int i = a.length - 1, j = b.length - 1;
        for (; i >= 0; i--, j--) {//从b的尾部(即int值的低位)开始copy数据
            if (j >= 0)
                a[i] = b[j];
            else
                a[i] = 0;//如果b.length不足4,则将高位补0
        }
        int v0 = (a[3] & 0xff) << 24;//&0xff将byte值无差异转成int,避免Java自动类型提升后,会保留高位的符号位
        int v1 = (a[2] & 0xff) << 16;
        int v2 = (a[1] & 0xff) << 8;
        int v3 = (a[0] & 0xff);
        return v0 + v1 + v2 + v3;
    }

    /**
     * 长整型转换为数组
     *
     * @param val 长整型数据
     * @return 字节数组
     */
    // 每次截取8位，然后左移8,
    public static byte[] long2ByteArr(long val) {
        System.out.println("原来的长整形数据：" + val);
        byte[] b = new byte[8];
        for (int i = 7; i > 0; i--) {
            //强制转型，后留下长整形的低8位
            b[i] = (byte) val;
            String str = Long.toBinaryString(val);
            String lb = Long.toBinaryString(b[i]);
            String lb2 = Long.toBinaryString(b[i] & 0xff);


            System.out.println("转换为字节：" + str);
            System.out.println(lb);
            System.out.println(lb2);
            //向右移动8位，则第二次循环则计算第二个8位数
            val >>>= 8;
        }
        b[0] = (byte) val;
        return b;
    }

    /**
     * 数组转换为长整型
     *
     * @param bytes  待转换的字节数组
     * @param offset 偏移
     * @param length 待转换的字节数组长度
     * @return
     */
    //每次得到的结果左移动8位，然后后面的字节依次拼接到后面
    public static long byteArr2Long(byte[] bytes, int offset, final int length) {
        long l = 0;
        for (int i = offset; i < offset + length; i++) {
            //每次计算的结果 向左移动8位，向左移动8位后低8位全为0
            l <<= 8;
            //上面计算的结果& 0xFF后得到高于8位全为0的结果
            long byteValue = bytes[i] & 0xFF;
            //当前字节结算后的结果，用异或拼接的l的低8位上面，因为l当前值的低8位全都是0，则异或后l低8位就变成了byteValue的值
            //异或运算符，相应位的值相同的，结果为 0，不相同的结果为 1。
            l ^= byteValue;
        }
        return l;
    }

    /**
     * 数组转换为长整型
     * 小端模式
     *
     * @param bytes  待转换的字节数组
     * @param offset 偏移
     * @param length 待转换的字节数组长度
     * @return
     */
    //每次得到的结果左移动8位，然后后面的字节依次拼接到后面
    public static long byteArr2LongSmallEnd(byte[] bytes, int offset, final int length) {
        long l = 0;
        for (int i = offset + length - 1; i > offset - 1; i--) {
            //每次计算的结果 向左移动8位，向左移动8位后低8位全为0
            l <<= 8;
            //上面计算的结果& 0xFF后得到高于8位全为0的结果
            long byteValue = bytes[i] & 0xFF;
            //当前字节结算后的结果，用异或拼接的l的低8位上面，因为l当前值的低8位全都是0，则异或后l低8位就变成了byteValue的值
            //异或运算符，相应位的值相同的，结果为 0，不相同的结果为 1。
            l ^= byteValue;
        }
        return l;
    }

    /**
     * 计算返回 2个字节 byte 数组校验和, 高位丢弃
     *
     * @param data byte 数据(10进制)
     * @return 校验和
     */
//    public static byte[] checkSum(byte[] data) {
//        if (null == data || data.length == 0) {
//            Log.e(TAG, "data invalid");
//            return null;
//        }
//        int all = 0;
//        for (byte b : data) {
//            all += b;
//        }
//        StringBuilder s = new StringBuilder(Integer.toHexString(all));
//        Log.d(TAG, "HEX: %s", s.toString());
//        while (s.length() < 4) {
//            s.insert(0, "0");
//        }
//        byte[] result = s.substring(s.length() - 4).toUpperCase().getBytes();
//        Log.d(TAG, "checkSum() data: %s, result: %s", all, new String(result));
//        return result;
//    }

    /**
     * 计算返回 2个字节 byte 数组校验和, 高位丢弃
     *
     * @param data byte 数据(16进制)
     * @return 校验和
     */
    public static byte[] checkHexSum(byte[] data) {
        if (null == data || data.length == 0) {
            Log.e(TAG, "data is empty");
            return new byte[]{};
        }

        int all = 0;
        for (byte datum : data) {
            //先将 byte 转换为 16 进制数据
            String hex = Byte2Hex(datum);
            //计算 16 进制的累加和
            all += HexToInt(hex);
        }
        StringBuilder s = new StringBuilder(Integer.toHexString(all));
        //不足4位需要填0
        while (s.length() < 4) {
            s.insert(0, "0");
        }
        //超过4位需要截取低位
        String s1 = s.substring(s.length() - 4).toUpperCase();
        byte[] result = new byte[2];
        result[0] = (byte) getCode(s1.substring(0, 2));
        result[1] = (byte) getCode(s1.substring(2, 4));
        return result;
    }


    /**
     * 计算 BIN 文件的校验和返回 4个字节 byte 数组, 高位丢弃
     *
     * @param data byte 数据(16进制)
     * @return 校验和
     */
    public static byte[] checkBinSum(byte[] data) {
        if (null == data || data.length == 0) {
            Log.e(TAG, "bin is empty");
            return new byte[]{};
        }
//        Log.d(TAG, "START CHECK BIN SUM: %s", data);
        Log.d(TAG, "START CHECK BIN SUM");

        int all = 0;
        for (byte datum : data) {
            //先将 byte 转换为 16 进制数据
            String hex = Byte2Hex(datum);
            //计算 16 进制的累加和
            all += HexToInt(hex);
        }
        StringBuilder s = new StringBuilder(Integer.toHexString(all));
        //不足8位需要填0
        while (s.length() < 8) {
            s.insert(0, "0");
        }
        //超过8位需要截取低位
        String s1 = s.substring(s.length() - 8).toUpperCase();
        byte[] result = new byte[4];
        result[0] = (byte) getCode(s1.substring(0, 2));
        result[1] = (byte) getCode(s1.substring(2, 4));
        result[2] = (byte) getCode(s1.substring(4, 6));
        result[3] = (byte) getCode(s1.substring(6, 8));
//        Log.d(TAG, "CHECK HEX SUM R: %s", result);
        return result;
    }

    /**
     * 普通字符转换成16进制字符串
     *
     * @param str
     * @return
     */
    public static String str2HexStr(String str) {
        Log.d(TAG, "str2HexStr() str: " + str);
        byte[] bytes = str.getBytes();
        // 如果不是宽类型的可以用Integer
        BigInteger bigInteger = new BigInteger(1, bytes);
        return bigInteger.toString(16);
    }

    /**
     * 1606382478738 => 17603db6592
     * 00 00 01 76 03 db 65 92
     *
     * @param time   毫秒时间戳(1606382478738)
     * @param length 16 进制数据字节数
     * @return 毫秒时间戳 (0000017603db6592)
     */
    public static String timestamp2HexString(long time, int length) {
        StringBuilder hexTime = new StringBuilder(Long.toHexString(time).toUpperCase());
        int fillCount = length * 2 - hexTime.length();
        for (int i = 0; i < fillCount; i++) {
            hexTime.insert(0, "0");
        }
        return hexTime.toString();
    }

    /**
     * 16进制的字符串转换成16进制字符串数组
     *
     * @param src
     * @return
     */
    public static byte[] HexString2Bytes(String src) {
        // 对齐数据,奇数时补零
        if (src.length() % 2 == 1) {
            src = "0" + src;
        }

        int len = src.length() / 2;
        byte[] ret = new byte[len];
        byte[] tmp = src.getBytes();
        for (int i = 0; i < len; i++) {
            ret[i] = uniteBytes(tmp[i * 2], tmp[i * 2 + 1]);
        }
        return ret;
    }

    public static byte uniteBytes(byte src0, byte src1) {
        byte _b0 = Byte.decode("0x" + new String(new byte[]{src0}));
        _b0 = (byte) (_b0 << 4);
        byte _b1 = Byte.decode("0x" + new String(new byte[]{src1}));
        return (byte) (_b0 ^ _b1);
    }

    /*
     * 16进制 byte[] 转16进制字符串显示
     */
    public static String bytes2HexString(byte[] b, int length) {
        StringBuilder r = new StringBuilder();

        if (length > b.length) {
            //防止遍历时数组角标越界
            length = b.length;
        }

        for (int i = 0; i < length; i++) {
            String hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1) {
                hex = "0" + hex;
            }
            r.append(hex.toUpperCase());
        }

        return r.toString();
    }

    /**
     * * 16进制 byte[] 转16进制字符串显示
     *
     * @param b      byte[] 数组
     * @param start  开始
     * @param length 长度
     * @return 转换后的字符串
     */
    public static String bytes2HexString(byte[] b, int start, int length) {
        StringBuilder r = new StringBuilder();

        if (start <= 0) {
            start = 0;
        }

        if (length > b.length - start) {
            //防止遍历时数组角标越界
            length = b.length - start;
        }

        for (int i = start; i < start + length; i++) {
            String hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1) {
                hex = "0" + hex;
            }
            r.append(hex.toUpperCase());
        }

        return r.toString();
    }

    /**
     * 10 进制毫秒数转换为 16 进制 byte 数组
     *
     * @param time 10 进制毫秒数
     * @return 16 进制 byte 数组(4个字节)
     */
    public static byte[] time2ByteArr(long time) {
        //毫秒转换为秒
        time /= 1000;
        //转换为16进制
        String hexString = Long.toHexString(time).toUpperCase();
//        Log.d(TAG, "src: %s, dest: %s", time, hexString);
        //time hex
        //16 进制的 string 切割为 byte[]
        String code0 = hexString.substring(0, 2);
        String code1 = hexString.substring(2, 4);
        String code2 = hexString.substring(4, 6);
        String code3 = hexString.substring(6, 8);
        return new byte[]{(byte) getCode(code0), (byte) getCode(code1), (byte) getCode(code2), (byte) getCode(code3)};
    }

    /**
     * 10 进制毫秒数转换为 16 进制 byte 数组
     *
     * @param time 10 进制毫秒数
     * @return 16 进制 byte 数组(4个字节)
     */
    public static byte[] time2ByteArr(String time) {
        //转换为16进制
        String hexString = Long.toHexString(Long.parseLong(time)).toUpperCase();
//        Log.d(TAG, "src: %s, dest: %s", time, hexString);
        //time hex
        //16 进制的 string 切割为 byte[]
        String code0 = hexString.substring(0, 2);
        String code1 = hexString.substring(2, 4);
        String code2 = hexString.substring(4, 6);
        String code3 = hexString.substring(6, 8);
        String code4 = hexString.substring(8, 10);
//        String code5 = hexString.substring(11);
        return new byte[]{(byte) getCode(code0), (byte) getCode(code1), (byte) getCode(code2), (byte) getCode(code3), (byte) getCode(code4)};
    }

    /**
     * 将截取16进制两个字节字符串转换为byte类型
     *
     * @param str
     * @return
     */
    public static int getCode(String str) {
        int h = Integer.parseInt(str.substring(0, 1), 16);
        int l = Integer.parseInt(str.substring(1, 2), 16);
        return (byte) (((byte) h << 4) + (byte) l);
    }
}