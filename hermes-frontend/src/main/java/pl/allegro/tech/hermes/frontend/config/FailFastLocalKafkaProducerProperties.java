package pl.allegro.tech.hermes.frontend.config;

import java.time.Duration;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaProducerParameters;

/**
 * Kafka producer maintains a single connection to each broker, over which produce request are sent.
 * When producer request duration exceeds requestTimeout, producer closes the connection to the
 * broker that the request was sent to. This causes all inflight requests that were sent to that
 * broker to be cancelled. The number of inflight requests is configured by
 * maxInflightRequestsPerConnection property.
 *
 * <p>Let's assume that we have requestTimeout set to 500ms, maxInflightRequestsPerConnection set to
 * 5, and there are following inflight batches in the producer being sent to broker1:
 *
 * <p>batchId | time spent in send buffer (socket) ------------------------------------ batch1 |
 * 10ms batch2 | 200ms batch3 | 300ms batch4 | 400ms batch5 | 501ms
 *
 * <p>Batch5 exceeded the requestTimeout so producer will close the connection to broker1. This
 * causes batch5 to be marked as failed but also causes batches 1-4 to be retried. This has the
 * following consequences: 1. Batches 1-4 will probably get duplicated - even tough they were
 * cancelled, they were probably sent to the broker, just haven't been ACKd yet. Retry would cause
 * them to be sent once again resulting in duplicates. 2. On retry, batches 1-4 will have a smaller
 * time budget to complete. Part of their budget was already wasted in send buffer + retryBackoff
 * will be applied to them. They will have little time to complete on retry which can cause them to
 * be timed out, potentially resulting in a vicious circle. 3. Connection to the broker must be
 * reestablished which takes time.
 *
 * <p>To avoid problems described above we actually set requestTimeout and deliveryTimeout to be
 * much higher than the maximum frontend request duration
 * (frontend.handlers.maxPublishRequestDuration). This means that when maxPublishRequestDuration is
 * exceeded for a message we received, a client will receive 5xx even tough the corresponding
 * message is still being processed in the producer. The message will eventually be ACKd by Kafka so
 * upon client-side retry the message will be duplicated. This however, would likely also happen if
 * the message was promptly timed-out by producer before maxPublishRequestDuration elapsed - the
 * message was likely already sent to Kafka, there just haven't been a response yet.
 *
 * <p>So by using large requestTimeout we cause the first slow message to be duplicated (by
 * client-side retry) but: - we protect other inflight messages from being duplicated, - we prevent
 * connections from being frequently dropped and reestablished.
 */
public class FailFastLocalKafkaProducerProperties implements KafkaProducerParameters {
  private Duration maxBlock = Duration.ofMillis(500);

  private Duration metadataMaxAge = Duration.ofMinutes(5);

  private String compressionCodec = "none";

  private int retries = Integer.MAX_VALUE;

  private Duration retryBackoff = Duration.ofMillis(50);

  private Duration requestTimeout = Duration.ofSeconds(30);

  private Duration deliveryTimeout = Duration.ofSeconds(30);

  private int batchSize = 16 * 1024;

  private int tcpSendBuffer = 128 * 1024;

  private int maxRequestSize = 1024 * 1024;

  private Duration linger = Duration.ofMillis(0);

  private Duration metricsSampleWindow = Duration.ofSeconds(30);

  private int maxInflightRequestsPerConnection = 5;

  private boolean reportNodeMetricsEnabled = false;

  private boolean idempotenceEnabled = false;

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

  @Override
  public Duration getDeliveryTimeout() {
    return deliveryTimeout;
  }

  public void setDeliveryTimeout(Duration deliveryTimeout) {
    this.deliveryTimeout = deliveryTimeout;
  }

  public boolean isIdempotenceEnabled() {
    return idempotenceEnabled;
  }

  public void setIdempotenceEnabled(boolean idempotenceEnabled) {
    this.idempotenceEnabled = idempotenceEnabled;
  }
}
