package pl.allegro.tech.hermes.common.kafka;

import pl.allegro.tech.hermes.api.Topic;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

public class KafkaTopic {

    private final String name;

    private final Topic.ContentType contentType;

    KafkaTopic(String name, Topic.ContentType contentType) {
        this.name = checkNotNull(name);
        this.contentType = contentType;
    }

    public String name() {
        return name;
    }

    public Topic.ContentType contentType() {
        return contentType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        KafkaTopic that = (KafkaTopic) o;
        return Objects.equals(name, that.name) && Objects.equals(contentType, that.contentType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, contentType);
    }

    @Override
    public String toString() {
        return com.google.common.base.Objects.toStringHelper(this)
                .add("name", name)
                .add("content-type", contentType)
                .toString();
    }
}
