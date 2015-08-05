package pl.allegro.tech.hermes.consumers.consumer.converter;

import org.apache.avro.Schema;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaRepository;

public class MessageConverterFactory {

    private final NoOperationMessageConverter noOperationMessageConverter = new NoOperationMessageConverter();

    public MessageConverter create(Topic.ContentType contentType, SchemaRepository<Schema> schemaRepository) {
        switch (contentType) {
            case AVRO:
                return new AvroToJsonMessageConverter(schemaRepository);
            default:
                return noOperationMessageConverter;
        }
    }
}
