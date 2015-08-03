package pl.allegro.tech.hermes.consumers.consumer.converter;

import org.apache.avro.Schema;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.message.converter.AvroToJsonConverter;
import pl.allegro.tech.hermes.common.schema.MessageSchemaRepository;
import pl.allegro.tech.hermes.consumers.consumer.Message;

import static pl.allegro.tech.hermes.consumers.consumer.Message.message;

public class AvroToJsonMessageConverter implements MessageConverter {

    private final MessageSchemaRepository<Schema> messageSchemaRepository;
    private final AvroToJsonConverter avroToJsonConverter;

    public AvroToJsonMessageConverter(MessageSchemaRepository<Schema> messageSchemaRepository, AvroToJsonConverter avroToJsonConverter) {
        this.messageSchemaRepository = messageSchemaRepository;
        this.avroToJsonConverter = avroToJsonConverter;
    }

    @Override
    public Message convert(Message message, Topic topic) {
        return message()
                .fromMessage(message)
                .withData(avroToJsonConverter.convert(message.getData(), messageSchemaRepository.getSchema(topic)))
                .build();
    }

}
