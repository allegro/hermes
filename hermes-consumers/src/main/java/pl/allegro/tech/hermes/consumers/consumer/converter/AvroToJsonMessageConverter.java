package pl.allegro.tech.hermes.consumers.consumer.converter;

import pl.allegro.tech.hermes.common.message.converter.AvroToJsonConverter;
import pl.allegro.tech.hermes.consumers.consumer.Message;

import static pl.allegro.tech.hermes.consumers.consumer.Message.message;

public class AvroToJsonMessageConverter implements MessageConverter {
    private final AvroToJsonConverter avroToJsonConverter;

    public AvroToJsonMessageConverter(AvroToJsonConverter avroToJsonConverter) {
        this.avroToJsonConverter = avroToJsonConverter;
    }

    @Override
    public Message convert(Message message) {
        return message()
            .fromMessage(message)
            .withData(avroToJsonConverter.convert(message.getData()))
            .build();
    }

}
