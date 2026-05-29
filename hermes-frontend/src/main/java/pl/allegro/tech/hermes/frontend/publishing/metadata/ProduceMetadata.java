package pl.allegro.tech.hermes.frontend.publishing.metadata;

import jakarta.annotation.Nullable;
import java.util.Optional;

public class ProduceMetadata {
  @Nullable private final String broker;
  @Nullable private final String datacenter;

  public ProduceMetadata(@Nullable String broker, @Nullable String datacenter) {
    this.broker = broker;
    this.datacenter = datacenter;
  }

  public ProduceMetadata(@Nullable String broker) {
    this(broker, null);
  }

  public Optional<String> getBroker() {
    return Optional.ofNullable(broker);
  }

  public Optional<String> getDatacenter() {
    return Optional.ofNullable(datacenter);
  }

  public static ProduceMetadata empty() {
    return new ProduceMetadata(null, null);
  }
}
