package pl.allegro.tech.hermes.frontend.schema;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;

import javax.inject.Inject;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class MessageSchemaRepository<T> {

    private static final Logger logger = LoggerFactory.getLogger(MessageSchemaRepository.class);

    private final LoadingCache<Topic, SchemaWithSource> schemaCache;
    private final MessageSchemaCompiler<T> schemaCompiler;

    @Inject
    public MessageSchemaRepository(MessageSchemaSourceRepository schemaRepository, ExecutorService reloadSchemaSourceExecutor, MessageSchemaCompiler<T> schemaCompiler) {
        this.schemaCompiler = schemaCompiler;
        this.schemaCache = CacheBuilder
                .newBuilder()
                .refreshAfterWrite(10, TimeUnit.MINUTES)
                .expireAfterWrite(24, TimeUnit.HOURS)
                .build(new CacheLoader<Topic, SchemaWithSource>() {
                    @Override
                    public SchemaWithSource load(Topic topic) throws Exception {
                        String newRawSource = schemaRepository.getSchemaSource(topic);
                        logger.info("Loading schema for topic {}", topic.getQualifiedName());
                        return createSchemaWithSource(newRawSource);
                    }

                    @Override
                    public ListenableFuture<SchemaWithSource> reload(Topic topic, SchemaWithSource oldSchemaWithSource) throws Exception {
                        String newRawSource = schemaRepository.getSchemaSource(topic);
                        if (oldSchemaWithSource.getRawSource().equals(newRawSource)) {
                            return Futures.immediateFuture(oldSchemaWithSource);
                        }

                        ListenableFutureTask<SchemaWithSource> task = ListenableFutureTask.create(() -> {
                            logger.info("Reloading schema for topic {}", topic.getQualifiedName());
                            return createSchemaWithSource(newRawSource);
                        });
                        reloadSchemaSourceExecutor.execute(task);
                        return task;
                    }
                });
    }

    public Optional<T> getSchema(Topic topic) {
        try {
            return Optional.of(schemaCache.get(topic).getSchema());
        } catch (ExecutionException e) {
            logger.warn("Couldn't load schema for topic {}", topic.getQualifiedName(), e);
            return Optional.empty();
        }
    }

    private SchemaWithSource createSchemaWithSource(String source) {
        return new SchemaWithSource(source, schemaCompiler.compile(source));
    }

    private class SchemaWithSource {

        private final String rawSource;
        private final T schema;

        private SchemaWithSource(String rawSource, T schema) {
            this.rawSource = rawSource;
            this.schema = schema;
        }

        public String getRawSource() {
            return rawSource;
        }

        public T getSchema() {
            return schema;
        }
    }

}
