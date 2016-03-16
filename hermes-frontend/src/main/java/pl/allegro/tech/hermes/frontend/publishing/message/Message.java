package pl.allegro.tech.hermes.frontend.publishing.message;

import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.domain.topic.schema.CompiledSchema;

import java.util.Optional;

public interface Message {
    String getId();

    byte[] getData();

    long getTimestamp();

    ContentType getContentType();

    <T> Optional<CompiledSchema<T>> getCompiledSchema();

    default <T> T getSchema() {
        return this.<T>getCompiledSchema().get().getSchema();
    }
}
