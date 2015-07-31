package pl.allegro.tech.hermes.frontend.schema;

import org.apache.avro.Schema;
import org.glassfish.hk2.api.Factory;

import javax.inject.Inject;
import java.util.concurrent.Executors;

public class AvroMessageSchemaRepositoryFactory implements Factory<MessageSchemaRepository<Schema>> {

    private final MessageSchemaSourceRepository messageSchemaSourceRepository;

    @Inject
    public AvroMessageSchemaRepositoryFactory(MessageSchemaSourceRepository messageSchemaSourceRepository) {
        this.messageSchemaSourceRepository = messageSchemaSourceRepository;
    }

    @Override
    public MessageSchemaRepository<Schema> provide() {
        return new MessageSchemaRepository<>(messageSchemaSourceRepository, Executors.newFixedThreadPool(2), source -> new Schema.Parser().parse(source));
    }

    @Override
    public void dispose(MessageSchemaRepository<Schema> instance) {

    }

}
