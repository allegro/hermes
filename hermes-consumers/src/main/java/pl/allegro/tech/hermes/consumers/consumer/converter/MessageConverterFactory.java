package pl.allegro.tech.hermes.consumers.consumer.converter;

import org.apache.avro.Schema;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.message.converter.AvroToJsonConverter;
import pl.allegro.tech.hermes.common.schema.MessageSchemaRepository;

public class MessageConverterFactory {

    private final NoOperationMessageConverter noOperationMessageConverter = new NoOperationMessageConverter();

    public MessageConverter create(Topic.ContentType contentType, MessageSchemaRepository<Schema> schemaRepository) {
        switch (contentType) {
            case AVRO:
                return new AvroToJsonMessageConverter(schemaRepository, new AvroToJsonConverter());
            default:
                return noOperationMessageConverter;
        }
    }
}
