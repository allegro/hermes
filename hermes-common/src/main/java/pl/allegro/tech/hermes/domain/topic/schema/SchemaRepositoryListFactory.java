package pl.allegro.tech.hermes.domain.topic.schema;

import com.github.fge.jsonschema.main.JsonSchema;
import com.google.common.collect.ImmutableList;
import org.apache.avro.Schema;
import org.glassfish.hk2.api.Factory;

import javax.inject.Inject;
import java.util.List;

public class SchemaRepositoryListFactory implements Factory<List<SchemaRepository>> {
    private SchemaRepository<Schema> avroSchemaRepo;
    private SchemaRepository<JsonSchema> jsonSchemaRepo;

    @Inject
    public SchemaRepositoryListFactory(SchemaRepository<Schema> avroSchemaRepo, SchemaRepository<JsonSchema> jsonSchemaRepo) {
        this.avroSchemaRepo = avroSchemaRepo;
        this.jsonSchemaRepo = jsonSchemaRepo;
    }

    @Override
    public List<SchemaRepository> provide() {
        return ImmutableList.of(avroSchemaRepo, jsonSchemaRepo);
    }

    @Override
    public void dispose(List<SchemaRepository> instance) {
    }
}
