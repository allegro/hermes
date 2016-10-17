package pl.allegro.tech.hermes.common.schema;

import org.apache.avro.Schema;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.schema.CompiledSchemaRepository;
import pl.allegro.tech.hermes.schema.SchemaRepository;
import pl.allegro.tech.hermes.schema.SchemaVersionsRepository;

import javax.inject.Inject;

public class SchemaRepositoryFactory implements Factory<SchemaRepository> {

    private final SchemaVersionsRepository schemaVersionsRepository;

    private final CompiledSchemaRepository<Schema> compiledAvroSchemaRepository;

    @Inject
    public SchemaRepositoryFactory(SchemaVersionsRepository schemaVersionsRepository,
                                   CompiledSchemaRepository<Schema> compiledAvroSchemaRepository) {
        this.schemaVersionsRepository = schemaVersionsRepository;
        this.compiledAvroSchemaRepository = compiledAvroSchemaRepository;
    }

    @Override
    public SchemaRepository provide() {
        return new SchemaRepository(schemaVersionsRepository, compiledAvroSchemaRepository);
    }

    @Override
    public void dispose(SchemaRepository instance) {

    }
}
