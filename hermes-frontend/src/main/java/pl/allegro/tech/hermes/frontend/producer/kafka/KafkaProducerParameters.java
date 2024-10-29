package pl.allegro.tech.hermes.frontend.producer.kafka;

import java.time.Duration;

public interface KafkaProducerParameters {

  Duration getMaxBlock();

  Duration getMetadataMaxAge();

  String getCompressionCodec();

  int getRetries();

  Duration getRetryBackoff();

  Duration getRequestTimeout();

  int getBatchSize();

  int getTcpSendBuffer();

  int getMaxRequestSize();

  Duration getDeliveryTimeout();

  Duration getLinger();

  Duration getMetricsSampleWindow();

  int getMaxInflightRequestsPerConnection();

  boolean isReportNodeMetricsEnabled();

  boolean isIdempotenceEnabled();
}
