package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import java.time.Duration;

public class MaxRateParameters {

    private final Duration balanceInterval;

    private final Duration updateInterval;

    private final int historySize;

    private final double busyTolerance;

    private final double minMaxRate;

    private final double minAllowedChangePercent;

    private final double minSignificantUpdatePercent;

    public Duration getBalanceInterval() {
        return balanceInterval;
    }

    public Duration getUpdateInterval() {
        return updateInterval;
    }

    public int getHistorySize() {
        return historySize;
    }

    public double getBusyTolerance() {
        return busyTolerance;
    }

    public double getMinMaxRate() {
        return minMaxRate;
    }

    public double getMinAllowedChangePercent() {
        return minAllowedChangePercent;
    }

    public double getMinSignificantUpdatePercent() {
        return minSignificantUpdatePercent;
    }

    public MaxRateParameters(Duration balanceInterval, Duration updateInterval, int historySize, double busyTolerance, double minMaxRate, double minAllowedChangePercent, double minSignificantUpdatePercent) {
        this.balanceInterval = balanceInterval;
        this.updateInterval = updateInterval;
        this.historySize = historySize;
        this.busyTolerance = busyTolerance;
        this.minMaxRate = minMaxRate;
        this.minAllowedChangePercent = minAllowedChangePercent;
        this.minSignificantUpdatePercent = minSignificantUpdatePercent;
    }
}
