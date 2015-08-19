package pl.allegro.tech.hermes.consumers.consumer.converter;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.consumers.consumer.converter.schema.AvroSchemaRepositoryMetadataAware;

public class MessageConverterFactory {

    private final NoOperationMessageConverter noOperationMessageConverter = new NoOperationMessageConverter();

    public MessageConverter create(Topic.ContentType contentType, AvroSchemaRepositoryMetadataAware schemaRepository) {
        switch (contentType) {
            case AVRO:
                return new AvroToJsonMessageConverter(schemaRepository);
            default:
                return noOperationMessageConverter;
        }
    }
}
