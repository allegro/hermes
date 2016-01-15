package pl.allegro.tech.hermes.frontend.publishing.avro;

import org.apache.avro.Schema;
import pl.allegro.tech.common.avro.JsonAvroConverter;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaRepository;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;

public class JsonToAvroMessageConverter {

    private final SchemaRepository<Schema> schemaRepository;
    private final JsonAvroConverter converter;

    public JsonToAvroMessageConverter(SchemaRepository<Schema> schemaRepository, JsonAvroConverter converter) {
        this.schemaRepository = schemaRepository;
        this.converter = converter;
    }

    public Message convert(Message message, Topic topic) {
        Schema schema = schemaRepository.getSchema(topic);
        return message.withDataReplaced(converter.convertToAvro(message.getData(), schema));
    }

}
