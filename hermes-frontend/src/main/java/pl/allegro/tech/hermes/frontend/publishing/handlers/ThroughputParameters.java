package pl.allegro.tech.hermes.frontend.publishing.handlers;

public class ThroughputParameters {

    private final String type;

    private final long fixedMax;

    private final long dynamicMax;

    private final long dynamicThreshold;

    private final long dynamicDesired;

    private final double dynamicIdle;

    private final int dynamicCheckInterval;

    public String getType() {
        return type;
    }

    public long getFixedMax() {
        return fixedMax;
    }

    public long getDynamicMax() {
        return dynamicMax;
    }

    public long getDynamicThreshold() {
        return dynamicThreshold;
    }

    public long getDynamicDesired() {
        return dynamicDesired;
    }

    public double getDynamicIdle() {
        return dynamicIdle;
    }

    public int getDynamicCheckInterval() {
        return dynamicCheckInterval;
    }

    public ThroughputParameters(String type,
                                long fixedMax,
                                long dynamicMax,
                                long dynamicThreshold,
                                long dynamicDesired,
                                double dynamicIdle,
                                int dynamicCheckInterval) {
        this.type = type;
        this.fixedMax = fixedMax;
        this.dynamicMax = dynamicMax;
        this.dynamicThreshold = dynamicThreshold;
        this.dynamicDesired = dynamicDesired;
        this.dynamicIdle = dynamicIdle;
        this.dynamicCheckInterval = dynamicCheckInterval;
    }
}
