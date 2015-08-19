package pl.allegro.tech.hermes.consumers.consumer.converter.schema;

import org.apache.avro.Schema;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaRepository;

import javax.inject.Inject;

import static pl.allegro.tech.hermes.common.config.Configs.SCHEMA_CACHE_EXPIRE_AFTER_WRITE_MINUTES;
import static pl.allegro.tech.hermes.common.config.Configs.SCHEMA_CACHE_REFRESH_AFTER_WRITE_MINUTES;

public class AvroSchemaRepositoryMetadataAwareFactory implements Factory<AvroSchemaRepositoryMetadataAware> {

    private final ConfigFactory configFactory;
    private final SchemaRepository<Schema> schemaRepository;

    @Inject
    public AvroSchemaRepositoryMetadataAwareFactory(ConfigFactory configFactory, SchemaRepository<Schema> schemaRepository) {
        this.configFactory = configFactory;
        this.schemaRepository = schemaRepository;
    }

    @Override
    public AvroSchemaRepositoryMetadataAware provide() {
        return new AvroSchemaRepositoryMetadataAware(
                schemaRepository,
                //+1 because we want to reload schema repo metadata aware cache after schema repo cache
                configFactory.getIntProperty(SCHEMA_CACHE_REFRESH_AFTER_WRITE_MINUTES) + 1,
                configFactory.getIntProperty(SCHEMA_CACHE_EXPIRE_AFTER_WRITE_MINUTES) + 1);
    }

    @Override
    public void dispose(AvroSchemaRepositoryMetadataAware instance) {

    }
}
