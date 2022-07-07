package pl.allegro.tech.hermes.frontend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaProducerParameters;

@ConfigurationProperties(prefix = "frontend.kafka.producer")
public class KafkaProducerProperties {

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

    public int getMaxBlockMs() {
        return maxBlockMs;
    }

    public void setMaxBlockMs(int maxBlockMs) {
        this.maxBlockMs = maxBlockMs;
    }

    public int getMetadataMaxAge() {
        return metadataMaxAge;
    }

    public void setMetadataMaxAge(int metadataMaxAge) {
        this.metadataMaxAge = metadataMaxAge;
    }

    public String getCompressionCodec() {
        return compressionCodec;
    }

    public void setCompressionCodec(String compressionCodec) {
        this.compressionCodec = compressionCodec;
    }

    public int getRetries() {
        return retries;
    }

    public void setRetries(int retries) {
        this.retries = retries;
    }

    public int getRetryBackoffMs() {
        return retryBackoffMs;
    }

    public void setRetryBackoffMs(int retryBackoffMs) {
        this.retryBackoffMs = retryBackoffMs;
    }

    public int getRequestTimeoutMs() {
        return requestTimeoutMs;
    }

    public void setRequestTimeoutMs(int requestTimeoutMs) {
        this.requestTimeoutMs = requestTimeoutMs;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getTcpSendBuffer() {
        return tcpSendBuffer;
    }

    public void setTcpSendBuffer(int tcpSendBuffer) {
        this.tcpSendBuffer = tcpSendBuffer;
    }

    public int getMaxRequestSize() {
        return maxRequestSize;
    }

    public void setMaxRequestSize(int maxRequestSize) {
        this.maxRequestSize = maxRequestSize;
    }

    public int getLingerMs() {
        return lingerMs;
    }

    public void setLingerMs(int lingerMs) {
        this.lingerMs = lingerMs;
    }

    public int getMetricsSampleWindowMs() {
        return metricsSampleWindowMs;
    }

    public void setMetricsSampleWindowMs(int metricsSampleWindowMs) {
        this.metricsSampleWindowMs = metricsSampleWindowMs;
    }

    public int getMaxInflightRequestsPerConnection() {
        return maxInflightRequestsPerConnection;
    }

    public void setMaxInflightRequestsPerConnection(int maxInflightRequestsPerConnection) {
        this.maxInflightRequestsPerConnection = maxInflightRequestsPerConnection;
    }

    public boolean isReportNodeMetricsEnabled() {
        return reportNodeMetricsEnabled;
    }

    public void setReportNodeMetricsEnabled(boolean reportNodeMetricsEnabled) {
        this.reportNodeMetricsEnabled = reportNodeMetricsEnabled;
    }

    protected KafkaProducerParameters toKafkaProducerParameters() {
        return new KafkaProducerParameters(
                this.maxBlockMs,
                this.metadataMaxAge,
                this.compressionCodec,
                this.retries,
                this.retryBackoffMs,
                this.requestTimeoutMs,
                this.batchSize,
                this.tcpSendBuffer,
                this.maxRequestSize,
                this.lingerMs,
                this.metricsSampleWindowMs,
                this.maxInflightRequestsPerConnection,
                this.reportNodeMetricsEnabled
        );
    }
}
