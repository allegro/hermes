package pl.allegro.tech.hermes.consumers.consumer.converter;

import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.Subscription;
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

    public MessageConverter converterFor(Message message, Subscription subscription) {
        if (message.getContentType() == ContentType.AVRO && subscription.getContentType() == ContentType.JSON) {
            return avroToJsonMessageConverter;
        }

        return noOperationMessageConverter;
    }
}
