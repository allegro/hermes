package pl.allegro.tech.hermes.schema;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.emptyList;

public class CachedSchemaVersionsRepository implements SchemaVersionsRepository {

    private static final Logger logger = LoggerFactory.getLogger(CachedSchemaVersionsRepository.class);

    private final RawSchemaClient rawSchemaClient;
    private final ExecutorService versionsReloader;
    private final LoadingCache<Topic, List<SchemaVersion>> versionsCache;

    public CachedSchemaVersionsRepository(RawSchemaClient rawSchemaClient, ExecutorService versionsReloader,
                                          int refreshAfterWriteMinutes, int expireAfterWriteMinutes) {
        this(rawSchemaClient, versionsReloader, refreshAfterWriteMinutes, expireAfterWriteMinutes, Ticker.systemTicker());
    }

    CachedSchemaVersionsRepository(RawSchemaClient rawSchemaClient, ExecutorService versionsReloader,
                                   int refreshAfterWriteMinutes, int expireAfterWriteMinutes, Ticker ticker) {
        this.rawSchemaClient = rawSchemaClient;
        this.versionsReloader = versionsReloader;
        this.versionsCache = CacheBuilder
                .newBuilder()
                .ticker(ticker)
                .refreshAfterWrite(refreshAfterWriteMinutes, TimeUnit.MINUTES)
                .expireAfterWrite(expireAfterWriteMinutes, TimeUnit.MINUTES)
                .build(new SchemaVersionsLoader(rawSchemaClient, versionsReloader));
    }

    @Override
    public List<SchemaVersion> versions(Topic topic, boolean online) {
        try {
            return online? rawSchemaClient.getVersions(topic.getName()) : versionsCache.get(topic);
        } catch (Exception e) {
            logger.error("Error while loading schema versions for topic {}", topic.getQualifiedName(), e);
            return emptyList();
        }
    }

    @Override
    public void close() {
        if (!versionsReloader.isShutdown()) {
            logger.info("Shutdown of schema-source-reloader executor");
            versionsReloader.shutdownNow();
        }
    }

    private static class SchemaVersionsLoader extends CacheLoader<Topic, List<SchemaVersion>> {

        private final RawSchemaClient rawSchemaClient;
        private final ExecutorService versionsReloader;

        public SchemaVersionsLoader(RawSchemaClient rawSchemaClient, ExecutorService versionsReloader) {
            this.rawSchemaClient = rawSchemaClient;
            this.versionsReloader = versionsReloader;
        }

        @Override
        public List<SchemaVersion> load(Topic topic) throws Exception {
            logger.debug("Loading schema versions for topic {}", topic.getQualifiedName());
            return rawSchemaClient.getVersions(topic.getName());
        }

        @Override
        public ListenableFuture<List<SchemaVersion>> reload(Topic topic, List<SchemaVersion> oldVersions) throws Exception {
            ListenableFutureTask<List<SchemaVersion>> task = ListenableFutureTask.create(() -> {
                logger.debug("Reloading schema versions for topic {}", topic.getQualifiedName());
                try {
                    return checkSchemaVersionsAreAvailable(topic, rawSchemaClient.getVersions(topic.getName()));
                } catch (Exception e) {
                    logger.error("Could not reload schema versions for topic {}, will use stale data", topic.getQualifiedName(), e);
                    return oldVersions;
                }
            });
            versionsReloader.execute(task);
            return task;
        }

        private List<SchemaVersion> checkSchemaVersionsAreAvailable(Topic topic, List<SchemaVersion> versions) {
            if (versions.isEmpty()) {
                throw new NoSchemaVersionsFoundException(topic);
            }
            return versions;
        }
    }
}
