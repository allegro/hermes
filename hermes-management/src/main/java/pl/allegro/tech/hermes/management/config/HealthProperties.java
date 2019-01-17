package pl.allegro.tech.hermes.management.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "health")
public class HealthProperties {
    private int maxLagInSeconds = 600;
    private double maxOtherErrorsRatio = 0.5;
    private double maxTimeoutsRatio = 0.1;
    private double max5xxErrorsRatio = 0.1;
    private double max4xxErrorsRatio = 0.1;
    private double minSubscriptionRateForReliableMetrics = 2.0;
    private boolean laggingIndicatorEnabled = true;
    private boolean malfunctioningIndicatorEnabled = true;
    private boolean receivingMalformedMessagesIndicatorEnabled = true;
    private boolean timingOutIndicatorEnabled = true;
    private boolean unreachableIndicatorEnabled = true;

    public int getMaxLagInSeconds() {
        return maxLagInSeconds;
    }

    public void setMaxLagInSeconds(int maxLagInSeconds) {
        this.maxLagInSeconds = maxLagInSeconds;
    }

    public double getMaxOtherErrorsRatio() {
        return maxOtherErrorsRatio;
    }

    public void setMaxOtherErrorsRatio(double maxOtherErrorsRatio) {
        this.maxOtherErrorsRatio = maxOtherErrorsRatio;
    }

    public double getMaxTimeoutsRatio() {
        return maxTimeoutsRatio;
    }

    public void setMaxTimeoutsRatio(double maxTimeoutsRatio) {
        this.maxTimeoutsRatio = maxTimeoutsRatio;
    }

    public double getMax5xxErrorsRatio() {
        return max5xxErrorsRatio;
    }

    public void setMax5xxErrorsRatio(double max5xxErrorsRatio) {
        this.max5xxErrorsRatio = max5xxErrorsRatio;
    }

    public double getMax4xxErrorsRatio() {
        return max4xxErrorsRatio;
    }

    public void setMax4xxErrorsRatio(double max4xxErrorsRatio) {
        this.max4xxErrorsRatio = max4xxErrorsRatio;
    }

    public double getMinSubscriptionRateForReliableMetrics() {
        return minSubscriptionRateForReliableMetrics;
    }

    public void setMinSubscriptionRateForReliableMetrics(double minSubscriptionRateForReliableMetrics) {
        this.minSubscriptionRateForReliableMetrics = minSubscriptionRateForReliableMetrics;
    }

    public boolean isLaggingIndicatorEnabled() {
        return laggingIndicatorEnabled;
    }

    public void setLaggingIndicatorEnabled(boolean laggingIndicatorEnabled) {
        this.laggingIndicatorEnabled = laggingIndicatorEnabled;
    }

    public boolean isMalfunctioningIndicatorEnabled() {
        return malfunctioningIndicatorEnabled;
    }

    public void setMalfunctioningIndicatorEnabled(boolean malfunctioningIndicatorEnabled) {
        this.malfunctioningIndicatorEnabled = malfunctioningIndicatorEnabled;
    }

    public boolean isReceivingMalformedMessagesIndicatorEnabled() {
        return receivingMalformedMessagesIndicatorEnabled;
    }

    public void setReceivingMalformedMessagesIndicatorEnabled(boolean receivingMalformedMessagesIndicatorEnabled) {
        this.receivingMalformedMessagesIndicatorEnabled = receivingMalformedMessagesIndicatorEnabled;
    }

    public boolean isTimingOutIndicatorEnabled() {
        return timingOutIndicatorEnabled;
    }

    public void setTimingOutIndicatorEnabled(boolean timingOutIndicatorEnabled) {
        this.timingOutIndicatorEnabled = timingOutIndicatorEnabled;
    }

    public boolean isUnreachableIndicatorEnabled() {
        return unreachableIndicatorEnabled;
    }

    public void setUnreachableIndicatorEnabled(boolean unreachableIndicatorEnabled) {
        this.unreachableIndicatorEnabled = unreachableIndicatorEnabled;
    }
}
