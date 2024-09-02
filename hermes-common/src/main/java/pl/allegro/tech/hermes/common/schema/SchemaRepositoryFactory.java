package pl.allegro.tech.hermes.common.schema;

import org.apache.avro.Schema;
import pl.allegro.tech.hermes.schema.CompiledSchemaRepository;
import pl.allegro.tech.hermes.schema.SchemaRepository;
import pl.allegro.tech.hermes.schema.SchemaVersionsRepository;

public class SchemaRepositoryFactory {

  private final SchemaVersionsRepository schemaVersionsRepository;

  private final CompiledSchemaRepository<Schema> compiledAvroSchemaRepository;

  public SchemaRepositoryFactory(
      SchemaVersionsRepository schemaVersionsRepository,
      CompiledSchemaRepository<Schema> compiledAvroSchemaRepository) {
    this.schemaVersionsRepository = schemaVersionsRepository;
    this.compiledAvroSchemaRepository = compiledAvroSchemaRepository;
  }

  public SchemaRepository provide() {
    return new SchemaRepository(schemaVersionsRepository, compiledAvroSchemaRepository);
  }
}
