package pl.allegro.tech.hermes.domain.topic.schema;

import com.google.common.base.Ticker;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.SchemaSource;
import pl.allegro.tech.hermes.api.Topic;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class DefaultCachedSchemaSourceProvider implements CachedSchemaSourceProvider {
    private static final Logger logger = LoggerFactory.getLogger(SchemaRepository.class);

    private final LoadingCache<Topic, Optional<SchemaSource>> cache;
    private final List<Consumer<TopicWithSchema<SchemaSource>>> schemaReloadedConsumers = Lists.newArrayList();
    private final List<Consumer<TopicWithSchema<SchemaSource>>> schemaRemovedConsumers = Lists.newArrayList();

    public DefaultCachedSchemaSourceProvider(int schemaCacheRefreshAfterWriteMinutes, int schemaCacheExpireAfterWriteMinutes,
                                             ExecutorService reloadSchemaSourceExecutor, SchemaSourceProvider schemaSourceProvider) {
        this(schemaCacheRefreshAfterWriteMinutes, schemaCacheExpireAfterWriteMinutes,
                reloadSchemaSourceExecutor,schemaSourceProvider, Ticker.systemTicker());
    }

    DefaultCachedSchemaSourceProvider(int schemaCacheRefreshAfterWriteMinutes, int schemaCacheExpireAfterWriteMinutes,
                                      ExecutorService reloadSchemaSourceExecutor, SchemaSourceProvider schemaSourceProvider,
                                      Ticker ticker) {
        this.cache = CacheBuilder
                .newBuilder()
                .ticker(ticker)
                .refreshAfterWrite(schemaCacheRefreshAfterWriteMinutes, TimeUnit.MINUTES)
                .expireAfterWrite(schemaCacheExpireAfterWriteMinutes, TimeUnit.MINUTES)
                .removalListener(new RemovalListener<Topic, Optional<SchemaSource>>() {
                    @Override
                    public void onRemoval(RemovalNotification<Topic, Optional<SchemaSource>> notification) {
                        schemaRemovedConsumers.forEach(consumer ->
                                consumer.accept(new TopicWithSchema<>(notification.getKey(), notification.getValue().get())));
                    }
                })
                .build(new SchemaSourceCacheLoader(schemaSourceProvider, reloadSchemaSourceExecutor));
    }

    @Override
    public void onReload(Consumer<TopicWithSchema<SchemaSource>> schemaSourceConsumer) {
        schemaReloadedConsumers.add(schemaSourceConsumer);
    }

    @Override
    public void onRemove(Consumer<TopicWithSchema<SchemaSource>> schemaSourceConsumer) {
        schemaRemovedConsumers.add(schemaSourceConsumer);
    }

    @Override
    public Optional<SchemaSource> get(Topic topic) {
        try {
            return cache.get(topic);
        } catch (ExecutionException e) {
            logger.error("Error while loading schema source for topic {}", topic.getQualifiedName(), e);
            return Optional.empty();
        }
    }

    @Override
    public void reload(Topic topic) {
        cache.refresh(topic);
    }

    private void notifyConsumersAboutSchemaReload(Topic topic, SchemaSource schemaSource) {
        schemaReloadedConsumers.forEach(consumer -> consumer.accept(new TopicWithSchema<>(topic, schemaSource)));
    }

    private class SchemaSourceCacheLoader extends CacheLoader<Topic, Optional<SchemaSource>> {

        private final SchemaSourceProvider schemaSourceProvider;
        private final ExecutorService reloadSchemaSourceExecutor;

        public SchemaSourceCacheLoader(SchemaSourceProvider schemaSourceProvider, ExecutorService reloadSchemaSourceExecutor) {
            this.schemaSourceProvider = schemaSourceProvider;
            this.reloadSchemaSourceExecutor = reloadSchemaSourceExecutor;
        }

        @Override
        public Optional<SchemaSource> load(Topic topic) throws Exception {
            logger.info("Loading schema source for topic {}", topic.getQualifiedName());
            return schemaSourceProvider.get(topic);
        }

        @Override
        public ListenableFuture<Optional<SchemaSource>> reload(Topic topic, Optional<SchemaSource> oldSchemaSource) throws Exception {
            ListenableFutureTask<Optional<SchemaSource>> task = ListenableFutureTask.create(() -> {
                logger.info("Reloading schema for topic {}", topic.getQualifiedName());
                try {
                    Optional<SchemaSource> newSchemaSource = schemaSourceProvider.get(topic);
                    if (!oldSchemaSource.equals(newSchemaSource)) {
                        notifyConsumersAboutSchemaReload(topic, newSchemaSource.get());
                    }
                    return newSchemaSource;
                } catch (Exception e) {
                    logger.warn("Could not reload schema source for topic {}", topic.getQualifiedName(), e);
                    throw e;
                }
            });
            reloadSchemaSourceExecutor.execute(task);
            return task;
        }

    }
}
