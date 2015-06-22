package pl.allegro.tech.hermes.consumers.consumer.converter;

import org.apache.avro.Schema;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.message.converter.AvroToJsonConverter;

public class MessageConverterFactory {

    private final NoOperationMessageConverter noOperationMessageConverter = new NoOperationMessageConverter();

    public MessageConverter create(Topic.ContentType contentType, String messageSchema) {
        switch(contentType) {
            case AVRO:
                return new AvroToJsonMessageConverter(new AvroToJsonConverter(new Schema.Parser().parse(messageSchema)));
            default:
                return noOperationMessageConverter;
        }
    }
}
