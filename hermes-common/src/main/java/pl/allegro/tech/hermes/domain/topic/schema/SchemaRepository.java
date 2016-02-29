package pl.allegro.tech.hermes.domain.topic.schema;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.SchemaSource;
import pl.allegro.tech.hermes.api.Topic;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static java.lang.String.format;

public class SchemaRepository<T> {

    private static final Logger logger = LoggerFactory.getLogger(SchemaRepository.class);

    private final List<Consumer<TopicWithSchema<T>>> schemaReloadedConsumers = Lists.newArrayList();
    private final List<Consumer<TopicWithSchema<T>>> schemaRemovedConsumers = Lists.newArrayList();

    private final SchemaCompiler<T> schemaCompiler;
    private final ContentType contentType;
    private final CachedSchemaSourceProvider cachedSchemaSourceProvider;
    private final Map<Topic, T> compiledSchemas = new ConcurrentHashMap<>();

    public SchemaRepository(ContentType contentType, CachedSchemaSourceProvider cachedSchemaSourceProvider, SchemaCompiler<T> schemaCompiler) {
        this.contentType = contentType;
        this.cachedSchemaSourceProvider = cachedSchemaSourceProvider;
        this.schemaCompiler = schemaCompiler;
        cachedSchemaSourceProvider.onRemove(topicWithSchemaSource -> {
            T compiled = compiledSchemas.remove(topicWithSchemaSource.getTopic());
            schemaRemovedConsumers.forEach(consumer -> consumer.accept(new TopicWithSchema<>(topicWithSchemaSource.getTopic(), compiled)));
        });
        cachedSchemaSourceProvider.onReload(topicWithSchema -> {
            try {
                T compiled = schemaCompiler.compile(topicWithSchema.getSchema());
                compiledSchemas.put(topicWithSchema.getTopic(), compiled);
                logger.info("Successful schema compilation type of {} for topic {}", contentType, topicWithSchema.getTopic().getQualifiedName());
                schemaReloadedConsumers.forEach(consumer -> consumer.accept(new TopicWithSchema<>(topicWithSchema.getTopic(), compiled)));
            } catch (RuntimeException exception) {
                logger.debug("Unsuccessful schema compilation type of {} for topic {}",
                        contentType, topicWithSchema.getTopic().getQualifiedName());
            }
        });
    }

    public T getSchema(Topic topic) {
        return compiledSchemas.computeIfAbsent(topic, key -> {
            try {
                SchemaSource schemaSource = cachedSchemaSourceProvider.get(topic).orElseThrow(() -> new SchemaSourceNotFoundException(topic));
                return schemaCompiler.compile(schemaSource);
            } catch (Exception e) {
                throw new CouldNotLoadSchemaException(
                    format("Could not load schema type of %s for topic %s", contentType, topic.getQualifiedName()), e);
            }
        });
    }

    public T getSchema(Topic topic, String schemaVersion) {
        throw new UnsupportedOperationException();
    }

    public void onReload(Consumer<TopicWithSchema<T>> topicWithSchemaConsumer) {
        schemaReloadedConsumers.add(topicWithSchemaConsumer);
    }

    public void onRemove(Consumer<TopicWithSchema<T>> topicWithSchemaConsumer) {
        schemaRemovedConsumers.add(topicWithSchemaConsumer);
    }
}
