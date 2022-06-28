package pl.allegro.tech.hermes.frontend.producer.kafka;

public class KafkaProducerParameters {

    private final int maxBlockMs;

    private final int metadataMaxAge;

    private final String compressionCodec;

    private final int retries;

    private final int retryBackoffMs;

    private final int requestTimeoutMs;

    private final int batchSize;

    private final int tcpSendBuffer;

    private final int maxRequestSize;

    private final int lingerMs;

    private final int metricsSampleWindowMs;

    private final int maxInflightRequestsPerConnection;

    private final boolean reportNodeMetricsEnabled;

    public int getMaxBlockMs() {
        return maxBlockMs;
    }

    public int getMetadataMaxAge() {
        return metadataMaxAge;
    }

    public String getCompressionCodec() {
        return compressionCodec;
    }

    public int getRetries() {
        return retries;
    }

    public int getRetryBackoffMs() {
        return retryBackoffMs;
    }

    public int getRequestTimeoutMs() {
        return requestTimeoutMs;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public int getTcpSendBuffer() {
        return tcpSendBuffer;
    }

    public int getMaxRequestSize() {
        return maxRequestSize;
    }

    public int getLingerMs() {
        return lingerMs;
    }

    public int getMetricsSampleWindowMs() {
        return metricsSampleWindowMs;
    }

    public int getMaxInflightRequestsPerConnection() {
        return maxInflightRequestsPerConnection;
    }

    public boolean isReportNodeMetricsEnabled() {
        return reportNodeMetricsEnabled;
    }

    public KafkaProducerParameters(int maxBlockMs,
                                   int metadataMaxAge,
                                   String compressionCodec,
                                   int retries,
                                   int retryBackoffMs,
                                   int requestTimeoutMs,
                                   int batchSize,
                                   int tcpSendBuffer,
                                   int maxRequestSize,
                                   int lingerMs,
                                   int metricsSampleWindowMs,
                                   int maxInflightRequestsPerConnection,
                                   boolean reportNodeMetricsEnabled) {
        this.maxBlockMs = maxBlockMs;
        this.metadataMaxAge = metadataMaxAge;
        this.compressionCodec = compressionCodec;
        this.retries = retries;
        this.retryBackoffMs = retryBackoffMs;
        this.requestTimeoutMs = requestTimeoutMs;
        this.batchSize = batchSize;
        this.tcpSendBuffer = tcpSendBuffer;
        this.maxRequestSize = maxRequestSize;
        this.lingerMs = lingerMs;
        this.metricsSampleWindowMs = metricsSampleWindowMs;
        this.maxInflightRequestsPerConnection = maxInflightRequestsPerConnection;
        this.reportNodeMetricsEnabled = reportNodeMetricsEnabled;
    }
}
