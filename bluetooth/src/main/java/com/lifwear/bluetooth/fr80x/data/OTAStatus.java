package com.lifwear.bluetooth.fr80x.data;

/**
 * @author WillXia
 * @date 2022/1/26.
 */
public class OTAStatus extends FR80xData {

    public static final int START = 0;
    public static final int GOING = 1;
    public static final int END_SUCCESS = 2;
    public static final int END_FAIL = 3;

    private int status;// 0 未开始 1 进行中 2完成，成功 3完成，失败

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

}
