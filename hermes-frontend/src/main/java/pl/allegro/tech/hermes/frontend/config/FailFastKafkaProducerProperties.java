package pl.allegro.tech.hermes.frontend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaProducerParameters;

import java.time.Duration;

@ConfigurationProperties(prefix = "frontend.kafka.fail-fast-producer")
public class FailFastKafkaProducerProperties implements KafkaProducerParameters {

    private Duration speculativeSendDelay = Duration.ofMillis(250);

    private FallbackSchedulerProperties fallbackScheduler = new FallbackSchedulerProperties();

    private Duration maxBlock = Duration.ofMillis(500);

    private Duration metadataMaxAge = Duration.ofMinutes(5);

    private String compressionCodec = "none";

    private int retries = Integer.MAX_VALUE;

    private Duration retryBackoff = Duration.ofMillis(50);

    private Duration requestTimeout = Duration.ofMillis(500);

    private Duration deliveryTimeout = Duration.ofMillis(500);

    private int batchSize = 16 * 1024;

    private int tcpSendBuffer = 128 * 1024;

    private int maxRequestSize = 1024 * 1024;

    private Duration linger = Duration.ofMillis(0);

    private Duration metricsSampleWindow = Duration.ofSeconds(30);

    private int maxInflightRequestsPerConnection = 5;

    private boolean reportNodeMetricsEnabled = false;

    @Override
    public Duration getMaxBlock() {
        return maxBlock;
    }

    public void setMaxBlock(Duration maxBlock) {
        this.maxBlock = maxBlock;
    }

    @Override
    public Duration getMetadataMaxAge() {
        return metadataMaxAge;
    }

    public void setMetadataMaxAge(Duration metadataMaxAge) {
        this.metadataMaxAge = metadataMaxAge;
    }

    @Override
    public String getCompressionCodec() {
        return compressionCodec;
    }

    public void setCompressionCodec(String compressionCodec) {
        this.compressionCodec = compressionCodec;
    }

    @Override
    public int getRetries() {
        return retries;
    }

    public void setRetries(int retries) {
        this.retries = retries;
    }

    @Override
    public Duration getRetryBackoff() {
        return retryBackoff;
    }

    public void setRetryBackoff(Duration retryBackoff) {
        this.retryBackoff = retryBackoff;
    }

    @Override
    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(Duration requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    @Override
    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    @Override
    public int getTcpSendBuffer() {
        return tcpSendBuffer;
    }

    public void setTcpSendBuffer(int tcpSendBuffer) {
        this.tcpSendBuffer = tcpSendBuffer;
    }

    @Override
    public int getMaxRequestSize() {
        return maxRequestSize;
    }

    public void setMaxRequestSize(int maxRequestSize) {
        this.maxRequestSize = maxRequestSize;
    }

    @Override
    public Duration getLinger() {
        return linger;
    }

    public void setLinger(Duration linger) {
        this.linger = linger;
    }

    @Override
    public Duration getMetricsSampleWindow() {
        return metricsSampleWindow;
    }

    public void setMetricsSampleWindow(Duration metricsSampleWindow) {
        this.metricsSampleWindow = metricsSampleWindow;
    }

    @Override
    public int getMaxInflightRequestsPerConnection() {
        return maxInflightRequestsPerConnection;
    }

    public void setMaxInflightRequestsPerConnection(int maxInflightRequestsPerConnection) {
        this.maxInflightRequestsPerConnection = maxInflightRequestsPerConnection;
    }

    @Override
    public boolean isReportNodeMetricsEnabled() {
        return reportNodeMetricsEnabled;
    }

    public void setReportNodeMetricsEnabled(boolean reportNodeMetricsEnabled) {
        this.reportNodeMetricsEnabled = reportNodeMetricsEnabled;
    }

    public Duration getSpeculativeSendDelay() {
        return speculativeSendDelay;
    }

    public void setSpeculativeSendDelay(Duration speculativeSendDelay) {
        this.speculativeSendDelay = speculativeSendDelay;
    }

    @Override
    public Duration getDeliveryTimeout() {
        return deliveryTimeout;
    }

    public void setDeliveryTimeout(Duration deliveryTimeout) {
        this.deliveryTimeout = deliveryTimeout;
    }

    public FallbackSchedulerProperties getFallbackScheduler() {
        return fallbackScheduler;
    }

    public void setFallbackScheduler(FallbackSchedulerProperties fallbackScheduler) {
        this.fallbackScheduler = fallbackScheduler;
    }

    public static class FallbackSchedulerProperties {

        private int threadPoolSize = 16;

        private boolean threadPoolMonitoringEnabled = false;


        public int getThreadPoolSize() {
            return threadPoolSize;
        }

        public void setThreadPoolSize(int threadPoolSize) {
            this.threadPoolSize = threadPoolSize;
        }

        public boolean isThreadPoolMonitoringEnabled() {
            return threadPoolMonitoringEnabled;
        }

        public void setThreadPoolMonitoringEnabled(boolean threadPoolMonitoringEnabled) {
            this.threadPoolMonitoringEnabled = threadPoolMonitoringEnabled;
        }
    }
}
