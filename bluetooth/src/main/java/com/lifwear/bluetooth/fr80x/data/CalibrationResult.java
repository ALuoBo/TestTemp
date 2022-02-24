package com.lifwear.bluetooth.fr80x.data;

/**
 * MCU返回校准参数
 * @author WillXia
 * @date 2022/1/13.
 */
public class CalibrationResult extends FR80xData{

    private int isSuccess;
    private String paramA;
    private String paramB;
    private String paramC;

    public int getIsSuccess() {
        return isSuccess;
    }

    public void setIsSuccess(int isSuccess) {
        this.isSuccess = isSuccess;
    }

    public String getParamA() {
        return paramA;
    }

    public void setParamA(String paramA) {
        this.paramA = paramA;
    }

    public String getParamB() {
        return paramB;
    }

    public void setParamB(String paramB) {
        this.paramB = paramB;
    }

    public String getParamC() {
        return paramC;
    }

    public void setParamC(String paramC) {
        this.paramC = paramC;
    }

    @Override
    public String toString() {
        return "CalibrationResult{" +
                "isSuccess=" + isSuccess +
                ", paramA='" + paramA + '\'' +
                ", paramB='" + paramB + '\'' +
                ", paramC='" + paramC + '\'' +
                '}';
    }
}
