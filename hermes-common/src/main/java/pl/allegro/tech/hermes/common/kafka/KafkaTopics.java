package pl.allegro.tech.hermes.common.kafka;

import java.util.Optional;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;

public class KafkaTopics {

    private final KafkaTopic primary;
    private final Optional<KafkaTopic> secondary;

    public KafkaTopics(KafkaTopic primary) {
        this.primary = checkNotNull(primary);
        this.secondary = Optional.empty();
    }

    public KafkaTopics(KafkaTopic primary, KafkaTopic secondary) {
        this.primary = checkNotNull(primary);
        this.secondary = Optional.of(secondary);
    }

    public KafkaTopic getPrimary() {
        return primary;
    }

    public Optional<KafkaTopic> getSecondary() {
        return secondary;
    }

    public void forEach(Consumer<KafkaTopic> consumer) {
        consumer.accept(primary);
        secondary.ifPresent(consumer);
    }
}
