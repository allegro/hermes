package pl.allegro.tech.hermes.common.schema;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import pl.allegro.tech.hermes.domain.notifications.InternalNotificationsBus;
import pl.allegro.tech.hermes.schema.CachedCompiledSchemaRepository;
import pl.allegro.tech.hermes.schema.CachedSchemaVersionsRepository;
import pl.allegro.tech.hermes.schema.CompiledSchemaRepository;
import pl.allegro.tech.hermes.schema.DirectSchemaVersionsRepository;
import pl.allegro.tech.hermes.schema.RawSchemaClient;
import pl.allegro.tech.hermes.schema.SchemaVersionsRepository;

public class SchemaVersionsRepositoryFactory {

  private final RawSchemaClient rawSchemaClient;
  private final SchemaVersionRepositoryParameters schemaVersionsRepositoryParameters;
  private final InternalNotificationsBus notificationsBus;
  private final CompiledSchemaRepository<?> compiledSchemaRepository;

  public SchemaVersionsRepositoryFactory(
      RawSchemaClient rawSchemaClient,
      SchemaVersionRepositoryParameters schemaVersionsRepositoryParameters,
      InternalNotificationsBus notificationsBus,
      CompiledSchemaRepository<?> compiledSchemaRepository) {
    this.rawSchemaClient = rawSchemaClient;
    this.schemaVersionsRepositoryParameters = schemaVersionsRepositoryParameters;
    this.notificationsBus = notificationsBus;
    this.compiledSchemaRepository = compiledSchemaRepository;
  }

  public SchemaVersionsRepository provide() {
    if (schemaVersionsRepositoryParameters.isCacheEnabled()) {
      CachedSchemaVersionsRepository cachedSchemaVersionsRepository =
          new CachedSchemaVersionsRepository(
              rawSchemaClient,
              getVersionsReloader(),
              schemaVersionsRepositoryParameters.getRefreshAfterWrite(),
              schemaVersionsRepositoryParameters.getExpireAfterWrite());

      notificationsBus.registerTopicCallback(
          new SchemaCacheRefresherCallback<>(
              cachedSchemaVersionsRepository,
              (CachedCompiledSchemaRepository<?>) compiledSchemaRepository));

      return cachedSchemaVersionsRepository;
    }
    return new DirectSchemaVersionsRepository(rawSchemaClient);
  }

  private ExecutorService getVersionsReloader() {
    return Executors.newFixedThreadPool(
        schemaVersionsRepositoryParameters.getReloadThreadPoolSize(),
        new ThreadFactoryBuilder().setNameFormat("schema-source-reloader-%d").build());
  }
}
