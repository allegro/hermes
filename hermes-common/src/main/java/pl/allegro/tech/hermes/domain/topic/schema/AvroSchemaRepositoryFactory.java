package pl.allegro.tech.hermes.domain.topic.schema;

import org.apache.avro.Schema;
import org.glassfish.hk2.api.Factory;

import javax.inject.Inject;
import java.util.concurrent.Executors;

public class AvroSchemaRepositoryFactory implements Factory<SchemaRepository<Schema>> {

    private final SchemaSourceProvider schemaSourceProvider;

    @Inject
    public AvroSchemaRepositoryFactory(SchemaSourceProvider schemaSourceProvider) {
        this.schemaSourceProvider = schemaSourceProvider;
    }

    @Override
    public SchemaRepository<Schema> provide() {
        return new SchemaRepository<>(schemaSourceProvider, Executors.newFixedThreadPool(2), source -> new Schema.Parser().parse(source));
    }

    @Override
    public void dispose(SchemaRepository<Schema> instance) {

    }

}
