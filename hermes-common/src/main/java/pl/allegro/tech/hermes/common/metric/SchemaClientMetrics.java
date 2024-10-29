package pl.allegro.tech.hermes.common.metric;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import pl.allegro.tech.hermes.metrics.HermesTimer;

public class SchemaClientMetrics {
  private final MeterRegistry meterRegistry;

  public SchemaClientMetrics(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  public HermesTimer schemaTimer() {
    return HermesTimer.from(timer("schema.get-schema"));
  }

  public HermesTimer versionsTimer() {
    return HermesTimer.from(timer("schema.get-versions"));
  }

  private Timer timer(String name) {
    return meterRegistry.timer(name, Tags.of("schema_repo_type", "schema-registry"));
  }
}
