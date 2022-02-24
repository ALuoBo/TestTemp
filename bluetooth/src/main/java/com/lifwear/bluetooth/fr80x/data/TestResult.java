package com.lifwear.bluetooth.fr80x.data;

/**
 * 测试结果
 * @author WillXia
 * @date 2022/1/6.
 */
public class TestResult extends FR80xData {

    private int testKind; //测试步骤

    /**
     * 佩戴状态测试结果
     */
    private int wearStateResult;// 0通过，1未通过，2未读到芯片

    /**
     * 充电状态测试结果
     */
    private int chargeStateResult;// 0通过，1未通过，2未读到芯片

    /**
     * 温度测试结果
     */
    private int tempTestResult; // 0通过，1未通过，2未读到芯片

    /**
     * 温度值
     */
    private float temp;

    public TestResult(int testKind, int wearStateResult, int chargeStateResult, int tempTestResult, float temp) {
        this.testKind = testKind;
        this.wearStateResult = wearStateResult;
        this.chargeStateResult = chargeStateResult;
        this.tempTestResult = tempTestResult;
        this.temp = temp;
    }

    public int getTestKind() {
        return testKind;
    }

    public void setTestKind(int testKind) {
        this.testKind = testKind;
    }

    public int getWearStateResult() {
        return wearStateResult;
    }

    public void setWearStateResult(int wearStateResult) {
        this.wearStateResult = wearStateResult;
    }

    public int getChargeStateResult() {
        return chargeStateResult;
    }

    public void setChargeStateResult(int chargeStateResult) {
        this.chargeStateResult = chargeStateResult;
    }

    public int getTempTestResult() {
        return tempTestResult;
    }

    public void setTempTestResult(int tempTestResult) {
        this.tempTestResult = tempTestResult;
    }

    public float getTemp() {
        return temp;
    }

    public void setTemp(float temp) {
        this.temp = temp;
    }

    @Override
    public String toString() {
        return "TestResult{" +
                "wearStateResult=" + wearStateResult +
                ", chargeStateResult=" + chargeStateResult +
                ", tempTestResult=" + tempTestResult +
                ", temp=" + temp +
                '}';
    }
}
