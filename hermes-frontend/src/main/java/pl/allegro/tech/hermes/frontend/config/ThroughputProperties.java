package pl.allegro.tech.hermes.frontend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.frontend.publishing.handlers.ThroughputParameters;

@ConfigurationProperties(prefix = "frontend.throughput")
class ThroughputProperties {

    private String type = "unlimited";

    private long fixedMax = Long.MAX_VALUE;

    private long dynamicMax = Long.MAX_VALUE;

    private long dynamicThreshold = Long.MAX_VALUE;

    private long dynamicDesired = Long.MAX_VALUE;

    private double dynamicIdle = 0.5;

    private int dynamicCheckInterval = 30;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getFixedMax() {
        return fixedMax;
    }

    public void setFixedMax(long fixedMax) {
        this.fixedMax = fixedMax;
    }

    public long getDynamicMax() {
        return dynamicMax;
    }

    public void setDynamicMax(long dynamicMax) {
        this.dynamicMax = dynamicMax;
    }

    public long getDynamicThreshold() {
        return dynamicThreshold;
    }

    public void setDynamicThreshold(long dynamicThreshold) {
        this.dynamicThreshold = dynamicThreshold;
    }

    public long getDynamicDesired() {
        return dynamicDesired;
    }

    public void setDynamicDesired(long dynamicDesired) {
        this.dynamicDesired = dynamicDesired;
    }

    public double getDynamicIdle() {
        return dynamicIdle;
    }

    public void setDynamicIdle(double dynamicIdle) {
        this.dynamicIdle = dynamicIdle;
    }

    public int getDynamicCheckInterval() {
        return dynamicCheckInterval;
    }

    public void setDynamicCheckInterval(int dynamicCheckInterval) {
        this.dynamicCheckInterval = dynamicCheckInterval;
    }

    protected ThroughputParameters toThroughputParameters() {
        return new ThroughputParameters(
                this.type,
                this.fixedMax,
                this.dynamicMax,
                this.dynamicThreshold,
                this.dynamicDesired,
                this.dynamicIdle,
                this.dynamicCheckInterval
        );
    }
}
