package com.lifwear;

import android.text.TextUtils;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;

/**
 * @author Corey
 * @date 2020/4/1.
 */
public class FileUtil {
    public static final String TAG = "FileUtil";

    /**
     * 计算文件的 md5
     *
     * @param file
     * @return md5
     */
    public static String getFileMd5(File file) {
        FileInputStream fileInputStream = null;
        byte[] buffer = new byte[100 * 1024];
        int len = 0;
        StringBuilder stringBuilder = new StringBuilder();
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            fileInputStream = new FileInputStream(file);
            while ((len = fileInputStream.read(buffer)) != -1) {
                messageDigest.update(buffer, 0, len);
            }

            for (byte b : messageDigest.digest()) {
                String md5 = Integer.toHexString(b & 0xff);
                if (TextUtils.isEmpty(md5)) {
                    return stringBuilder.toString();
                }
                if (md5.length() < 2) {
                    stringBuilder.append("0");
                }
                stringBuilder.append(md5);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "getFileMd5() Exception: ", e);
        } finally {
            try {

                if (fileInputStream != null) {
                    fileInputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "getFileMd5() Exception: ", e);
            }
        }
        return stringBuilder.toString();
    }

    private static final String EXPORT_PATH = "D:\\";
    private String outfilePath = EXPORT_PATH + "hex.txt";

    public static String getFileMd5(InputStream inputStream) {
        if (null == inputStream) {
            Log.e(TAG, "inputStream is null");
            return "";
        }
        byte[] buffer = new byte[100 * 1024];
        int len = 0;
        StringBuilder stringBuilder = new StringBuilder();
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            while ((len = inputStream.read(buffer)) != -1) {
                messageDigest.update(buffer, 0, len);
            }

            for (byte b : messageDigest.digest()) {
                String md5 = Integer.toHexString(b & 0xff);
                if (TextUtils.isEmpty(md5)) {
                    return stringBuilder.toString();
                }
                if (md5.length() < 2) {
                    stringBuilder.append("0");
                }
                stringBuilder.append(md5);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "getFileMd5() Exception: ", e);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "getFileMd5() Exception: ", e);
            }
        }
        return stringBuilder.toString();
    }

    /**
     * 文件转成十六进制
     *
     * @param inFilePath 升级包 bin 文件存放的路径
     */
    public static String fileToHexMcu(String inFilePath) {
        Log.d(TAG, "fileToHex");
        try {
            StringBuffer sb = new StringBuffer();
            FileInputStream fis = new FileInputStream(inFilePath);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            byte[] buffer = new byte[1024];
            int read = 1024;
            int readSize = 1024;
            while (read == readSize) {
                read = fis.read(buffer, 0, readSize);
                bos.write(buffer, 0, read);
            }
            byte[] result = bos.toByteArray();
            // 字节数组转成十六进制
            String str = byte2HexStr(result);
//            Trace.d(TAG, "FileHexStr: %s", str);
            return str;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 文件转成十六进制
     *
     * @param fileInputStream 升级包 bin 文件的流
     */
    public static String streamToHexMcu(FileInputStream fileInputStream) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[10240];
            int read = 10240;
            int readSize = 10240;
            int current_read_num = 0;
            while (read == readSize) {
                current_read_num++;
                read = fileInputStream.read(buffer, 0, readSize);
                bos.write(buffer, 0, read);
            }
            Log.d(TAG, "bos.toByteArray---START");
            byte[] result = bos.toByteArray();
            Log.d(TAG, "bos.toByteArray---END");
            // 字节数组转成十六进制
            return byte2HexStr(result);

        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "streamToHexMcu() Exception: ", e);
        }
        return null;
    }

    /**
     * 十六进制转成文件
     *
     * @param hex
     * @param filePath
     */
    public static void hexToFile(String hex, String filePath) {
        StringBuilder sb = new StringBuilder();
        sb.append(hex);
        saveToFile(sb.toString().toUpperCase(), EXPORT_PATH + filePath);
    }

    /**
     * hex 转为文件
     *
     * @param src
     * @param output
     */
    public static void saveToFile(String src, String output) {
        if (src == null || src.length() == 0) {
            return;
        }
        try {
            FileOutputStream out = new FileOutputStream(new File(output));
            byte[] bytes = src.getBytes();
            for (int i = 0; i < bytes.length; i += 2) {
                out.write(charToInt(bytes[i]) * 16 + charToInt(bytes[i + 1]));
            }
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static int charToInt(byte ch) {
        int val = 0;
        if (ch >= 0x30 && ch <= 0x39) {
            val = ch - 0x30;
        } else if (ch >= 0x41 && ch <= 0x46) {
            val = ch - 0x41 + 10;
        }
        return val;
    }

    /*
     * 实现字节数组向十六进制的转换方法一
     */
    public static String byte2HexStr(byte[] b) {
        Log.d(TAG, "byte2HexStr---START");
        StringBuilder hs = new StringBuilder();
        String stmp = "";
        for (byte value : b) {
            stmp = (Integer.toHexString(value & 0XFF));
            if (stmp.length() == 1) {
                hs.append("0").append(stmp);
            } else {
                hs.append(stmp);
            }
        }
        Log.d(TAG, "byte2HexStr---END");
        return hs.toString().toUpperCase();
    }
}
