package pl.allegro.tech.hermes.domain.topic.schema;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import pl.allegro.tech.hermes.api.SchemaSource;
import pl.allegro.tech.hermes.api.Topic;

import java.util.Objects;

import static java.lang.String.format;

public class CachedConcreteSchemaRepository<T> implements ConcreteSchemaRepository<T> {

    private final LoadingCache<TopicAndSchemaVersion, VersionedSchema<T>> cache;

    public CachedConcreteSchemaRepository(SchemaSourceProvider schemaSourceProvider, long maximumCacheSize,
                                          SchemaCompiler<T> schemaCompiler) {
        this.cache = CacheBuilder
                .newBuilder()
                .maximumSize(maximumCacheSize)
                .build(new SchemaSourceLoader<>(schemaSourceProvider, schemaCompiler));
    }

    @Override
    public VersionedSchema<T> getSchema(Topic topic, int version) {
        try {
            return cache.get(new TopicAndSchemaVersion(topic, version));
        } catch (Exception e) {
            throw new CouldNotLoadSchemaException(e);
        }
    }

    private static class SchemaSourceLoader<T> extends CacheLoader<TopicAndSchemaVersion, VersionedSchema<T>> {

        private final SchemaSourceProvider schemaSourceProvider;
        private final SchemaCompiler<T> schemaCompiler;

        public SchemaSourceLoader(SchemaSourceProvider schemaSourceProvider, SchemaCompiler<T> schemaCompiler) {
            this.schemaSourceProvider = schemaSourceProvider;
            this.schemaCompiler = schemaCompiler;
        }

        @Override
        public VersionedSchema<T> load(TopicAndSchemaVersion key) {
            try {
                SchemaSource schemaSource = schemaSourceProvider.get(key.topic, key.schemaVersion)
                        .orElseThrow(() -> new SchemaSourceNotFoundException(key.topic, key.schemaVersion));
                return new VersionedSchema<>(schemaCompiler.compile(schemaSource), key.schemaVersion);
            } catch (Exception e) {
                throw new CouldNotLoadSchemaException(
                        format("Could not load schema type of %s for topic %s", key.topic.getContentType(), key.topic.getQualifiedName()), e);
            }
        }
    }

    private static class TopicAndSchemaVersion {

        private final Topic topic;
        private final int schemaVersion;

        TopicAndSchemaVersion(Topic topic, int schemaVersion) {
            this.topic = topic;
            this.schemaVersion = schemaVersion;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            TopicAndSchemaVersion that = (TopicAndSchemaVersion) o;
            return schemaVersion == that.schemaVersion &&
                    Objects.equals(topic, that.topic);
        }

        @Override
        public int hashCode() {
            return Objects.hash(topic, schemaVersion);
        }
    }

}
