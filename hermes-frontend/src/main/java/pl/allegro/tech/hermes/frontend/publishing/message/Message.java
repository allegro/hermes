package pl.allegro.tech.hermes.frontend.publishing.message;

import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.schema.CompiledSchema;

import java.util.Optional;

public interface Message {
    String getId();

    byte[] getData();

    long getTimestamp();

    ContentType getContentType();

    default <T> Optional<CompiledSchema<T>> getCompiledSchema() { return Optional.empty(); }

    default <T> T getSchema() {
        return this.<T>getCompiledSchema().get().getSchema();
    }
}
