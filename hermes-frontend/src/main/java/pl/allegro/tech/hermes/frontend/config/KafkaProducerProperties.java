package pl.allegro.tech.hermes.frontend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaProducerParameters;

@ConfigurationProperties(prefix = "frontend.kafka.producer")
public class KafkaProducerProperties implements KafkaProducerParameters {

    private int maxBlockMs = 500;

    private int metadataMaxAge = 5 * 60 * 1000;

    private String compressionCodec = "none";

    private int retries = Integer.MAX_VALUE;

    private int retryBackoffMs = 256;

    private int requestTimeoutMs = 30 * 60 * 1000;

    private int batchSize = 16 * 1024;

    private int tcpSendBuffer = 128 * 1024;

    private int maxRequestSize = 1024 * 1024;

    private int lingerMs = 0;

    private int metricsSampleWindowMs = 30_000;

    private int maxInflightRequestsPerConnection = 5;

    private boolean reportNodeMetricsEnabled = false;

    @Override
    public int getMaxBlockMs() {
        return maxBlockMs;
    }

    public void setMaxBlockMs(int maxBlockMs) {
        this.maxBlockMs = maxBlockMs;
    }

    @Override
    public int getMetadataMaxAge() {
        return metadataMaxAge;
    }

    public void setMetadataMaxAge(int metadataMaxAge) {
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
    public int getRetryBackoffMs() {
        return retryBackoffMs;
    }

    public void setRetryBackoffMs(int retryBackoffMs) {
        this.retryBackoffMs = retryBackoffMs;
    }

    @Override
    public int getRequestTimeoutMs() {
        return requestTimeoutMs;
    }

    public void setRequestTimeoutMs(int requestTimeoutMs) {
        this.requestTimeoutMs = requestTimeoutMs;
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
    public int getLingerMs() {
        return lingerMs;
    }

    public void setLingerMs(int lingerMs) {
        this.lingerMs = lingerMs;
    }

    @Override
    public int getMetricsSampleWindowMs() {
        return metricsSampleWindowMs;
    }

    public void setMetricsSampleWindowMs(int metricsSampleWindowMs) {
        this.metricsSampleWindowMs = metricsSampleWindowMs;
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
}
