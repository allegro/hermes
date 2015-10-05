package pl.allegro.tech.hermes.consumers.consumer.converter;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.consumers.consumer.Message;

import javax.inject.Inject;

public class MessageConverterResolver {

    private final AvroToJsonMessageConverter avroToJsonMessageConverter;
    private final NoOperationMessageConverter noOperationMessageConverter;

    @Inject
    public MessageConverterResolver(AvroToJsonMessageConverter avroToJsonMessageConverter,
                                    NoOperationMessageConverter noOperationMessageConverter) {
        this.avroToJsonMessageConverter = avroToJsonMessageConverter;
        this.noOperationMessageConverter = noOperationMessageConverter;
    }

    public MessageConverter converterFor(Message message, Topic topic) {
        if (message.getContentType() == Topic.ContentType.AVRO) {
            return avroToJsonMessageConverter;
        }

        return noOperationMessageConverter;
    }
}
