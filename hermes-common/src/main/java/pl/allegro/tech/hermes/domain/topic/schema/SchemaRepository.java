package pl.allegro.tech.hermes.domain.topic.schema;

import com.google.common.base.Ticker;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.SchemaSource;
import pl.allegro.tech.hermes.api.Topic;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class SchemaRepository<T> {

    private static final Logger logger = LoggerFactory.getLogger(SchemaRepository.class);

    private final LoadingCache<Topic, SchemaWithSource> schemaCache;
    private final SchemaCompiler<T> schemaCompiler;
    private final Topic.ContentType contentType;
    private List<Consumer<TopicWithSchema<T>>> topicWithSchemaReloadConsumers = Lists.newArrayList();
    private List<Consumer<TopicWithSchema<T>>> topicWithSchemaRemoveConsumers = Lists.newArrayList();

    @Inject
    public SchemaRepository(Topic.ContentType contentType, SchemaSourceProvider schemaRepository, ExecutorService reloadSchemaSourceExecutor,
                            int schemaCacheRefreshAfterWriteMinutes, int schemaCacheExpireAfterWriteMinutes, SchemaCompiler<T> schemaCompiler) {
        this(
            contentType, schemaRepository, reloadSchemaSourceExecutor, Ticker.systemTicker(),
            schemaCacheRefreshAfterWriteMinutes, schemaCacheExpireAfterWriteMinutes, schemaCompiler
        );
    }

    SchemaRepository(Topic.ContentType contentType, SchemaSourceProvider schemaSourceProvider, ExecutorService reloadSchemaSourceExecutor, Ticker ticker,
                     int schemaCacheRefreshAfterWriteMinutes, int schemaCacheExpireAfterWriteMinutes, SchemaCompiler<T> schemaCompiler) {
        this.contentType = contentType;
        this.schemaCompiler = schemaCompiler;
        this.schemaCache = CacheBuilder
                .newBuilder()
                .ticker(ticker)
                .refreshAfterWrite(schemaCacheRefreshAfterWriteMinutes, TimeUnit.MINUTES)
                .expireAfterWrite(schemaCacheExpireAfterWriteMinutes, TimeUnit.MINUTES)
                .removalListener(new RemovalListener<Topic, SchemaWithSource>() {
                    @Override
                    public void onRemoval(RemovalNotification<Topic, SchemaWithSource> notification) {
                        topicWithSchemaRemoveConsumers.forEach(consumer ->
                            consumer.accept(new TopicWithSchema<>(notification.getKey(), notification.getValue().getSchema())));
                    }
                })
                .build(new SchemaCacheLoader(schemaSourceProvider, reloadSchemaSourceExecutor));
    }

    public T getSchema(Topic topic) {
        try {
            return schemaCache.get(topic).getSchema();
        } catch (Exception e) {
            throw new CouldNotLoadSchemaException("Could not load schema for topic " + topic.getQualifiedName(), e);
        }
    }

    public Topic.ContentType supportedContentType() {
        return contentType;
    }

    public void onReload(Consumer<TopicWithSchema<T>> topicWithSchemaConsumer) {
        topicWithSchemaReloadConsumers.add(topicWithSchemaConsumer);
    }

    public void onRemove(Consumer<TopicWithSchema<T>> topicWithSchemaConsumer) {
        topicWithSchemaRemoveConsumers.add(topicWithSchemaConsumer);
    }

    private void notifyConsumersAboutSchemaReload(Topic topic, SchemaWithSource schemaWithSource) {
        topicWithSchemaReloadConsumers.forEach(consumer -> consumer.accept(new TopicWithSchema<>(topic, schemaWithSource.getSchema())));
    }

    private class SchemaWithSource {

        private final SchemaSource source;
        private final T schema;

        private SchemaWithSource(SchemaSource source, T schema) {
            this.source = source;
            this.schema = schema;
        }

        public SchemaSource getSource() {
            return source;
        }

        public T getSchema() {
            return schema;
        }
    }

    private class SchemaCacheLoader extends CacheLoader<Topic, SchemaWithSource> {

        private final SchemaSourceProvider schemaSourceProvider;
        private final ExecutorService reloadSchemaSourceExecutor;

        public SchemaCacheLoader(SchemaSourceProvider schemaSourceProvider, ExecutorService reloadSchemaSourceExecutor) {
            this.schemaSourceProvider = schemaSourceProvider;
            this.reloadSchemaSourceExecutor = reloadSchemaSourceExecutor;
        }

        @Override
        public SchemaWithSource load(Topic topic) throws Exception {
            SchemaSource newRawSource = findSchemaSource(topic);
            logger.info("Loading schema for topic {}", topic.getQualifiedName());
            return createSchemaWithSource(newRawSource);
        }

        @Override
        public ListenableFuture<SchemaWithSource> reload(Topic topic, SchemaWithSource oldSchemaWithSource) throws Exception {
            SchemaSource newRawSource;
            try {
                newRawSource = findSchemaSource(topic);
            } catch (Exception e) {
                logger.warn("Could not reload schema for topic {}", topic.getQualifiedName(), e);
                return Futures.immediateFuture(oldSchemaWithSource);
            }

            if (oldSchemaWithSource.getSource().equals(newRawSource)) {
                return Futures.immediateFuture(oldSchemaWithSource);
            }

            ListenableFutureTask<SchemaWithSource> task = ListenableFutureTask.create(() -> {
                logger.info("Reloading schema for topic {}", topic.getQualifiedName());
                try {
                    SchemaWithSource schemaWithSource = createSchemaWithSource(newRawSource);
                    notifyConsumersAboutSchemaReload(topic, schemaWithSource);
                    return schemaWithSource;
                } catch (Exception e) {
                    logger.warn("Could not compile schema for topic {}", topic.getQualifiedName(), e);
                    throw e;
                }
            });
            reloadSchemaSourceExecutor.execute(task);
            return task;
        }

        private SchemaSource findSchemaSource(Topic topic) {
            return schemaSourceProvider.get(topic).orElseThrow(() -> new SchemaSourceNotFoundException(topic));
        }

        private SchemaWithSource createSchemaWithSource(SchemaSource source) {
            try {
                return new SchemaWithSource(source, schemaCompiler.compile(source));
            } catch (Exception e) {
                throw new CouldNotCompileSchemaException(e);
            }
        }
    }

}
