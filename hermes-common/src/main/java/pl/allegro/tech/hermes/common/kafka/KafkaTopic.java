package pl.allegro.tech.hermes.common.kafka;

import pl.allegro.tech.hermes.api.ContentType;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

public class KafkaTopic {

    private final KafkaTopicName name;
    private final ContentType contentType;

    public KafkaTopic(KafkaTopicName name, ContentType contentType) {
        this.name = checkNotNull(name);
        this.contentType = checkNotNull(contentType);
    }

    public KafkaTopicName name() {
        return name;
    }

    public ContentType contentType() {
        return contentType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KafkaTopic that = (KafkaTopic) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(contentType, that.contentType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, contentType);
    }

    @Override
    public String toString() {
        return com.google.common.base.Objects.toStringHelper(this)
                .add("name", name)
                .add("contentType", contentType)
                .toString();
    }
}
