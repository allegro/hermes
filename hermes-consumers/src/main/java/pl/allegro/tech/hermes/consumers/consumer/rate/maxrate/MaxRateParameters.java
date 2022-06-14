package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

public class MaxRateParameters {

    private final int balanceIntervalSeconds;

    private final int updateIntervalSeconds;

    private final int historySize;

    private final double busyTolerance;

    private final double minMaxRate;

    private final double minAllowedChangePercent;

    private final double minSignificantUpdatePercent;

    public int getBalanceIntervalSeconds() {
        return balanceIntervalSeconds;
    }

    public int getUpdateIntervalSeconds() {
        return updateIntervalSeconds;
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

    public MaxRateParameters(int balanceIntervalSeconds, int updateIntervalSeconds, int historySize, double busyTolerance, double minMaxRate, double minAllowedChangePercent, double minSignificantUpdatePercent) {
        this.balanceIntervalSeconds = balanceIntervalSeconds;
        this.updateIntervalSeconds = updateIntervalSeconds;
        this.historySize = historySize;
        this.busyTolerance = busyTolerance;
        this.minMaxRate = minMaxRate;
        this.minAllowedChangePercent = minAllowedChangePercent;
        this.minSignificantUpdatePercent = minSignificantUpdatePercent;
    }
}
