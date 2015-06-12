package pl.allegro.tech.hermes.consumers.consumer.converter;

import org.apache.avro.Schema;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.message.converter.AvroToJsonConverter;

import static pl.allegro.tech.hermes.api.Topic.ContentType.AVRO;

public class MessageConverterFactory {

    private final DefaultMessageConverter defaultMessageConverter = new DefaultMessageConverter();

    public MessageConverter create(Topic.ContentType contentType, String messageSchema) {
        if (AVRO == contentType) {
            return new AvroToJsonMessageConverter(new AvroToJsonConverter(new Schema.Parser().parse(messageSchema)));
        } else {
            return defaultMessageConverter;
        }
    }
}
