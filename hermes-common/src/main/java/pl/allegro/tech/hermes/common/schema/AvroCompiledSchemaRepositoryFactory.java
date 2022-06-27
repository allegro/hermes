package pl.allegro.tech.hermes.common.schema;

import org.apache.avro.Schema;
import pl.allegro.tech.hermes.schema.CachedCompiledSchemaRepository;
import pl.allegro.tech.hermes.schema.CompiledSchemaRepository;
import pl.allegro.tech.hermes.schema.DirectCompiledSchemaRepository;
import pl.allegro.tech.hermes.schema.RawSchemaClient;
import pl.allegro.tech.hermes.schema.SchemaCompilersFactory;

public class AvroCompiledSchemaRepositoryFactory {

    private final RawSchemaClient rawSchemaClient;
    private final int maximumSize;
    private final int expireAfterAccessMinutes;
    private final boolean cacheEnabled;

    public AvroCompiledSchemaRepositoryFactory(RawSchemaClient rawSchemaClient,
                                               int maximumSize,
                                               int expireAfterAccessMinutes,
                                               boolean cacheEnabled) {
        this.rawSchemaClient = rawSchemaClient;
        this.maximumSize = maximumSize;
        this.expireAfterAccessMinutes = expireAfterAccessMinutes;
        this.cacheEnabled = cacheEnabled;
    }

    public CompiledSchemaRepository<Schema> provide() {
        CompiledSchemaRepository<Schema> repository = new DirectCompiledSchemaRepository<>(rawSchemaClient,
                SchemaCompilersFactory.avroSchemaCompiler());

        if (cacheEnabled) {
            return new CachedCompiledSchemaRepository<>(repository,
                    maximumSize,
                    expireAfterAccessMinutes);
        } else {
            return repository;
        }
    }
}
