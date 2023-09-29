package pl.allegro.tech.hermes.frontend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "frontend.broker-latency.reporter")
public class BrokerLatencyReporterProperties {
    private boolean perBrokerLatencyReportingEnabled;
    private Duration kafkaPartitionLeaderRefreshInterval = Duration.ofMinutes(5);
    private Duration slowResponseLoggingThreshold = Duration.ofMillis(100);


    public boolean isPerBrokerLatencyReportingEnabled() {
        return perBrokerLatencyReportingEnabled;
    }

    public void setPerBrokerLatencyReportingEnabled(boolean perBrokerLatencyReportingEnabled) {
        this.perBrokerLatencyReportingEnabled = perBrokerLatencyReportingEnabled;
    }

    public Duration getKafkaPartitionLeaderRefreshInterval() {
        return kafkaPartitionLeaderRefreshInterval;
    }

    public void setKafkaPartitionLeaderRefreshInterval(Duration kafkaPartitionLeaderRefreshInterval) {
        this.kafkaPartitionLeaderRefreshInterval = kafkaPartitionLeaderRefreshInterval;
    }

    public Duration getSlowResponseLoggingThreshold() {
        return slowResponseLoggingThreshold;
    }

    public void setSlowResponseLoggingThreshold(Duration slowResponseLoggingThreshold) {
        this.slowResponseLoggingThreshold = slowResponseLoggingThreshold;
    }
}
