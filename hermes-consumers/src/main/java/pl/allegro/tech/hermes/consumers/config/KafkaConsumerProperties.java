package pl.allegro.tech.hermes.consumers.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.KafkaConsumerParameters;

@ConfigurationProperties(prefix = "kafka.consumer")
public class KafkaConsumerProperties {

    private int sendBufferBytes = 256 * 1024;

    private int receiveBufferBytes = 256 * 1024;

    private int fetchMinBytes = 1;

    private int fetchMaxWaitMs = 500;

    private int reconnectBackoffMs = 500;

    private int retryBackoffMs = 500;

    private boolean checkCrcs = true;

    private int metricsSampleWindowMs = 30_000;

    private int metricsNumSamples = 2;

    private int requestTimeoutMs = 250_000;

    private int connectionsMaxIdleMs = 9 * 60 * 1000;

    private int maxPollRecords = 1;

    private int maxPollIntervalMs = Integer.MAX_VALUE;

    private String autoOffsetReset = "earliest";

    private int sessionTimeoutMs = 200_000;

    private int heartbeatIntervalMs = 3000;

    private int metadataMaxAgeMs = 5 * 60 * 1000;

    private int maxPartitionFetchMin = Topic.MIN_MESSAGE_SIZE;

    private int maxPartitionFetchMax = Topic.MAX_MESSAGE_SIZE;

    public int getSendBufferBytes() {
        return sendBufferBytes;
    }

    public void setSendBufferBytes(int sendBufferBytes) {
        this.sendBufferBytes = sendBufferBytes;
    }

    public int getReceiveBufferBytes() {
        return receiveBufferBytes;
    }

    public void setReceiveBufferBytes(int receiveBufferBytes) {
        this.receiveBufferBytes = receiveBufferBytes;
    }

    public int getFetchMinBytes() {
        return fetchMinBytes;
    }

    public void setFetchMinBytes(int fetchMinBytes) {
        this.fetchMinBytes = fetchMinBytes;
    }

    public int getFetchMaxWaitMs() {
        return fetchMaxWaitMs;
    }

    public void setFetchMaxWaitMs(int fetchMaxWaitMs) {
        this.fetchMaxWaitMs = fetchMaxWaitMs;
    }

    public int getReconnectBackoffMs() {
        return reconnectBackoffMs;
    }

    public void setReconnectBackoffMs(int reconnectBackoffMs) {
        this.reconnectBackoffMs = reconnectBackoffMs;
    }

    public int getRetryBackoffMs() {
        return retryBackoffMs;
    }

    public void setRetryBackoffMs(int retryBackoffMs) {
        this.retryBackoffMs = retryBackoffMs;
    }

    public boolean isCheckCrcsEnabled() {
        return checkCrcs;
    }

    public void setCheckCrcs(boolean checkCrcs) {
        this.checkCrcs = checkCrcs;
    }

    public int getMetricsSampleWindowMs() {
        return metricsSampleWindowMs;
    }

    public void setMetricsSampleWindowMs(int metricsSampleWindowMs) {
        this.metricsSampleWindowMs = metricsSampleWindowMs;
    }

    public int getMetricsNumSamples() {
        return metricsNumSamples;
    }

    public void setMetricsNumSamples(int metricsNumSamples) {
        this.metricsNumSamples = metricsNumSamples;
    }

    public int getRequestTimeoutMs() {
        return requestTimeoutMs;
    }

    public void setRequestTimeoutMs(int requestTimeoutMs) {
        this.requestTimeoutMs = requestTimeoutMs;
    }

    public int getConnectionsMaxIdleMs() {
        return connectionsMaxIdleMs;
    }

    public void setConnectionsMaxIdleMs(int connectionsMaxIdleMs) {
        this.connectionsMaxIdleMs = connectionsMaxIdleMs;
    }

    public int getMaxPollRecords() {
        return maxPollRecords;
    }

    public void setMaxPollRecords(int maxPollRecords) {
        this.maxPollRecords = maxPollRecords;
    }

    public int getMaxPollIntervalMs() {
        return maxPollIntervalMs;
    }

    public void setMaxPollIntervalMs(int maxPollIntervalMs) {
        this.maxPollIntervalMs = maxPollIntervalMs;
    }

    public String getAutoOffsetReset() {
        return autoOffsetReset;
    }

    public void setAutoOffsetReset(String autoOffsetReset) {
        this.autoOffsetReset = autoOffsetReset;
    }

    public int getSessionTimeoutMs() {
        return sessionTimeoutMs;
    }

    public void setSessionTimeoutMs(int sessionTimeoutMs) {
        this.sessionTimeoutMs = sessionTimeoutMs;
    }

    public int getHeartbeatIntervalMs() {
        return heartbeatIntervalMs;
    }

    public void setHeartbeatIntervalMs(int heartbeatIntervalMs) {
        this.heartbeatIntervalMs = heartbeatIntervalMs;
    }

    public int getMetadataMaxAgeMs() {
        return metadataMaxAgeMs;
    }

    public void setMetadataMaxAgeMs(int metadataMaxAgeMs) {
        this.metadataMaxAgeMs = metadataMaxAgeMs;
    }

    public int getMaxPartitionFetchMin() {
        return maxPartitionFetchMin;
    }

    public void setMaxPartitionFetchMin(int maxPartitionFetchMin) {
        this.maxPartitionFetchMin = maxPartitionFetchMin;
    }

    public int getMaxPartitionFetchMax() {
        return maxPartitionFetchMax;
    }

    public void setMaxPartitionFetchMax(int maxPartitionFetchMax) {
        this.maxPartitionFetchMax = maxPartitionFetchMax;
    }

    protected KafkaConsumerParameters toKafkaConsumerParameters() {
        return new KafkaConsumerParameters(
                this.sendBufferBytes,
                this.receiveBufferBytes,
                this.fetchMinBytes,
                this.fetchMaxWaitMs,
                this.reconnectBackoffMs,
                this.retryBackoffMs,
                this.checkCrcs,
                this.metricsSampleWindowMs,
                this.metricsNumSamples,
                this.requestTimeoutMs,
                this.connectionsMaxIdleMs,
                this.maxPollRecords,
                this.maxPollIntervalMs,
                this.autoOffsetReset,
                this.sessionTimeoutMs,
                this.heartbeatIntervalMs,
                this.metadataMaxAgeMs,
                this.maxPartitionFetchMin,
                this.maxPartitionFetchMax
        );
    }
}
