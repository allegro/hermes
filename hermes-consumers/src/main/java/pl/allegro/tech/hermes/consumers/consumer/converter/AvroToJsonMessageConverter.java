package pl.allegro.tech.hermes.consumers.consumer.converter;

import org.apache.avro.Schema;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.message.converter.AvroToJsonConverter;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaRepository;
import pl.allegro.tech.hermes.consumers.consumer.Message;

import static pl.allegro.tech.hermes.consumers.consumer.Message.message;

public class AvroToJsonMessageConverter implements MessageConverter {

    private final SchemaRepository<Schema> schemaRepository;

    public AvroToJsonMessageConverter(SchemaRepository<Schema> schemaRepository) {
        this.schemaRepository = schemaRepository;
    }

    @Override
    public Message convert(Message message, Topic topic) {
        return message()
                .fromMessage(message)
                .withData(AvroToJsonConverter.convert(message.getData(), schemaRepository.getSchema(topic)))
                .build();
    }

}
