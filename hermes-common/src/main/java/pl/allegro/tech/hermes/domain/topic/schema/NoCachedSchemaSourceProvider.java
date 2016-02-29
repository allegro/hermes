package pl.allegro.tech.hermes.domain.topic.schema;

import pl.allegro.tech.hermes.api.SchemaSource;
import pl.allegro.tech.hermes.api.Topic;

import java.util.Optional;
import java.util.function.Consumer;

public class NoCachedSchemaSourceProvider implements CachedSchemaSourceProvider {

    private final SchemaSourceProvider schemaSourceProvider;

    public NoCachedSchemaSourceProvider(SchemaSourceProvider schemaSourceProvider) {
        this.schemaSourceProvider = schemaSourceProvider;
    }

    @Override
    public void onReload(Consumer<TopicWithSchema<SchemaSource>> schemaSourceConsumer) {
    }

    @Override
    public void onRemove(Consumer<TopicWithSchema<SchemaSource>> schemaSourceConsumer) {
    }

    @Override
    public void reload(Topic topic) {
    }

    @Override
    public Optional<SchemaSource> get(Topic topic) {
        return schemaSourceProvider.get(topic);
    }

    @Override
    public Optional<SchemaSource> get(Topic topic, int version) {
        return schemaSourceProvider.get(topic, version);
    }
}
