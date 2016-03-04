package pl.allegro.tech.hermes.domain.topic.schema;

import com.google.common.base.Ticker;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.emptyList;

public class CachedSchemaVersionsRepository implements SchemaVersionsRepository {

    private static final Logger logger = LoggerFactory.getLogger(CachedSchemaVersionsRepository.class);

    private final LoadingCache<Topic, List<SchemaVersion>> versionsCache;

    public CachedSchemaVersionsRepository(SchemaSourceProvider schemaSourceProvider, ExecutorService versionsReloader,
                                          int refreshAfterWriteMinutes, int expireAfterWriteMinutes) {
        this(schemaSourceProvider, versionsReloader, refreshAfterWriteMinutes, expireAfterWriteMinutes, Ticker.systemTicker());
    }

    CachedSchemaVersionsRepository(SchemaSourceProvider schemaSourceProvider, ExecutorService versionsReloader,
                                   int refreshAfterWriteMinutes, int expireAfterWriteMinutes, Ticker ticker) {
        this.versionsCache = CacheBuilder
                .newBuilder()
                .ticker(ticker)
                .refreshAfterWrite(refreshAfterWriteMinutes, TimeUnit.MINUTES)
                .expireAfterWrite(expireAfterWriteMinutes, TimeUnit.MINUTES)
                .build(new SchemaVersionsLoader(schemaSourceProvider, versionsReloader));
    }

    @Override
    public List<SchemaVersion> versions(Topic topic) {
        try {
            return versionsCache.get(topic);
        } catch (ExecutionException e) {
            logger.error("Error while loading schema versions for topic {}", topic.getQualifiedName(), e);
            return emptyList();
        }
    }

    private static class SchemaVersionsLoader extends CacheLoader<Topic, List<SchemaVersion>> {

        private final SchemaSourceProvider schemaSourceProvider;
        private final ExecutorService versionsReloader;

        public SchemaVersionsLoader(SchemaSourceProvider schemaSourceProvider, ExecutorService versionsReloader) {
            this.schemaSourceProvider = schemaSourceProvider;
            this.versionsReloader = versionsReloader;
        }

        @Override
        public List<SchemaVersion> load(Topic topic) throws Exception {
            logger.info("Loading schema versions for topic {}", topic.getQualifiedName());
            return schemaSourceProvider.versions(topic);
        }

        @Override
        public ListenableFuture<List<SchemaVersion>> reload(Topic topic, List<SchemaVersion> oldVersions) throws Exception {
            ListenableFutureTask<List<SchemaVersion>> task = ListenableFutureTask.create(() -> {
                logger.info("Reloading schema versions for topic {}", topic.getQualifiedName());
                try {
                    return schemaSourceProvider.versions(topic);
                } catch (Exception e) {
                    logger.warn("Could not reload schema versions for topic {}", topic.getQualifiedName(), e);
                    throw e;
                }
            });
            versionsReloader.execute(task);
            return task;
        }
    }

}
