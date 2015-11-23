package pl.allegro.tech.hermes.domain.topic.schema;

import pl.allegro.tech.hermes.api.SchemaSource;

import java.util.function.Consumer;

public interface CachedSchemaSourceProvider extends SchemaSourceProvider {
    void onReload(Consumer<TopicWithSchema<SchemaSource>> schemaSourceConsumer);
    void onRemove(Consumer<TopicWithSchema<SchemaSource>> schemaSourceConsumer);
}
