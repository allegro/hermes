package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import java.time.Duration;

public class KafkaConsumerParameters {

    private final int sendBufferBytes;

    private final int receiveBufferBytes;

    private final int fetchMinBytes;

    private final Duration fetchMaxWait;

    private final Duration reconnectBackoff;

    private final Duration retryBackoff;

    private final boolean checkCrcs;

    private final Duration metricsSampleWindow;

    private final int metricsNumSamples;

    private final Duration requestTimeout;

    private final Duration connectionsMaxIdle;

    private final int maxPollRecords;

    private final Duration maxPollInterval;

    private final String autoOffsetReset;

    private final Duration sessionTimeout;

    private final Duration heartbeatInterval;

    private final Duration metadataMaxAge;

    private final int maxPartitionFetchMin;

    private final int maxPartitionFetchMax;

    public KafkaConsumerParameters(int sendBufferBytes,
                                   int receiveBufferBytes,
                                   int fetchMinBytes,
                                   Duration fetchMaxWait,
                                   Duration reconnectBackoff,
                                   Duration retryBackoff,
                                   boolean checkCrcs,
                                   Duration metricsSampleWindow,
                                   int metricsNumSamples,
                                   Duration requestTimeout,
                                   Duration connectionsMaxIdle,
                                   int maxPollRecords,
                                   Duration maxPollInterval,
                                   String autoOffsetReset,
                                   Duration sessionTimeout,
                                   Duration heartbeatInterval,
                                   Duration metadataMaxAge,
                                   int maxPartitionFetchMin,
                                   int maxPartitionFetchMax) {
        this.sendBufferBytes = sendBufferBytes;
        this.receiveBufferBytes = receiveBufferBytes;
        this.fetchMinBytes = fetchMinBytes;
        this.fetchMaxWait = fetchMaxWait;
        this.reconnectBackoff = reconnectBackoff;
        this.retryBackoff = retryBackoff;
        this.checkCrcs = checkCrcs;
        this.metricsSampleWindow = metricsSampleWindow;
        this.metricsNumSamples = metricsNumSamples;
        this.requestTimeout = requestTimeout;
        this.connectionsMaxIdle = connectionsMaxIdle;
        this.maxPollRecords = maxPollRecords;
        this.maxPollInterval = maxPollInterval;
        this.autoOffsetReset = autoOffsetReset;
        this.sessionTimeout = sessionTimeout;
        this.heartbeatInterval = heartbeatInterval;
        this.metadataMaxAge = metadataMaxAge;
        this.maxPartitionFetchMin = maxPartitionFetchMin;
        this.maxPartitionFetchMax = maxPartitionFetchMax;
    }

    public int getSendBufferBytes() {
        return sendBufferBytes;
    }

    public int getReceiveBufferBytes() {
        return receiveBufferBytes;
    }

    public int getFetchMinBytes() {
        return fetchMinBytes;
    }

    public Duration getFetchMaxWait() {
        return fetchMaxWait;
    }

    public Duration getReconnectBackoff() {
        return reconnectBackoff;
    }

    public Duration getRetryBackoff() {
        return retryBackoff;
    }

    public boolean isCheckCrcs() {
        return checkCrcs;
    }

    public Duration getMetricsSampleWindow() {
        return metricsSampleWindow;
    }

    public int getMetricsNumSamples() {
        return metricsNumSamples;
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    public Duration getConnectionsMaxIdle() {
        return connectionsMaxIdle;
    }

    public int getMaxPollRecords() {
        return maxPollRecords;
    }

    public Duration getMaxPollInterval() {
        return maxPollInterval;
    }

    public String getAutoOffsetReset() {
        return autoOffsetReset;
    }

    public Duration getSessionTimeout() {
        return sessionTimeout;
    }

    public Duration getHeartbeatInterval() {
        return heartbeatInterval;
    }

    public Duration getMetadataMaxAge() {
        return metadataMaxAge;
    }

    public int getMaxPartitionFetchMin() {
        return maxPartitionFetchMin;
    }

    public int getMaxPartitionFetchMax() {
        return maxPartitionFetchMax;
    }
}
