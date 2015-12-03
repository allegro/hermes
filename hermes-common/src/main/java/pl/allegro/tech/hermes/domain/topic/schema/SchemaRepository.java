package pl.allegro.tech.hermes.domain.topic.schema;

import com.google.common.collect.Lists;
import pl.allegro.tech.hermes.api.SchemaSource;
import pl.allegro.tech.hermes.api.Topic;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class SchemaRepository<T> {

    private final List<Consumer<TopicWithSchema<T>>> schemaReloadedConsumers = Lists.newArrayList();
    private final List<Consumer<TopicWithSchema<T>>> schemaRemovedConsumers = Lists.newArrayList();

    private final SchemaCompiler<T> schemaCompiler;
    private final Topic.ContentType contentType;
    private final CachedSchemaSourceProvider cachedSchemaSourceProvider;
    private final Map<Topic, T> compiledSchemas = new ConcurrentHashMap<>();

    public SchemaRepository(Topic.ContentType contentType, CachedSchemaSourceProvider cachedSchemaSourceProvider, SchemaCompiler<T> schemaCompiler) {
        this.contentType = contentType;
        this.cachedSchemaSourceProvider = cachedSchemaSourceProvider;
        this.schemaCompiler = schemaCompiler;
        cachedSchemaSourceProvider.onRemove(topicWithSchemaSource -> {
            T compiled = compiledSchemas.remove(topicWithSchemaSource.getTopic());
            schemaRemovedConsumers.forEach(consumer -> consumer.accept(new TopicWithSchema<>(topicWithSchemaSource.getTopic(), compiled)));
        });
        cachedSchemaSourceProvider.onReload(topicWithSchema -> {
            T compiled = schemaCompiler.compile(topicWithSchema.getSchema());
            compiledSchemas.put(topicWithSchema.getTopic(), compiled);
            schemaReloadedConsumers.forEach(consumer -> consumer.accept(new TopicWithSchema<>(topicWithSchema.getTopic(), compiled)));
        });
    }

    public T getSchema(Topic topic) {
        return compiledSchemas.computeIfAbsent(topic, key -> {
            try {
                SchemaSource schemaSource = cachedSchemaSourceProvider.get(topic).orElseThrow(() -> new SchemaSourceNotFoundException(topic));
                return schemaCompiler.compile(schemaSource);
            } catch (Exception e) {
                throw new CouldNotLoadSchemaException("Could not load schema for topic " + topic.getQualifiedName(), e);
            }
        });
    }

    public Topic.ContentType supportedContentType() {
        return contentType;
    }

    public void onReload(Consumer<TopicWithSchema<T>> topicWithSchemaConsumer) {
        schemaReloadedConsumers.add(topicWithSchemaConsumer);
    }

    public void onRemove(Consumer<TopicWithSchema<T>> topicWithSchemaConsumer) {
        schemaRemovedConsumers.add(topicWithSchemaConsumer);
    }
}
