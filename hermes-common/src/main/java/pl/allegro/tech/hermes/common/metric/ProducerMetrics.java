package pl.allegro.tech.hermes.common.metric;

import static pl.allegro.tech.hermes.common.metric.Gauges.INFLIGHT_REQUESTS;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.util.concurrent.TimeUnit;
import java.util.function.ToDoubleFunction;

public class ProducerMetrics {
  private final MeterRegistry meterRegistry;
  private final GaugeRegistrar gaugeRegistrar;

  public ProducerMetrics(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
    this.gaugeRegistrar = new GaugeRegistrar(meterRegistry);
  }

  public <T> void registerAckAllTotalBytesGauge(
      T stateObj, ToDoubleFunction<T> f, String sender, String datacenter) {
    gaugeRegistrar.registerGauge(ACK_ALL_BUFFER_TOTAL_BYTES, stateObj, f, tags(sender, datacenter));
  }

  public <T> void registerAckLeaderTotalBytesGauge(
      T stateObj, ToDoubleFunction<T> f, String sender, String datacenter) {
    gaugeRegistrar.registerGauge(
        ACK_LEADER_BUFFER_TOTAL_BYTES, stateObj, f, tags(sender, datacenter));
  }

  public <T> void registerAckAllAvailableBytesGauge(
      T stateObj, ToDoubleFunction<T> f, String sender, String datacenter) {
    gaugeRegistrar.registerGauge(
        ACK_ALL_BUFFER_AVAILABLE_BYTES, stateObj, f, tags(sender, datacenter));
  }

  public <T> void registerAckLeaderAvailableBytesGauge(
      T stateObj, ToDoubleFunction<T> f, String sender, String datacenter) {
    gaugeRegistrar.registerGauge(
        ACK_LEADER_BUFFER_AVAILABLE_BYTES, stateObj, f, tags(sender, datacenter));
  }

  public <T> void registerAckAllCompressionRateGauge(
      T stateObj, ToDoubleFunction<T> f, String sender, String datacenter) {
    gaugeRegistrar.registerGauge(ACK_ALL_COMPRESSION_RATE, stateObj, f, tags(sender, datacenter));
  }

  public <T> void registerAckLeaderCompressionRateGauge(
      T stateObj, ToDoubleFunction<T> f, String sender, String datacenter) {
    gaugeRegistrar.registerGauge(
        ACK_LEADER_COMPRESSION_RATE, stateObj, f, tags(sender, datacenter));
  }

  public <T> void registerAckAllFailedBatchesGauge(
      T stateObj, ToDoubleFunction<T> f, String sender, String datacenter) {
    gaugeRegistrar.registerGauge(
        ACK_ALL_FAILED_BATCHES_TOTAL, stateObj, f, tags(sender, datacenter));
  }

  public <T> void registerAckLeaderFailedBatchesGauge(
      T stateObj, ToDoubleFunction<T> f, String sender, String datacenter) {
    gaugeRegistrar.registerGauge(
        ACK_LEADER_FAILED_BATCHES_TOTAL, stateObj, f, tags(sender, datacenter));
  }

  public <T> void registerAckAllMetadataAgeGauge(
      T stateObj, ToDoubleFunction<T> f, String sender, String datacenter) {
    registerTimeGauge(
        stateObj, f, ACK_ALL_METADATA_AGE, tags(sender, datacenter), TimeUnit.SECONDS);
  }

  public <T> void registerAckLeaderMetadataAgeGauge(
      T stateObj, ToDoubleFunction<T> f, String sender, String datacenter) {
    registerTimeGauge(
        stateObj, f, ACK_LEADER_METADATA_AGE, tags(sender, datacenter), TimeUnit.SECONDS);
  }

  public <T> void registerAckAllRecordQueueTimeMaxGauge(
      T stateObj, ToDoubleFunction<T> f, String sender, String datacenter) {
    registerTimeGauge(
        stateObj,
        f,
        ACK_ALL_RECORD_QUEUE_TIME_MAX,
        tags(sender, datacenter),
        TimeUnit.MILLISECONDS);
  }

  public <T> void registerAckLeaderRecordQueueTimeMaxGauge(
      T stateObj, ToDoubleFunction<T> f, String sender, String datacenter) {
    registerTimeGauge(
        stateObj,
        f,
        ACK_LEADER_RECORD_QUEUE_TIME_MAX,
        tags(sender, datacenter),
        TimeUnit.MILLISECONDS);
  }

  public double getBufferTotalBytes() {
    return meterRegistry.get(ACK_ALL_BUFFER_TOTAL_BYTES).gauge().value()
        + meterRegistry.get(ACK_LEADER_BUFFER_TOTAL_BYTES).gauge().value();
  }

  public double getBufferAvailableBytes() {
    return meterRegistry.get(ACK_ALL_BUFFER_AVAILABLE_BYTES).gauge().value()
        + meterRegistry.get(ACK_LEADER_BUFFER_AVAILABLE_BYTES).gauge().value();
  }

  public <T> void registerAckLeaderRecordSendCounter(
      T stateObj, ToDoubleFunction<T> f, String sender, String datacenter) {
    registerCounter(ACK_LEADER_RECORD_SEND_TOTAL, tags(sender, datacenter), stateObj, f);
  }

  public <T> void registerAckAllRecordSendCounter(
      T stateObj, ToDoubleFunction<T> f, String sender, String datacenter) {
    registerCounter(ACK_ALL_RECORD_SEND_TOTAL, tags(sender, datacenter), stateObj, f);
  }

  public <T> void registerProducerInflightRequestGauge(T stateObj, ToDoubleFunction<T> f) {
    meterRegistry.gauge(INFLIGHT_REQUESTS, stateObj, f);
  }

  private static Tags tags(String sender, String datacenter) {
    return Tags.of("storageDc", datacenter, "sender", sender);
  }

  private <T> void registerTimeGauge(
      T stateObj, ToDoubleFunction<T> f, String name, Tags tags, TimeUnit timeUnit) {
    meterRegistry.more().timeGauge(name, tags, stateObj, timeUnit, f);
  }

  private <T> void registerCounter(String name, Tags tags, T stateObj, ToDoubleFunction<T> f) {
    meterRegistry.more().counter(name, tags, stateObj, f);
  }

  private static final String KAFKA_PRODUCER = "kafka-producer.";
  private static final String ACK_LEADER = "ack-leader.";
  private static final String ACK_ALL = "ack-all.";

  private static final String ACK_ALL_BUFFER_TOTAL_BYTES =
      KAFKA_PRODUCER + ACK_ALL + "buffer-total-bytes";
  private static final String ACK_ALL_BUFFER_AVAILABLE_BYTES =
      KAFKA_PRODUCER + ACK_ALL + "buffer-available-bytes";
  private static final String ACK_ALL_METADATA_AGE = KAFKA_PRODUCER + ACK_ALL + "metadata-age";
  private static final String ACK_ALL_RECORD_QUEUE_TIME_MAX =
      KAFKA_PRODUCER + ACK_ALL + "record-queue-time-max";
  private static final String ACK_ALL_COMPRESSION_RATE =
      KAFKA_PRODUCER + ACK_ALL + "compression-rate-avg";
  private static final String ACK_ALL_FAILED_BATCHES_TOTAL =
      KAFKA_PRODUCER + ACK_ALL + "failed-batches-total";
  private static final String ACK_ALL_RECORD_SEND_TOTAL = KAFKA_PRODUCER + ACK_ALL + "record-send";

  private static final String ACK_LEADER_FAILED_BATCHES_TOTAL =
      KAFKA_PRODUCER + ACK_LEADER + "failed-batches-total";
  private static final String ACK_LEADER_BUFFER_TOTAL_BYTES =
      KAFKA_PRODUCER + ACK_LEADER + "buffer-total-bytes";
  private static final String ACK_LEADER_METADATA_AGE =
      KAFKA_PRODUCER + ACK_LEADER + "metadata-age";
  private static final String ACK_LEADER_RECORD_QUEUE_TIME_MAX =
      KAFKA_PRODUCER + ACK_LEADER + "record-queue-time-max";
  private static final String ACK_LEADER_BUFFER_AVAILABLE_BYTES =
      KAFKA_PRODUCER + ACK_LEADER + "buffer-available-bytes";
  private static final String ACK_LEADER_COMPRESSION_RATE =
      KAFKA_PRODUCER + ACK_LEADER + "compression-rate-avg";
  private static final String ACK_LEADER_RECORD_SEND_TOTAL =
      KAFKA_PRODUCER + ACK_LEADER + "record-send";
}
