package pl.allegro.tech.hermes.common.schema;

import java.time.Duration;
import org.apache.avro.Schema;
import pl.allegro.tech.hermes.schema.CachedCompiledSchemaRepository;
import pl.allegro.tech.hermes.schema.CompiledSchemaRepository;
import pl.allegro.tech.hermes.schema.DirectCompiledSchemaRepository;
import pl.allegro.tech.hermes.schema.RawSchemaClient;
import pl.allegro.tech.hermes.schema.SchemaCompilersFactory;

public class AvroCompiledSchemaRepositoryFactory {

  private final RawSchemaClient rawSchemaClient;
  private final int maximumSize;
  private final Duration expireAfterAccess;
  private final boolean cacheEnabled;

  public AvroCompiledSchemaRepositoryFactory(
      RawSchemaClient rawSchemaClient,
      int maximumSize,
      Duration expireAfterAccess,
      boolean cacheEnabled) {
    this.rawSchemaClient = rawSchemaClient;
    this.maximumSize = maximumSize;
    this.expireAfterAccess = expireAfterAccess;
    this.cacheEnabled = cacheEnabled;
  }

  public CompiledSchemaRepository<Schema> provide() {
    CompiledSchemaRepository<Schema> repository =
        new DirectCompiledSchemaRepository<>(
            rawSchemaClient, SchemaCompilersFactory.avroSchemaCompiler());

    if (cacheEnabled) {
      return new CachedCompiledSchemaRepository<>(repository, maximumSize, expireAfterAccess);
    } else {
      return repository;
    }
  }
}
