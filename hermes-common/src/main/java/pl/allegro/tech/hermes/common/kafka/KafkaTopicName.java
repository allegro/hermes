package pl.allegro.tech.hermes.common.kafka;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

public class KafkaTopicName {

    private final String value;

    private KafkaTopicName(String value) {
        this.value = checkNotNull(value);
    }

    public static KafkaTopicName valueOf(String value) {
        return new KafkaTopicName(value);
    }

    public String asString() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        KafkaTopicName that = (KafkaTopicName) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "KafkaTopicName(" + value + ")";
    }
}
