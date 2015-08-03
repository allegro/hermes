package pl.allegro.tech.hermes.frontend.publishing.avro;

import org.apache.avro.Schema;
import pl.allegro.tech.hermes.common.message.converter.JsonToAvroConverter;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;

public class JsonToAvroMessageConverter {
    private final JsonToAvroConverter converter = new JsonToAvroConverter();

    public Message convert(Message message, Schema messageSchema) {
        return new Message(message.getId(), converter.convert(message.getData(), messageSchema), message.getTimestamp());
    }
}
