package com.lifwear.bluetooth.fr80x.data;

/**
 * @author WillXia
 * @date 2022/2/10.
 */
public class TestTemp extends FR80xData{
    private float AimTemp;
    private float environmentTemp;
    private Long  timestamp;

    public float getAimTemp() {
        return AimTemp;
    }

    public void setAimTemp(float aimTemp) {
        AimTemp = aimTemp;
    }

    public float getEnvironmentTemp() {
        return environmentTemp;
    }

    public void setEnvironmentTemp(float environmentTemp) {
        this.environmentTemp = environmentTemp;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "TestTemp{" +
                "AimTemp=" + AimTemp +
                ", environmentTemp=" + environmentTemp +
                ", timestamp=" + timestamp +
                '}';
    }
}
