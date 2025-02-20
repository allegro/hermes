package pl.allegro.tech.hermes.schema;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import pl.allegro.tech.hermes.api.Topic;

public class CachedCompiledSchemaRepository<T> implements CompiledSchemaRepository<T> {

  private final LoadingCache<TopicAndSchemaVersion, CompiledSchema<T>> topicVersionCache;
  private final LoadingCache<TopicAndSchemaId, CompiledSchema<T>> topicIdCache;
  private final CompiledSchemaRepository<T> compiledSchemaRepository;

  public CachedCompiledSchemaRepository(
      CompiledSchemaRepository<T> delegate, long maximumCacheSize, Duration expireAfterAccess) {
    this.topicVersionCache =
        CacheBuilder.newBuilder()
            .maximumSize(maximumCacheSize)
            .expireAfterAccess(expireAfterAccess.toMinutes(), TimeUnit.MINUTES)
            .build(new CompiledSchemaByVersionLoader<>(delegate));

    this.topicIdCache =
        CacheBuilder.newBuilder()
            .maximumSize(maximumCacheSize)
            .expireAfterAccess(expireAfterAccess.toMinutes(), TimeUnit.MINUTES)
            .build(new CompiledSchemaByIdLoader<>(delegate));

    this.compiledSchemaRepository = delegate;
  }

  @Override
  public CompiledSchema<T> getSchema(Topic topic, SchemaVersion version, boolean online) {
    try {
      if (online) {
        CompiledSchema<T> compiledSchema = compiledSchemaRepository.getSchema(topic, version);
        topicVersionCache.put(new TopicAndSchemaVersion(topic, version), compiledSchema);
        topicIdCache.put(new TopicAndSchemaId(topic, compiledSchema.getId()), compiledSchema);
        return compiledSchema;
      }
      return topicVersionCache.get(new TopicAndSchemaVersion(topic, version));
    } catch (Exception e) {
      throw new CouldNotLoadSchemaException(e);
    }
  }

  @Override
  public CompiledSchema<T> getSchema(Topic topic, SchemaId id) {
    try {
      return topicIdCache.get(new TopicAndSchemaId(topic, id));
    } catch (Exception e) {
      throw new CouldNotLoadSchemaException(e);
    }
  }

  public void removeFromCache(Topic topic) {
    Set<TopicAndSchemaVersion> topicWithSchemas =
        topicVersionCache.asMap().keySet().stream()
            .filter(topicWithSchema -> topicWithSchema.topic.equals(topic))
            .collect(Collectors.toSet());

    Set<TopicAndSchemaId> topicAndSchemaIds =
        topicIdCache.asMap().keySet().stream()
            .filter(topicAndSchemaId -> topicAndSchemaId.topic.equals(topic))
            .collect(Collectors.toSet());

    topicVersionCache.invalidateAll(topicWithSchemas);
    topicIdCache.invalidateAll(topicAndSchemaIds);
  }

  private static class CompiledSchemaByVersionLoader<T>
      extends CacheLoader<TopicAndSchemaVersion, CompiledSchema<T>> {

    private final CompiledSchemaRepository<T> delegate;

    public CompiledSchemaByVersionLoader(CompiledSchemaRepository<T> delegate) {
      this.delegate = delegate;
    }

    @Override
    public CompiledSchema<T> load(TopicAndSchemaVersion key) {
      return delegate.getSchema(key.topic, key.schemaVersion);
    }
  }

  private static class CompiledSchemaByIdLoader<T>
      extends CacheLoader<TopicAndSchemaId, CompiledSchema<T>> {

    private final CompiledSchemaRepository<T> delegate;

    public CompiledSchemaByIdLoader(CompiledSchemaRepository<T> delegate) {
      this.delegate = delegate;
    }

    @Override
    public CompiledSchema<T> load(TopicAndSchemaId key) {
      return delegate.getSchema(key.topic, key.schemaId);
    }
  }

  private static class TopicAndSchemaVersion {

    private final Topic topic;
    private final SchemaVersion schemaVersion;

    TopicAndSchemaVersion(Topic topic, SchemaVersion schemaVersion) {
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
      return Objects.equals(schemaVersion, that.schemaVersion) && Objects.equals(topic, that.topic);
    }

    @Override
    public int hashCode() {
      return Objects.hash(topic, schemaVersion);
    }
  }

  private static class TopicAndSchemaId {

    private final Topic topic;
    private final SchemaId schemaId;

    TopicAndSchemaId(Topic topic, SchemaId schemaId) {
      this.topic = topic;
      this.schemaId = schemaId;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      TopicAndSchemaId that = (TopicAndSchemaId) o;
      return Objects.equals(schemaId, that.schemaId) && Objects.equals(topic, that.topic);
    }

    @Override
    public int hashCode() {
      return Objects.hash(topic, schemaId);
    }
  }
}
