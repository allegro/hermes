package pl.allegro.tech.hermes.frontend.producer.kafka;

public interface KafkaProducerParameters {

    int getMaxBlockMs();

    int getMetadataMaxAge();

    String getCompressionCodec();

    int getRetries();

    int getRetryBackoffMs();

    int getRequestTimeoutMs();

    int getBatchSize();

    int getTcpSendBuffer();

    int getMaxRequestSize();

    int getLingerMs();

    int getMetricsSampleWindowMs();

    int getMaxInflightRequestsPerConnection();

    boolean isReportNodeMetricsEnabled();
}
