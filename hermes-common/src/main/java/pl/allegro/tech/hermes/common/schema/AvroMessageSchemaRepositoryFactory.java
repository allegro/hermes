package pl.allegro.tech.hermes.common.schema;

import org.apache.avro.Schema;
import org.glassfish.hk2.api.Factory;

import javax.inject.Inject;
import java.util.concurrent.Executors;

public class AvroMessageSchemaRepositoryFactory implements Factory<MessageSchemaRepository<Schema>> {

    private final MessageSchemaSourceProvider messageSchemaSourceProvider;

    @Inject
    public AvroMessageSchemaRepositoryFactory(MessageSchemaSourceProvider messageSchemaSourceProvider) {
        this.messageSchemaSourceProvider = messageSchemaSourceProvider;
    }

    @Override
    public MessageSchemaRepository<Schema> provide() {
        return new MessageSchemaRepository<>(messageSchemaSourceProvider, Executors.newFixedThreadPool(2), source -> new Schema.Parser().parse(source));
    }

    @Override
    public void dispose(MessageSchemaRepository<Schema> instance) {

    }

}
