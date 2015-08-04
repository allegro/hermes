package pl.allegro.tech.hermes.consumers.consumer.converter;

import org.apache.avro.Schema;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.message.converter.AvroToJsonConverter;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaRepository;
import pl.allegro.tech.hermes.consumers.consumer.Message;

import static pl.allegro.tech.hermes.consumers.consumer.Message.message;

public class AvroToJsonMessageConverter implements MessageConverter {

    private final SchemaRepository<Schema> schemaRepository;
    private final AvroToJsonConverter avroToJsonConverter;

    public AvroToJsonMessageConverter(SchemaRepository<Schema> schemaRepository, AvroToJsonConverter avroToJsonConverter) {
        this.schemaRepository = schemaRepository;
        this.avroToJsonConverter = avroToJsonConverter;
    }

    @Override
    public Message convert(Message message, Topic topic) {
        return message()
                .fromMessage(message)
                .withData(avroToJsonConverter.convert(message.getData(), schemaRepository.getSchema(topic)))
                .build();
    }

}
