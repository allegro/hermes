package pl.allegro.tech.hermes.management.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "health")
public class HealthProperties {
    private int maxLagInSeconds = 600;
    private double minSubscriptionToTopicSpeedRatio = 0.8;
    private double maxOtherErrorsRatio = 0.5;
    private double maxTimeoutsRatio = 0.1;
    private double max5xxErrorsRatio = 0.1;
    private double max4xxErrorsRatio = 0.1;
    private double minSubscriptionRateForReliableMetrics = 2.0;

    public int getMaxLagInSeconds() {
        return maxLagInSeconds;
    }

    public void setMaxLagInSeconds(int maxLagInSeconds) {
        this.maxLagInSeconds = maxLagInSeconds;
    }

    public double getMinSubscriptionToTopicSpeedRatio() {
        return minSubscriptionToTopicSpeedRatio;
    }

    public void setMinSubscriptionToTopicSpeedRatio(double minSubscriptionToTopicSpeedRatio) {
        this.minSubscriptionToTopicSpeedRatio = minSubscriptionToTopicSpeedRatio;
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
}
