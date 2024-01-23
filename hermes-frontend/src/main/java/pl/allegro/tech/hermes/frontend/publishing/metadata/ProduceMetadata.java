package pl.allegro.tech.hermes.frontend.publishing.metadata;

import jakarta.annotation.Nullable;

import java.util.Optional;

public class ProduceMetadata {
    @Nullable
    private final String broker;

    public ProduceMetadata(@Nullable String broker) {
        this.broker = broker;
    }

    public Optional<String> getBroker() {
        return Optional.ofNullable(broker);
    }

    public static ProduceMetadata empty() {
        return new ProduceMetadata(null);
    }
}
