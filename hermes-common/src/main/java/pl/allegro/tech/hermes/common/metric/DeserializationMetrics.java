package pl.allegro.tech.hermes.common.metric;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import pl.allegro.tech.hermes.metrics.HermesCounter;
import pl.allegro.tech.hermes.metrics.counters.HermesCounters;

public class DeserializationMetrics {
  private final MeterRegistry meterRegistry;

  private static final String BASE_PATH = "content.avro.deserialization";
  private static final String ERRORS_PATH = BASE_PATH + ".errors";

  public DeserializationMetrics(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  public HermesCounter errorsForHeaderSchemaVersion() {
    return HermesCounters.from(deserializationErrorCounter("headerSchemaVersion"));
  }

  public HermesCounter errorsForHeaderSchemaId() {
    return HermesCounters.from(deserializationErrorCounter("headerSchemaId"));
  }

  public HermesCounter errorsForSchemaIdAwarePayload() {
    return HermesCounters.from(deserializationErrorCounter("payloadWithSchemaId"));
  }

  public HermesCounter errorsForSchemaVersionTruncation() {
    return HermesCounters.from(deserializationErrorCounter("schemaVersionTruncation"));
  }

  private io.micrometer.core.instrument.Counter deserializationErrorCounter(String schemaSource) {
    return meterRegistry.counter(ERRORS_PATH, Tags.of("deserialization_type", schemaSource));
  }

  public HermesCounter missingSchemaIdInPayload() {
    return HermesCounters.from(meterRegistry.counter(BASE_PATH + ".missing_schemaIdInPayload"));
  }

  public HermesCounter usingHeaderSchemaVersion() {
    return HermesCounters.from(deserializationAttemptCounter("headerSchemaVersion"));
  }

  public HermesCounter usingHeaderSchemaId() {
    return HermesCounters.from(deserializationAttemptCounter("headerSchemaId"));
  }

  public HermesCounter usingSchemaIdAware() {
    return HermesCounters.from(deserializationAttemptCounter("payloadWithSchemaId"));
  }

  public HermesCounter usingSchemaVersionTruncation() {
    return HermesCounters.from(deserializationAttemptCounter("schemaVersionTruncation"));
  }

  private Counter deserializationAttemptCounter(String deserializationType) {
    return meterRegistry.counter(BASE_PATH, Tags.of("deserialization_type", deserializationType));
  }
}
