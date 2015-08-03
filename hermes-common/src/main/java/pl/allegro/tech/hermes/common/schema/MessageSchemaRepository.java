package pl.allegro.tech.hermes.common.schema;

import com.google.common.base.Ticker;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class MessageSchemaRepository<T> {

    private static final Logger logger = LoggerFactory.getLogger(MessageSchemaRepository.class);

    private final LoadingCache<Topic, SchemaWithSource> schemaCache;
    private final MessageSchemaCompiler<T> schemaCompiler;

    @Inject
    public MessageSchemaRepository(MessageSchemaSourceRepository schemaRepository, ExecutorService reloadSchemaSourceExecutor, MessageSchemaCompiler<T> schemaCompiler) {
        this(schemaRepository, reloadSchemaSourceExecutor, Ticker.systemTicker(), schemaCompiler);
    }

    MessageSchemaRepository(MessageSchemaSourceRepository schemaRepository, ExecutorService reloadSchemaSourceExecutor, Ticker ticker, MessageSchemaCompiler<T> schemaCompiler) {
        this.schemaCompiler = schemaCompiler;
        this.schemaCache = CacheBuilder
                .newBuilder()
                .ticker(ticker)
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
                        String newRawSource;
                        try {
                            newRawSource = schemaRepository.getSchemaSource(topic);
                        } catch (Exception e) {
                            logger.warn("Could not reload schema for topic {}", topic.getQualifiedName(), e);
                            return Futures.immediateFuture(oldSchemaWithSource);
                        }

                        if (oldSchemaWithSource.getRawSource().equals(newRawSource)) {
                            return Futures.immediateFuture(oldSchemaWithSource);
                        }

                        ListenableFutureTask<SchemaWithSource> task = ListenableFutureTask.create(() -> {
                            logger.info("Reloading schema for topic {}", topic.getQualifiedName());
                            try {
                                return createSchemaWithSource(newRawSource);
                            } catch (Exception e) {
                                logger.warn("Could not compile schema for topic {}", topic.getQualifiedName(), e);
                                throw e;
                            }
                        });
                        reloadSchemaSourceExecutor.execute(task);
                        return task;
                    }
                });
    }

    public T getSchema(Topic topic) {
        try {
            return schemaCache.get(topic).getSchema();
        } catch (Exception e) {
            throw new CouldNotLoadSchemaException("Could not load schema for topic " + topic.getQualifiedName(), e);
        }
    }

    private SchemaWithSource createSchemaWithSource(String source) {
        try {
            return new SchemaWithSource(source, schemaCompiler.compile(source));
        } catch (Exception e) {
            throw new CouldNotCompileSchemaException(e);
        }
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
