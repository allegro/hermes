package pl.allegro.tech.hermes.consumers.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.consumers.consumer.rate.maxrate.MaxRateParameters;

@ConfigurationProperties(prefix = "consumer.maxrate")
public class MaxRateProperties {

    private int registryBinaryEncoderMaxRateBufferSizeBytes = 100_000;

    private int registryBinaryEncoderHistoryBufferSizeBytes = 100_000;

    private int balanceIntervalSeconds = 30;

    private int updateIntervalSeconds = 15;

    private int historySize = 1;

    private double busyTolerance = 0.1;

    private double minMaxRate = 1.0;

    private double minAllowedChangePercent = 1.0;

    private double minSignificantUpdatePercent = 9.0;

    public int getRegistryBinaryEncoderMaxRateBufferSizeBytes() {
        return registryBinaryEncoderMaxRateBufferSizeBytes;
    }

    public void setRegistryBinaryEncoderMaxRateBufferSizeBytes(int registryBinaryEncoderMaxRateBufferSizeBytes) {
        this.registryBinaryEncoderMaxRateBufferSizeBytes = registryBinaryEncoderMaxRateBufferSizeBytes;
    }

    public int getRegistryBinaryEncoderHistoryBufferSizeBytes() {
        return registryBinaryEncoderHistoryBufferSizeBytes;
    }

    public void setRegistryBinaryEncoderHistoryBufferSizeBytes(int registryBinaryEncoderHistoryBufferSizeBytes) {
        this.registryBinaryEncoderHistoryBufferSizeBytes = registryBinaryEncoderHistoryBufferSizeBytes;
    }

    public int getBalanceIntervalSeconds() {
        return balanceIntervalSeconds;
    }

    public void setBalanceIntervalSeconds(int balanceIntervalSeconds) {
        this.balanceIntervalSeconds = balanceIntervalSeconds;
    }

    public int getUpdateIntervalSeconds() {
        return updateIntervalSeconds;
    }

    public void setUpdateIntervalSeconds(int updateIntervalSeconds) {
        this.updateIntervalSeconds = updateIntervalSeconds;
    }

    public int getHistorySize() {
        return historySize;
    }

    public void setHistorySize(int historySize) {
        this.historySize = historySize;
    }

    public double getBusyTolerance() {
        return busyTolerance;
    }

    public void setBusyTolerance(double busyTolerance) {
        this.busyTolerance = busyTolerance;
    }

    public double getMinMaxRate() {
        return minMaxRate;
    }

    public void setMinMaxRate(double minMaxRate) {
        this.minMaxRate = minMaxRate;
    }

    public double getMinAllowedChangePercent() {
        return minAllowedChangePercent;
    }

    public void setMinAllowedChangePercent(double minAllowedChangePercent) {
        this.minAllowedChangePercent = minAllowedChangePercent;
    }

    public double getMinSignificantUpdatePercent() {
        return minSignificantUpdatePercent;
    }

    public void setMinSignificantUpdatePercent(double minSignificantUpdatePercent) {
        this.minSignificantUpdatePercent = minSignificantUpdatePercent;
    }

    protected MaxRateParameters toMaxRateParameters() {
        return new MaxRateParameters(
                this.balanceIntervalSeconds,
                this.updateIntervalSeconds,
                this.historySize,
                this.busyTolerance,
                this.minMaxRate,
                this.minAllowedChangePercent,
                this.minSignificantUpdatePercent
        );
    }
}
