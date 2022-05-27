package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

public class KafkaConsumerParameters {

    private final int sendBufferBytes;

    private final int receiveBufferBytes;

    private final int fetchMinBytes;

    private final int fetchMaxWaitMs;

    private final int reconnectBackoffMs;

    private final int retryBackoffMs;

    private final boolean checkCrcs;

    private final int metricsSampleWindowMs;

    private final int metricsNumSamples;

    private final int requestTimeoutMs;

    private final int connectionsMaxIdleMs;

    private final int maxPollRecords;

    private final int maxPollIntervalMs;

    private final String autoOffsetReset;

    private final int sessionTimeoutMs;

    private final int heartbeatIntervalMs;

    private final int metadataMaxAgeMs;

    private final int maxPartitionFetchMin;

    private final int maxPartitionFetchMax;

    public int getSendBufferBytes() {
        return sendBufferBytes;
    }

    public int getReceiveBufferBytes() {
        return receiveBufferBytes;
    }

    public int getFetchMinBytes() {
        return fetchMinBytes;
    }

    public int getFetchMaxWaitMs() {
        return fetchMaxWaitMs;
    }

    public int getReconnectBackoffMs() {
        return reconnectBackoffMs;
    }

    public int getRetryBackoffMs() {
        return retryBackoffMs;
    }

    public boolean isCheckCrcsEnabled() {
        return checkCrcs;
    }

    public int getMetricsSampleWindowMs() {
        return metricsSampleWindowMs;
    }

    public int getMetricsNumSamples() {
        return metricsNumSamples;
    }

    public int getRequestTimeoutMs() {
        return requestTimeoutMs;
    }

    public int getConnectionsMaxIdleMs() {
        return connectionsMaxIdleMs;
    }

    public int getMaxPollRecords() {
        return maxPollRecords;
    }

    public int getMaxPollIntervalMs() {
        return maxPollIntervalMs;
    }

    public String getAutoOffsetReset() {
        return autoOffsetReset;
    }

    public int getSessionTimeoutMs() {
        return sessionTimeoutMs;
    }

    public int getHeartbeatIntervalMs() {
        return heartbeatIntervalMs;
    }

    public int getMetadataMaxAgeMs() {
        return metadataMaxAgeMs;
    }

    public int getMaxPartitionFetchMin() {
        return maxPartitionFetchMin;
    }

    public int getMaxPartitionFetchMax() {
        return maxPartitionFetchMax;
    }

    public KafkaConsumerParameters(int sendBufferBytes, int receiveBufferBytes,
                                   int fetchMinBytes, int fetchMaxWaitMs,
                                   int reconnectBackoffMs, int retryBackoffMs,
                                   boolean checkCrcs, int metricsSampleWindowMs,
                                   int metricsNumSamples, int requestTimeoutMs,
                                   int connectionsMaxIdleMs, int maxPollRecords,
                                   int maxPollIntervalMs, String autoOffsetReset,
                                   int sessionTimeoutMs, int heartbeatIntervalMs,
                                   int metadataMaxAgeMs, int maxPartitionFetchMin, int maxPartitionFetchMax) {
        this.sendBufferBytes = sendBufferBytes;
        this.receiveBufferBytes = receiveBufferBytes;
        this.fetchMinBytes = fetchMinBytes;
        this.fetchMaxWaitMs = fetchMaxWaitMs;
        this.reconnectBackoffMs = reconnectBackoffMs;
        this.retryBackoffMs = retryBackoffMs;
        this.checkCrcs = checkCrcs;
        this.metricsSampleWindowMs = metricsSampleWindowMs;
        this.metricsNumSamples = metricsNumSamples;
        this.requestTimeoutMs = requestTimeoutMs;
        this.connectionsMaxIdleMs = connectionsMaxIdleMs;
        this.maxPollRecords = maxPollRecords;
        this.maxPollIntervalMs = maxPollIntervalMs;
        this.autoOffsetReset = autoOffsetReset;
        this.sessionTimeoutMs = sessionTimeoutMs;
        this.heartbeatIntervalMs = heartbeatIntervalMs;
        this.metadataMaxAgeMs = metadataMaxAgeMs;
        this.maxPartitionFetchMin = maxPartitionFetchMin;
        this.maxPartitionFetchMax = maxPartitionFetchMax;
    }
}
