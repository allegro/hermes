package pl.allegro.tech.hermes.consumers.consumer.converter;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.consumers.consumer.converter.schema.AvroSchemaRepositoryMetadataAware;

import javax.inject.Inject;

public class MessageConverterFactory {

    private final NoOperationMessageConverter noOperationMessageConverter = new NoOperationMessageConverter();
    private final AvroSchemaRepositoryMetadataAware schemaRepository;

    @Inject
    public MessageConverterFactory(AvroSchemaRepositoryMetadataAware schemaRepository) {
        this.schemaRepository = schemaRepository;
    }

    public MessageConverter create(Topic.ContentType contentType) {
        switch (contentType) {
            case AVRO:
                return new AvroToJsonMessageConverter(schemaRepository);
            default:
                return noOperationMessageConverter;
        }
    }
}
