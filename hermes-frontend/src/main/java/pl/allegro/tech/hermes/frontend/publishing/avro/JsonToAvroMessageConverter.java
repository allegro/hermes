package pl.allegro.tech.hermes.frontend.publishing.avro;

import org.apache.avro.Schema;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.message.converter.JsonToAvroConverter;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaRepository;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;

public class JsonToAvroMessageConverter {

    private final SchemaRepository<Schema> schemaRepository;

    public JsonToAvroMessageConverter(SchemaRepository<Schema> schemaRepository) {
        this.schemaRepository = schemaRepository;
    }

    public Message convert(Message message, Topic topic) {
        Schema schema = schemaRepository.getSchema(topic);
        return new Message(message.getId(), JsonToAvroConverter.convert(message.getData(), schema), message.getTimestamp());
    }

}
