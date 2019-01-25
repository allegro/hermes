package pl.allegro.tech.hermes.common.kafka;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

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

    public boolean allMatch(Function<KafkaTopic, Boolean> matcher) {
        return matcher.apply(primary) && secondary.map(matcher).orElse(true);
    }

    public Stream<KafkaTopic> stream() {
        return secondary.map(secondary -> Stream.of(primary, secondary)).orElse(Stream.of(primary));
    }
}
